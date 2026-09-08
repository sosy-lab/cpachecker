// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis;

import static com.google.common.base.Preconditions.checkArgument;
import static org.sosy_lab.common.collect.Collections3.elementAndList;
import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;
import static org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis.DssBlockAnalysis.blockStateOf;

import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import java.util.Collection;
import java.util.LinkedHashMap;
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
 * group of equally-located preconditions at a time.
 *
 * <p>Preconditions and violation conditions are both grouped by program point, and every
 * combination of one precondition group with one condition group becomes its own CPA run. Because a
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
   * Explores the block from all known preconditions, one group of equally-located preconditions at
   * a time, and merges what the individual rounds found.
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
      // shared
      // by all of them, so the contexts have to be kept apart by exploring per group instead.
      conditionsPerLocation =
          Multimaps.index(
              violationConditions.statesOf(Optional.empty()),
              condition -> analysis.getDcpa().computeProgramPointId(condition));
    }

    Precision precisionOfAnalysis =
        analysis.getOptions().doResetPrecisionsForEveryRun() || preconditions.isEmpty()
            ? analysis.makeStartPrecision()
            : analysis.combinePrecisions(preconditions.getStatesAndPrecisions());

    Multimap<Object, Object> safeRuns = ArrayListMultimap.create();
    Map<ImmutableList<Object>, AnalysisResult> rounds = new LinkedHashMap<>();
    for (Object conditionProgramPoint : conditionsPerLocation.keySet()) {
      ImmutableList<AbstractState> conditionsAtLocation =
          conditionsPerLocation.get(conditionProgramPoint);
      for (Object preconditionProgramPoint : preconditions.getAllProgramPoints()) {
        AnalysisResult round =
            exploreFrom(
                preconditions.getStatesPerLocation(preconditionProgramPoint),
                conditionsAtLocation,
                precisionOfAnalysis,
                false);
        if (!round.summaries().isEmpty()) {
          safeRuns.put(preconditionProgramPoint, conditionProgramPoint);
        }
        rounds.put(ImmutableList.of(preconditionProgramPoint, conditionProgramPoint), round);
      }
    }
    for (Object preconditionProgramPoint : safeRuns.keySet()) {
      Collection<Object> vcProgramPoints = safeRuns.get(preconditionProgramPoint);
      if (vcProgramPoints.size() > 1) {
        vcProgramPoints.forEach(v -> rounds.remove(ImmutableList.of(preconditionProgramPoint, v)));
        AnalysisResult round =
            exploreFrom(
                preconditions.getStatesPerLocation(preconditionProgramPoint),
                FluentIterable.from(vcProgramPoints)
                    .transformAndConcat(conditionsPerLocation::get)
                    .toList(),
                precisionOfAnalysis,
                false);
        rounds.put(elementAndList(preconditionProgramPoint, vcProgramPoints), round);
      }
    }
    if (preconditions.isEmpty() || preconditions.isAnyPredecessorTrulyEmpty()) {
      // a predecessor that has not sent anything yet does not restrict the block entry, so
      // explore speculatively from the unconstrained start state to find violations early
      AnalysisResult topExploration =
          exploreFrom(
              ImmutableSet.of(analysis.makeStartState(true)),
              violationConditions.statesOf(Optional.empty()),
              precisionOfAnalysis,
              true);
      Preconditions.checkState(topExploration.summaries().isEmpty());
      rounds.put(ImmutableList.of(), topExploration);
    }
    return merge(rounds.values());
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
      Collection<AbstractState> pViolationConditions,
      Precision precision,
      boolean pDiscardSummaries)
      throws CPAException, InterruptedException {

    statesToProcess = transformedImmutableListCopy(statesToProcess, analysis.getDcpa()::reset);
    if (analysis.getOptions().combinePreconditionsByHash()) {
      statesToProcess =
          ImmutableList.of(
              analysis.getDcpa().getCombineOperator().combinePreconditions(statesToProcess));
    }

    ImmutableSet.Builder<StateAndPrecision> summaries = ImmutableSet.builder();
    ImmutableSet.Builder<ArgPathAndCondition> violations = ImmutableSet.builder();

    for (AbstractState state : statesToProcess) {
      DssBlockAnalysisResult result =
          analysis.runBlockAnalysis(
              analysis.getDcpa().reset(state), precision, pViolationConditions);

      if (!result.getAllViolations().isEmpty()) {
        violations.addAll(analysis.pathsWithCondition(result.getViolationConditionViolations()));
        violations.addAll(analysis.pathsFromOrigin(result.getTargetStates()));
      } else if (!pDiscardSummaries) {
        summaries.addAll(analysis.summariesOf(result));
      }
    }

    Set<StateAndPrecision> finalSummaries = summaries.build();
    Set<ArgPathAndCondition> finalViolations = violations.build();

    if (finalViolations.isEmpty() && finalSummaries.isEmpty()) {
      // the exploration produced no state at the final location
      return AnalysisResult.unreachableBlockEnd();
    }

    if (!finalViolations.isEmpty()) {
      // summaries found alongside a violation are discarded: the violation has to be resolved first
      return AnalysisResult.ofViolationConditions(finalViolations);
    }

    Set<AbstractState> violationsToConsider =
        FluentIterable.from(finalSummaries)
            .transform(sap -> blockStateOf(sap.state()))
            .filter(b -> !b.getHinderedByCallstack().isEmpty())
            .transformAndConcat(b -> b.getHinderedByCallstack())
            .toSet();

    if (!violationsToConsider.isEmpty() && !pDiscardSummaries) {
      finalViolations =
          exploreFrom(
                  ImmutableSet.of(analysis.makeStartState(true)),
                  pViolationConditions,
                  precision,
                  true)
              .violationConditions();
    }

    return new AnalysisResult(finalSummaries, finalViolations, false);
  }
}
