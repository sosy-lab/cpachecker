// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis;

import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;
import static org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis.DssBlockAnalysis.blockStateOf;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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
 * <p>Each precondition group is explored under all known violation conditions. If any run finds an
 * error, the round reports all its violation conditions and no forward summary, including when
 * another group or a speculative run produced a different kind of result.
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
      // The initial run starts without a received precondition to discover local violations.
      // Forward summaries are computed when actual preconditions arrive.
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
  public AnalysisResult explore(boolean pViolationConditionsChanged)
      throws CPAException, InterruptedException {
    BlockToProgramLocationMap preconditions = preconditionHandler.getPreconditions();
    if (!pViolationConditionsChanged && preconditions.isUnreachable()) {
      // every predecessor reported an unreachable block end, so this block cannot be entered.
      // A new violation condition still explores the block, so that the successor asking about it
      // learns about violations reachable from the unconstrained start state.
      return AnalysisResult.unreachableBlockEnd();
    }
    ImmutableList<StateAndPrecision> received =
        ImmutableList.<StateAndPrecision>builder()
            .addAll(preconditions.getStatesAndPrecisions())
            .addAll(violationConditions.getKnownConditions())
            .build();
    Precision precisionOfAnalysis =
        received.isEmpty() ? analysis.makeStartPrecision() : analysis.combinePrecisions(received);

    List<AnalysisResult> rounds = new ArrayList<>();
    for (Object preconditionProgramPoint : preconditions.getAllProgramPoints()) {
      rounds.add(
          exploreFrom(
              preconditions.getStatesPerLocation(preconditionProgramPoint),
              violationConditions.states(),
              precisionOfAnalysis,
              false));
    }
    AnalysisResult result = AnalysisResult.merge(rounds);
    if (!result.violationConditions().isEmpty()) {
      // Real preconditions already provide errors to propagate. Speculation is only needed
      // when these runs do not find a violation.
      return result;
    }
    if (preconditions.isEmpty() || preconditions.isAnyPredecessorTrulyEmpty()) {
      // A predecessor that has not sent anything yet does not restrict the block entry.
      // Explore speculatively to propagate violations before its precondition arrives.
      AnalysisResult topExploration =
          exploreFrom(
              ImmutableSet.of(analysis.makeStartState(true)),
              violationConditions.states(),
              precisionOfAnalysis,
              true);
      rounds.add(topExploration);
    }
    return AnalysisResult.merge(rounds);
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
    statesToProcess = analysis.combineStates(statesToProcess);

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
      if (pDiscardSummaries) {
        // A speculative round throws its summaries away by construction, so finding nothing says
        // that it found no violation -- not that the block end is out of reach. Reporting it as
        // unreachable would be a claim about a block this round did not even enter from its real
        // preconditions, and successors would take it as proof that they can never be entered.
        return AnalysisResult.empty();
      }
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

    if (!finalViolations.isEmpty()) {
      return AnalysisResult.ofViolationConditions(finalViolations);
    }
    return AnalysisResult.ofSummaries(finalSummaries);
  }
}
