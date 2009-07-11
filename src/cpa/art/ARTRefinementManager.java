package cpa.art;

import cfa.objectmodel.CFAEdge;
import cfa.objectmodel.CFANode;
import cpa.common.ReachedElements;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.AbstractElementWithLocation;
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
  public boolean performRefinement(ReachedElements pReached) {
    
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
    while(currentARTElement != null){
      AbstractElementWithLocation currentWrappedElement = currentARTElement.getAbstractElementOnArtNode();
      CFANode currentNode = currentWrappedElement.getLocationNode();
      ARTElement parentElement = currentARTElement.getParent();
      AbstractElementWithLocation parentWrappedElement;
      CFANode parentNode = null;
      if(parentElement != null){
        parentWrappedElement = parentElement.getAbstractElementOnArtNode();
        parentNode = parentWrappedElement.getLocationNode();
      }
      else{
        parentElement = null;
      }

      CFAEdge foundEdge = null;
      for(int i=0; i<currentNode.getNumEnteringEdges(); i++){
        CFAEdge edge = currentNode.getEnteringEdge(i);
        if(edge.getPredecessor().equals(parentNode)){
          foundEdge = edge;
          break;
        }
      }

      path.addToPathAsFirstElem(currentWrappedElement, foundEdge);
      currentARTElement = parentElement;
      System.out.println(currentARTElement);
    }

    return wrappedRefinementManager.performRefinement(path);
  }

  @Override
  public boolean performRefinement(Path pPath) {
    // TODO exception
    // should not be performed
    assert(false);
    return false;
  }

}
