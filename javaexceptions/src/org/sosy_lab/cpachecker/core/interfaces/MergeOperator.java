// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.interfaces;

import org.sosy_lab.cpachecker.core.algorithm.CPAAlgorithm;
import org.sosy_lab.cpachecker.core.defaults.MergeJoinOperator;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * This interface defines the merge operator used by {@link CPAAlgorithm}. This operator is used to
 * (optionally) merge newly-created abstract states with existing abstract states from the reached
 * set.
 *
 * <p>There are several default implementations available, that should be sufficient for many
 * analyses: {@link MergeSepOperator}, {@link MergeJoinOperator}.
 */
public interface MergeOperator {

  /**
   * The actual method for merging abstract states. Merging abstract states is defined by weakening
   * the state in the second parameter by taking information from the state in the first parameter.
   *
   * <p>This method may decide to not merge the states at all (i.e., returning simply the state from
   * the second input parameter), or to join them by delegating to {@link
   * AbstractDomain#join(AbstractState, AbstractState)}, or to somehow otherwise weaken the state
   * from the second input parameter. It may also decide to use any of these options only sometimes,
   * depending for example on the input states or the precision. For trivial cases, check the
   * default implementations of this class.
   *
   * <p>For soundness, the resulting state needs to be as least as abstract as the state in the
   * second parameter, i.e., state2 <= result <= top (as defined by the {@link
   * AbstractDomain#isLessOrEqual(AbstractState, AbstractState)} method).
   *
   * @param state1 The first input state.
   * @param state2 The second input state, from which the result is produced.
   * @param precision The precision.
   * @return An abstract state between state2 and the top state.
   */
  AbstractState merge(AbstractState state1, AbstractState state2, Precision precision)
      throws CPAException, InterruptedException;
}
