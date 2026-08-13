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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Set;
import java.util.logging.Level;
import org.jspecify.annotations.NonNull;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.DssSingleWorkerStatistics;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis.DssBlockAnalyses.DssBlockAnalysisResult;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessageFactory;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssPostConditionMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssViolationConditionMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis.StateAndPrecision;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DssMessageProcessing;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DssAnalysisOptions;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.block.BlockState;
import org.sosy_lab.cpachecker.cpa.pathrestriction.SegmentedPaths;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.java_smt.api.SolverException;

public class AlwaysReplaceDssBlockAnalysis extends DssBlockAnalysis<StringId, StringId> {

  private Precision unifiedPrecision;

  public AlwaysReplaceDssBlockAnalysis(
      LogManager pLogger,
      BlockNode pBlock,
      CFA pCFA,
      Specification pSpecification,
      Configuration pConfiguration,
      DssAnalysisOptions pOptions,
      DssMessageFactory pMessageFactory,
      ShutdownManager pShutdownManager,
      DssSingleWorkerStatistics pWorkerStats)
      throws CPAException, InterruptedException, InvalidConfigurationException {
    super(
        pLogger,
        pBlock,
        pCFA,
        pSpecification,
        pConfiguration,
        pOptions,
        pMessageFactory,
        pShutdownManager,
        pWorkerStats);
    unifiedPrecision = makeStartPrecision();
  }

  @Override
  public Collection<DssMessage> runInitialAnalysis()
      throws CPAException, InterruptedException, SolverException {
    reachedSet.clear();
    reachedSet.add(makeStartState(), makeStartPrecision());

    DssBlockAnalysisResult result = DssBlockAnalyses.runAlgorithm(algorithm, reachedSet, block);

    status = status.update(result.getStatus());

    if (!result.getAllViolations().isEmpty()) {
      return reportFirstViolationConditions(result.getAllViolations());
    }

    return ImmutableList.of();
  }

  @Override
  public DssMessageProcessing storePrecondition(DssPostConditionMessage pReceived)
      throws InterruptedException, SolverException, CPAException {
    resetStates();
    ImmutableList<@NonNull StateAndPrecision> deserializedStatesAndPrecisions =
        deserialize(pReceived);
    workerStats.getStorePreconditionStatesTimer().start();
    try {
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

      int equal = 0;
      for (StateAndPrecision newSap : deserializedStatesAndPrecisions) {
        for (StateAndPrecision oldSap : preconditions.get(StringId.of(pReceived.getSenderId()))) {
          if (dcpa.getCoverageOperator().areStatesEqual(newSap.state(), oldSap.state())) {
            equal++;
            break;
          }
        }
      }

      preconditions.removeAll(StringId.of(pReceived.getSenderId()));
      preconditions.putAll(StringId.of(pReceived.getSenderId()), deserializedStatesAndPrecisions);

      if (equal == deserializedStatesAndPrecisions.size()) {
        // All states are equal, no need to proceed
        return DssMessageProcessing.stop();
      }

      return DssMessageProcessing.proceed();
    } finally {
      workerStats.getStorePreconditionStatesTimer().stop();
      workerStats.getStorePreconditionStatesCounter().add(deserializedStatesAndPrecisions.size());
    }
  }

  @Override
  public DssMessageProcessing storeViolationCondition(
      DssViolationConditionMessage pNewViolationCondition)
      throws InterruptedException, SolverException {
    logger.log(Level.INFO, "Running forward analysis with respect to error condition");
    // merge all states into the reached set
    ImmutableList<StateAndPrecision> newVcs = deserialize(pNewViolationCondition);
    workerStats.getStoreViolationConditionStatesTimer().start();
    try {
      Collection<@NonNull StateAndPrecision> vcs =
          violationConditions.removeAll(StringId.of(pNewViolationCondition.getSenderId()));
      Set<SegmentedPaths> oldVcs =
          transformedImmutableSetCopy(vcs, sap -> extractWitnessFromState(sap.state()));
      int equal = 0;
      for (StateAndPrecision newVc : newVcs) {
        if (oldVcs.contains(extractWitnessFromState(newVc.state()))) {
          equal++;
          if (combineByHash) {
            continue;
          }
        }
        DssMessageProcessing current = dcpa.getProceedOperator().processBackward(newVc.state());
        if (current.shouldProceed()) {
          violationConditions.put(StringId.of(pNewViolationCondition.getSenderId()), newVc);
        }
      }
      if (violationConditions.get(StringId.of(pNewViolationCondition.getSenderId())).isEmpty()
          || equal == newVcs.size()) {
        return DssMessageProcessing.stop();
      }
      return DssMessageProcessing.proceed();
    } finally {
      workerStats.getStoreViolationConditionStatesTimer().stop();
      workerStats.getStoreViolationConditionStatesCounter().add(newVcs.size());
    }
  }

  @Override
  public Collection<DssMessage> analyzePreconditions(String idFromLastUpdate)
      throws SolverException, InterruptedException, CPAException {

    ImmutableSet.Builder<DssMessage> messages = ImmutableSet.builder();
    AnalysisResult result = analyzeViolationCondition();
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
    AnalysisResult result = analyzeViolationCondition();
    if (!result.summaries().isEmpty()) {
      messages.addAll(reportPostconditions(result.summaries()));
    }
    if (!result.violationConditions().isEmpty()) {
      messages.addAll(reportViolationConditions(result.violationConditions()));
    }
    return messages.build();
  }

  private AnalysisResult analyzeViolationCondition() throws CPAException, InterruptedException {

    ImmutableSet.Builder<StateAndPrecision> summaries = ImmutableSet.builder();
    ImmutableSet.Builder<ArgPathAndCondition> vcs = ImmutableSet.builder();

    ImmutableList.Builder<StateAndPrecision> preconditionsToAnalyze = ImmutableList.builder();
    if (preconditions.isEmpty()) {
      preconditionsToAnalyze.add(new StateAndPrecision(makeStartState(), unifiedPrecision));
    } else {
      preconditionsToAnalyze.addAll(preconditions.values());
    }

    for (StateAndPrecision stateAndPrecision : preconditionsToAnalyze.build()) {
      resetStates();
      reachedSet.clear();
      reachedSet.add(stateAndPrecision.state(), unifiedPrecision);
      BlockState blockState = stateAndPrecision.getBlockState();
      blockState.setViolationConditions(
          transformedImmutableListCopy(violationConditions.values(), sap -> sap.state()));

      DssBlockAnalysisResult result = runBlockAnalysis();

      status = status.update(result.getStatus());
      if (!result.getAllViolations().isEmpty()) {
        vcs.addAll(computeViolationConditionStates(result.getViolationConditionViolations()));
        vcs.addAll(computeViolationConditionStatesFromOrigin(result.getTargetStates()));
      } else if (!preconditions.isEmpty() || block.isRoot()) {
        for (ARGState summary : result.getFinalLocationStates()) {
          summaries.add(new StateAndPrecision(summary, reachedSet.getPrecision(summary)));
        }
      }
    }
    ImmutableSet<ArgPathAndCondition> newViolations = vcs.build();
    ImmutableSet<StateAndPrecision> newSummaries = ImmutableSet.of();
    if (newViolations.isEmpty()) {
      newSummaries = summaries.build();
    }
    return new AnalysisResult(newSummaries, newViolations);
  }
}
