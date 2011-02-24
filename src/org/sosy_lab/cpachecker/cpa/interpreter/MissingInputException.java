package org.sosy_lab.cpachecker.cpa.interpreter;

import org.sosy_lab.cpachecker.exceptions.CPATransferException;

public class MissingInputException extends CPATransferException {

  /**
   * 
   */
  private static final long serialVersionUID = 1661234220479181479L;
  
  private final String mVariableName;
  
  public MissingInputException(String pVariableName) {
    mVariableName = pVariableName;
  }
  
  @Override
  public String toString() {
    return "Missing input for variable " + mVariableName;
  }

}
