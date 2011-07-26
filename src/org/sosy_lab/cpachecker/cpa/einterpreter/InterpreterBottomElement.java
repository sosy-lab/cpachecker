package org.sosy_lab.cpachecker.cpa.einterpreter;

import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;

class InterpreterBottomElement implements AbstractElement {
  
  public static final InterpreterBottomElement INSTANCE = new InterpreterBottomElement();
  
  private InterpreterBottomElement() {
    
  }
  
  @Override
  public String toString() {
    return "InterpreterBottomElement";
  }
  
}
