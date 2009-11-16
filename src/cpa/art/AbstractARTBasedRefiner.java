package cpa.art;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

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
  public final RefinementOutcome performRefinement(ReachedElements pReached) throws CPAException {
    AbstractElement lastElement = pReached.getLastElement();
    assert lastElement instanceof ARTElement;
    Path path = buildPath((ARTElement)lastElement);
    
    ARTElement root = performRefinement(pReached, path);
    
    if (root != null) {
      return cleanART(pReached, root);
    } else {
      return new RefinementOutcome();
    }
  }


  protected abstract ARTElement performRefinement(ReachedElements pReached,
                                                  Path pPath) throws CPAException ;

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
  
  private RefinementOutcome cleanART(ReachedElements pReached, ARTElement root) {
    assert(root != null);
    
    Collection<ARTElement> toWaitlist = new HashSet<ARTElement>();
    toWaitlist.add(root);
    Collection<ARTElement> toUnreach = root.getSubtree();
    
    // clear everything below root
    root.clearChildren();
    for (ARTElement ae : toUnreach) {
      ae.setCovered(false);
    }
    
    // re-add those elements to the waitlist, which could have been covered by
    // elements which were removed now
    // only necessary if we do not throw away the whole ART
    if (root != pReached.getFirstElement()) {
      List<ARTElement> toUncover = new ArrayList<ARTElement>();
      
      int m = root.getMark();
      for (ARTElement ae : mArtCpa.getCovered()) {
        if (ae.getMark() > m) {
          toUncover.add(ae);
        }
      }
      
      for (ARTElement ae : toUncover) {
        ae.setCovered(false);
        // TODO adding all parents? check this
        toWaitlist.addAll(ae.getParents());
        ae.removeFromART();
      }
    }
    
    return new RefinementOutcome(true, toUnreach, toWaitlist, root);
  }
}
