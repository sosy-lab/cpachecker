package org.sosy_lab.cpachecker.cpa.alwaystop;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

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
