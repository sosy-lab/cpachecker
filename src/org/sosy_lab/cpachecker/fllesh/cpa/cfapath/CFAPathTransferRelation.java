package org.sosy_lab.cpachecker.fllesh.cpa.cfapath;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

public class CFAPathTransferRelation implements TransferRelation {
  
  @Override
  public Collection<? extends AbstractElement> getAbstractSuccessors(
      AbstractElement pElement, Precision pPrecision, CFAEdge pCfaEdge)
      throws CPATransferException {
    if (pElement.equals(CFAPathBottomElement.getInstance())) {
      return Collections.emptySet();
    }
    
    if (pElement.equals(CFAPathTopElement.getInstance())) {
      return Collections.singleton(CFAPathTopElement.getInstance());
    }
    
    if (!(pElement instanceof CFAPathStandardElement)) {
      throw new IllegalArgumentException();
    }
    
    CFAPathStandardElement lCurrentElement = (CFAPathStandardElement)pElement;
    
    CFAPathStandardElement lSuccessor = new CFAPathStandardElement(lCurrentElement, pCfaEdge);
    
    return Collections.singleton(lSuccessor);
  }

  @Override
  public Collection<? extends AbstractElement> strengthen(
      AbstractElement pElement, List<AbstractElement> pOtherElements,
      CFAEdge pCfaEdge, Precision pPrecision) throws CPATransferException {
    
    return null;
  }

}
