// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.slab;

import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class SLABDomain implements AbstractDomain {

  private AbstractDomain wrappedDomain;

  public SLABDomain(AbstractDomain pWrappedDomain) {
    wrappedDomain = pWrappedDomain;
  }

  @Override
  public AbstractState join(AbstractState pState1, AbstractState pState2)
      throws CPAException, InterruptedException {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isLessOrEqual(AbstractState pState1, AbstractState pState2)
      throws CPAException, InterruptedException {
    SLARGState state1 = (SLARGState) pState1;
    SLARGState state2 = (SLARGState) pState2;
    return wrappedSubsumption(state1, state2)
        && state2.getParents().containsAll(state1.getParents());
  }

  protected boolean wrappedSubsumption(
      AbstractSingleWrapperState pState1, AbstractSingleWrapperState pState2)
      throws CPAException, InterruptedException {
    return wrappedDomain.isLessOrEqual(pState1.getWrappedState(), pState2.getWrappedState());
  }
}
