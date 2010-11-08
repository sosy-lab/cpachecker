package org.sosy_lab.cpachecker.fllesh.cpa.symbpredabsCPA;

import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;

public class SymbPredAbsBottomElement implements AbstractElement {

  public static final SymbPredAbsBottomElement INSTANCE = new SymbPredAbsBottomElement();
  
  private SymbPredAbsBottomElement() {
    
  }
  
  @Override
  public String toString() {
    return "<BOTTOM>";
  }
  
}
