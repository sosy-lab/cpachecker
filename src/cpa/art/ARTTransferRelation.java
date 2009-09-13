package cpa.art;

import java.util.ArrayList;
import java.util.List;

import cfa.objectmodel.CFAEdge;
import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.AbstractElementWithLocation;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.TransferRelation;
import exceptions.CPAException;
import exceptions.CPATransferException;

public class ARTTransferRelation implements TransferRelation {
  
  private final TransferRelation transferRelation;
  private AbstractDomain domain;
  
  public ARTTransferRelation(AbstractDomain pDomain, TransferRelation tr) {
    transferRelation = tr;
    domain = pDomain;
  }
  

  @Override
  public AbstractElement getAbstractSuccessor(AbstractElement pPrevElement,
      CFAEdge pCfaEdge, Precision pPrecision) throws CPATransferException {
//    AbstractElement abstractElement = ((ArtElement)pPrevElement).getParent();
//    ArtElement parent = (ArtElement)pPrevElement;
//    return new ArtElement((AbstractElementWithLocation)abstractElement, parent);
    // we don't use this method
    assert(false);
    return null;
  }

  @Override
  public List<AbstractElementWithLocation> getAllAbstractSuccessors(
      AbstractElementWithLocation pElement, Precision pPrecision)
      throws CPAException, CPATransferException {
    ARTElement element = (ARTElement)pElement;
    ARTCPA artCpa = (ARTCPA)((ARTDomain)domain).getCpa();
    
    if(artCpa.getRoot() == null){
      artCpa.setRoot(element);
    }
    
    AbstractElementWithLocation wrappedElement = element.getAbstractElementOnArtNode();
    Precision wrappedPrecision = null;
    if(pPrecision != null){
      wrappedPrecision = ((ARTPrecision)pPrecision).getPrecision();
    }
    List<AbstractElementWithLocation> successors = transferRelation.getAllAbstractSuccessors(wrappedElement, wrappedPrecision);
    List<AbstractElementWithLocation> wrappedSuccessors = new ArrayList<AbstractElementWithLocation>();
    for(AbstractElementWithLocation absElement:successors){
      ARTElement successorElem = new ARTElement(domain, absElement, element); 
      wrappedSuccessors.add(successorElem);
    }
    element.setMark();
    return wrappedSuccessors;
  }

  @Override
  public AbstractElement strengthen(AbstractElement element,
                         List<AbstractElement> otherElements, CFAEdge cfaEdge,
                         Precision precision) {    
    return null;
  }
}
