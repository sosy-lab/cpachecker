package org.sosy_lab.cpachecker.fllesh.cpa.productautomaton;

import org.sosy_lab.cpachecker.core.interfaces.Targetable;

public class ProductAutomatonAcceptingElement implements ProductAutomatonElement, Targetable {

  private static final ProductAutomatonAcceptingElement mInstance = new ProductAutomatonAcceptingElement();
  
  public static ProductAutomatonAcceptingElement getInstance() {
    return mInstance;
  }
  
  private ProductAutomatonAcceptingElement() {
    
  }
  
  @Override
  public String toString() {
    return "Accept";
  }

  @Override
  public boolean isTarget() {
    return true;
  }

}
