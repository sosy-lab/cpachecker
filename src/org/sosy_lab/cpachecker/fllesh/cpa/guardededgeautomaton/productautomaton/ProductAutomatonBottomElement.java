package org.sosy_lab.cpachecker.fllesh.cpa.guardededgeautomaton.productautomaton;

public class ProductAutomatonBottomElement implements ProductAutomatonElement {

  private static final ProductAutomatonBottomElement mInstance = new ProductAutomatonBottomElement();
  
  public static ProductAutomatonBottomElement getInstance() {
    return mInstance;
  }
  
  private ProductAutomatonBottomElement() {
    
  }

}
