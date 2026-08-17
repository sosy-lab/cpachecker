// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis;

import static com.google.common.base.Preconditions.checkArgument;
import static org.sosy_lab.common.collect.Collections3.listAndElement;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimaps;
import java.util.Collection;
import java.util.Optional;
import org.jspecify.annotations.NonNull;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.DssDebugUtils;
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

  private final BlockToProgramLocationMap preconditions;

  private final DssBlockAnalysis analysis;

  private Precision currentPrecisionOfAnalysis;

  AlwaysReplacePreconditionHandler(DssBlockAnalysis pAnalysis) throws InterruptedException {
    analysis = pAnalysis;
    currentPrecisionOfAnalysis = pAnalysis.makeStartPrecision();
    if (analysis.getBlock().isRoot()) {
      preconditions = new BlockToProgramLocationMap(analysis.getDcpa(), ImmutableSet.of("root"));
    } else {
      preconditions =
          new BlockToProgramLocationMap(
              analysis.getDcpa(), analysis.getBlock().getPredecessorIds());
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
    if (pReceived.indicatesUnreachableBlockEnd()) {
      preconditions.markUnreachable(pReceived.getSenderId());
      if (preconditions.isEmpty(pReceived.getSenderId()) && !preconditions.isUnreachable()) {
        return DssMessageProcessing.stop();
      }
      preconditions.clearKey(pReceived.getSenderId());
      return DssMessageProcessing.proceed();
    }
    ImmutableList<@NonNull StateAndPrecision> received = analysis.deserialize(pReceived);
    preconditions.markReachable(pReceived.getSenderId());
    ImmutableListMultimap<Integer, @NonNull StateAndPrecision> hashToState =
        Multimaps.index(received, sap -> analysis.getDcpa().computeProgramPointHash(sap.state()));
    DssSingleWorkerStatistics stats = analysis.statistics();
    stats.getStorePreconditionStatesTimer().start();
    try {
      DssMessageProcessing processing = analysis.shouldProceedForward(received);
      if (!processing.shouldProceed()) {
        return processing;
      }

      preconditions.removeStatesWithIgnoreCallstackIfMorePrecise(pReceived.getSenderId(), received);

      currentPrecisionOfAnalysis = analysis.combinePrecisions(currentPrecisionOfAnalysis, received);

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
    messages.addAll(postConditionsOf(round));
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
    messages.addAll(postConditionsOf(round));
    if (!round.violationConditions().isEmpty()) {
      messages.addAll(analysis.reportViolationConditions(round.violationConditions()));
    }
    return messages.build();
  }

  /**
   * Turns the postcondition side of a round into messages: either an explicit unreachable-block-end
   * signal, or the summaries the round found, or nothing at all.
   */
  private Collection<DssMessage> postConditionsOf(AnalysisResult pRound) {
    if (pRound.blockEndUnreachable()) {
      return analysis.reportUnreachableBlockEnd();
    }
    return analysis.reportPostconditions(pRound.summaries());
  }

  @Override
  public ImmutableList<@NonNull StateAndPrecision> getKnownPreconditions() {
    return preconditions.getStatesAndPrecisions();
  }

  @Override
  public void violationConditionsChanged() {
    // nothing to do, the block is always explored from all known preconditions
  }

  private String prettyPrint() {
    return DssDebugUtils.prettyPrintPredicateAnalysisBlock(
        analysis.getBlock(),
        preconditions.asMultimapByKey(),
        ((AlwaysReplaceViolationConditionHandler) analysis.getViolationConditionHandler())
            .getConditions()
            .asMultimapByKey());
  }

  /**
   * A round whose block end is unreachable, i.e., that publishes no postcondition at all.
   *
   * @param pViolationConditions the violations found before the block end turned out unreachable
   */
  private static AnalysisResult prepareUnreachableBlockEnd(
      ImmutableSet<ArgPathAndCondition> pViolationConditions) {
    return new AnalysisResult(ImmutableSet.of(), pViolationConditions, true);
  }

  private static AnalysisResult prepareViolationConditions(
      ImmutableSet<ArgPathAndCondition> pViolationConditions) {
    return new AnalysisResult(ImmutableSet.of(), pViolationConditions, false);
  }

  private static AnalysisResult preparePostconditions(Collection<StateAndPrecision> pSummaries) {
    return new AnalysisResult(pSummaries, ImmutableSet.of(), false);
  }

  /** Explores the block once from every known precondition. */
  private AnalysisResult explore(boolean isBackward) throws CPAException, InterruptedException {
    if (analysis.getViolationConditionHandler().isEmpty()) {
      return new AnalysisResult(ImmutableList.of(), ImmutableSet.of());
    }

    if (!isBackward && preconditions.isUnreachable()) {
      // every predecessor reported an unreachable block end, so this block cannot be entered
      return prepareUnreachableBlockEnd(ImmutableSet.of());
    }

    ImmutableSet.Builder<StateAndPrecision> summaries = ImmutableSet.builder();
    ImmutableSet.Builder<ArgPathAndCondition> violations = ImmutableSet.builder();
    ImmutableSet.Builder<ArgPathAndCondition> extraViolations = ImmutableSet.builder();

    Precision precision = currentPrecisionOfAnalysis;
    Collection<AbstractState> statesToProcess = preconditions.getStates();
    AbstractState lastState = null;
    boolean isEmpty = preconditions.isAnyPredecessorTrulyEmpty() || preconditions.isEmpty();
    if (isEmpty) {
      analysis.setIgnoreCallstack(true);
      lastState = analysis.makeStartState();
      statesToProcess = listAndElement(statesToProcess, lastState);
      analysis.setIgnoreCallstack(false);
      precision = analysis.makeStartPrecision();
    }

    for (AbstractState state : statesToProcess) {
      DssBlockAnalysisResult result =
          analysis.runBlockAnalysis(
              analysis.getDcpa().reset(state),
              analysis.getOptions().doResetPrecisionsForEveryRun()
                  ? analysis.makeStartPrecision()
                  : precision,
              analysis.getViolationConditionHandler().statesOf(Optional.empty()));

      if (isEmpty && state == lastState) {
        if (!result.getAllViolations().isEmpty()) {
          extraViolations.addAll(
              analysis.pathsWithCondition(result.getViolationConditionViolations()));
          extraViolations.addAll(analysis.pathsFromOrigin(result.getTargetStates()));
        }
      } else {
        if (!result.getAllViolations().isEmpty()) {
          violations.addAll(analysis.pathsWithCondition(result.getViolationConditionViolations()));
          violations.addAll(analysis.pathsFromOrigin(result.getTargetStates()));
        } else if (!statesToProcess.isEmpty() || analysis.getBlock().isRoot()) {
          summaries.addAll(analysis.summariesOf(result));
        }
      }
    }

    ImmutableSet<StateAndPrecision> finalSummaries = summaries.build();
    ImmutableSet<ArgPathAndCondition> finalExtraViolations = extraViolations.build();
    ImmutableSet<ArgPathAndCondition> finalViolations = violations.build();

    if (finalExtraViolations.isEmpty() && finalViolations.isEmpty() && finalSummaries.isEmpty()) {
      // the exploration produced no state at the final location
      return prepareUnreachableBlockEnd(ImmutableSet.of());
    }

    if (!finalViolations.isEmpty()) {
      // summaries found alongside a violation are discarded: the violation has to be resolved first
      return prepareViolationConditions(
          FluentIterable.concat(finalViolations, finalExtraViolations).toSet());
    }

    if (preconditions.isEmpty()
        && finalSummaries.stream()
            .allMatch(sap -> analysis.getDcpa().isMostGeneralBlockEntryState(sap.state()))) {
      if (!finalExtraViolations.isEmpty()) {
        return prepareViolationConditions(finalExtraViolations);
      }
      return preparePostconditions(ImmutableSet.of());
    }

    return new AnalysisResult(
        analysis.deduplicateStatesAndPrecisions(finalSummaries), finalExtraViolations, false);
  }
}
