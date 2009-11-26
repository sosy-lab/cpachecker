package cpa.symbpredabsCPA;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import symbpredabstraction.UpdateablePredicateMap;
import symbpredabstraction.interfaces.Predicate;
import symbpredabstraction.interfaces.SymbolicFormulaManager;
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
  private final SymbolicFormulaManager symbolicFormulaManager;
  private final SymbPredAbstFormulaManager abstractFormulaManager;

  private final Map<ArrayList<SymbPredAbsAbstractElement>, Integer> seenAbstractCounterexamples;

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

    symbolicFormulaManager = mCpa.getSymbolicFormulaManager();
    abstractFormulaManager = mCpa.getAbstractFormulaManager();
    seenAbstractCounterexamples = new HashMap<ArrayList<SymbPredAbsAbstractElement>, Integer>();
  }

  @Override
  public ARTElement performRefinement(ReachedElements pReached,
      Path pPath) throws CPAException {

    // create path with all elements directly before an abstraction location,
    // last element is the element corresponding to the error location
    // (which is twice in pPath)
    ArrayList<SymbPredAbsAbstractElement> path = new ArrayList<SymbPredAbsAbstractElement>();
    SymbPredAbsAbstractElement lastElement = null;
    for (Pair<ARTElement,CFAEdge> artPair : pPath) {
      SymbPredAbsAbstractElement symbElement = (SymbPredAbsAbstractElement)
        artPair.getFirst().retrieveElementOfType("SymbPredAbsAbstractElement");
      
      if (symbElement.isAbstractionNode()) {
        path.add(lastElement);
      }
      lastElement = symbElement;
    }
    
    // TODO PW I can't imagine why this path has to be like this, is it correct?
        
    // build the counterexample
    CounterexampleTraceInfo info = abstractFormulaManager.buildCounterexampleTrace(
        symbolicFormulaManager, path);
    
    // if error is spurious refine
    if (info.isSpurious()) {
      CPAMain.logManager.log(Level.FINEST,
            "Found spurious error trace, refining the abstraction");
      return performRefinement(pReached, path, pPath, info);
    } else {
      // we have a real error
      return null;
    }
  }

  private ARTElement performRefinement(ReachedElements pReached,
      ArrayList<SymbPredAbsAbstractElement> pPath, Path pArtPath, CounterexampleTraceInfo pInfo) throws CPAException {

    // TODO check
    int numSeen = 0;
    if (seenAbstractCounterexamples.containsKey(pPath)) {
      numSeen = seenAbstractCounterexamples.get(pPath);
    }
    seenAbstractCounterexamples.put(pPath, numSeen+1);
    UpdateablePredicateMap curpmap =
      (UpdateablePredicateMap)mCpa.getPredicateMap();
    ARTElement root = null;
    SymbPredAbsAbstractElement symbPredRootElement = null;
    SymbPredAbsAbstractElement firstInterpolant = null;
    SymbPredAbsAbstractElement previous = null;
    for (SymbPredAbsAbstractElement e : pPath) {
      Collection<Predicate> newpreds = pInfo.getPredicatesForRefinement(e);
      if (firstInterpolant == null && newpreds.size() > 0) {
        firstInterpolant = e;
      }
      if (curpmap.update(e.getAbstractionLocation(), newpreds)) {
        if (symbPredRootElement == null) {
          assert previous != null;
          symbPredRootElement = previous;
        }
      }
      previous = e;
    }
    if (symbPredRootElement == null) {
      assert(firstInterpolant != null);
      if (numSeen > 1) {
//      assert(numSeen == 2);
//        if (CPAMain.cpaConfig.getBooleanValue("cpas.symbpredabs.abstraction.cartesian")) {
          throw new CPAException("not enough predicates");
//        }
      }

      CFANode loc = firstInterpolant.getAbstractionLocation(); 
      root = this.getArtCpa().findHighest(pArtPath.getLast().getFirst(), loc);
    }
    else{
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
