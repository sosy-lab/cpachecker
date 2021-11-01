// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.traceabstraction;

import static java.util.Objects.requireNonNull;

import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.exceptions.CPAException;

class TraceAbstractionAbstractDomain implements AbstractDomain {

  private AbstractDomain delegateDomain;

  TraceAbstractionAbstractDomain(AbstractDomain pDelegateDomain) {
    delegateDomain = requireNonNull(pDelegateDomain);
  }

  @Override
  public boolean isLessOrEqual(AbstractState pState1, AbstractState pState2)
      throws CPAException, InterruptedException {
    TraceAbstractionState taState1 = (TraceAbstractionState) pState1;
    TraceAbstractionState taState2 = (TraceAbstractionState) pState2;

    PredicateAbstractState wrappedState1 = (PredicateAbstractState) taState1.getWrappedState();
    PredicateAbstractState wrappedState2 = (PredicateAbstractState) taState2.getWrappedState();

    boolean continueCheck = true;
    if (!wrappedState1.isAbstractionState() || !wrappedState2.isAbstractionState()) {
      // PredicateAbstractDomain checks the coverage of the contained abstraction formulae, which
      // are always <true> when both wrapped states are abstraction states. The result is
      // thus trivially true (since both abstraction-states are equal to each other),
      // meaning we can take a short-cut here.
      continueCheck = delegateDomain.isLessOrEqual(wrappedState1, wrappedState2);
    }

    if (!continueCheck) {
      return false;
    }

    return taState1.isLessOrEqual(taState2);
  }

  @Override
  public AbstractState join(AbstractState pState1, AbstractState pState2)
      throws CPAException, InterruptedException {
    throw new UnsupportedOperationException(
        TraceAbstractionAbstractDomain.class.getSimpleName() + "does not support this method-call");
  }
}
