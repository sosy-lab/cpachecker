package exceptions;

import cfa.objectmodel.CFAEdge;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.Precision;

public class TransferTimeOutException extends CPATransferException {

  private static final long serialVersionUID = -8621199013744809508L;
  
  private final CFAEdge cfaEdge;
  private final AbstractElement abstractElement;
  private final Precision precision;
  
  public TransferTimeOutException(CFAEdge pCfaEdge, AbstractElement pAbstractElement, Precision pPrecision){
    super("transfer timed out for edge " + pCfaEdge.toString());
    cfaEdge = pCfaEdge;
    abstractElement = pAbstractElement;
    precision = pPrecision;
  }

  public CFAEdge getCfaEdge() {
    return cfaEdge;
  }

  public AbstractElement getAbstractElement() {
    return abstractElement;
  }
  
  public Precision getPrecision() {
    return precision;
  }
  
}
