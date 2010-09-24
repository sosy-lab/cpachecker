package org.sosy_lab.cpachecker.cpa.interpreter;

import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;

public class InterpreterBottomElement implements AbstractElement {
  
  public static final InterpreterBottomElement INSTANCE = new InterpreterBottomElement();
  
  private InterpreterBottomElement() {
    
  }
  
  @Override
  public String toString() {
    return "InterpreterBottomElement";
  }
  
}
