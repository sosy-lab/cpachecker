package org.sosy_lab.cpachecker.fllesh.cpa.guardededgeautomaton.productautomaton;

import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.JoinOperator;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class ProductAutomatonJoinOperator implements JoinOperator {

  private static final ProductAutomatonJoinOperator sInstance = new ProductAutomatonJoinOperator();
  
  public static ProductAutomatonJoinOperator getInstance() {
    return sInstance;
  }
  
  private ProductAutomatonJoinOperator() {
    
  }
  
  @Override
  public AbstractElement join(AbstractElement pElement1,
      AbstractElement pElement2) throws CPAException {
    
    if (!(pElement1 instanceof ProductAutomatonElement)) {
      throw new IllegalArgumentException();
    }
    
    if (!(pElement2 instanceof ProductAutomatonElement)) {
      throw new IllegalArgumentException();
    }
    
    if (pElement1.equals(pElement2)) {
      return pElement1;
    }
    
    if (pElement1.equals(ProductAutomatonTopElement.getInstance())) {
      return pElement1;
    }
    
    if (pElement2.equals(ProductAutomatonTopElement.getInstance())) {
      return pElement2;
    }
    
    return ProductAutomatonUndeterminedElement.getInstance();
  }

}
