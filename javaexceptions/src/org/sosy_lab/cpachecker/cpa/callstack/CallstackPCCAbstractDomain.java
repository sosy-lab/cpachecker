// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.callstack;

import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class CallstackPCCAbstractDomain implements AbstractDomain {

  @Override
  public AbstractState join(AbstractState pState1, AbstractState pState2)
      throws CPAException, InterruptedException {
    return pState2;
  }

  @Override
  public boolean isLessOrEqual(AbstractState pState1, AbstractState pState2)
      throws CPAException, InterruptedException {
    if (pState1 instanceof CallstackState && pState2 instanceof CallstackState) {
      return isLessOrEqual((CallstackState) pState1, (CallstackState) pState2);
    }
    return false;
  }

  private boolean isLessOrEqual(CallstackState state1, CallstackState state2) {
    if (state1 == state2) {
      return true;
    }
    if (state1 == null || state2 == null) {
      return false;
    }
    if (state1.sameStateInProofChecking(state2)) {
      return true;
    }
    return false;
  }
}
