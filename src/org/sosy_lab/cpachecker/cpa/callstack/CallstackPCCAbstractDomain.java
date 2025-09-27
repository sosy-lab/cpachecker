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
    return pState1 instanceof CallstackState state1
        && pState2 instanceof CallstackState state2
        && isLessOrEqual(state1, state2);
  }

  private boolean isLessOrEqual(CallstackState state1, CallstackState state2) {
    if (state1 == state2) {
      return true;
    }
    if (state1 == null || state2 == null) {
      return false;
    }
    return state1.sameStateInProofChecking(state2);
  }
}
