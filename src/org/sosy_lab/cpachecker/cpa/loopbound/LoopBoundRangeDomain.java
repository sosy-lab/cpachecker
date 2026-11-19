// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.loopbound;

import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;

/**
 * Abstract domain for {@link LoopBoundState} that allows states with different per-loop iteration
 * counts to be merged.
 *
 * <p>A merged state keeps, for each loop, the smallest and the largest iteration count of the
 * merged states, so the iteration counts of every represented path stay within these bounds,
 * however often the loops are iterated afterwards. {@link LoopBoundState#getDeepestIteration()}
 * reports the lower bound, so a merged state is only cut off by the loop bound once all of its
 * paths exceed the bound. Analyses that select states by their iteration (see {@link
 * org.sosy_lab.cpachecker.core.interfaces.LoopIterationReportingState}) have to take the upper
 * bounds into account as well.
 *
 * <p>Fewer iterations mean more future paths below the loop bound, so a state covers another one if
 * its iteration ranges enclose those of the other one.
 */
enum LoopBoundRangeDomain implements AbstractDomain {
  INSTANCE;

  @Override
  public AbstractState join(AbstractState pState1, AbstractState pState2) {
    return ((LoopBoundState) pState2).joinIterationRanges((LoopBoundState) pState1);
  }

  @Override
  public boolean isLessOrEqual(AbstractState pState1, AbstractState pState2) {
    return pState1 == pState2
        || ((LoopBoundState) pState2).coversIterationRanges((LoopBoundState) pState1);
  }
}
