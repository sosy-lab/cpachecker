package cpa.art;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
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
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
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
  public final boolean performRefinement(ReachedElements pReached) throws CPAException {
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
    
    boolean result = performRefinement(new ARTReachedSet(pReached, mArtCpa), path);
    
    assert checkART(pReached);

    CPAMain.logManager.log(Level.FINEST, "ART based refinement finished, result is", result);

    return result;
  }


  /**
   * Perform refinement.
   * @param pReached
   * @param pPath
   * @return whether the refinement was successful 
   */
  protected abstract boolean performRefinement(ARTReachedSet pReached, Path pPath)
            throws CPAException;

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
    
    // check if (e \in Reached) => (e \in ART)
    assert art.containsAll(reached) : "Element in reached but not in ART";

    return true;
  }
}
