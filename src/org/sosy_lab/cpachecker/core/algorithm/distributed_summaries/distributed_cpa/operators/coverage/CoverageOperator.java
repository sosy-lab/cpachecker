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
  boolean covers(AbstractState state1, AbstractState state2)
      throws CPAException, InterruptedException;
}
