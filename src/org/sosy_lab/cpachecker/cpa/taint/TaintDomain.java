// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.taint;

import static com.google.common.base.Preconditions.checkArgument;

import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.exceptions.CPAException;

class TaintDomain implements AbstractDomain {

  @Override
  public AbstractState join(AbstractState pState1, AbstractState pState2) throws CPAException {

    checkArgument(pState1 instanceof TaintState, "Wrong type for first state passed: %s", pState1);
    checkArgument(pState2 instanceof TaintState, "Wrong type for second state passed: %s", pState2);

    if (isLessOrEqual(pState1, pState2)) {
      return pState2;
    } else {
      TaintState taintState1 = (TaintState) pState1;
      TaintState taintState2 = (TaintState) pState2;
      return taintState1.union(taintState2);
    }
  }

  @Override
  public boolean isLessOrEqual(AbstractState pLhs, AbstractState pRhs) throws CPAException {

    checkArgument(pLhs instanceof TaintState, "Wrong type for left-hand state passed: %s", pLhs);
    checkArgument(pRhs instanceof TaintState, "Wrong type for right-hand state passed: %s", pRhs);

    TaintState taintState1 = (TaintState) pLhs;
    TaintState taintState2 = (TaintState) pRhs;

    return taintState1.containsAll(taintState2);
  }
}
