// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis;

import static com.google.common.base.Preconditions.checkArgument;
import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;
import static org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis.DssBlockAnalysis.blockStateOf;

import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimaps;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis.DssBlockAnalyses.DssBlockAnalysisResult;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis.StateAndPrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * Explores the block from all preconditions an {@link AlwaysReplacePreconditionHandler} holds, one
 * input state at a time.
 *
 * <p>Preconditions and violation conditions are both grouped by program point, and every
 * combination of one input state with one condition group becomes its own CPA run. Because a
 * program-point hash covers the callstack, conditions of different call contexts of the same block
 * land in different groups, so exploring per group keeps the contexts apart.
 */
final class AlwaysReplaceExplorationEngine implements DssExplorationEngine {

  private final DssBlockAnalysis analysis;
  private final AlwaysReplacePreconditionHandler preconditionHandler;
  private final DssViolationConditionHandler violationConditions;

  AlwaysReplaceExplorationEngine(
      DssBlockAnalysis pAnalysis,
      AlwaysReplacePreconditionHandler pPreconditionHandler,
      DssViolationConditionHandler pViolationConditions) {
    analysis = pAnalysis;
    preconditionHandler = pPreconditionHandler;
    violationConditions = pViolationConditions;
  }

  @Override
  public AnalysisResult exploreInitially() throws CPAException, InterruptedException {
    DssBlockAnalysisResult result =
        analysis.runInitialBlockAnalysis(
            analysis.makeStartState(true), analysis.makeStartPrecision());

    if (result.getAllViolations().isEmpty()) {
      // The initial run publishes only the violations that originate inside the block. Its
      // postcondition stays unpublished, matching explore(Optional), which explores nothing as
      // long as no violation condition is known.
      return AnalysisResult.empty();
    }
    return AnalysisResult.ofViolationConditions(
        analysis.pathsFromOrigin(result.getAllViolations()));
  }

  /**
   * Explores the block from each known input under the different exit contexts and merges what the
   * individual rounds found.
   */
  @Override
  public AnalysisResult explore(Optional<String> pViolationConditionSender)
      throws CPAException, InterruptedException {
    pViolationConditionSender.ifPresent(
        sender ->
            checkArgument(
                !violationConditions.isEmptyFor(sender),
                "No violation condition found for sender ID: %s",
                sender));
    if (violationConditions.isEmpty()) {
      return AnalysisResult.empty();
    }
    BlockToProgramLocationMap preconditions = preconditionHandler.getPreconditions();
    if (pViolationConditionSender.isEmpty() && preconditions.isUnreachable()) {
      // every predecessor reported an unreachable block end, so this block cannot be entered
      return AnalysisResult.unreachableBlockEnd();
    }
    ImmutableListMultimap<Object, AbstractState> conditionsPerLocation;
    if (analysis.getOptions().callStackStateRequiresStateReset()) {
      conditionsPerLocation =
          ImmutableListMultimap.<Object, AbstractState>builder()
              .putAll(0, violationConditions.statesOf(Optional.empty()))
              .build();
    } else {
      // Violation conditions are grouped by program point exactly like preconditions are. Because a
      // program-point hash covers the callstack, conditions of different call contexts of the same
      // block land in different groups. Exploring a block under all of them at once would mix those
      // contexts: with inlining every call site would be its own block, but here one block is
      // shared by all of them, so the contexts have to be kept apart by exploring per group
      // instead.
      conditionsPerLocation =
          Multimaps.index(
              violationConditions.statesOf(Optional.empty()),
              condition -> analysis.getDcpa().computeProgramPointId(condition));
    }

    Precision precisionOfAnalysis =
        preconditions.isEmpty()
            ? analysis.makeStartPrecision()
            : analysis.combinePrecisions(preconditions.getStatesAndPrecisions());

    List<AnalysisResult> rounds = new ArrayList<>();
    Set<ImmutableList<AbstractState>> pendingFallbacks = new LinkedHashSet<>();
    for (Object preconditionProgramPoint : preconditions.getAllProgramPoints()) {
      Collection<AbstractState> inputs =
          preconditions.getStatesPerLocation(preconditionProgramPoint);
      if (analysis.getOptions().combinePreconditionsByHash()) {
        inputs =
            ImmutableList.of(
                analysis
                    .getDcpa()
                    .getCombineOperator()
                    .combinePreconditions(
                        transformedImmutableListCopy(inputs, analysis.getDcpa()::reset)));
      }
      for (AbstractState input : inputs) {
        rounds.add(
            exploreEntry(input, conditionsPerLocation, precisionOfAnalysis, pendingFallbacks));
      }
    }
    if (preconditions.isEmpty() || preconditions.isAnyPredecessorTrulyEmpty()) {
      // A predecessor with no known state does not restrict the entry.
      pendingFallbacks.add(ImmutableList.copyOf(violationConditions.statesOf(Optional.empty())));
    }
    // Several inputs can request the same unconstrained-caller check. Keep these obligations
    // separate from known-input refinement and check each condition group only once in this
    // exploration. Requests from individual rounds survive their replacement by a combined round.
    for (ImmutableList<AbstractState> conditions : pendingFallbacks) {
      AnalysisResult topExploration =
          exploreFrom(
              analysis.makeStartState(true),
              conditions,
              precisionOfAnalysis,
              true,
              pendingFallbacks);
      Preconditions.checkState(topExploration.summaries().isEmpty());
      rounds.add(topExploration);
    }
    return merge(rounds);
  }

  /** Combines successful refinements only when they describe the same input state. */
  private AnalysisResult exploreEntry(
      AbstractState pInput,
      ImmutableListMultimap<Object, AbstractState> pConditionsPerLocation,
      Precision pPrecision,
      Set<ImmutableList<AbstractState>> pPendingFallbacks)
      throws CPAException, InterruptedException {
    Map<Object, AnalysisResult> rounds = new LinkedHashMap<>();
    ImmutableSet.Builder<Object> successfulGroups = ImmutableSet.builder();
    for (Object conditionProgramPoint : pConditionsPerLocation.keySet()) {
      AnalysisResult round =
          exploreFrom(
              pInput,
              pConditionsPerLocation.get(conditionProgramPoint),
              pPrecision,
              false,
              pPendingFallbacks);
      rounds.put(conditionProgramPoint, round);
      if (!round.summaries().isEmpty()) {
        // A summary means this particular input was fully explored. Speculative callers are
        // considered separately after refining the known inputs.
        successfulGroups.add(conditionProgramPoint);
      }
    }
    Set<Object> groupsToCombine = successfulGroups.build();
    if (groupsToCombine.size() <= 1) {
      return merge(rounds.values());
    }

    groupsToCombine.forEach(rounds::remove);
    List<AnalysisResult> remainingRounds = new ArrayList<>(rounds.values());
    // Combining the conditions refines the complete postcondition for this input across exit
    // contexts. Combining whole entry-location groups would also replace the exits of other inputs
    // for which some of these conditions are still unresolved, potentially losing forward progress.
    remainingRounds.add(
        exploreFrom(
            pInput,
            FluentIterable.from(groupsToCombine)
                .transformAndConcat(pConditionsPerLocation::get)
                .toList(),
            pPrecision,
            false,
            pPendingFallbacks));
    return merge(remainingRounds);
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
   * Explores the block from one input state.
   *
   * @param pDiscardSummaries whether this is a speculative run whose summaries must not be
   *     published, in which case the violations it finds are reported separately
   * @param pPendingFallbacks collects the unconstrained-caller checks required by known inputs;
   *     speculative runs do not add requests
   */
  private AnalysisResult exploreFrom(
      AbstractState pInput,
      Collection<AbstractState> pViolationConditions,
      Precision pPrecision,
      boolean pDiscardSummaries,
      Set<ImmutableList<AbstractState>> pPendingFallbacks)
      throws CPAException, InterruptedException {
    DssBlockAnalysisResult result =
        analysis.runBlockAnalysis(
            analysis.getDcpa().reset(pInput), pPrecision, pViolationConditions);
    ImmutableSet.Builder<StateAndPrecision> summaries = ImmutableSet.builder();
    ImmutableSet.Builder<ArgPathAndCondition> violations = ImmutableSet.builder();
    if (!result.getAllViolations().isEmpty()) {
      violations.addAll(analysis.pathsWithCondition(result.getViolationConditionViolations()));
      violations.addAll(analysis.pathsFromOrigin(result.getTargetStates()));
    } else if (!pDiscardSummaries) {
      // Unresolved analyses can have coarse exit states that erase refinement around loops.
      // A completed analysis, including one whose conditions have incompatible callstacks, must
      // retain all reachable exits: other call contexts may still need them for forward progress.
      summaries.addAll(analysis.summariesOf(result));
    }

    if (!pDiscardSummaries
        && result.getFinalLocationStates().stream()
            .anyMatch(state -> !blockStateOf(state).getHinderedByCallstack().isEmpty())) {
      // A feasible violation on one path does not discharge conditions rejected by the callstack
      // on another path. Retain the request even if another round already has a violation or this
      // round will later be replaced by a combined refinement.
      pPendingFallbacks.add(ImmutableList.copyOf(pViolationConditions));
    }

    Set<StateAndPrecision> finalSummaries = summaries.build();
    Set<ArgPathAndCondition> finalViolations = violations.build();
    if (finalViolations.isEmpty() && finalSummaries.isEmpty()) {
      return AnalysisResult.unreachableBlockEnd();
    }
    return new AnalysisResult(finalSummaries, finalViolations, false);
  }
}
