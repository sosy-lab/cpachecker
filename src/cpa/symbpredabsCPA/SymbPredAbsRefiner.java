package cpa.symbpredabsCPA;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import logging.CustomLogLevel;
import logging.LazyLogger;
import symbpredabstraction.UpdateablePredicateMap;
import symbpredabstraction.interfaces.AbstractFormulaManager;
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
  private final AbstractFormulaManager abstractFormulaManager;

  private final Map<Deque<SymbPredAbsAbstractElement>, Integer> seenAbstractCounterexamples;

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
    seenAbstractCounterexamples = new HashMap<Deque<SymbPredAbsAbstractElement>, Integer>();
  }

  @Override
  public ARTElement performRefinement(ReachedElements pReached,
      Path pPath) {

    // error element is the second last at the array 
    Pair<AbstractElement, CFAEdge> errorElementPair = 
      pPath.getElementAt(pPath.size()-2);
    ARTElement errorARTElement =  (ARTElement)errorElementPair.getFirst();
    assert (errorARTElement.isError());

    SymbPredAbsAbstractElement symbPredAbstElement =
        (SymbPredAbsAbstractElement) errorARTElement.retrieveElementOfType("SymbPredAbsAbstractElement");
    assert(symbPredAbstElement != null);

    Deque<SymbPredAbsAbstractElement> path = new LinkedList<SymbPredAbsAbstractElement>();
    path.addFirst(symbPredAbstElement);
    SymbPredAbsAbstractElement artParent = symbPredAbstElement.getArtParent();
    while (artParent != null) {
      path.addFirst(artParent);
      artParent = artParent.getArtParent();
    }
    BDDMathsatSymbPredAbstractionAbstractManager bddAbstractFormulaManager  = (BDDMathsatSymbPredAbstractionAbstractManager)abstractFormulaManager;
    // build the counterexample
    CounterexampleTraceInfo info = bddAbstractFormulaManager.buildCounterexampleTrace(
        symbolicFormulaManager, path);
    // if error is spurious refine
    if (info.isSpurious()) {
      LazyLogger.log(CustomLogLevel.SpecificCPALevel,
          "Found spurious error trace, refining the ",
      "abstraction");
      return performRefinement(pReached, path, pPath, info);
    }
    // we have a real error
    else {
      CPAMain.cpaStats.setErrorReached(true);
      return null;
    }
  }

  private ARTElement performRefinement(ReachedElements pReached,
      Deque<SymbPredAbsAbstractElement> pPath, Path pArtPath, CounterexampleTraceInfo pInfo) {

    assert(pReached.getLastElement() instanceof ARTElement);
    ARTElement lastElem = (ARTElement)pReached.getLastElement();

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
    AbstractElement firstInterpolant = null;
    for (SymbPredAbsAbstractElement e : pPath) {
      Collection<Predicate> newpreds = pInfo.getPredicatesForRefinement(e);
      if (firstInterpolant == null && newpreds.size() > 0) {
        firstInterpolant = e;
      }
      if (curpmap.update(e.getAbstractionLocation(), newpreds)) {
        if (symbPredRootElement == null) {
          symbPredRootElement = e.getArtParent();
        }
      }
    }
    if (symbPredRootElement == null) {
      assert(firstInterpolant != null);
      if (numSeen > 1) {
//      assert(numSeen == 2);
        if (CPAMain.cpaConfig.getBooleanValue(
        "cpas.symbpredabs.abstraction.cartesian")) {
          // not enough predicates
          assert(false);
          System.exit(1);
        }
      } else {
        assert(numSeen <= 1);
      }

      CFANode loc = ((SymbPredAbsAbstractElement)firstInterpolant).getAbstractionLocation(); 
      root = this.getArtCpa().findHighest(lastElem, loc);
    }
    else{
      long start = System.currentTimeMillis();
      root = findARTElementof(symbPredRootElement, pArtPath.lastElement());
      long end = System.currentTimeMillis();
      CEGARAlgorithm.totalfindArtTime= CEGARAlgorithm.totalfindArtTime + (end - start);
    }

    return root;
  }

  private ARTElement findARTElementof(
      SymbPredAbsAbstractElement pSymbPredRootElement,
      Pair<AbstractElement, CFAEdge> pLastElement) {

    List<ARTElement> workList = new ArrayList<ARTElement>();

    ARTElement currentElement = null;
    // get the error element
    workList.add(((ARTElement)pLastElement.getFirst()).getFirstParent());

    // go backwards
    while(workList.size() > 0){
      currentElement = workList.remove(0);
      SymbPredAbsAbstractElement currentSymbPredElement = (SymbPredAbsAbstractElement)
      currentElement.retrieveElementOfType("SymbPredAbsAbstractElement");
      if(currentSymbPredElement == pSymbPredRootElement){
        return currentElement;
      }
      if(!workList.contains(currentElement.getFirstParent())){
        workList.add(currentElement.getFirstParent());
      }
    }
    // no such element
    return null;
  }
}