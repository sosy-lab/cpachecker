package org.sosy_lab.cpachecker.cpa.art;

import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.cfa.blocks.BlockPartitioning;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;


public class ARTReducer implements Reducer {

  private final Reducer wrappedReducer;
  
  public ARTReducer(Reducer pWrappedReducer) {
    wrappedReducer = pWrappedReducer;
  }

  @Override
  public AbstractElement getVariableReducedElement(
      AbstractElement pExpandedElement, Block pContext,
      CFANode pLocation) {
    
    return new ARTElement(wrappedReducer.getVariableReducedElement(((ARTElement)pExpandedElement).getWrappedElement(), pContext, pLocation), null);
  }

  @Override
  public AbstractElement getVariableExpandedElement(
      AbstractElement pRootElement, Block pRootContext,
      AbstractElement pReducedElement) {

    return new ARTElement(wrappedReducer.getVariableExpandedElement(((ARTElement)pRootElement).getWrappedElement(), pRootContext, ((ARTElement)pReducedElement).getWrappedElement()), null);
  }

  @Override
  public boolean isEqual(AbstractElement pReducedTargetElement,
      AbstractElement pCandidateElement) {
    
    return wrappedReducer.isEqual(((ARTElement)pReducedTargetElement).getWrappedElement(), ((ARTElement)pCandidateElement).getWrappedElement());
  }

  @Override
  public Object getHashCodeForElement(AbstractElement pElementKey,
      Precision pPrecisionKey, Block pContext,
      BlockPartitioning pPartitioning) {
    
    return wrappedReducer.getHashCodeForElement(((ARTElement)pElementKey).getWrappedElement(), pPrecisionKey, pContext, pPartitioning);
  }

}
