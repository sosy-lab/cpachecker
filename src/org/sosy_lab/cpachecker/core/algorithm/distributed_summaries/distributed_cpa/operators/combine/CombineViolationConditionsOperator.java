// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.combine;

import java.util.Collection;
import java.util.Optional;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public interface CombineViolationConditionsOperator {

  /**
   * Attempts a union at one program point. A domain with additional compatibility requirements may
   * decline; callers must then retain all original conditions.
   */
  default Optional<AbstractState> combineIfPossible(Collection<AbstractState> states)
      throws InterruptedException, CPAException {
    return Optional.of(combineViolationConditionsAtSameProgramHash(states));
  }

  AbstractState combineViolationConditionsAtSameProgramHash(Collection<AbstractState> states)
      throws InterruptedException, CPAException;
}
