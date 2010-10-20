package org.sosy_lab.cpachecker.fllesh.cpa.guardededgeautomaton.productautomaton;

import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.JoinOperator;

public class ProductAutomatonDomain implements AbstractDomain {
  
  private static final ProductAutomatonDomain sInstance = new ProductAutomatonDomain();
  
  public static ProductAutomatonDomain getInstance() {
    return sInstance;
  }
  
  private ProductAutomatonDomain() {
    
  }


  @Override
  public JoinOperator getJoinOperator() {
    return ProductAutomatonJoinOperator.getInstance();
  }

  @Override
  public ProductAutomatonPartialOrder getPartialOrder() {
    return ProductAutomatonPartialOrder.getInstance();
  }

  @Override
  public ProductAutomatonTopElement getTopElement() {
    return ProductAutomatonTopElement.getInstance();
  }
  
}
