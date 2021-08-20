// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.traceabstraction;

import com.google.common.base.Preconditions;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.exceptions.CPAException;

class TraceAbstractionAbstractDomain implements AbstractDomain {

  private AbstractDomain delegateDomain;

  TraceAbstractionAbstractDomain(AbstractDomain pDelegateDomain) {
    delegateDomain = pDelegateDomain;
  }

  @Override
  public AbstractState join(AbstractState pState1, AbstractState pState2)
      throws CPAException, InterruptedException {
    throw new UnsupportedOperationException(
        TraceAbstractionAbstractDomain.class.getSimpleName() + "does not support this method-call");
  }

  @Override
  public boolean isLessOrEqual(AbstractState pState1, AbstractState pState2)
      throws CPAException, InterruptedException {
    Preconditions.checkArgument(pState1 instanceof TraceAbstractionState);
    Preconditions.checkArgument(pState2 instanceof TraceAbstractionState);

    return delegateDomain.isLessOrEqual(
        ((TraceAbstractionState) pState1).getWrappedState(),
        ((TraceAbstractionState) pState2).getWrappedState());
  }
}
