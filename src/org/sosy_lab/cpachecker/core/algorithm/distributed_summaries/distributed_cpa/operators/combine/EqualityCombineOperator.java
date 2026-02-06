// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.combine;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import java.util.Collection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.coverage.CoverageOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class EqualityCombineOperator implements CombineOperator {

  private final CoverageOperator coverageOperator;
  private final Class<? extends AbstractState> stateClass;

  public EqualityCombineOperator(
      CoverageOperator pCoverageOperator, Class<? extends AbstractState> pStateClass) {
    coverageOperator = pCoverageOperator;
    stateClass = pStateClass;
  }

  @Override
  public AbstractState combine(Collection<AbstractState> states)
      throws CPAException, InterruptedException {
    Preconditions.checkArgument(!states.isEmpty(), "There must be at least one state to combine.");
    Preconditions.checkArgument(
        states.stream().allMatch(stateClass::isInstance),
        "Can only combine classes from same type.");
    for (AbstractState state1 : states) {
      for (AbstractState state2 : states) {
        if (state1 == state2) {
          continue;
        }
        if (!coverageOperator.areStatesEqual(state1, state2)) {
          throw new CPAException("Can only combine completely identical states.");
        }
      }
    }
    return Iterables.get(states, 0);
  }
}
