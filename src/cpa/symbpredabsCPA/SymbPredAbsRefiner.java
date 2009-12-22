package cpa.symbpredabsCPA;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import symbpredabstraction.UpdateablePredicateMap;
import symbpredabstraction.interfaces.Predicate;
import symbpredabstraction.trace.CounterexampleTraceInfo;
import cfa.objectmodel.CFAEdge;
import cfa.objectmodel.CFANode;
import cmdline.CPAMain;

import common.Pair;
import compositeCPA.CompositeCPA;

import cpa.art.ARTElement;
import cpa.art.AbstractARTBasedRefiner;
import cpa.common.Path;
import cpa.common.ReachedElements;
import cpa.common.algorithm.CEGARAlgorithm;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import exceptions.CPAException;

public class SymbPredAbsRefiner extends AbstractARTBasedRefiner {

  private final SymbPredAbsCPA mCpa;
  private final SymbPredAbstFormulaManager formulaManager;

  private int numSeenAbstractCounterexample = 0;
  private List<Integer> seenAbstractCounterexample = null;
  
  public SymbPredAbsRefiner(final ConfigurableProgramAnalysis pCpa) throws CPAException {
    super(pCpa);

    ConfigurableProgramAnalysis cpa = this.getArtCpa().getWrappedCPA();
    
    if (cpa instanceof SymbPredAbsCPA) {
      mCpa = (SymbPredAbsCPA)pCpa;
    
    } else {
      SymbPredAbsCPA symbPredAbsCpa = null;
      if (cpa instanceof CompositeCPA) {
        for (ConfigurableProgramAnalysis compCPA : ((CompositeCPA)cpa).getComponentCPAs()) {
          if (compCPA instanceof SymbPredAbsCPA) {
            symbPredAbsCpa = (SymbPredAbsCPA)compCPA;
            break;
          }
        }
      }
      if (symbPredAbsCpa == null) {
        throw new CPAException(getClass().getSimpleName() + " needs a SymbPredAbsCPA");
      }
      mCpa = symbPredAbsCpa;
    }

    formulaManager = mCpa.getFormulaManager();
  }

  @Override
  public ARTElement performRefinement(ReachedElements pReached,
      Path pPath) throws CPAException {

    CPAMain.logManager.log(Level.FINEST, "Starting refinement for SymbPredAbsCPA");
    
    // create path with all abstraction location elements (excluding the initial
    // element, which is not in pPath)
    // the last element is the element corresponding to the error location
    // (which is twice in pPath)
    ArrayList<SymbPredAbsAbstractElement> path = new ArrayList<SymbPredAbsAbstractElement>();
    SymbPredAbsAbstractElement lastElement = null;
    for (Pair<ARTElement,CFAEdge> artPair : pPath) {
      SymbPredAbsAbstractElement symbElement = (SymbPredAbsAbstractElement)
        artPair.getFirst().retrieveElementOfType("SymbPredAbsAbstractElement");
      
      if (symbElement.isAbstractionNode() && symbElement != lastElement) {
        path.add(symbElement);
      }
      lastElement = symbElement;
    }
    
    CPAMain.logManager.log(Level.ALL, "Abstraction trace is", path);
        
    // build the counterexample
    CounterexampleTraceInfo info = formulaManager.buildCounterexampleTrace(path);
        
    // if error is spurious refine
    if (info.isSpurious()) {
      CPAMain.logManager.log(Level.FINEST, "Error trace is spurious, refining the abstraction");
      return performRefinement(pReached, path, pPath, info);
    } else {
      CPAMain.logManager.log(Level.FINEST, "Error trace is not spurious");
      // we have a real error
      return null;
    }
  }

  private ARTElement performRefinement(ReachedElements pReached,
      ArrayList<SymbPredAbsAbstractElement> pPath, Path pArtPath, CounterexampleTraceInfo pInfo) throws CPAException {

    // TODO check

    UpdateablePredicateMap curpmap = (UpdateablePredicateMap)mCpa.getPredicateMap();
    SymbPredAbsAbstractElement symbPredRootElement = null;
    SymbPredAbsAbstractElement firstInterpolationElement = null;
    
    for (SymbPredAbsAbstractElement e : pPath) {
      Collection<Predicate> newpreds = pInfo.getPredicatesForRefinement(e);
      if (firstInterpolationElement == null && newpreds.size() > 0) {
        firstInterpolationElement = e;
      }
      if (curpmap.update(e.getAbstractionLocation(), newpreds)) {
        if (symbPredRootElement == null) {
          symbPredRootElement = e;
        }
      }
    }
    
    // It can happen that we discovered interpolants and curpmap.update returns
    // false. This occurs when we discovered the same predicate on the same
    // CFANode in another error path before. In this case we try a new strategy,
    // because there might be more paths where this predicate will help. So we
    // remove everything below the first occurrence of this CFANode in the error
    // path from the ART. Then hopefully we won't have to refine all of those paths
    // one by one.
    
    // Another strategy would be to always remove everything below all occurrences
    // of this CFANode from the ART.
    
    CPAMain.logManager.log(Level.ALL, "Predicate map now is", curpmap);

    // FIXME (test/tests/ssh-simple/s3_clnt_4.cil.c.symbpredabsCPA-2.log) what to do here?
    assert(firstInterpolationElement != null);

    ARTElement root;
    if (symbPredRootElement == null) {
      SymbPredAbsAbstractElement errorElement = pPath.get(pPath.size()-1);
      
      if ((numSeenAbstractCounterexample > 1) &&
          errorElement.getAbstractionPathList().equals(seenAbstractCounterexample)) {
        
        CPAMain.logManager.log(Level.FINEST, "Found spurious counterexample",
            errorElement.getAbstractionPathList(), ", but no new predicates, terminating analysis");

        throw new CPAException("Not enough predicates");
      }

      seenAbstractCounterexample = errorElement.getAbstractionPathList();
      numSeenAbstractCounterexample++;

      CFANode loc = firstInterpolationElement.getAbstractionLocation(); 

      CPAMain.logManager.log(Level.FINEST, "Found spurious counterexample",
          seenAbstractCounterexample,
          "again, trying new strategy: remove everything below node", loc, "from ART.");

      root = this.getArtCpa().findHighest(pArtPath.getLast().getFirst(), loc);
    }
    else{
      seenAbstractCounterexample = null;
      numSeenAbstractCounterexample = 0;

      CPAMain.logManager.log(Level.FINEST, "New predicates were discovered.");
      
      long start = System.currentTimeMillis();
      root = findARTElementof(symbPredRootElement, pArtPath.getLast());
      long end = System.currentTimeMillis();
      CEGARAlgorithm.totalfindArtTime= CEGARAlgorithm.totalfindArtTime + (end - start);
    }

    return root;
  }

  private ARTElement findARTElementof(SymbPredAbsAbstractElement pSymbPredRootElement,
      Pair<ARTElement, CFAEdge> pLastElement) throws CPAException {

    Deque<ARTElement> workList = new ArrayDeque<ARTElement>();
    Set<ARTElement> handled = new HashSet<ARTElement>();

    // get the error element
    workList.add(pLastElement.getFirst());

    // go backwards
    while (!workList.isEmpty()) {
      ARTElement currentElement = workList.removeFirst();
      if (!handled.add(currentElement)) {
        // currentElement was already handled
        continue;
      }
      
      AbstractElement currentSymbPredElement = 
                currentElement.retrieveElementOfType("SymbPredAbsAbstractElement");
      if (currentSymbPredElement == pSymbPredRootElement){
        return currentElement;
      }
      workList.addAll(currentElement.getParents());
    }

    throw new CPAException("Inconsistent ART");
  }
}
