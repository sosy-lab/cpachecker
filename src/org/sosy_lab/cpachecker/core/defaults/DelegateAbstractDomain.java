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
    implements AbstractDomain<E> {
  private DelegateAbstractDomain() {}

  public static <E extends LatticeAbstractState<E>>
        DelegateAbstractDomain<E> getInstance() {
    return new DelegateAbstractDomain<>();
  }


  @Override
  public E join(E state1, E state2)
      throws CPAException {
    return state1.join(state2);
  }

  @Override
  public boolean isLessOrEqual(E state1, E state2)
      throws CPAException, InterruptedException {
    return state1.isLessOrEqual(state2);
  }
}
