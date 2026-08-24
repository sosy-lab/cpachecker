// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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

  private final Map<BlockGraphPath, @NonNull StatesByPath> preconditions = new LinkedHashMap<>();

  private final Multimap<BlockGraphPath, StatesByPath> coveredStates = ArrayListMultimap.create();

  private final List<BlockGraphPath> pathsToAnalyze = new ArrayList<>();

  private final DssBlockAnalysis analysis;
  private final boolean resetPrecisionsForEveryRun;

  PathBasedPreconditionHandler(DssBlockAnalysis pAnalysis) throws InterruptedException {
    analysis = pAnalysis;
    resetPrecisionsForEveryRun = pAnalysis.getOptions().doResetPrecisionsForEveryRun();

    preconditions.put(
        BlockGraphPath.of(),
        new StatesByPath(
            BlockGraphPath.of(),
            ImmutableList.of(
                new StateAndPrecision(
                    pAnalysis.makeStartState(true), pAnalysis.makeStartPrecision()))));
  }

  @Override
  public Collection<DssMessage> runInitialAnalysis()
      throws CPAException, InterruptedException, SolverException {
    DssBlockAnalysisResult result =
        analysis.runInitialBlockAnalysis(
            analysis.makeStartState(true), analysis.makeStartPrecision());

    ImmutableList.Builder<DssMessage> initialMessages = ImmutableList.builder();
    if (!result.getFinalLocationStates().isEmpty()) {
      initialMessages.addAll(analysis.reportPostconditions(analysis.finalLocationStatesOf(result)));
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
    ImmutableList<@NonNull StateAndPrecision> received = analysis.deserialize(pReceived);
    DssSingleWorkerStatistics stats = analysis.statistics();
    stats.getStorePreconditionStatesTimer().start();
    try {
      DssMessageProcessing processing = analysis.shouldProceedForward(received);
      if (!processing.shouldProceed()) {
        return processing;
      }

      Map<BlockGraphPath, StatesByPath> groupedStates = groupStatesByPath(received);

      if (preconditions.isEmpty()) {
        preconditions.putAll(groupedStates);
        pathsToAnalyze.addAll(groupedStates.keySet());
        return DssMessageProcessing.proceed();
      }

      storeStates(groupedStates);

      if (pathsToAnalyze.isEmpty()) {
        return DssMessageProcessing.stop();
      }

      return processing;
    } finally {
      stats.getStorePreconditionStatesTimer().stop();
      stats.getStorePreconditionStatesCounter().add(received.size());
    }
  }

  private void storeStates(Map<@NonNull BlockGraphPath, StatesByPath> groupedStates)
      throws CPAException, InterruptedException {

    groupedStates = Maps.filterKeys(groupedStates, this::shouldConsiderPath);

    ImmutableSet.Builder<StatesByPath> noLongerCovered = ImmutableSet.builder();

    newStateLoop:
    for (StatesByPath newStates : groupedStates.values()) {

      // Check for unchanged state
      for (StatesByPath existing : preconditions.values()) {
        if (statesByPathEqual(newStates, existing)) {
          // TODO should work similarly with suffix, where we just replace the path stored with
          // the new path, but as long as the race condition exists, we deny shorter paths and
          // would not unroll loops properly
          continue newStateLoop;
        }
      }

      // remove states that now originate from paths that have been updated / superseded with states
      // that have a path with more knowledge at the beginning
      ImmutableList.Builder<BlockGraphPath> toRemove = ImmutableList.builder();
      for (StatesByPath existing : preconditions.values()) {
        if (newStates.path.overlapsWith(existing.path)) {
          toRemove.add(existing.path);
        }
      }

      for (BlockGraphPath pathToRemove : toRemove.build()) {
        preconditions.remove(pathToRemove);
        pathsToAnalyze.remove(pathToRemove);
        noLongerCovered.addAll(coveredStates.removeAll(pathToRemove));
      }

      // remove from the right side of covered as well, if we would remove it according to above
      // condition
      ImmutableList.Builder<Entry<BlockGraphPath, StatesByPath>> toRemoveFromCovered =
          ImmutableList.builder();

      for (Entry<BlockGraphPath, StatesByPath> entry : coveredStates.entries()) {
        if (newStates.path.overlapsWith(entry.getValue().path)) {
          toRemoveFromCovered.add(entry);
        }
      }

      for (Entry<BlockGraphPath, StatesByPath> entry : toRemoveFromCovered.build()) {
        coveredStates.remove(entry.getKey(), entry.getValue());
      }

      // Check whether existing states already covered these states
      boolean coveredByExisting = false;

      for (StatesByPath existing : preconditions.values()) {

        if (analysis.allCovered(newStates.states, existing.states)) {
          coveredStates.put(existing.path, newStates);
          coveredByExisting = true;
          break;
        }
      }

      // Analyze the new one, if it was not covered
      if (!coveredByExisting) {
        preconditions.put(newStates.path, newStates);
        pathsToAnalyze.add(newStates.path);
      }
    }

    // All the states that were covered by a path that has been removed should be considered again
    ImmutableMap.Builder<BlockGraphPath, StatesByPath> toAddBuilder = ImmutableMap.builder();
    for (StatesByPath statesByPath : noLongerCovered.build()) {
      toAddBuilder.put(statesByPath.path, statesByPath);
    }

    ImmutableMap<BlockGraphPath, StatesByPath> toAdd = toAddBuilder.buildKeepingLast();
    if (!toAdd.isEmpty()) {
      storeStates(toAdd);
    }
  }

  private boolean statesByPathEqual(StatesByPath states1, StatesByPath states2)
      throws CPAException, InterruptedException {
    return analysis.allCovered(states1.states, states2.states)
        && analysis.allCovered(states2.states, states1.states);
  }

  private boolean shouldConsiderPath(BlockGraphPath pBlockGraphPath) {

    // Do not unroll a loop without prefix
    if (pBlockGraphPath.path().getFirst().equals(analysis.getBlock().getId())) {
      return false;
    }

    // if this message had been here, it would have been removed because of the overlap check
    // AB
    //  BB
    // L0
    // L0,L5
    for (BlockGraphPath existingPath : preconditions.keySet()) {
      if (!existingPath.isPrefixOf(pBlockGraphPath) && existingPath.overlapsWith(pBlockGraphPath)) {
        return false;
      }
    }

    // TODO fix race condition with old messages that are on the way

    return true;
  }

  private record StatesByPath(BlockGraphPath path, Collection<StateAndPrecision> states) {

    @Override
    public String toString() {
      return "StatesByPath[path=" + path + ", states=" + states.size() + "]";
    }
  }

  private Map<BlockGraphPath, StatesByPath> groupStatesByPath(
      Collection<StateAndPrecision> states) {
    ImmutableMap.Builder<BlockGraphPath, StatesByPath> builder = ImmutableMap.builder();
    for (Entry<BlockGraphPath, Collection<StateAndPrecision>> entry :
        Multimaps.index(states, state -> state.getBlockGraphPath()).asMap().entrySet()) {
      builder.put(entry.getKey(), new StatesByPath(entry.getKey(), entry.getValue()));
    }
    return builder.build();
  }

  @Override
  public Collection<DssMessage> analyze()
      throws SolverException, InterruptedException, CPAException {
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
    return FluentIterable.from(preconditions.values())
        .transformAndConcat(StatesByPath::states)
        .toList();
  }

  @Override
  public void violationConditionsChanged() {
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
    ImmutableList.Builder<StateAndPrecision> summaries = ImmutableList.builder();
    ImmutableSet.Builder<ArgPathAndCondition> violations = ImmutableSet.builder();

    for (BlockGraphPath path : pathsToAnalyze) {
      Precision currentPrecision = analysis.combinePrecisions(preconditions.get(path).states);
      for (StateAndPrecision precondition : ImmutableList.copyOf(preconditions.get(path).states)) {
        boolean isTrivial = analysis.getDcpa().isMostGeneralBlockEntryState(precondition.state());
        Precision precision =
            resetPrecisionsForEveryRun || isTrivial
                ? analysis.makeStartPrecision()
                : currentPrecision;

        DssBlockAnalysisResult result =
            analysis.runBlockAnalysis(
                analysis.getDcpa().reset(precondition.state()),
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
    return new AnalysisResult(
        analysis.deduplicateStatesAndPrecisions(summaries.build()), violations.build());
  }
}
