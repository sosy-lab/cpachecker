// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.termination;

import com.google.common.base.Preconditions;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class TerminationAbstractDomain implements AbstractDomain {

  private final AbstractDomain abstractDomain;

  public TerminationAbstractDomain(AbstractDomain pAbstractDomain) {
    abstractDomain = Preconditions.checkNotNull(pAbstractDomain);
  }

  @Override
  public TerminationState join(AbstractState pState1, AbstractState pState2)
      throws CPAException, InterruptedException {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isLessOrEqual(AbstractState pState1, AbstractState pState2)
      throws CPAException, InterruptedException {
    TerminationState state1 = (TerminationState) pState1;
    TerminationState state2 = (TerminationState) pState2;
    AbstractState wrappedState1 = state1.getWrappedState();
    AbstractState wrappedState2 = state2.getWrappedState();

    return state1.isPartOfLoop() == state2.isPartOfLoop()
        && abstractDomain.isLessOrEqual(wrappedState1, wrappedState2);
  }
}
