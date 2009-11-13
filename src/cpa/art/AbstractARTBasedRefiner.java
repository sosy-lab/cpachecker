package cpa.art;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import cfa.objectmodel.CFAEdge;
import cfa.objectmodel.CFANode;
import cpa.common.Path;
import cpa.common.ReachedElements;
import cpa.common.RefinementOutcome;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpa.common.interfaces.Refiner;
import exceptions.CPAException;

public abstract class AbstractARTBasedRefiner implements Refiner {

  private final ARTCPA mArtCpa;
  
  protected AbstractARTBasedRefiner(ConfigurableProgramAnalysis pCpa) throws CPAException {
    if (!(pCpa instanceof ARTCPA)) {
      throw new CPAException("ARTCPA needed for refinement");
    }
    mArtCpa = (ARTCPA)pCpa;
  }
  
  protected ARTCPA getArtCpa() {
    return mArtCpa;
  }
  
  @Override
  public final RefinementOutcome performRefinement(ReachedElements pReached) {
    AbstractElement lastElement = pReached.getLastElement();
    assert lastElement instanceof ARTElement;
    Path path = buildPath((ARTElement)lastElement);
    
    ARTElement root = performRefinement(pReached, path);
    
    if (root != null) {
      return cleanART(root);
    } else {
      return new RefinementOutcome();
    }
  }


  protected abstract ARTElement performRefinement(ReachedElements pReached,
                                                  Path pPath);

  /**
   * Create a path in the ART from root to the given element.
   * @param pLastElement The last element in the path.
   * @return A path from root to lastElement.
   */
  private Path buildPath(ARTElement pLastElement) { 
    Path path = new Path();

    ARTElement currentARTElement = pLastElement;
    // add the error node and its -first- outgoing edge
    // that edge is not important so we pick the first even
    // if there are more outgoing edges
    CFAEdge lastEdge = pLastElement.getLocationNode().getLeavingEdge(0);
    path.addToPathAsFirstElem(currentARTElement, lastEdge);
    while(currentARTElement != null){
      CFANode currentNode = currentARTElement.getLocationNode();
      ARTElement parentElement = currentARTElement.getFirstParent();
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
    return path;
  }
  
  private RefinementOutcome cleanART(ARTElement root) {
    assert(root != null);
    
    Collection<ARTElement> toWaitlist = new HashSet<ARTElement>();
    toWaitlist.add(root);
    Collection<ARTElement> toUnreach = root.getSubtree();
    
    for (ARTElement ae : toUnreach) {
      if (ae.isCovered()) {
        ae.setCovered(false);
        mArtCpa.setUncovered(ae);
      }
    }
    if (root != mArtCpa.getRoot()) {
      // then, we have to unmark some nodes
      Collection<ARTElement> tmp = mArtCpa.getCovered();
      int m = root.getMark();
      for (Iterator<ARTElement> i = tmp.iterator(); i.hasNext(); ) {
        ARTElement e = i.next();
        assert(e.isCovered());
        if (e.getMark() > m) {
          e.setCovered(false);
          i.remove();
          // TODO adding all parents? check this
          toWaitlist.addAll(e.getParents());
//          toUnreach.add(e);
          // TODO instead: remove from ART
        }
      }
    }
    return new RefinementOutcome(true, toUnreach, toWaitlist, root);
  }
}
