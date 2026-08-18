// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis;

import static com.google.common.base.Preconditions.checkArgument;
import static org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis.DssBlockAnalysis.blockStateOf;

import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableSet;
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

  /** Stands in for the (nonexistent) predecessor of the root block. */
  private static final String ROOT_KEY = "root";

  private final BlockToProgramLocationMap preconditions;

  private final DssBlockAnalysis analysis;

  private Precision currentPrecisionOfAnalysis;

  AlwaysReplacePreconditionHandler(DssBlockAnalysis pAnalysis) throws InterruptedException {
    analysis = pAnalysis;
    currentPrecisionOfAnalysis = pAnalysis.makeStartPrecision();
    if (analysis.getBlock().isRoot()) {
      // the root block has no predecessor to receive a precondition from, so it starts from the
      // unconstrained entry state under the synthetic key ROOT_KEY
      preconditions = new BlockToProgramLocationMap(analysis.getDcpa(), ImmutableSet.of(ROOT_KEY));
      preconditions.addStateForKey(
          ROOT_KEY,
          new StateAndPrecision(analysis.makeStartState(false), analysis.makeStartPrecision()));
    } else {
      preconditions =
          new BlockToProgramLocationMap(
              analysis.getDcpa(), analysis.getBlock().getPredecessorIds());
    }
  }

  @Override
  public Collection<DssMessage> runInitialAnalysis()
      throws CPAException, InterruptedException, SolverException {
    DssBlockAnalysisResult result =
        analysis.runInitialBlockAnalysis(
            analysis.makeStartState(true), analysis.makeStartPrecision());

    if (!result.getAllViolations().isEmpty()) {
      return analysis.reportFirstViolationConditions(result.getAllViolations());
    }
    return ImmutableList.of();
  }

  @Override
  public DssMessageProcessing store(DssPostConditionMessage pReceived)
      throws InterruptedException, SolverException, CPAException {
    if (pReceived.indicatesUnreachableBlockEnd()) {
      String sender = pReceived.getSenderId();
      // Re-analysing on an update that changes nothing republishes the very same message. Around a
      // cycle in the block graph that never terminates => stop.
      boolean alreadyRecorded =
          preconditions.isMarkedUnreachable(sender) && preconditions.isEmpty(sender);
      preconditions.markUnreachable(sender);
      if (alreadyRecorded) {
        return DssMessageProcessing.stop();
      }
      if (preconditions.isEmpty(sender) && !preconditions.isUnreachable()) {
        return DssMessageProcessing.stop();
      }
      preconditions.clearKey(sender);
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
            statesAtLocation,
            preconditions.getStatesAndPrecisionsForKeyAndId(pReceived.getSenderId(), id))) {
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

  /**
   * Explores the block from all known preconditions, one group of equally-located preconditions at
   * a time, and merges what the individual rounds found.
   */
  private AnalysisResult explore(boolean isBackward) throws CPAException, InterruptedException {
    if (analysis.getViolationConditionHandler().isEmpty()) {
      return AnalysisResult.empty();
    }
    if (!isBackward && preconditions.isUnreachable()) {
      // every predecessor reported an unreachable block end, so this block cannot be entered
      return AnalysisResult.unreachableBlockEnd();
    }
    ImmutableList<AbstractState> violationConditions =
        analysis.getViolationConditionHandler().statesOf(Optional.empty());

    ImmutableList.Builder<AnalysisResult> rounds = ImmutableList.builder();
    for (Integer locationHash : preconditions.getAllLocationHashes()) {
      rounds.add(
          exploreFrom(
              preconditions.getStatesPerLocation(locationHash), violationConditions, false));
    }
    if (preconditions.isEmpty() || preconditions.isAnyPredecessorTrulyEmpty()) {
      // a predecessor that has not sent anything yet does not restrict the block entry, so explore
      // speculatively from the unconstrained start state to find violations early
      AnalysisResult topExploration =
          exploreFrom(ImmutableSet.of(analysis.makeStartState(true)), violationConditions, true);
      Preconditions.checkState(topExploration.summaries().isEmpty());
      rounds.add(topExploration);
    }
    return merge(rounds.build());
  }

  /**
   * Combines the rounds of one exploration: summaries and violation conditions accumulate, while
   * the block end counts as unreachable only if every round found it unreachable.
   */
  private AnalysisResult merge(Collection<AnalysisResult> pRounds)
      throws CPAException, InterruptedException {
    ImmutableSet.Builder<StateAndPrecision> summaries = ImmutableSet.builder();
    ImmutableSet.Builder<ArgPathAndCondition> violations = ImmutableSet.builder();
    boolean unreachable = true;
    for (AnalysisResult round : pRounds) {
      summaries.addAll(round.summaries());
      violations.addAll(round.violationConditions());
      unreachable &= round.blockEndUnreachable();
    }
    return new AnalysisResult(
        analysis.deduplicateStatesAndPrecisions(summaries.build()),
        violations.build(),
        unreachable);
  }

  /**
   * Explores the block once from each of the given states.
   *
   * @param pDiscardSummaries whether this is a speculative run whose summaries must not be
   *     published, in which case the violations it finds are reported separately
   */
  private AnalysisResult exploreFrom(
      Collection<AbstractState> statesToProcess,
      Collection<AbstractState> violationConditions,
      boolean pDiscardSummaries)
      throws CPAException, InterruptedException {

    ImmutableSet.Builder<StateAndPrecision> summaries = ImmutableSet.builder();
    ImmutableSet.Builder<ArgPathAndCondition> violations = ImmutableSet.builder();
    ImmutableSet.Builder<ArgPathAndCondition> extraViolations = ImmutableSet.builder();
    ImmutableSet.Builder<ArgPathAndCondition> foundViolations =
        pDiscardSummaries ? extraViolations : violations;

    for (AbstractState state : statesToProcess) {
      DssBlockAnalysisResult result =
          analysis.runBlockAnalysis(
              analysis.getDcpa().reset(state),
              analysis.getOptions().doResetPrecisionsForEveryRun()
                  ? analysis.makeStartPrecision()
                  : currentPrecisionOfAnalysis,
              violationConditions);

      if (!result.getAllViolations().isEmpty()) {
        foundViolations.addAll(
            analysis.pathsWithCondition(result.getViolationConditionViolations()));
        foundViolations.addAll(analysis.pathsFromOrigin(result.getTargetStates()));
      } else if (!pDiscardSummaries) {
        summaries.addAll(analysis.summariesOf(result));
      }
    }

    ImmutableSet<StateAndPrecision> finalSummaries = summaries.build();
    ImmutableSet<ArgPathAndCondition> finalExtraViolations = extraViolations.build();
    ImmutableSet<ArgPathAndCondition> finalViolations = violations.build();

    if (finalExtraViolations.isEmpty() && finalViolations.isEmpty() && finalSummaries.isEmpty()) {
      // the exploration produced no state at the final location
      return AnalysisResult.unreachableBlockEnd();
    }

    if (!finalViolations.isEmpty()) {
      // summaries found alongside a violation are discarded: the violation has to be resolved first
      return AnalysisResult.ofViolationConditions(
          FluentIterable.concat(finalViolations, finalExtraViolations).toSet());
    }

    if (!pDiscardSummaries
        && finalSummaries.stream()
            .allMatch(sap -> analysis.getDcpa().isMostGeneralBlockEntryState(sap.state()))) {
      if (!finalExtraViolations.isEmpty()) {
        return AnalysisResult.ofViolationConditions(finalExtraViolations);
      }
      return AnalysisResult.empty();
    }

    ImmutableSet<AbstractState> violationsToConsider =
        FluentIterable.from(finalSummaries)
            .transform(sap -> blockStateOf(sap.state()))
            .filter(b -> !b.getHinderedByCallstack().isEmpty())
            .transformAndConcat(b -> b.getHinderedByCallstack())
            .toSet();

    if (!violationsToConsider.isEmpty() && !pDiscardSummaries) {
      AnalysisResult extension =
          exploreFrom(ImmutableSet.of(analysis.makeStartState(true)), violationConditions, true);
      finalExtraViolations =
          FluentIterable.concat(finalViolations, extension.violationConditions()).toSet();
    }

    return new AnalysisResult(finalSummaries, finalExtraViolations, false);
  }
}
