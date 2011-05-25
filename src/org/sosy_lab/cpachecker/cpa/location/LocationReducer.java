package org.sosy_lab.cpachecker.cpa.location;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElementHash;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;

import de.upb.agw.cpachecker.cpa.abm.util.CachedSubtree;
import de.upb.agw.cpachecker.cpa.abm.util.CachedSubtreeManager;

public class LocationReducer implements Reducer {

  @Override
  public AbstractElement getVariableReducedElement(
      AbstractElement pExpandedElement, CachedSubtree pContext,
      CFANode pLocation) {

//    assert ((LocationElement)pExpandedElement).getLocationNode().equals(pLocation);
    return pExpandedElement;
  }

  @Override
  public AbstractElement getVariableExpandedElement(
      AbstractElement pRootElement, CachedSubtree pRootContext,
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
  public AbstractElementHash getHashCodeForElement(AbstractElement pElementKey,
      Precision pPrecisionKey, CachedSubtree pContext, CachedSubtreeManager csmgr) {
    // TODO Auto-generated method stub
    return null;
  }

}
