// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.callstack;

import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.coverage.CoverageOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;
import org.sosy_lab.cpachecker.cpa.callstack.DssCallstackState;

public class CallstackStateCoverageOperator implements CoverageOperator {

  @Override
  public boolean isSubsumed(AbstractState state1, AbstractState state2) {
    DssCallstackState callstackState1 = (DssCallstackState) state1;
    DssCallstackState callstackState2 = (DssCallstackState) state2;

    if (!callstackState2.canBeTopState()) {
      if (callstackState1.canBeTopState()) {
        return false;
      }
      return callstackState1.sameStateInProofChecking(callstackState2);
    }

    CallstackState curr2 = callstackState2.getWrappedState();
    CallstackState curr1 = callstackState1.getWrappedState();

    while (curr2.getPreviousState() != null) {
      if (!stackFrameEqual(curr1, curr2)) {
        return false;
      }
      curr2 = curr2.getPreviousState();
      curr1 = curr1.getPreviousState();
    }

    return true;
  }

  private boolean stackFrameEqual(CallstackState curr1, CallstackState curr2) {
    return (curr1 != null)
        && curr2.getCallNode().equals(curr1.getCallNode())
        && curr2.getCurrentFunction().equals(curr1.getCurrentFunction());
  }

  @Override
  public boolean isBasedOnEquality() {
    return false;
  }
}
