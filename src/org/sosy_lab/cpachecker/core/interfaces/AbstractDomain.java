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

import org.sosy_lab.cpachecker.exceptions.CPAException;

public interface AbstractDomain {

  /**
   * Returns the smallest state of the lattice that is greater than both
   * states (the join).
   *
   * This is an optional method. If a domain is expected to be used only with
   * merge-sep, it does not have to provide an implementation of this method.
   * This method should then throw an {@link UnsupportedOperationException}.
   *
   * @param state1 an abstract state
   * @param state2 an abstract state
   * @return the join of state1 and state2
   * @throws CPAException If any error occurred.
   * @throws UnsupportedOperationException If this domain does not provide a join method.
   * @throws InterruptedException If the operation could not complete due to a shutdown request.
   */
  public AbstractState join(AbstractState state1, AbstractState state2) throws CPAException, InterruptedException;

  /**
   * Returns true if state1 is less or equal than state2 with respect to
   * the lattice.
   *
   * The is-less-or-equal relation needs to be consistent with the equality relation
   * defined by {@link AbstractState#equals(Object)}, i.e.
   * {@code s1.equals(s2) ==> isLessOrEqual(s1, s2) && isLessOrEqual(s2, s1)}.
   *
   * @param state1 an abstract state
   * @param state2 an abstract state
   * @return (state1 <= state2)
   * @throws CPAException If any error occurred.
   * @throws InterruptedException If the operation could not complete due to a shutdown request.
   */
  public boolean isLessOrEqual(AbstractState state1, AbstractState state2) throws CPAException, InterruptedException;

}
