package cpa.art;

import java.util.List;

import cfa.objectmodel.CFAEdge;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.AbstractElementWithLocation;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.TransferRelation;
import exceptions.CPAException;
import exceptions.CPATransferException;

public class ArtTransferRelation implements TransferRelation {
  
  

  @Override
  public AbstractElement getAbstractSuccessor(AbstractElement pPrevElement,
      CFAEdge pCfaEdge, Precision pPrecision) throws CPATransferException {
    AbstractElement abstractElement = ((ArtElement)pPrevElement).getParent();
    ArtElement parent = (ArtElement)pPrevElement;
    return new ArtElement(abstractElement, parent);
  }

  @Override
  public List<AbstractElementWithLocation> getAllAbstractSuccessors(
      AbstractElementWithLocation pElement, Precision pPrecision)
      throws CPAException, CPATransferException {
    throw new CPAException ("Cannot get all abstract successors from non-location domain");
  }

}
