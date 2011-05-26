package org.sosy_lab.cpachecker.cpa.callstack;

import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.cfa.blocks.BlockPartitioning;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElementHash;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;


public class CallstackReducer implements Reducer {

  @Override
  public AbstractElement getVariableReducedElement(
      AbstractElement pExpandedElement, Block pContext, CFANode callNode) {
    
    CallstackElement element = (CallstackElement)pExpandedElement;
        
    return copyCallstackUpToCallNode(element, callNode);
//    return new CallstackElement(null, element.getCurrentFunction(), location);
  }
  
  private CallstackElement copyCallstackUpToCallNode(CallstackElement element, CFANode callNode) {
    if (element.getCurrentFunction().equals(callNode.getFunctionName())) {
      return new CallstackElement(null, element.getCurrentFunction(), callNode);
    } else {
      assert element.getPreviousElement() != null;
      CallstackElement recursiveResult = copyCallstackUpToCallNode(element.getPreviousElement(), callNode);
      return new CallstackElement(recursiveResult, element.getCurrentFunction(), element.getCallNode());
    }
  }
  
  @Override
  public AbstractElement getVariableExpandedElement(
      AbstractElement pRootElement, Block pRootContext,
      AbstractElement pReducedElement) {
    
    CallstackElement rootElement = (CallstackElement)pRootElement;
    CallstackElement reducedElement = (CallstackElement)pReducedElement;
    
    // the stackframe on top of rootElement and the stackframe on bottom of reducedElement are the same function
    // now glue both stacks together at this element
    
    return copyCallstackExceptLast(rootElement, reducedElement);
  }
  
  private CallstackElement copyCallstackExceptLast(CallstackElement target, CallstackElement source) {
    if (source.getDepth() == 1) {
      assert source.getPreviousElement() == null;
      assert source.getCurrentFunction().equals(target.getCurrentFunction());
      return target;
    } else {
      CallstackElement recursiveResult = copyCallstackExceptLast(target, source.getPreviousElement());
      return new CallstackElement(recursiveResult, source.getCurrentFunction(), source.getCallNode());
    }
  }

  @Override
  public boolean isEqual(AbstractElement pReducedTargetElement,
      AbstractElement pCandidateElement) {
    
    CallstackElement reducedTargetElement = (CallstackElement)pReducedTargetElement;
    CallstackElement candidateElement = (CallstackElement)pCandidateElement;
    
    if (reducedTargetElement.getDepth() != candidateElement.getDepth()) {
      return false;
    }
    
    while (reducedTargetElement != null) {
      if ( !reducedTargetElement.getCallNode().equals(candidateElement.getCallNode())
        || !reducedTargetElement.getCurrentFunction().equals(candidateElement.getCurrentFunction())) {
          return false;
      }
      reducedTargetElement = reducedTargetElement.getPreviousElement();
      candidateElement = candidateElement.getPreviousElement();
    }
    
    return true;
  }

  @Override
  public AbstractElementHash getHashCodeForElement(
      AbstractElement pPredicateKey, Precision pPrecisionKey,
      Block pContext, BlockPartitioning pPartitioning) {
    
    // TODO Auto-generated method stub
    return null;
  }

}
