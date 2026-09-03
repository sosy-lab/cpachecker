// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.predicate;

import com.google.common.base.Preconditions;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.coverage.PrecisionCoverageOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecision;

public class PredicatePrecisionCoverageOperator implements PrecisionCoverageOperator {

  /**
   * For a state B to cover state A, it has to be explored with at least the same precision as A
   *
   * <p>This is a syntactic check on the tracked predicates, not a semantic one: two precisions that
   * track equivalent but differently written predicates do not cover each other.
   *
   * @param precision1 The precision of the state that is to be covered.
   * @param precision2 The precision of the covering state, which has to track at least as much.
   * @return True if precision2 misses none of the predicates of precision1
   */
  @Override
  public boolean isSubsumed(Precision precision1, Precision precision2) {
    Preconditions.checkArgument(
        precision1 instanceof PredicatePrecision && precision2 instanceof PredicatePrecision,
        "Expected PredicatePrecisions, but got %s and %s",
        precision1,
        precision2);
    PredicatePrecision predicatePrecision1 = (PredicatePrecision) precision1;
    PredicatePrecision predicatePrecision2 = (PredicatePrecision) precision2;
    return predicatePrecision1.calculateDifferenceTo(predicatePrecision2) == 0;
  }
}
