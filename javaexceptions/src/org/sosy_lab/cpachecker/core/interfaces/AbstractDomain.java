// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.interfaces;

import org.sosy_lab.cpachecker.exceptions.CPAException;

public interface AbstractDomain {

  /**
   * Returns the smallest state of the lattice that is greater than both states (the join).
   *
   * <p>This is an optional method. If a domain is expected to be used only with merge-sep, it does
   * not have to provide an implementation of this method. This method should then throw an {@link
   * UnsupportedOperationException}.
   *
   * @param state1 an abstract state
   * @param state2 an abstract state
   * @return the join of state1 and state2
   * @throws CPAException If any error occurred.
   * @throws UnsupportedOperationException If this domain does not provide a join method.
   * @throws InterruptedException If the operation could not complete due to a shutdown request.
   */
  AbstractState join(AbstractState state1, AbstractState state2)
      throws CPAException, InterruptedException;

  /**
   * Returns true if state1 is less or equal than state2 with respect to the lattice.
   *
   * <p>The is-less-or-equal relation needs to be consistent with the equality relation defined by
   * {@link AbstractState#equals(Object)}, i.e. {@code s1.equals(s2) ==> isLessOrEqual(s1, s2) &&
   * isLessOrEqual(s2, s1)}.
   *
   * @param state1 an abstract state
   * @param state2 an abstract state
   * @return (state1 <= state2)
   * @throws CPAException If any error occurred.
   * @throws InterruptedException If the operation could not complete due to a shutdown request.
   */
  boolean isLessOrEqual(AbstractState state1, AbstractState state2)
      throws CPAException, InterruptedException;
}
