package cpa.art;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import cfa.objectmodel.CFAEdge;
import cfa.objectmodel.CFANode;
import cfa.objectmodel.c.FunctionCallEdge;
import cmdline.CPAMain;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Collections2;
import com.google.common.collect.Sets;
import common.Pair;

import cpa.common.Path;
import cpa.common.ReachedElements;
import cpa.common.RefinementOutcome;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.Refiner;
import exceptions.CPAException;

public abstract class AbstractARTBasedRefiner implements Refiner {

  private final ARTCPA mArtCpa;

  private final Set<Path> seenCounterexamples = Sets.newHashSet();
  
  protected AbstractARTBasedRefiner(ConfigurableProgramAnalysis pCpa) throws CPAException {
    if (!(pCpa instanceof ARTCPA)) {
      throw new CPAException("ARTCPA needed for refinement");
    }
    mArtCpa = (ARTCPA)pCpa;
  }
  
  protected ARTCPA getArtCpa() {
    return mArtCpa;
  }
  
  private static final Function<Pair<ARTElement, CFAEdge>, String> pathToFunctionCalls 
        = new Function<Pair<ARTElement, CFAEdge>, String>() {
    public String apply(Pair<ARTElement,CFAEdge> arg) {
      
      if (arg.getSecond() instanceof FunctionCallEdge) {
        FunctionCallEdge funcEdge = (FunctionCallEdge)arg.getSecond();
        return "line " + funcEdge.getSuccessor().getLineNumber() + ":\t" + funcEdge.getRawStatement();
      } else {
        return null;
      }
    }
  };
  
  @Override
  public final RefinementOutcome performRefinement(ReachedElements pReached) throws CPAException {
    CPAMain.logManager.log(Level.FINEST, "Starting ART based refinement");
    
    assert checkART(pReached);
    
    AbstractElement lastElement = pReached.getLastElement();
    assert lastElement instanceof ARTElement;
    Path path = buildPath((ARTElement)lastElement);

    if (CPAMain.logManager.wouldBeLogged(Level.ALL)) {
      CPAMain.logManager.log(Level.ALL, "Error path:\n", path);
      CPAMain.logManager.log(Level.ALL, "Function calls on Error path:\n",
          Joiner.on("\n ").skipNulls().join(Collections2.transform(path, pathToFunctionCalls)));
    }
    
    assert seenCounterexamples.add(path);
    
    Pair<ARTElement, Precision> refinementResult = performRefinement(pReached, path);
    
    if (refinementResult != null) {
      CPAMain.logManager.log(Level.FINEST, "ART based refinement successful");
      CPAMain.logManager.log(Level.ALL, "Refinement root is", refinementResult.getFirst());
      return cleanART(path, pReached, refinementResult.getFirst(), refinementResult.getSecond());
    } else {
      CPAMain.logManager.log(Level.FINEST, "ART based refinement unsuccessful");
      return new RefinementOutcome(path);
    }
  }


  /**
   * Perform refinement.
   * @param pReached
   * @param pPath
   * @return
   * @throws CPAException
   */
  protected abstract Pair<ARTElement, Precision> performRefinement(
                      ReachedElements pReached, Path pPath) throws CPAException;

  /**
   * Create a path in the ART from root to the given element.
   * @param pLastElement The last element in the path.
   * @return A path from root to lastElement.
   */
  private Path buildPath(ARTElement pLastElement) { 
    Path path = new Path();

    // each element of the path consists of the abstract element and the incoming
    // edge from its predecessor
    // an exception is the last element: it is contained two times in the path,
    // first with the incoming edge and second with the outgoing edge
    
    ARTElement currentARTElement = pLastElement;
    assert pLastElement.isError();
    // add the error node and its -first- outgoing edge
    // that edge is not important so we pick the first even
    // if there are more outgoing edges
    CFAEdge lastEdge = currentARTElement.retrieveLocationElement().getLocationNode().getLeavingEdge(0);
    path.addFirst(new Pair<ARTElement, CFAEdge>(currentARTElement, lastEdge));
    
    while(currentARTElement != null){
      CFANode currentNode = currentARTElement.retrieveLocationElement().getLocationNode();
      ARTElement parentElement = currentARTElement.getFirstParent();
      
      CFANode parentNode = null;
      if(parentElement != null) {
        parentNode = parentElement.retrieveLocationElement().getLocationNode();
      }

      boolean foundEdge = false;
      for(int i=0; i<currentNode.getNumEnteringEdges(); i++){
        CFAEdge edge = currentNode.getEnteringEdge(i);
        if(edge.getPredecessor().equals(parentNode)){
          foundEdge = true;
          path.addFirst(new Pair<ARTElement, CFAEdge>(currentARTElement, edge));
          break;
        }
      }
      assert (parentElement == null) || foundEdge;
      currentARTElement = parentElement;
    }
    return path;
  }
  
  private RefinementOutcome cleanART(Path errorPath, ReachedElements pReached,
                ARTElement root, Precision newPrecision) {
    assert root != null;
    assert !root.getParents().isEmpty() : "initial element makes no sense as refinement root";
    
    Collection<ARTElement> toWaitlist = new HashSet<ARTElement>();
    Set<ARTElement> toUnreach = root.getSubtree();

    // remove root and all nodes below root from ART
    // re-add their parents to the waitlist
    for (ARTElement ae : toUnreach) {
      for (ARTElement parent : ae.getParents()) {
        if (!toUnreach.contains(parent)) {
          toWaitlist.add(parent);
        }
      }
      ae.removeFromART();
    }
    
    // re-add those elements to the waitlist, which were covered by
    // elements which are removed now
    List<ARTElement> toUncover = new ArrayList<ARTElement>();
    
    for (ARTElement ae : mArtCpa.getCovered()) {
      if (toUnreach.contains(ae.getCoveredBy())) {
        toUncover.add(ae);
      }
    }
    
    for (ARTElement ae : toUncover) {
      toWaitlist.addAll(ae.getParents());
      ae.removeFromART(); // removes ae from parents and covered set
    }
    
    return new RefinementOutcome(newPrecision, toUnreach, toWaitlist, errorPath);
  }
  
  private boolean checkART(ReachedElements pReached) {
    Set<? extends AbstractElement> reached = pReached.getReached();
    
    Deque<AbstractElement> workList = new ArrayDeque<AbstractElement>();
    Set<ARTElement> art = new HashSet<ARTElement>();
    
    workList.add(pReached.getFirstElement());
    while (!workList.isEmpty()) {
      ARTElement currentElement = (ARTElement)workList.removeFirst();
      for (ARTElement parent : currentElement.getParents()) {
        assert parent.getChildren().contains(currentElement);
      }
      for (ARTElement child : currentElement.getChildren()) {
        assert child.getParents().contains(currentElement);
      }
      
      // check if (e \in ART) => ((e \in Reached ^ e.isCovered()) ^ (e == Bottom))
      assert reached.contains(currentElement) ^ currentElement.isCovered() ^ currentElement.isBottom();
      
      if (art.add(currentElement)) {
        workList.addAll(currentElement.getChildren());
      }
    }
    
    for (AbstractElement currentElement : reached) {
      // check if (e \in Reached) => (e \in ART)
      assert art.contains(currentElement);
    }

    return true;
  }
}
