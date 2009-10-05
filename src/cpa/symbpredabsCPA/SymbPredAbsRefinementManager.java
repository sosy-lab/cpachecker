package cpa.symbpredabsCPA;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import logging.CustomLogLevel;
import logging.LazyLogger;
import symbpredabstraction.BDDMathsatSymbPredAbstractionAbstractManager;
import cfa.objectmodel.CFAEdge;
import cfa.objectmodel.CFANode;
import cmdline.CPAMain;

import common.Pair;

import cpa.art.ARTCPA;
import cpa.art.ARTDomain;
import cpa.art.ARTElement;
import cpa.art.Path;
import cpa.common.CPAWithRefinement;
import cpa.common.ReachedElements;
import cpa.common.RefinementOutcome;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.RefinementManager;
import cpa.symbpredabs.AbstractFormulaManager;
import cpa.symbpredabs.CounterexampleTraceInfo;
import cpa.symbpredabs.Predicate;
import cpa.symbpredabs.SymbolicFormulaManager;
import cpa.symbpredabs.UpdateablePredicateMap;
import exceptions.CPATransferException;

public class SymbPredAbsRefinementManager implements RefinementManager{

  private SymbPredAbsCPA cpa;
  private SymbolicFormulaManager symbolicFormulaManager;
  private AbstractFormulaManager abstractFormulaManager;

  private Map<Deque<SymbPredAbsAbstractElement>, Integer> seenAbstractCounterexamples;

  public SymbPredAbsRefinementManager(SymbPredAbsCPA pCpa) {
    cpa = pCpa;
    symbolicFormulaManager = cpa.getSymbolicFormulaManager();
    abstractFormulaManager = cpa.getAbstractFormulaManager();
    seenAbstractCounterexamples = new HashMap<Deque<SymbPredAbsAbstractElement>, Integer>();
  }

  @Override
  public RefinementOutcome performRefinement(ReachedElements pReached,
      Path pPath) {

    // error element is the second last at the array 
    Pair<AbstractElement, CFAEdge> errorElementPair = 
      pPath.getElementAt(pPath.size()-2);
    ARTElement errorARTElement =  (ARTElement)errorElementPair.getFirst();

    AbstractElement retrievedElement = 
      errorARTElement.retrieveElementOfType("SymbPredAbsAbstractElement");

    assert(retrievedElement != null);

    SymbPredAbsAbstractElement symbPredAbstElement = (SymbPredAbsAbstractElement) retrievedElement;

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
      try {
        return performRefinement(pReached, path, pPath, info);
      } catch (CPATransferException e) {
        e.printStackTrace();
        return null;
      }
    }
    // we have a real error
    else {
      CPAMain.cpaStats.setErrorReached(true);
      return new RefinementOutcome();
    }
  }

  public RefinementOutcome performRefinement(ReachedElements pReached,
      Deque<SymbPredAbsAbstractElement> pPath, Path pArtPath, CounterexampleTraceInfo pInfo) throws CPATransferException {

    assert(pReached.getLastElement() instanceof ARTElement);
    ARTElement lastElem = (ARTElement)pReached.getLastElement();
    ARTCPA artCpa = (ARTCPA)((ARTDomain)lastElem.getDomain()).getCpa();

    // TODO check
    int numSeen = 0;
    if (seenAbstractCounterexamples.containsKey(pPath)) {
      numSeen = seenAbstractCounterexamples.get(pPath);
    }
    seenAbstractCounterexamples.put(pPath, numSeen+1);
    UpdateablePredicateMap curpmap =
      (UpdateablePredicateMap)cpa.getPredicateMap();
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
      root = artCpa.findHighest(loc);
    }
    else{
      long start = System.currentTimeMillis();
      root = findARTElementof(symbPredRootElement, pArtPath.lastElement());
      long end = System.currentTimeMillis();
      CPAWithRefinement.totalfindArtTime= CPAWithRefinement.totalfindArtTime + (end - start);
    }
    if (CPAMain.cpaConfig.getBooleanValue("analysis.bfs")) {
      // TODO When using bfs traversal, we would have to traverse the ART
      // computed so far, and check for each leaf whether to re-add it
      // to the waiting list or not, similarly to what Blast does
      // (file psrc/be/modelChecker/lazyModelChecker.ml, function
      // update_tree_after_refinment). But for now, for simplicity we
      // just restart from scratch
      root = (ARTElement)pArtPath.firstElement().getFirst();
    }
    assert(root != null);
    Collection<ARTElement> toWaitlist = new HashSet<ARTElement>();
    toWaitlist.add(root);
    Collection<ARTElement> toUnreach = root.getSubtree();
//  SummaryCPA cpa = domain.getCPA();
    for (ARTElement ae : toUnreach) {
      if (ae.isCovered()) {
        ae.setCovered(false);
        artCpa.setUncovered(ae);
      }
    }
    if (root != artCpa.getRoot()) {
      // then, we have to unmark some nodes
      Collection<ARTElement> tmp = artCpa.getCovered();
      int m = root.getMark();
      for (Iterator<ARTElement> i = tmp.iterator(); i.hasNext(); ) {
        ARTElement e = i.next();
        assert(e.isCovered());
        if (e.getMark() > m) {
          e.setCovered(false);
          i.remove();
          SymbPredAbsAbstractElement elem = (SymbPredAbsAbstractElement)e.retrieveElementOfType("SymbPredAbsAbstractElement");
          if(elem.isAbstractionNode()){
            // TODO check
//            toWaitlist.add(e.getParent());
//            toUnreach.add(e);
          }
        }
      }
    }
    return new RefinementOutcome(true, toUnreach, toWaitlist, root);

//  LazyLogger.log(LazyLogger.DEBUG_1, "REFINEMENT - toWaitlist: ", root);
//  LazyLogger.log(LazyLogger.DEBUG_1, "REFINEMENT - toUnreach: ",
//  toUnreach);
//  throw new RefinementNeededException(toUnreach, toWaitlist);
  }

  private ARTElement findARTElementof(
      SymbPredAbsAbstractElement pSymbPredRootElement,
      Pair<AbstractElement, CFAEdge> pLastElement) {

    List<ARTElement> workList = new ArrayList<ARTElement>();

    ARTElement currentElement = null;
    // get the error element
    workList.add(((ARTElement)pLastElement.getFirst()).getParent());

    // go backwards
    while(workList.size() > 0){
      currentElement = workList.remove(0);
      SymbPredAbsAbstractElement currentSymbPredElement = (SymbPredAbsAbstractElement)
      currentElement.retrieveElementOfType("SymbPredAbsAbstractElement");
      if(currentSymbPredElement == pSymbPredRootElement){
        return currentElement;
      }
      if(!workList.contains(currentElement.getParent())){
        workList.add(currentElement.getParent());
      }

//    if(currentElement.getSecondParent() != null){
//    if(!workList.contains(currentElement.getSecondParent())){
//    workList.add(currentElement.getSecondParent());
//    }
//    }
    }
    // no such element
    return null;
  }
}
