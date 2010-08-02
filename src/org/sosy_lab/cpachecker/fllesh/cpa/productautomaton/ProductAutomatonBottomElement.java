package org.sosy_lab.cpachecker.fllesh.cpa.productautomaton;

public class ProductAutomatonBottomElement implements ProductAutomatonElement {

  private static ProductAutomatonBottomElement mInstance = new ProductAutomatonBottomElement();
  
  public static ProductAutomatonBottomElement getInstance() {
    return mInstance;
  }
  
  private ProductAutomatonBottomElement() {
    
  }

}
