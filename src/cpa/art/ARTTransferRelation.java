package cpa.art;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import cfa.objectmodel.CFAEdge;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.AbstractElementWithLocation;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.TransferRelation;
import exceptions.CPATransferException;

public class ARTTransferRelation implements TransferRelation {
  
  private final TransferRelation transferRelation;
  
  public ARTTransferRelation(TransferRelation tr) {
    transferRelation = tr;
  }
  
  @Override
  public Collection<ARTElement> getAbstractSuccessors(
      AbstractElement pElement, Precision pPrecision, CFAEdge pCfaEdge)
      throws CPATransferException {
    ARTElement element = (ARTElement)pElement;
    
    AbstractElementWithLocation wrappedElement = element.getAbstractElementOnArtNode();
    Precision wrappedPrecision = null;
    if(pPrecision != null){
      wrappedPrecision = ((ARTPrecision)pPrecision).getPrecision();
    }
    Collection<? extends AbstractElement> successors = transferRelation.getAbstractSuccessors(wrappedElement, wrappedPrecision, pCfaEdge);
    if (successors.isEmpty()) {
      return Collections.emptySet();
    }
    
    Collection<ARTElement> wrappedSuccessors = new ArrayList<ARTElement>();
    for (AbstractElement absElement : successors) {
      ARTElement successorElem = new ARTElement(element.getCpa(), (AbstractElementWithLocation)absElement, element);
      wrappedSuccessors.add(successorElem);
    }
    return wrappedSuccessors;
  }

  @Override
  public AbstractElement strengthen(AbstractElement element,
                         List<AbstractElement> otherElements, CFAEdge cfaEdge,
                         Precision precision) {    
    return null;
  }
}
