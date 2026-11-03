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
 * Abstract domain for {@link LoopBoundState} that compares only the deepest loop iteration instead
 * of the whole per-loop iteration vector.
 *
 * <p>Two states are comparable as soon as they agree on the deepest iteration and on whether the
 * bound forced them to stop. States that merely visited a different <em>set</em> of loops therefore
 * become comparable, which is what allows merge^JOIN to collapse them.
 *
 * <p>This is deliberately coarser than {@link
 * org.sosy_lab.cpachecker.core.defaults.FlatLatticeDomain}: the per-loop counts of a state that was
 * merged from several states are no longer meaningful. Only use this domain for analyses that
 * inspect {@link LoopBoundState#getDeepestIteration()} and never the individual per-loop counts.
 */
enum LoopBoundDeepestIterationDomain implements AbstractDomain {
  INSTANCE;

  @Override
  public AbstractState join(AbstractState pState1, AbstractState pState2) {
    // Like FlatLatticeDomain this domain does not provide a join: the composite CPA only needs
    // isLessOrEqual for this component, because LoopBoundCPA uses merge^SEP.
    throw new UnsupportedOperationException("LoopBoundCPA does not support joining states");
  }

  @Override
  public boolean isLessOrEqual(AbstractState pState1, AbstractState pState2) {
    if (pState1 == pState2) {
      return true;
    }
    return pState1 instanceof LoopBoundState state1
        && pState2 instanceof LoopBoundState state2
        && state1.mustDumpAssumptionForAvoidance() == state2.mustDumpAssumptionForAvoidance()
        && state1.getDeepestIteration() == state2.getDeepestIteration();
  }
}
