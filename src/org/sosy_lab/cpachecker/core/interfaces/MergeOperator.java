/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.core.interfaces;

import org.sosy_lab.cpachecker.core.algorithm.CPAAlgorithm;
import org.sosy_lab.cpachecker.core.defaults.MergeJoinOperator;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * This interface defines the merge operator used by {@link CPAAlgorithm}.
 * This operator is used to (optionally) merge newly-created abstract states
 * with existing abstract states from the reached set.
 *
 * There are several default implementations available,
 * that should be sufficient for many analyses:
 * {@link MergeSepOperator}, {@link MergeJoinOperator}.
 */
public interface MergeOperator {

  /**
   * The actual method for merging abstract states.
   * Merging abstract states is defined by weakening the state in the second parameter
   * by taking information from the state in the first parameter.
   *
   * This method may decide to not merge the states at all
   * (i.e., returning simply the state from the second input parameter),
   * or to join them by delegating to {@link AbstractDomain#join(AbstractState, AbstractState)},
   * or to somehow otherwise weaken the state from the second input parameter.
   * It may also decide to use any of these options only sometimes,
   * depending for example on the input states or the precision.
   * For trivial cases, check the default implementations of this class.
   *
   * For soundness, the resulting state needs to be as least as abstract
   * as the state in the second parameter,
   * i.e., state2 <= result <= top
   * (as defined by the {@link AbstractDomain#isLessOrEqual(AbstractState, AbstractState)} method).
   *
   * @param state1 The first input state.
   * @param state2 The second input state, from which the result is produced.
   * @param precision The precision.
   * @return An abstract state between state2 and the top state.
   */
  public AbstractState merge(AbstractState state1, AbstractState state2, Precision precision)
      throws CPAException, InterruptedException;
}
