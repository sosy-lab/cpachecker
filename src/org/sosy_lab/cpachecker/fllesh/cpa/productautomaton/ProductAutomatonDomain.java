package org.sosy_lab.cpachecker.fllesh.cpa.productautomaton;

import org.sosy_lab.cpachecker.core.defaults.FlatLatticeDomain;

public class ProductAutomatonDomain extends FlatLatticeDomain {

  public ProductAutomatonDomain() {
    super(ProductAutomatonTopElement.getInstance(), ProductAutomatonBottomElement.getInstance());
  }
  
}
