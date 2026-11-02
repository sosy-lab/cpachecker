// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.combine;

import java.util.Collection;
import java.util.Optional;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * An operator to combine multiple abstract states into a single abstract state. This is useful in
 * distributed CPA settings where states from different analysis nodes need to be merged.
 *
 * <p>The resulting state must follow the contract that it over-approximates all input states.
 */
public interface CombinePreconditionsOperator {

  /**
   * Combines states at one program point only if their union is exactly representable.
   *
   * <p>This must not introduce additional concrete states. Domains closed under disjunction can opt
   * in; other domains may combine equal states. Unsupported combinations stay separate.
   *
   * @param states the states to combine; the default implementation declines every combination
   * @throws CPAException if a domain-specific combination fails
   * @throws InterruptedException if a domain-specific combination is interrupted
   */
  default Optional<AbstractState> combineIfPossible(Collection<AbstractState> states)
      throws CPAException, InterruptedException {
    return Optional.empty();
  }

  AbstractState combinePreconditions(Collection<AbstractState> states)
      throws CPAException, InterruptedException;
}
