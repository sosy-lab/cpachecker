// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.coverage;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public interface CoverageOperator {

  /**
   * Whether the concretization of state1 is a subset of the concretization of state2
   *
   * @param state1 First abstract state
   * @param state2 Second abstract state
   * @return Whether state1 <= state2
   */
  boolean isSubsumed(AbstractState state1, AbstractState state2)
      throws CPAException, InterruptedException;

  /**
   * Whether state1 covers state2 if and only if state1 and state2 are equal.
   *
   * @return True if the operator is based on equality, false otherwise.
   */
  boolean isBasedOnEquality();

  default boolean areStatesEqual(AbstractState state1, AbstractState state2)
      throws CPAException, InterruptedException {
    if (isSubsumed(state1, state2)) {
      return isBasedOnEquality() || isSubsumed(state2, state1);
    }
    return false;
  }
}
