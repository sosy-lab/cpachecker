package org.sosy_lab.cpachecker.cpa.abm;

import org.sosy_lab.common.Timer;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElementHash;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;

import de.upb.agw.cpachecker.cpa.abm.util.CachedSubtree;
import de.upb.agw.cpachecker.cpa.abm.util.CachedSubtreeManager;

class TimedReducer implements Reducer {

  final Timer reduceTime = new Timer();
  final Timer expandTime = new Timer();
  
  private final Reducer wrappedReducer;
  
  public TimedReducer(Reducer pWrappedReducer) {
    wrappedReducer = pWrappedReducer;
  }

  @Override
  public AbstractElement getVariableReducedElement(
      AbstractElement pExpandedElement, CachedSubtree pContext,
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
      AbstractElement pRootElement, CachedSubtree pRootContext,
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
  public AbstractElementHash getHashCodeForElement(AbstractElement pElementKey,
      Precision pPrecisionKey, CachedSubtree pContext,
      CachedSubtreeManager pCsmgr) {
    return wrappedReducer.getHashCodeForElement(pElementKey, pPrecisionKey, pContext, pCsmgr);
  }

}
