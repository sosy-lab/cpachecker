// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimaps;
import java.util.Collection;
import java.util.Optional;
import org.jspecify.annotations.NonNull;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.DssSingleWorkerStatistics;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis.DssBlockAnalyses.DssBlockAnalysisResult;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssPostConditionMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis.StateAndPrecision;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DssMessageProcessing;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.java_smt.api.SolverException;

/**
 * Groups preconditions by the block that sent them, so that every postcondition update replaces
 * everything previously received from that block.
 *
 * <p>The block stops as soon as an update carries only states it already knows for the sending
 * block. Every known precondition is re-explored on every round, always with the combined precision
 * of everything received so far.
 */
final class AlwaysReplacePreconditionHandler implements DssPreconditionHandler {

  private final ScopedLocationMap preconditions;

  private final DssBlockAnalysis analysis;

  private Precision unifiedPrecision;

  AlwaysReplacePreconditionHandler(DssBlockAnalysis pAnalysis) throws InterruptedException {
    analysis = pAnalysis;
    unifiedPrecision = pAnalysis.makeStartPrecision();
    if (analysis.getBlock().isRoot()) {
      preconditions = new ScopedLocationMap(analysis.getDcpa(), ImmutableSet.of("root"));
    } else {
      preconditions =
          new ScopedLocationMap(analysis.getDcpa(), analysis.getBlock().getPredecessorIds());
    }

    if (analysis.getBlock().isRoot()) {
      analysis.setIgnoreCallstack(false);
      StateAndPrecision stateAndPrecision =
          new StateAndPrecision(analysis.makeStartState(), analysis.makeStartPrecision());
      preconditions.addStateForKey("root", stateAndPrecision);
    }
  }

  @Override
  public Collection<DssMessage> runInitialAnalysis()
      throws CPAException, InterruptedException, SolverException {
    analysis.setIgnoreCallstack(true);
    DssBlockAnalysisResult result =
        analysis.runInitialBlockAnalysis(analysis.makeStartState(), analysis.makeStartPrecision());

    if (!result.getAllViolations().isEmpty()) {
      return analysis.reportFirstViolationConditions(result.getAllViolations());
    }
    return ImmutableList.of();
  }

  @Override
  public DssMessageProcessing store(DssPostConditionMessage pReceived)
      throws InterruptedException, SolverException, CPAException {
    preconditions.resetStates();
    ImmutableList<@NonNull StateAndPrecision> received = analysis.deserialize(pReceived);
    if (received.size() == 1
        && analysis
            .getDcpa()
            .isMostGeneralBlockEntryState(Iterables.getOnlyElement(received).state())) {
      if (preconditions.isEmpty(pReceived.getSenderId())) {
        return DssMessageProcessing.stop();
      }
      preconditions.clearKey(pReceived.getSenderId());
      return DssMessageProcessing.proceed();
    }
    ImmutableListMultimap<Integer, @NonNull StateAndPrecision> hashToState =
        Multimaps.index(received, sap -> analysis.getDcpa().computeProgramPointHash(sap.state()));
    DssSingleWorkerStatistics stats = analysis.statistics();
    stats.getStorePreconditionStatesTimer().start();
    try {
      DssMessageProcessing processing = analysis.shouldProceedForward(received);
      if (!processing.shouldProceed()) {
        return processing;
      }

      unifiedPrecision = analysis.combinePrecisions(unifiedPrecision, received);

      boolean stop = true;

      for (Integer id : hashToState.keySet()) {
        ImmutableList<@NonNull StateAndPrecision> statesAtLocation = hashToState.get(id);
        if (!analysis.allCovered(
            statesAtLocation, preconditions.getStateForKeyAndId(pReceived.getSenderId(), id))) {
          preconditions.overwriteStatesForKey(pReceived.getSenderId(), id, statesAtLocation);
          stop = false;
        }
      }

      if (stop) {
        // All states are equal, no need to proceed
        return DssMessageProcessing.stop();
      }
      return DssMessageProcessing.proceed();
    } finally {
      stats.getStorePreconditionStatesTimer().stop();
      stats.getStorePreconditionStatesCounter().add(received.size());
    }
  }

  @Override
  public Collection<DssMessage> analyze()
      throws SolverException, InterruptedException, CPAException {
    ImmutableSet.Builder<DssMessage> messages = ImmutableSet.builder();
    AnalysisResult round = explore(false);
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
    AnalysisResult round = explore(true);
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
    return preconditions.getStatesAndPrecisions();
  }

  @Override
  public void violationConditionsChanged() {
    // nothing to do here, the block will be re-explored on the next round anyway
  }

  /** Explores the block once from every known precondition. */
  private AnalysisResult explore(boolean isBackward) throws CPAException, InterruptedException {
    if (analysis.getViolationConditionHandler().isEmpty()) {
      return new AnalysisResult(ImmutableList.of(), ImmutableSet.of());
    }

    ImmutableSet.Builder<StateAndPrecision> summaries = ImmutableSet.builder();
    ImmutableSet.Builder<ArgPathAndCondition> violations = ImmutableSet.builder();

    Precision precision = unifiedPrecision;
    Collection<AbstractState> receivedStates = preconditions.getStates();
    boolean forceTop = false;
    if (receivedStates.isEmpty()) {
      analysis.setIgnoreCallstack(true);
      receivedStates = ImmutableList.of(analysis.makeStartState());
      precision = analysis.makeStartPrecision();
      forceTop = !isBackward;
    }
    for (AbstractState state : receivedStates) {
      preconditions.resetStates();
      DssBlockAnalysisResult result =
          analysis.runBlockAnalysis(
              state, precision, analysis.getViolationConditionHandler().statesOf(Optional.empty()));

      if (!result.getAllViolations().isEmpty()) {
        violations.addAll(analysis.pathsWithCondition(result.getViolationConditionViolations()));
        violations.addAll(analysis.pathsFromOrigin(result.getTargetStates()));
      } else if (!receivedStates.isEmpty() || analysis.getBlock().isRoot()) {
        summaries.addAll(analysis.summariesOf(result));
      }
    }

    if (violations.build().isEmpty() && summaries.build().isEmpty()) {
      return new AnalysisResult(
          ImmutableList.of(
              new StateAndPrecision(
                  analysis.makeTopState(analysis.getBlock().getFinalLocation()), precision)),
          ImmutableSet.of());
    }

    if (forceTop) {
      if (violations.build().isEmpty() && summaries.build().isEmpty()) {
        return new AnalysisResult(
            ImmutableList.of(
                new StateAndPrecision(
                    analysis.makeTopState(analysis.getBlock().getFinalLocation()), precision)),
            violations.build());
      }
    }

    ImmutableSet<ArgPathAndCondition> newViolations = violations.build();
    // summaries found alongside a violation are discarded: the violation has to be resolved first
    return new AnalysisResult(
        newViolations.isEmpty() ? summaries.build() : ImmutableSet.of(), newViolations);
  }
}
