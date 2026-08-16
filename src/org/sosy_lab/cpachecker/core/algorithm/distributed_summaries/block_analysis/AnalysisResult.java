// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis;

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

  /** A round that reached the block end, so the summaries describe it. */
  AnalysisResult(
      Collection<StateAndPrecision> pSummaries, Set<ArgPathAndCondition> pViolationConditions) {
    this(pSummaries, pViolationConditions, false);
  }
}
