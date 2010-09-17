package org.sosy_lab.cpachecker.cpa.interpreter;

import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;

public class InterpreterTopElement implements AbstractElement {
  
  public static final InterpreterTopElement INSTANCE = new InterpreterTopElement();
  
  private InterpreterTopElement() {
    
  }
  
  @Override
  public String toString() {
    return "InterpreterTopElement";
  }
  
}
