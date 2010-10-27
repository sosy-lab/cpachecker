package org.sosy_lab.cpachecker.fllesh.cpa.composite;

import java.util.List;

import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class CompoundDomain implements AbstractDomain {

  private CompoundJoinOperator mJoinOperator;
  private CompoundPartialOrder mPartialOrder;
  
  public CompoundDomain(List<AbstractDomain> domains) {
      this.mJoinOperator = new CompoundJoinOperator(domains);
      this.mPartialOrder = new CompoundPartialOrder(domains);
  }

  @Override
  public AbstractElement join(AbstractElement pElement1,
      AbstractElement pElement2) throws CPAException {
    return mJoinOperator.join(pElement1, pElement2);
  }

  @Override
  public boolean satisfiesPartialOrder(AbstractElement pElement1,
      AbstractElement pElement2) throws CPAException {
    return mPartialOrder.satisfiesPartialOrder(pElement1, pElement2);
  }
}
