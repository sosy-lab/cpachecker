// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.constraints.domain;

import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.constraints.ConstraintsCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * Less-or-equal operator for {@link ConstraintsCPA} that defines less-or-equal as <code>
 * c less or equal c' iff c' subset of c</code> with <code>c, c'</code> being ConstraintsStates.
 */
public class SubsetLessOrEqualOperator implements AbstractDomain {

  private static final SubsetLessOrEqualOperator SINGLETON = new SubsetLessOrEqualOperator();

  private SubsetLessOrEqualOperator() {
    // DO NOTHING
  }

  public static SubsetLessOrEqualOperator getInstance() {
    return SINGLETON;
  }

  @Override
  public boolean isLessOrEqual(final AbstractState pLesserState, final AbstractState pBiggerState) {
    assert pLesserState instanceof ConstraintsState;
    assert pBiggerState instanceof ConstraintsState;

    ConstraintsState lesserState = (ConstraintsState) pLesserState;
    ConstraintsState biggerState = (ConstraintsState) pBiggerState;

    if (biggerState.size() > lesserState.size()) {
      return false;
    }

    return lesserState.containsAll(biggerState);
  }

  @Override
  public AbstractState join(AbstractState state1, AbstractState state2)
      throws CPAException, InterruptedException {
    throw new UnsupportedOperationException("ConstraintsCPA's domain does not support join");
  }
}
