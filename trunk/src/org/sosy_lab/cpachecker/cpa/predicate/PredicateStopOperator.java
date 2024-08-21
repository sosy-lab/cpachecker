// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate;

import org.sosy_lab.cpachecker.core.defaults.StopSepOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ForcedCoveringStopOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPAException;

class PredicateStopOperator extends StopSepOperator implements ForcedCoveringStopOperator {

  PredicateStopOperator(AbstractDomain pD) {
    super(pD);
  }

  @Override
  public boolean isForcedCoveringPossible(
      AbstractState pElement, AbstractState pReachedState, Precision pPrecision)
      throws CPAException {

    // We support forced covering, so this is always possible,
    // if we have two abstraction elements.
    // Note that this does not say that the element will actually be covered,
    // it says only that we can try to cover it.
    return ((PredicateAbstractState) pElement).isAbstractionState()
        && ((PredicateAbstractState) pReachedState).isAbstractionState();
  }
}
