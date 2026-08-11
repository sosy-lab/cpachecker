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

public class CallstackStateCoverageOperator implements CoverageOperator {

  @Override
  public boolean isSubsumed(AbstractState state1, AbstractState state2) {
    CallstackState callstackState1 = (CallstackState) state1;
    CallstackState callstackState2 = (CallstackState) state2;
    boolean eq = callstackState1.sameStateInProofChecking(callstackState2);
    if (!eq) System.out.println(state1 + " != " + state2);
    return eq;
  }

  @Override
  public boolean isBasedOnEquality() {
    return true;
  }
}
