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
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.util.Optional;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis.DssBlockAnalyses.DssBlockAnalysisResult;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis.StateAndPrecision;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * Explores the block once per precondition that a {@link PathBasedPreconditionHandler} marked as
 * changed, under all violation conditions at once.
 *
 * <p>Which preconditions those are is entirely the handler's decision: it groups them by the path
 * through the block graph they were produced along and only hands out the paths that the last
 * update actually affected, so this engine explores neither a context it has already seen nor one
 * that is covered by another.
 */
final class PathBasedExplorationEngine implements DssExplorationEngine {

  private final DssBlockAnalysis analysis;
  private final PathBasedPreconditionHandler preconditions;
  private final PathBasedViolationConditionHandler violationConditions;

  PathBasedExplorationEngine(
      DssBlockAnalysis pAnalysis,
      PathBasedPreconditionHandler pPreconditions,
      PathBasedViolationConditionHandler pViolationConditions) {
    analysis = pAnalysis;
    preconditions = pPreconditions;
    violationConditions = pViolationConditions;
  }

  @Override
  public AnalysisResult exploreInitially() throws CPAException, InterruptedException {
    // Before any message arrived, the handler holds exactly the unconstrained entry state.
    StateAndPrecision initialTopState =
        Iterables.getOnlyElement(preconditions.getKnownPreconditions());

    DssBlockAnalysisResult result =
        analysis.runInitialBlockAnalysis(initialTopState.state(), initialTopState.precision());

    // This run carries no violation condition, so every violation it finds originates inside this
    // block and is reported from its origin.
    return new AnalysisResult(
        analysis.finalLocationStatesOf(result),
        analysis.pathsFromOrigin(result.getAllViolations()));
  }

  @Override
  public AnalysisResult explore(Optional<String> pViolationConditionSender)
      throws CPAException, InterruptedException {
    pViolationConditionSender.ifPresent(
        sender ->
            checkArgument(
                !violationConditions.isEmptyFor(sender),
                "No violation condition found for sender ID: %s",
                sender));
    ImmutableList.Builder<StateAndPrecision> summaries = ImmutableList.builder();
    ImmutableSet.Builder<ArgPathAndCondition> violations = ImmutableSet.builder();

    for (StateAndPrecision precondition : preconditions.getPreconditionsToAnalyze()) {

      DssBlockAnalysisResult result =
          analysis.runBlockAnalysis(
              analysis.getDcpa().reset(precondition.state()),
              precondition.precision(),
              violationConditions.statesOf(pViolationConditionSender));

      summaries.addAll(analysis.leafSummariesOf(result));

      // TODO we only want to combine violations with the same precondition id
      if (!result.getAllViolations().isEmpty()) {
        violations.addAll(analysis.pathsWithCondition(result.getViolationConditionViolations()));
        violations.addAll(analysis.pathsFromOrigin(result.getTargetStates()));
      }
    }
    return new AnalysisResult(
        analysis.deduplicateStatesAndPrecisions(summaries.build()), violations.build());
  }
}
