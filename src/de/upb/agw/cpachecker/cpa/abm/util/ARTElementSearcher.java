package de.upb.agw.cpachecker.cpa.abm.util;

import org.sosy_lab.common.Timer;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.cpa.art.ARTReachedSet;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackElement;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractElement;
import org.sosy_lab.cpachecker.util.AbstractElements;

import de.upb.agw.cpachecker.cpa.abm.predicate.TargetPredicateAbstractElement;

/**
 * Helper class to search for <code>ARTElement</code>s in a <code>ReachedSet</code> that have the same abstraction as the given <code>ARTElement</code> after reduction to the relevant predicates.
 * @author dwonisch
 *
 */
public class ARTElementSearcher {
  public static Timer searchForARTElementTimer = new Timer();

  public static ARTElement searchForARTElement(ARTReachedSet reached, ARTElement target, PredicateReducer reducer, CachedSubtreeManager manager) {
    return searchForARTElement(reached.asReachedSet(), target, reducer, manager);
  }
  
  public static ARTElement searchForARTElement(UnmodifiableReachedSet reached, ARTElement target, PredicateReducer reducer, CachedSubtreeManager manager) {
    PredicateAbstractElement targetPredicateElement = AbstractElements.extractElementByType(target, PredicateAbstractElement.class);
    CFANode targetNode = target.retrieveLocationElement().getLocationNode();
    CallstackElement targetCallstack = AbstractElements.extractElementByType(target, CallstackElement.class);
    return searchForARTElement(reached, targetPredicateElement, targetNode, targetCallstack, reducer, manager);
  }
  
  public static ARTElement searchForARTElement(UnmodifiableReachedSet reached, PredicateAbstractElement targetPredicateElement, CFANode targetNode, CallstackElement targetCallstack, PredicateReducer reducer, CachedSubtreeManager manager) {
    searchForARTElementTimer.start();
    try {
      if(targetPredicateElement instanceof TargetPredicateAbstractElement) {
        //shortcut
        return (ARTElement) reached.getLastElement();
      }
      CFANode callNode = ((ARTElement)reached.getFirstElement()).retrieveLocationElement().getLocationNode();
      PredicateAbstractElement reducedTarget = getVariableReducedElement(targetPredicateElement, manager == null?null:manager.getCachedSubtreeForCallNode(callNode), reducer);    
            
      for(AbstractElement element : reached.getReached()) {
        CallstackElement callstackElement = AbstractElements.extractElementByType(element, CallstackElement.class);
        PredicateAbstractElement currentPredicateElement = AbstractElements.extractElementByType(element, PredicateAbstractElement.class);
        CFANode currentNode = ((ARTElement)element).retrieveLocationElement().getLocationNode();      
        
        if(currentPredicateElement.getAbstractionFormula().asRegion().equals(reducedTarget.getAbstractionFormula().asRegion()) && currentNode.equals(targetNode) && (callstackElement.getDepth() == 1 || targetCallstack.getCallNode() == callstackElement.getCallNode())) {
          //assert result == null;
         // result = (ARTElement) element;
         return (ARTElement) element;
        }
      }    
      //assert result != null;
      //return result;
      return null;
    }
    finally {
      searchForARTElementTimer.stop();
    }
  }
  
  private static PredicateAbstractElement getVariableReducedElement(PredicateAbstractElement predicateAbstractElement, CachedSubtree context, PredicateReducer reducer) {
    if(reducer == null) {
      return predicateAbstractElement;
    }
    else {
      return reducer.getVariableReducedElement(predicateAbstractElement, context);
    }    
  }
}
