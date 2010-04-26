package org.sosy_lab.cpachecker.exceptions;

import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;

public class CBMCException extends CPATransferException {

  private static final long serialVersionUID = -8621199013744809508L;
  
  private final AbstractElement abstractElement;
  
  public CBMCException(AbstractElement pAbstractElement){
    abstractElement = pAbstractElement;
  }

  public AbstractElement getAbstractElement() {
    return abstractElement;
  }
}