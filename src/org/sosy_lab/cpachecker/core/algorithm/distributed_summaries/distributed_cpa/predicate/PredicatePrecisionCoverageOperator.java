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
   * Check whether precision1 is subsumed by precision2, i.e., whether precision2 tracks a superset
   * of the predicates of precision1. Predicates count as different when they are tracked at
   * different locations, so a predicate that precision2 only tracks locally does not cover the same
   * predicate tracked globally by precision1.
   *
   * <p>This is a syntactic check on the tracked predicates, not a semantic one: two precisions that
   * track equivalent but differently written predicates do not cover each other. Since a missed
   * cover only means that the block is analyzed again, this is on the safe side.
   *
   * @param precision1 First precision
   * @param precision2 Second precision
   * @return True if precision1 is subsumed by precision2
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
    return predicatePrecision2.calculateDifferenceTo(predicatePrecision1) == 0;
  }
}
