// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.induction;

import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.induction.InductionPrecision.LoopPrecision;

/**
 * Abstractions cover each other regardless of how often the path abstracted before, which gives the
 * fixpoints of precisions with one set of predicates per abstraction. All other states are only
 * covered by equal states, so that other analyses merge only states with equal counters. The
 * abstractions of an unrolling are only covered by the equal abstraction of the same block that
 * they were merged into, which lets them merge as native ISMC merges them, but a covered path never
 * hides another path from the concrete checks of the refiner, also if the ARG does not merge a
 * state that arrives late. The same holds for the ends of the blocks without abstraction.
 */
final class InductionDomain implements AbstractDomain {

  @Override
  public AbstractState join(AbstractState pState1, AbstractState pState2) {
    if (isLessOrEqual(pState1, pState2)) {
      return pState2;
    }
    throw new UnsupportedOperationException("InductionCPA does not join different states");
  }

  @Override
  public boolean isLessOrEqual(AbstractState pState1, AbstractState pState2) {
    InductionState state1 = (InductionState) pState1;
    InductionState state2 = (InductionState) pState2;
    if (!state1.isAbstraction() || !state2.isAbstraction()) {
      // The ends of the blocks without abstraction merge within a block, as in native IMC and ISMC.
      return state1.equals(state2)
          && (!state1.endsBlockWithoutAbstraction() || state1.wasMergedInto(state2));
    }
    if (state1.isInInductionStep() != state2.isInInductionStep()) {
      return false;
    }
    LoopPrecision precision1 = state1.getLoopPrecision();
    LoopPrecision precision2 = state2.getLoopPrecision();
    if (precision1.unrolling() || precision2.unrolling()) {
      return state1.equals(state2) && state1.wasMergedInto(state2);
    }
    // The induction argument needs one k for all induction steps that cover each other.
    return !state1.isInInductionStep()
        || (precision1.k() == precision2.k() && precision1.induction() == precision2.induction());
  }
}
