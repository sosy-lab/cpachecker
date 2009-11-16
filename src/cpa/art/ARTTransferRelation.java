package cpa.art;

import java.util.ArrayList;
import java.util.List;

import cfa.objectmodel.CFAEdge;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.AbstractElementWithLocation;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.TransferRelation;
import exceptions.CPAException;
import exceptions.CPATransferException;
import exceptions.TransferRelationException;

public class ARTTransferRelation implements TransferRelation {
  
  private final TransferRelation transferRelation;
  
  public ARTTransferRelation(TransferRelation tr) {
    transferRelation = tr;
  }
  

  @Override
  public AbstractElement getAbstractSuccessor(AbstractElement pPrevElement,
      CFAEdge pCfaEdge, Precision pPrecision) throws CPATransferException {
    // we don't use this method because this CPA contains AbstractElementsWithLocation
    throw new TransferRelationException("ARTTransferRelation does not support getAbstractSuccessor()");
  }

  @Override
  public List<AbstractElementWithLocation> getAllAbstractSuccessors(
      AbstractElementWithLocation pElement, Precision pPrecision)
      throws CPAException, CPATransferException {
    ARTElement element = (ARTElement)pElement;
    
    AbstractElementWithLocation wrappedElement = element.getAbstractElementOnArtNode();
    Precision wrappedPrecision = null;
    if(pPrecision != null){
      wrappedPrecision = ((ARTPrecision)pPrecision).getPrecision();
    }
    List<AbstractElementWithLocation> successors = transferRelation.getAllAbstractSuccessors(wrappedElement, wrappedPrecision);
    List<AbstractElementWithLocation> wrappedSuccessors = new ArrayList<AbstractElementWithLocation>();
    for(AbstractElementWithLocation absElement:successors){
      ARTElement successorElem = new ARTElement(element.getCpa(), absElement, element);
      successorElem.setMark();
      wrappedSuccessors.add(successorElem);
    }
    // TODO moved up
//    element.setMark();
    return wrappedSuccessors;
  }

  @Override
  public AbstractElement strengthen(AbstractElement element,
                         List<AbstractElement> otherElements, CFAEdge cfaEdge,
                         Precision precision) {    
    return null;
  }
}
