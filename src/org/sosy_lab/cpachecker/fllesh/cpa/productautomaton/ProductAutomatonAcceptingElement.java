package org.sosy_lab.cpachecker.fllesh.cpa.productautomaton;

public class ProductAutomatonAcceptingElement implements ProductAutomatonElement {

  private static ProductAutomatonAcceptingElement mInstance = new ProductAutomatonAcceptingElement();
  
  public static ProductAutomatonAcceptingElement getInstance() {
    return mInstance;
  }
  
  private ProductAutomatonAcceptingElement() {
    
  }
  
  @Override
  public boolean isError() {
    return true;
  }

}
