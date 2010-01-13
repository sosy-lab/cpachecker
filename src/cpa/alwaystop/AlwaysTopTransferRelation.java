package cpa.alwaystop;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import cfa.objectmodel.CFAEdge;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.TransferRelation;
import exceptions.CPATransferException;

public class AlwaysTopTransferRelation implements TransferRelation {

  private static AlwaysTopTransferRelation mInstance = new AlwaysTopTransferRelation();
  
  private AlwaysTopTransferRelation() {
    
  }
  
  public static AlwaysTopTransferRelation getInstance() {
    return mInstance;
  }
  
  @Override
  public Collection<? extends AbstractElement> getAbstractSuccessors(
      AbstractElement pElement, Precision pPrecision, CFAEdge pCfaEdge)
      throws CPATransferException {
    assert(pElement != null);
    assert(pPrecision != null);
    assert(pCfaEdge != null);
    
    if (AlwaysTopBottomElement.getInstance().equals(pElement)) {
      return Collections.emptySet();
    }
    
    return Collections.singleton(AlwaysTopTopElement.getInstance());
  }

  @Override
  public Collection<? extends AbstractElement> strengthen(AbstractElement pElement,
      List<AbstractElement> pOtherElements, CFAEdge pCfaEdge,
      Precision pPrecision) throws CPATransferException {
    // TODO Auto-generated method stub
    return null;
  }

}
