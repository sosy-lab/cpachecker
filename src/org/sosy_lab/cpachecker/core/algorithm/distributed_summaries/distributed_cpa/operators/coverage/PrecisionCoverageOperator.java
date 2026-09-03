// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.coverage;

import org.sosy_lab.cpachecker.core.interfaces.Precision;

/**
 * The counterpart of {@link CoverageOperator} for precisions.
 *
 * <p>An abstract state only tells half the story of what a block analysis computed: the same state
 * explored with a finer precision can yield a stronger result. A coverage check that ignores the
 * precision therefore drops information, because it lets a result that was obtained with a coarse
 * precision cover the request to analyze the same states with a finer one.
 */
public interface PrecisionCoverageOperator {

  /**
   * Whether an analysis run with {@code precision2} is at least as precise as one run with {@code
   * precision1}, i.e., whether {@code precision2} tracks everything that {@code precision1} tracks.
   *
   * <p>Note that this is the same direction as {@link CoverageOperator#isSubsumed}: the first
   * argument is the one that is covered. It is <em>not</em> the direction of the lattice of
   * abstract states, though, since a precision that tracks more makes the analysis more precise.
   *
   * @param precision1 First precision
   * @param precision2 Second precision
   * @return Whether precision1 <= precision2
   */
  boolean isSubsumed(Precision precision1, Precision precision2);
}
