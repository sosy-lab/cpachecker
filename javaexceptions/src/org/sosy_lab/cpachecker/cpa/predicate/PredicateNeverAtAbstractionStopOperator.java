// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate;

import java.util.Collection;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.exceptions.CPAException;

class PredicateNeverAtAbstractionStopOperator implements StopOperator {

  private final AbstractDomain domain;

  public PredicateNeverAtAbstractionStopOperator(AbstractDomain pDomain) {
    domain = pDomain;
  }

  @Override
  public boolean stop(
      AbstractState pState, Collection<AbstractState> pReached, Precision pPrecision)
      throws CPAException, InterruptedException {
    PredicateAbstractState e1 = (PredicateAbstractState) pState;
    for (AbstractState reachedState : pReached) {
      PredicateAbstractState e2 = (PredicateAbstractState) reachedState;
      if (e1.isAbstractionState()
          && e2.isAbstractionState()
          && !e1.getPreviousAbstractionState().equals(e2.getPreviousAbstractionState())) {
        continue;
      }
      if (domain.isLessOrEqual(e1, e2)) {
        return true;
      }
    }
    return false;
  }
}
