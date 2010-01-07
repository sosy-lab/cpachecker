package exceptions;

import cfa.objectmodel.CFAEdge;
import cfa.objectmodel.CFANode;
import cpa.common.interfaces.AbstractElement;

public class TransferTimeOutException extends Exception {

  private static final long serialVersionUID = -8621199013744809508L;
  
  private final CFAEdge cfaEdge;
  private final AbstractElement abstractElement;
  
  public TransferTimeOutException(CFAEdge pCfaEdge, AbstractElement pAbstractElement){
    super();
    cfaEdge = pCfaEdge;
    abstractElement = pAbstractElement;
  }

  public CFAEdge getCfaEdge() {
    return cfaEdge;
  }

  public AbstractElement getAbstractElement() {
    return abstractElement;
  }
  
}
