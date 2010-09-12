package org.sosy_lab.cpachecker.fllesh.cpa.productautomaton;

import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.PartialOrder;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class ProductAutomatonPartialOrder implements PartialOrder {

  private static final ProductAutomatonPartialOrder sInstance = new ProductAutomatonPartialOrder();
  
  public static ProductAutomatonPartialOrder getInstance() {
    return sInstance;
  }
  
  private ProductAutomatonPartialOrder() {
    
  }
  
  @Override
  public boolean satisfiesPartialOrder(AbstractElement pElement1,
      AbstractElement pElement2) throws CPAException {
    
    if (!(pElement1 instanceof ProductAutomatonElement)) {
      throw new IllegalArgumentException();
    }
    
    if (!(pElement2 instanceof ProductAutomatonElement)) {
      throw new IllegalArgumentException();
    }
    
    if (pElement1.equals(ProductAutomatonBottomElement.getInstance())) {
      return true;
    }
    
    if (pElement2.equals(ProductAutomatonTopElement.getInstance())) {
      return true;
    }
    
    if (pElement1.equals(pElement2)) {
      return true;
    }
    
    if (pElement1.equals(ProductAutomatonTopElement.getInstance())) {
      return false;
    }
    
    if (pElement2.equals(ProductAutomatonBottomElement.getInstance())) {
      return false;
    }
    
    if (pElement2.equals(ProductAutomatonUndeterminedElement.getInstance())) {
      return true;
    }
    
    return false;
  }

}
