// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis;

import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;
import static org.sosy_lab.common.collect.Collections3.transformedImmutableSetCopy;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import org.jspecify.annotations.NonNull;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis.DssBlockAnalyses.DssBlockAnalysisResult;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessageFactory;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssPostConditionMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssViolationConditionMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.BlockGraphPath;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.BlockGraphPath.PathCase;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis.StateAndPrecision;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DssMessageProcessing;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DssAnalysisOptions;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.block.BlockState;
import org.sosy_lab.cpachecker.cpa.path.SegmentedPaths;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.java_smt.api.SolverException;

public class PathBasedReplacementDssBlockAnalysis
    extends DssBlockAnalysis<BlockGraphPath, StringId> {

  private final List<BlockGraphPath> pathsToAnalyze = new ArrayList<>();

  private Precision unifiedPrecision;

  private final boolean resetPrecisionsForEveryRun;

  public PathBasedReplacementDssBlockAnalysis(
      LogManager pLogger,
      BlockNode pBlock,
      CFA pCFA,
      Specification pSpecification,
      Configuration pConfiguration,
      DssAnalysisOptions pOptions,
      DssMessageFactory pMessageFactory,
      ShutdownManager pShutdownManager)
      throws CPAException, InterruptedException, InvalidConfigurationException {
    super(
        pLogger,
        pBlock,
        pCFA,
        pSpecification,
        pConfiguration,
        pOptions,
        pMessageFactory,
        pShutdownManager);

    resetPrecisionsForEveryRun = pOptions.resetPrecisionsForEveryRun();
    unifiedPrecision = makeStartPrecision();

    for (String predecessorId : block.getPredecessorIds()) {
      preconditions.put(
          BlockGraphPath.of(predecessorId),
          new StateAndPrecision(makeStartState(), makeStartPrecision()));
    }
  }

  /**
   * Executes the configured CPA algorithm on the block with the initial state and precision.
   *
   * @return Important messages for other blocks.
   * @throws CPAException thrown if CPA runs into an error
   * @throws InterruptedException thrown if thread is interrupted unexpectedly
   * @throws SolverException thrown if solver runs into an error
   */
  @Override
  public Collection<DssMessage> runInitialAnalysis()
      throws CPAException, InterruptedException, SolverException {
    reachedSet.clear();
    reachedSet.add(makeStartState(), makeStartPrecision());

    DssBlockAnalysisResult result = DssBlockAnalyses.runAlgorithm(algorithm, reachedSet, block);

    status = status.update(result.getStatus());

    final ImmutableList.Builder<DssMessage> initialMessages = ImmutableList.builder();

    if (!result.getFinalLocationStates().isEmpty()) {
      initialMessages.addAll(
          reportPostconditions(
              transformedImmutableListCopy(
                  result.getFinalLocationStates(),
                  state -> new StateAndPrecision(state, reachedSet.getPrecision(state)))));
    }

    if (!result.getAllViolations().isEmpty()) {
      initialMessages.addAll(reportFirstViolationConditions(result.getAllViolations()));
    }

    return initialMessages.build();
  }

  @Override
  public DssMessageProcessing storePrecondition(DssPostConditionMessage pReceived)
      throws InterruptedException, SolverException, CPAException {
    pathsToAnalyze.clear();
    logger.log(Level.INFO, "Running forward analysis with new precondition");
    resetStates();
    ImmutableList<@NonNull StateAndPrecision> deserializedStatesAndPrecisions =
        deserialize(pReceived);
    DssMessageProcessing processing = DssMessageProcessing.proceed();
    for (StateAndPrecision stateAndPrecision : deserializedStatesAndPrecisions) {
      processing =
          processing.merge(
              dcpa.getProceedOperator().processForward(stateAndPrecision.state()), true);
    }
    if (!processing.shouldProceed()) {
      return processing;
    }

    unifiedPrecision =
        dcpa.getCombinePrecisionOperator()
            .combine(
                ImmutableSet.<Precision>builder()
                    .addAll(
                        transformedImmutableSetCopy(
                            deserializedStatesAndPrecisions, StateAndPrecision::precision))
                    .add(unifiedPrecision)
                    .build());

    // group incoming states by block graph path
    ImmutableListMultimap.Builder<BlockGraphPath, StateAndPrecision> newPreconditionsBuilder =
        ImmutableListMultimap.builder();
    for (StateAndPrecision dsap : deserializedStatesAndPrecisions) {
      newPreconditionsBuilder.put(dsap.getBlockGraphPath(), dsap);
    }

    ImmutableListMultimap<BlockGraphPath, StateAndPrecision> newPreconditions =
        newPreconditionsBuilder.build();

    ImmutableList.Builder<BlockGraphPath> removeBuilder = new ImmutableList.Builder<>();
    ImmutableList.Builder<BlockGraphPath> addBuilder = new ImmutableList.Builder<>();
    boolean fixPointReached = true;
    for (BlockGraphPath newPath : newPreconditions.keySet()) {
      ImmutableListMultimap.Builder<PathCase, BlockGraphPath> caseBuilder =
          ImmutableListMultimap.builder();
      for (BlockGraphPath oldPath : preconditions.keySet()) {
        caseBuilder.put(newPath.getFirstMatchingCase(oldPath), oldPath);
      }
      ImmutableListMultimap<PathCase, BlockGraphPath> cases = caseBuilder.build();
      if (cases.containsKey(PathCase.SUFFIX_OR_EQUAL)) {
        boolean allowedToStop = false;
        for (BlockGraphPath oldPathForCase : cases.get(PathCase.SUFFIX_OR_EQUAL)) {
          if (!allowedToStop
              && isCoveredBy(newPreconditions.get(newPath), preconditions.get(oldPathForCase))) {
            allowedToStop = true;
          }
          removeBuilder.add(oldPathForCase);
        }
        addBuilder.add(newPath);
        fixPointReached &= allowedToStop;
        // ABC (existing)
        //  BCCC (incoming)
      } else if (cases.containsKey(PathCase.OVERLAP)) {
        removeBuilder.addAll(cases.get(PathCase.OVERLAP));
        addBuilder.add(newPath);
        fixPointReached = false;
      } else if (cases.containsKey(PathCase.REAL_PREFIX)) {
        boolean allowedToStop = false;
        for (BlockGraphPath oldPathForCase : cases.get(PathCase.REAL_PREFIX)) {
          if (isCoveredBy(newPreconditions.get(newPath), preconditions.get(oldPathForCase))) {
            allowedToStop = true;
            break;
          }
        }
        if (!allowedToStop) {
          addBuilder.add(newPath);
        }
      } else {
        Preconditions.checkState(cases.containsKey(PathCase.OTHER));
        addBuilder.add(newPath);
        boolean covered = false;
        for (BlockGraphPath oldPathForOther : cases.get(PathCase.OTHER)) {
          if (isCoveredBy(newPreconditions.get(newPath), preconditions.get(oldPathForOther))) {
            covered = true;
            break;
          }
        }
        fixPointReached &= covered;
      }
    }

    for (StateAndPrecision dsap : deserializedStatesAndPrecisions) {
      dsap.getBlockState().addHistory(block);
    }

    removeBuilder.build().forEach(preconditions::removeAll);
    addBuilder.build().forEach(path -> preconditions.putAll(path, newPreconditions.get(path)));

    if (fixPointReached) {
      return DssMessageProcessing.stop();
    }

    pathsToAnalyze.clear();
    pathsToAnalyze.addAll(preconditions.keySet());

    return processing;
  }

  private boolean isCoveredBy(
      Collection<@NonNull StateAndPrecision> newStates,
      Collection<@NonNull StateAndPrecision> oldStates)
      throws CPAException, InterruptedException {
    // TODO rather inefficient
    for (StateAndPrecision newState : newStates) {
      boolean isCovered = false;
      for (StateAndPrecision oldState : oldStates) {
        // TODO
        if (dcpa.getCoverageOperator().areStatesEqual(newState.state(), oldState.state())) {
          isCovered = true;
          break;
        }
      }
      if (!isCovered) {
        return false;
      }
    }
    return true;
  }

  @Override
  public DssMessageProcessing storeViolationCondition(
      DssViolationConditionMessage pNewViolationCondition)
      throws InterruptedException, SolverException {
    logger.log(Level.INFO, "Running forward analysis with respect to error condition");
    // merge all states into the reached set
    ImmutableList<StateAndPrecision> deserializedStates = deserialize(pNewViolationCondition);
    Collection<@NonNull StateAndPrecision> vcs;
    if (combineByHash) {
      vcs = violationConditions.get(StringId.of(pNewViolationCondition.getSenderId()));
    } else {
      vcs = violationConditions.removeAll(StringId.of(pNewViolationCondition.getSenderId()));
    }
    Set<SegmentedPaths> oldVcs =
        transformedImmutableSetCopy(vcs, sap -> extractWitnessFromState(sap.state()));
    int equal = 0;
    for (StateAndPrecision stateAndPrecision : deserializedStates) {
      if (oldVcs.contains(extractWitnessFromState(stateAndPrecision.state()))) {
        equal++;
        if (combineByHash) {
          continue;
        }
      }
      DssMessageProcessing current =
          dcpa.getProceedOperator().processBackward(stateAndPrecision.state());
      if (current.shouldProceed()) {
        violationConditions.put(
            StringId.of(pNewViolationCondition.getSenderId()), stateAndPrecision);
      }
    }
    if (violationConditions.get(StringId.of(pNewViolationCondition.getSenderId())).isEmpty()
        || equal == deserializedStates.size()) {
      return DssMessageProcessing.stop();
    }
    pathsToAnalyze.clear();
    pathsToAnalyze.addAll(preconditions.keySet());
    return DssMessageProcessing.proceed();
  }

  @Override
  public Collection<DssMessage> analyzePreconditions(String idFromLastUpdate)
      throws SolverException, InterruptedException, CPAException {
    if (!containsViolationInsideBlock && violationConditions.isEmpty()) {
      return ImmutableSet.of();
    }
    ImmutableSet.Builder<DssMessage> messages = ImmutableSet.builder();
    AnalysisResult result = analyzeViolationCondition(Optional.empty());
    if (!result.violationConditions().isEmpty()) {
      messages.addAll(reportViolationConditions(result.violationConditions()));
    }
    if (!result.summaries().isEmpty()) {
      messages.addAll(reportPostconditions(result.summaries()));
    }
    return messages.build();
  }

  @Override
  public Collection<DssMessage> analyzeViolationConditions(String idFormLastUpdate)
      throws SolverException, InterruptedException, CPAException {
    Collection<@NonNull StateAndPrecision> violations =
        violationConditions.get(StringId.of(idFormLastUpdate));
    if (violations.isEmpty()) {
      throw new IllegalArgumentException(
          "No violation condition found for sender ID: " + idFormLastUpdate);
    }
    ImmutableList.Builder<DssMessage> messages = ImmutableList.builder();
    AnalysisResult result = analyzeViolationCondition(Optional.of(StringId.of(idFormLastUpdate)));
    if (!result.summaries().isEmpty()) {
      messages.addAll(reportPostconditions(result.summaries()));
    }
    if (!result.violationConditions().isEmpty()) {
      messages.addAll(reportViolationConditions(result.violationConditions()));
    }
    return messages.build();
  }

  /**
   * Runs the CPA under an error condition, i.e., if the current block contains a block-end edge,
   * the error condition will be attached to that edge. In case this makes the path formula
   * infeasible, we compute an abstraction. If no error condition is present, we run the CPA.
   *
   * @return Important messages for other blocks.
   * @throws InterruptedException thrown if thread is interrupted unexpectedly
   * @throws CPAException thrown if CPA runs into an error
   */
  private AnalysisResult analyzeViolationCondition(Optional<StringId> sender)
      throws CPAException, InterruptedException {
    if (violationConditions.isEmpty()) {
      return new AnalysisResult(ImmutableList.of(), ImmutableSet.of());
    }

    ImmutableList.Builder<StateAndPrecision> summaries = ImmutableList.builder();
    ImmutableSet.Builder<ArgPathAndCondition> vcs = ImmutableSet.builder();

    for (BlockGraphPath path : pathsToAnalyze) {
      for (StateAndPrecision stateAndPrecision : ImmutableList.copyOf(preconditions.get(path))) {
        boolean isTrivial = dcpa.isMostGeneralBlockEntryState(stateAndPrecision.state());
        resetStates();
        reachedSet.clear();
        reachedSet.add(
            stateAndPrecision.state(),
            resetPrecisionsForEveryRun || isTrivial ? makeStartPrecision() : unifiedPrecision);
        BlockState blockState = stateAndPrecision.getBlockState();
        blockState.setViolationConditions(
            transformedImmutableListCopy(
                sender.map(violationConditions::get).orElse(violationConditions.values()),
                sap -> sap.state()));

        DssBlockAnalysisResult result = DssBlockAnalyses.runAlgorithm(algorithm, reachedSet, block);

        status = status.update(result.getStatus());

        if (!preconditions.isEmpty() || block.isRoot()) {
          for (ARGState summary : result.getFinalLocationStates()) {
            summaries.add(new StateAndPrecision(summary, reachedSet.getPrecision(summary)));
          }
        }

        if (!result.getAllViolations().isEmpty()) {
          vcs.addAll(computeViolationConditionStates(result.getViolationConditionViolations()));
          vcs.addAll(computeViolationConditionStatesFromOrigin(result.getTargetStates()));
        }
      }
    }
    return new AnalysisResult(summaries.build(), vcs.build());
  }
}
