// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.callstack;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import java.util.Collection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.combine.CombineViolationConditionsOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;

public class CallstackStateCombineViolationConditionOperator
    implements CombineViolationConditionsOperator {
  @Override
  public AbstractState combineViolationConditionsAtSameProgramHash(
      Collection<AbstractState> states) {
    if (states.size() == 1) {
      return Iterables.getOnlyElement(states);
    }
    CallstackState last = null;
    for (AbstractState state : states) {
      if (last == null) {
        last = (CallstackState) state;
      } else {
        Preconditions.checkState(last.sameStateInProofChecking((CallstackState) state));
      }
    }
    return last;
  }
}
