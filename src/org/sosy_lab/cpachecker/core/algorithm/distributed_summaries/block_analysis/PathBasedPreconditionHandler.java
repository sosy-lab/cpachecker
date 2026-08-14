// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import org.jspecify.annotations.NonNull;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.DssSingleWorkerStatistics;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis.DssBlockAnalyses.DssBlockAnalysisResult;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssPostConditionMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.BlockGraphPath;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.BlockGraphPath.PathCase;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis.StateAndPrecision;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DssMessageProcessing;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.java_smt.api.SolverException;

/**
 * Groups preconditions by the path through the block graph along which they were produced.
 *
 * <p>An incoming postcondition only replaces the preconditions whose path relates to the incoming
 * one (see {@link PathCase}), which lets the block tell a genuinely new context apart from a
 * repetition of one it has already explored. Only the paths affected by the last update are
 * re-explored, and a precondition that does not constrain the block entry is explored with the
 * initial precision instead of the accumulated one.
 */
final class PathBasedPreconditionHandler implements DssPreconditionHandler {

  private final Multimap<BlockGraphPath, @NonNull StateAndPrecision> preconditions =
      ArrayListMultimap.create();

  private final List<BlockGraphPath> pathsToAnalyze = new ArrayList<>();

  private final DssBlockAnalysis analysis;
  private final boolean resetPrecisionsForEveryRun;

  private Precision unifiedPrecision;

  PathBasedPreconditionHandler(DssBlockAnalysis pAnalysis) throws InterruptedException {
    analysis = pAnalysis;
    resetPrecisionsForEveryRun = pAnalysis.getOptions().doResetPrecisionsForEveryRun();
    unifiedPrecision = pAnalysis.makeStartPrecision();

    for (String predecessorId : pAnalysis.getBlock().getPredecessorIds()) {
      preconditions.put(
          BlockGraphPath.of(predecessorId),
          new StateAndPrecision(pAnalysis.makeStartState(), pAnalysis.makeStartPrecision()));
    }
  }

  @Override
  public Collection<DssMessage> runInitialAnalysis()
      throws CPAException, InterruptedException, SolverException {
    DssBlockAnalysisResult result =
        analysis.runInitialBlockAnalysis(analysis.makeStartState(), analysis.makeStartPrecision());

    ImmutableList.Builder<DssMessage> initialMessages = ImmutableList.builder();
    if (!result.getFinalLocationStates().isEmpty()) {
      initialMessages.addAll(analysis.reportPostconditions(analysis.summariesOf(result)));
    }
    if (!result.getAllViolations().isEmpty()) {
      initialMessages.addAll(analysis.reportFirstViolationConditions(result.getAllViolations()));
    }
    return initialMessages.build();
  }

  @Override
  public DssMessageProcessing store(DssPostConditionMessage pReceived)
      throws InterruptedException, SolverException, CPAException {
    pathsToAnalyze.clear();
    analysis.getLogger().log(Level.INFO, "Running forward analysis with new precondition");
    analysis.resetStates(preconditions);
    ImmutableList<@NonNull StateAndPrecision> received = analysis.deserialize(pReceived);
    DssSingleWorkerStatistics stats = analysis.statistics();
    stats.getStorePreconditionStatesTimer().start();
    try {
      DssMessageProcessing processing = analysis.shouldProceedForward(received);
      if (!processing.shouldProceed()) {
        return processing;
      }

      unifiedPrecision = analysis.combinePrecisions(unifiedPrecision, received);

      // group incoming states by block graph path
      ImmutableListMultimap.Builder<BlockGraphPath, StateAndPrecision> newPreconditionsBuilder =
          ImmutableListMultimap.builder();
      for (StateAndPrecision stateAndPrecision : received) {
        newPreconditionsBuilder.put(stateAndPrecision.getBlockGraphPath(), stateAndPrecision);
      }
      ImmutableListMultimap<BlockGraphPath, StateAndPrecision> newPreconditions =
          newPreconditionsBuilder.build();

      ImmutableList.Builder<BlockGraphPath> removeBuilder = ImmutableList.builder();
      ImmutableList.Builder<BlockGraphPath> addBuilder = ImmutableList.builder();
      boolean fixPointReached = true;
      for (BlockGraphPath newPath : newPreconditions.keySet()) {
        ImmutableListMultimap.Builder<PathCase, BlockGraphPath> caseBuilder =
            ImmutableListMultimap.builder();
        for (BlockGraphPath oldPath : preconditions.keySet()) {
          caseBuilder.put(newPath.getFirstMatchingCase(oldPath), oldPath);
        }
        ImmutableListMultimap<PathCase, BlockGraphPath> cases = caseBuilder.build();
        if (cases.containsKey(PathCase.SUFFIX_OR_EQUAL)) {
          // ABC (existing)
          //  BCCC (incoming)
          boolean allowedToStop = false;
          for (BlockGraphPath oldPathForCase : cases.get(PathCase.SUFFIX_OR_EQUAL)) {
            if (!allowedToStop
                && analysis.allCovered(
                    newPreconditions.get(newPath), preconditions.get(oldPathForCase))) {
              allowedToStop = true;
            }
            removeBuilder.add(oldPathForCase);
          }
          addBuilder.add(newPath);
          fixPointReached &= allowedToStop;
        } else if (cases.containsKey(PathCase.OVERLAP)) {
          removeBuilder.addAll(cases.get(PathCase.OVERLAP));
          addBuilder.add(newPath);
          fixPointReached = false;
        } else if (cases.containsKey(PathCase.REAL_PREFIX)) {
          boolean allowedToStop = false;
          for (BlockGraphPath oldPathForCase : cases.get(PathCase.REAL_PREFIX)) {
            if (analysis.allCovered(
                newPreconditions.get(newPath), preconditions.get(oldPathForCase))) {
              allowedToStop = true;
              break;
            }
          }
          if (!allowedToStop) {
            addBuilder.add(newPath);
          }
        } else {
          checkState(cases.containsKey(PathCase.OTHER));
          addBuilder.add(newPath);
          boolean covered = false;
          for (BlockGraphPath oldPathForOther : cases.get(PathCase.OTHER)) {
            if (analysis.allCovered(
                newPreconditions.get(newPath), preconditions.get(oldPathForOther))) {
              covered = true;
              break;
            }
          }
          fixPointReached &= covered;
        }
      }

      for (StateAndPrecision stateAndPrecision : received) {
        stateAndPrecision.getBlockState().addHistory(analysis.getBlock());
      }

      removeBuilder.build().forEach(preconditions::removeAll);
      addBuilder.build().forEach(path -> preconditions.putAll(path, newPreconditions.get(path)));

      if (fixPointReached) {
        return DssMessageProcessing.stop();
      }

      scheduleAllPaths();
      return processing;
    } finally {
      stats.getStorePreconditionStatesTimer().stop();
      stats.getStorePreconditionStatesCounter().add(received.size());
    }
  }

  @Override
  public Collection<DssMessage> analyze()
      throws SolverException, InterruptedException, CPAException {
    if (!analysis.containsViolationInsideBlock()
        && analysis.getViolationConditionHandler().isEmpty()) {
      return ImmutableSet.of();
    }
    ImmutableSet.Builder<DssMessage> messages = ImmutableSet.builder();
    AnalysisResult round = explore(Optional.empty());
    if (!round.violationConditions().isEmpty()) {
      messages.addAll(analysis.reportViolationConditions(round.violationConditions()));
    }
    if (!round.summaries().isEmpty()) {
      messages.addAll(analysis.reportPostconditions(round.summaries()));
    }
    return messages.build();
  }

  @Override
  public Collection<DssMessage> analyzeFor(String pViolationConditionSender)
      throws SolverException, InterruptedException, CPAException {
    checkArgument(
        !analysis.getViolationConditionHandler().isEmptyFor(pViolationConditionSender),
        "No violation condition found for sender ID: %s",
        pViolationConditionSender);
    ImmutableList.Builder<DssMessage> messages = ImmutableList.builder();
    AnalysisResult round = explore(Optional.of(pViolationConditionSender));
    if (!round.summaries().isEmpty()) {
      messages.addAll(analysis.reportPostconditions(round.summaries()));
    }
    if (!round.violationConditions().isEmpty()) {
      messages.addAll(analysis.reportViolationConditions(round.violationConditions()));
    }
    return messages.build();
  }

  @Override
  public ImmutableList<@NonNull StateAndPrecision> getKnownPreconditions() {
    return ImmutableList.copyOf(preconditions.values());
  }

  @Override
  public void violationConditionsChanged() {
    scheduleAllPaths();
  }

  private void scheduleAllPaths() {
    pathsToAnalyze.clear();
    pathsToAnalyze.addAll(preconditions.keySet());
  }

  /**
   * Runs the CPA under an error condition, i.e., if the current block contains a block-end edge,
   * the error condition will be attached to that edge. In case this makes the path formula
   * infeasible, we compute an abstraction. If no error condition is present, we run the CPA.
   *
   * @param pSender restricts the exploration to the violation conditions of one block, if given
   */
  private AnalysisResult explore(Optional<String> pSender)
      throws CPAException, InterruptedException {
    if (analysis.getViolationConditionHandler().isEmpty()) {
      return new AnalysisResult(ImmutableList.of(), ImmutableSet.of());
    }

    ImmutableList.Builder<StateAndPrecision> summaries = ImmutableList.builder();
    ImmutableSet.Builder<ArgPathAndCondition> violations = ImmutableSet.builder();

    for (BlockGraphPath path : pathsToAnalyze) {
      for (StateAndPrecision precondition : ImmutableList.copyOf(preconditions.get(path))) {
        boolean isTrivial = analysis.getDcpa().isMostGeneralBlockEntryState(precondition.state());
        Precision precision =
            resetPrecisionsForEveryRun || isTrivial
                ? analysis.makeStartPrecision()
                : unifiedPrecision;
        analysis.resetStates(preconditions);

        DssBlockAnalysisResult result =
            analysis.runBlockAnalysis(
                precondition.state(),
                precision,
                analysis.getViolationConditionHandler().statesOf(pSender));

        if (!preconditions.isEmpty() || analysis.getBlock().isRoot()) {
          summaries.addAll(analysis.summariesOf(result));
        }
        if (!result.getAllViolations().isEmpty()) {
          violations.addAll(analysis.pathsWithCondition(result.getViolationConditionViolations()));
          violations.addAll(analysis.pathsFromOrigin(result.getTargetStates()));
        }
      }
    }
    return new AnalysisResult(summaries.build(), violations.build());
  }
}
