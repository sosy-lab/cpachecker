package org.sosy_lab.cpachecker.cpa.abm;

import static org.sosy_lab.cpachecker.util.AbstractElements.filterLocation;

import org.sosy_lab.common.Timer;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.art.ARTElement;

import de.upb.agw.cpachecker.cpa.abm.util.CachedSubtree;
import de.upb.agw.cpachecker.cpa.abm.util.CachedSubtreeManager;

/**
 * Helper class to search for <code>ARTElement</code>s in a <code>ReachedSet</code> that have the same abstraction as the given <code>ARTElement</code> after reduction to the relevant predicates.
 * @author dwonisch
 *
 */
public class ARTElementSearcher {
  public static Timer searchForARTElementTimer = new Timer();
  
  public static ARTElement searchForARTElement(UnmodifiableReachedSet reached, ARTElement targetElement, Reducer reducer, CachedSubtreeManager manager) {
    CFANode targetNode = targetElement.retrieveLocationElement().getLocationNode();
    searchForARTElementTimer.start();
    try {
      if (targetElement.isTarget()) {
        //shortcut
        ARTElement result = (ARTElement)reached.getLastElement();
        assert result != null;
        return result;
      }
      CFANode callNode = ((ARTElement)reached.getFirstElement()).retrieveLocationElement().getLocationNode();
      CachedSubtree context = manager.getCachedSubtreeForCallNode(callNode);
      AbstractElement reducedTarget = reducer.getVariableReducedElement(targetElement, context, callNode);    
            
      Iterable<AbstractElement> localReached = filterLocation(reached, targetNode);
      
      for(AbstractElement element : localReached) {
        if (reducer.isEqual(reducedTarget, element)) {
          return (ARTElement)element;
        }
      }
      
      assert false;
      return null;
    }
    finally {
      searchForARTElementTimer.stop();
    }
  }
}
