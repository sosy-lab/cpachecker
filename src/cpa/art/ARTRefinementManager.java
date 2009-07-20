package cpa.art;

import cfa.objectmodel.CFAEdge;
import cfa.objectmodel.CFANode;
import cpa.common.ReachedElements;
import cpa.common.RefinementOutcome;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.RefinableCPA;
import cpa.common.interfaces.RefinementManager;

public class ARTRefinementManager implements RefinementManager {

  private RefinableCPA wrappedCpa;
  private RefinementManager wrappedRefinementManager;

  public ARTRefinementManager(RefinableCPA pWrappedCPA) {
    wrappedCpa = pWrappedCPA;
    wrappedRefinementManager = wrappedCpa.getRefinementManager();
  }

  @Override
  public RefinementOutcome performRefinement(ReachedElements pReached, Path pPath) {
    
    AbstractElement lastElement = pReached.getLastElement();
    
    // we expect ART element to wrap other elements
    if(!(lastElement instanceof ARTElement)){
      // TODO an exception here
      assert(false);
    }

    // build the path here
    ARTElement lastARTElement = (ARTElement) lastElement;
    Path path = new Path();

    ARTElement currentARTElement = lastARTElement;
    // add the error node and its -first- outgoing edge
    // that edge is not important so we pick the first even
    // if there are more outgoing edges
    CFAEdge lastEdge = lastARTElement.getLocationNode().getLeavingEdge(0);
    path.addToPathAsFirstElem(currentARTElement, lastEdge);
    while(currentARTElement != null){
      CFANode currentNode = currentARTElement.getLocationNode();
      ARTElement parentElement = currentARTElement.getParent();
      CFANode parentNode = null;
      if(parentElement != null){
        parentNode = parentElement.getLocationNode();
      }
      else{
        parentElement = null;
      }

      CFAEdge foundEdge = null;
      for(int i=0; i<currentNode.getNumEnteringEdges(); i++){
        CFAEdge edge = currentNode.getEnteringEdge(i);
        if(edge.getPredecessor().equals(parentNode)){
          foundEdge = edge;
          path.addToPathAsFirstElem(currentARTElement, foundEdge);
          break;
        }
      }
      currentARTElement = parentElement;
    }

    return wrappedRefinementManager.performRefinement(pReached, path);
  }

}
