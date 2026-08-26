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
import org.sosy_lab.cpachecker.cpa.callstack.DssCallstackState;

public class CallstackStateCoverageOperator implements CoverageOperator {

  @Override
  public boolean isSubsumed(AbstractState state1, AbstractState state2) {
    DssCallstackState callstackState1 = (DssCallstackState) state1;
    DssCallstackState callstackState2 = (DssCallstackState) state2;
    if (callstackState1.allowsAllTransfers() && callstackState2.allowsAllTransfers()) {
      return true;
    }
    return callstackState1.sameStateInProofChecking(callstackState2);
  }

  @Override
  public boolean isBasedOnEquality() {
    return true;
  }
}
