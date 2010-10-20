package org.sosy_lab.cpachecker.fllesh.cpa.composite;

import java.util.ArrayList;
import java.util.List;

import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.JoinOperator;
import org.sosy_lab.cpachecker.core.interfaces.PartialOrder;

public class CompoundDomain implements AbstractDomain {

  private CompoundElement mTopElement;
  private CompoundJoinOperator mJoinOperator;
  private CompoundPartialOrder mPartialOrder;
  
  public CompoundDomain(List<AbstractDomain> pSubdomains) {
    
    List<AbstractElement> lTopElements = new ArrayList<AbstractElement>(pSubdomains.size());
    List<JoinOperator> lJoinOperators = new ArrayList<JoinOperator>(pSubdomains.size());
    List<PartialOrder> lPartialOrders = new ArrayList<PartialOrder>(pSubdomains.size());
    
    for (AbstractDomain lSubdomain : pSubdomains) {
      lTopElements.add(lSubdomain.getTopElement());
      lJoinOperators.add(lSubdomain.getJoinOperator());
      lPartialOrders.add(lSubdomain.getPartialOrder());
    }
    
    mTopElement = new CompoundElement(lTopElements);
    mJoinOperator = new CompoundJoinOperator(lJoinOperators);
    mPartialOrder = new CompoundPartialOrder(lPartialOrders);
    
  }

  @Override
  public JoinOperator getJoinOperator() {
    return mJoinOperator;
  }

  @Override
  public PartialOrder getPartialOrder() {
    return mPartialOrder;
  }

  @Override
  public AbstractElement getTopElement() {
    return mTopElement;
  }

}
