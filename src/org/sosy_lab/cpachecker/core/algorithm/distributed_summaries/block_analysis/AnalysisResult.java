// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Set;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis.StateAndPrecision;

/**
 * What one round of exploring a block produced, before it is turned into messages.
 *
 * @param summaries the postconditions to publish to successor blocks
 * @param violationConditions the violating paths to publish to predecessor blocks
 * @param blockEndUnreachable whether the block end turned out to be unreachable, i.e., the
 *     postcondition is {@code false}. This is tracked explicitly instead of being encoded as a top
 *     state among the {@code summaries}, because top is also a valid postcondition of a reachable
 *     but unconstrained block end.
 */
record AnalysisResult(
    Collection<StateAndPrecision> summaries,
    Set<ArgPathAndCondition> violationConditions,
    boolean blockEndUnreachable) {

  AnalysisResult {
    checkArgument(
        violationConditions.isEmpty() || (summaries.isEmpty() && !blockEndUnreachable),
        "A round with violations must not publish a postcondition");
    checkArgument(
        !blockEndUnreachable || summaries.isEmpty(),
        "An unreachable block end must not have reachable summaries");
  }

  /** Merges runs of one round, retaining every violation and publishing summaries only if safe. */
  static AnalysisResult merge(Collection<AnalysisResult> pRounds) {
    ImmutableSet.Builder<StateAndPrecision> summaries = ImmutableSet.builder();
    ImmutableSet.Builder<ArgPathAndCondition> violations = ImmutableSet.builder();
    boolean unreachable = !pRounds.isEmpty();
    for (AnalysisResult round : pRounds) {
      summaries.addAll(round.summaries());
      violations.addAll(round.violationConditions());
      unreachable &= round.blockEndUnreachable();
    }
    ImmutableSet<ArgPathAndCondition> allViolations = violations.build();
    if (!allViolations.isEmpty()) {
      return ofViolationConditions(allViolations);
    }
    return new AnalysisResult(summaries.build(), ImmutableSet.of(), unreachable);
  }

  /** A round without errors whose summaries describe the reachable block end. */
  static AnalysisResult ofSummaries(Collection<StateAndPrecision> pSummaries) {
    return new AnalysisResult(pSummaries, ImmutableSet.of(), false);
  }

  /** A round that produced nothing, e.g. because there was nothing to explore. */
  static AnalysisResult empty() {
    return new AnalysisResult(ImmutableSet.of(), ImmutableSet.of(), false);
  }

  /** A round whose block end is unreachable and reports false as its postcondition. */
  static AnalysisResult unreachableBlockEnd() {
    return new AnalysisResult(ImmutableSet.of(), ImmutableSet.of(), true);
  }

  /** A round with errors reports its violation conditions instead of a forward summary. */
  static AnalysisResult ofViolationConditions(Set<ArgPathAndCondition> pViolationConditions) {
    return new AnalysisResult(ImmutableSet.of(), pViolationConditions, false);
  }
}
