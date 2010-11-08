package org.sosy_lab.cpachecker.fllesh.cpa.symbpredabsCPA;

import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;

public class SymbPredAbsTopElement implements AbstractElement {

  public static final SymbPredAbsTopElement INSTANCE = new SymbPredAbsTopElement();
  
  private SymbPredAbsTopElement() {
    
  }
  
  @Override
  public String toString() {
    return "<TOP>";
  }
  
}
