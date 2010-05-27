package org.sosy_lab.cpachecker.fllesh.cpa.productautomaton;

public class ProductAutomatonNonAcceptingElement implements
    ProductAutomatonElement {

  private static ProductAutomatonNonAcceptingElement mInstance = new ProductAutomatonNonAcceptingElement();
  
  public static ProductAutomatonNonAcceptingElement getInstance() {
    return mInstance;
  }
  
  private ProductAutomatonNonAcceptingElement() {
    
  }
  
  @Override
  public boolean isError() {
    return false;
  }
  
  @Override
  public String toString() {
    return "NonAccept";
  }

}
