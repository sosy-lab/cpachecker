package org.sosy_lab.cpachecker.fllesh.cpa.productautomaton;

import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.JoinOperator;

public class ProductAutomatonDomain implements AbstractDomain {
  
  private ProductAutomatonPartialOrder mPartialOrder;
  private ProductAutomatonJoinOperator mJoinOperator;
  
  public ProductAutomatonDomain() {
    mPartialOrder = new ProductAutomatonPartialOrder();
    mJoinOperator = new ProductAutomatonJoinOperator();
  }

  @Override
  public ProductAutomatonBottomElement getBottomElement() {
    return ProductAutomatonBottomElement.getInstance();
  }

  @Override
  public JoinOperator getJoinOperator() {
    return mJoinOperator;
  }

  @Override
  public ProductAutomatonPartialOrder getPartialOrder() {
    return mPartialOrder;
  }

  @Override
  public ProductAutomatonTopElement getTopElement() {
    return ProductAutomatonTopElement.getInstance();
  }
  
}
