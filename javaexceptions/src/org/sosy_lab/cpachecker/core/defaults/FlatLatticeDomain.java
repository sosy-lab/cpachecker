// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.defaults;

import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * This class implements a domain for CPAs, where the partial order is identical to the equality
 * relation, if both of the two operands are neither bottom nor top. The resulting lattice is a
 * layered graph with three layers (one for top, one for bottom and one for all other states) and
 * edges only between different layers.
 */
public class FlatLatticeDomain implements AbstractDomain {
  private final AbstractState mTopState;

  private static class TopState implements AbstractState {
    @Override
    public String toString() {
      return "<TOP>";
    }
  }

  public FlatLatticeDomain(AbstractState pTopState) {
    assert pTopState != null;

    mTopState = pTopState;
  }

  public FlatLatticeDomain() {
    this(new TopState());
  }

  @Override
  public AbstractState join(AbstractState pState1, AbstractState pState2) throws CPAException {
    if (isLessOrEqual(pState1, pState2)) {
      return pState2;
    }

    if (isLessOrEqual(pState2, pState1)) {
      return pState1;
    }

    return mTopState;
  }

  @Override
  public boolean isLessOrEqual(AbstractState newState, AbstractState reachedState)
      throws CPAException {
    return (mTopState.equals(reachedState) || newState.equals(reachedState));
  }
}
