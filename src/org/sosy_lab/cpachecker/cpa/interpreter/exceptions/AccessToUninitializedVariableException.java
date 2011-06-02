package org.sosy_lab.cpachecker.cpa.interpreter.exceptions;

import org.sosy_lab.cpachecker.exceptions.CPATransferException;

public class AccessToUninitializedVariableException extends
    CPATransferException {

  /**
   *
   */
  private static final long serialVersionUID = -954636600896070300L;

  private final String mVariableName;

  public AccessToUninitializedVariableException(String pVariableName) {
    mVariableName = pVariableName;
  }

  @Override
  public String toString() {
    return "Access to uninitialized variable " + mVariableName;
  }

}
