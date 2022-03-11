// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
// SPDX-FileCopyrightText: 2014-2017 Université Grenoble Alpes
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.defaults;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * Interface for the abstract state which supports joining and partial order comparison.
 *
 * <p>Using this class in conjunction with {@link DelegateAbstractDomain} saves the user from
 * writing {@link org.sosy_lab.cpachecker.core.interfaces.AbstractDomain} implementation which just
 * delegates the method to the abstract state.
 */
public interface LatticeAbstractState<T extends LatticeAbstractState<T>> extends AbstractState {

  /**
   * Delegate method for convenience.
   *
   * <p>See {@link org.sosy_lab.cpachecker.core.interfaces.AbstractDomain#join} for the description.
   */
  T join(T other) throws CPAException, InterruptedException;

  /**
   * Delegate method for convenience.
   *
   * <p>See {@link org.sosy_lab.cpachecker.core.interfaces.AbstractDomain#isLessOrEqual} for the
   * description.
   */
  boolean isLessOrEqual(T other) throws CPAException, InterruptedException;
}
