package org.sosy_lab.cpachecker.cpa.abm;

import org.sosy_lab.common.Timer;
import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.cfa.blocks.BlockPartitioning;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;


class TimedReducer implements Reducer {

  final Timer reduceTime = new Timer();
  final Timer expandTime = new Timer();
  
  private final Reducer wrappedReducer;
  
  public TimedReducer(Reducer pWrappedReducer) {
    wrappedReducer = pWrappedReducer;
  }

  @Override
  public AbstractElement getVariableReducedElement(
      AbstractElement pExpandedElement, Block pContext,
      CFANode pCallNode) {

    reduceTime.start();
    try {
      return wrappedReducer.getVariableReducedElement(pExpandedElement, pContext, pCallNode);
    } finally {
      reduceTime.stop();
    }
  }

  @Override
  public AbstractElement getVariableExpandedElement(
      AbstractElement pRootElement, Block pRootContext,
      AbstractElement pReducedElement) {
    
    expandTime.start();
    try {
      return wrappedReducer.getVariableExpandedElement(pRootElement, pRootContext, pReducedElement);
    } finally {
      expandTime.stop();
    }
  }

  @Override
  public boolean isEqual(AbstractElement pReducedTargetElement,
      AbstractElement pCandidateElement) {
    return wrappedReducer.isEqual(pReducedTargetElement, pCandidateElement);
  }

  @Override
  public Object getHashCodeForElement(AbstractElement pElementKey,
      Precision pPrecisionKey, Block pContext,
      BlockPartitioning pPartitioning) {
    return wrappedReducer.getHashCodeForElement(pElementKey, pPrecisionKey, pContext, pPartitioning);
  }

}
