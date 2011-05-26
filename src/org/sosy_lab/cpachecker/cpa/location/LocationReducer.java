package org.sosy_lab.cpachecker.cpa.location;

import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.cfa.blocks.BlockPartitioning;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;


public class LocationReducer implements Reducer {

  @Override
  public AbstractElement getVariableReducedElement(
      AbstractElement pExpandedElement, Block pContext,
      CFANode pLocation) {

//    assert ((LocationElement)pExpandedElement).getLocationNode().equals(pLocation);
    return pExpandedElement;
  }

  @Override
  public AbstractElement getVariableExpandedElement(
      AbstractElement pRootElement, Block pRootContext,
      AbstractElement pReducedElement) {

    return pReducedElement;
  }

  @Override
  public boolean isEqual(AbstractElement pReducedTargetElement,
      AbstractElement pCandidateElement) {
    
    LocationElement reducedTargetElement = (LocationElement)pReducedTargetElement;
    LocationElement candidateElement = (LocationElement)pCandidateElement;
    
    return candidateElement.getLocationNode().equals(reducedTargetElement.getLocationNode());
  }

  @Override
  public Object getHashCodeForElement(AbstractElement pElementKey,
      Precision pPrecisionKey, Block pContext, BlockPartitioning pPartitioning) {
    return ((LocationElement)pElementKey).getLocationNode();
  }

}
