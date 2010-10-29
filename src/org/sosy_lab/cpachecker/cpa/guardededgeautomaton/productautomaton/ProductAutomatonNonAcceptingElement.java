package org.sosy_lab.cpachecker.cpa.guardededgeautomaton.productautomaton;

public class ProductAutomatonNonAcceptingElement implements
    ProductAutomatonElement {

  private static final ProductAutomatonNonAcceptingElement mInstance = new ProductAutomatonNonAcceptingElement();
  
  public static ProductAutomatonNonAcceptingElement getInstance() {
    return mInstance;
  }
  
  private ProductAutomatonNonAcceptingElement() {
    
  }

  @Override
  public String toString() {
    return "NonAccept";
  }

}
