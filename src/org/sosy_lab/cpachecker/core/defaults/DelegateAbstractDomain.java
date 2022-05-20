// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
// SPDX-FileCopyrightText: 2014-2017 Université Grenoble Alpes
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.defaults;

import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * Default implementation of the abstract domain which delegates the
 * {@link #join} and {@link #isLessOrEqual} methods to the {@link LatticeAbstractState}
 * implementation.
 *
 * @param <E> Parametrize by the {@link AbstractState} implementation used for
 * the analysis.
 */
public class DelegateAbstractDomain<E extends LatticeAbstractState<E>>
    implements AbstractDomain {
  private DelegateAbstractDomain() {}

  public static <E extends LatticeAbstractState<E>>
        DelegateAbstractDomain<E> getInstance() {
    return new DelegateAbstractDomain<>();
  }


  @Override
  @SuppressWarnings("unchecked")
  public AbstractState join(AbstractState state1, AbstractState state2)
      throws CPAException, InterruptedException {
    return ((E) state1).join((E) state2);
  }

  @Override
  @SuppressWarnings("unchecked")
  public boolean isLessOrEqual(AbstractState state1, AbstractState state2)
      throws CPAException, InterruptedException {
    return ((E) state1).isLessOrEqual((E) state2);
  }
}
