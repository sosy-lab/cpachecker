// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.invariants;

import com.google.common.collect.Iterables;
import java.util.Collection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.combine.CombineOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.invariants.InvariantsState;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class CombineInvariantsStateOperator implements CombineOperator {
  @Override
  public AbstractState combine(Collection<AbstractState> states)
      throws CPAException, InterruptedException {
    if (states.isEmpty()) {
      throw new IllegalArgumentException("Cannot combine empty collection of InvariantsStates");
    }
    InvariantsState first = ((InvariantsState) Iterables.get(states, 0));
    for (AbstractState t : Iterables.skip(states, 1)) {
      InvariantsState next = (InvariantsState) t;
      first = first.join(next);
    }

    return first;
  }
}
