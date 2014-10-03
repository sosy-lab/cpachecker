package org.sosy_lab.cpachecker.cpa.stator.policy;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class PolicyMergeOperator implements MergeOperator{
  private final PolicyAbstractDomain domain;
  public PolicyMergeOperator(PolicyAbstractDomain d) {
    this.domain = d;
  }

  @Override
  public AbstractState merge(AbstractState newState, AbstractState prevState, Precision p)
      throws CPAException {

    AbstractState out = domain.join(newState, prevState);

    // Note: returning an exactly same state is important, due to the issues
    // with .equals() handling.
    if (out.equals(prevState)) {
      return prevState;
    }
    return out;
  }
}
