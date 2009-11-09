package cpa.predicateabstraction;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import logging.CustomLogLevel;
import logging.LazyLogger;
import symbpredabstraction.UpdateablePredicateMap;
import symbpredabstraction.interfaces.Predicate;
import symbpredabstraction.trace.CounterexampleTraceInfo;
import cfa.objectmodel.CFAEdge;
import cmdline.CPAMain;

import common.Pair;

import cpa.art.ARTCPA;
import cpa.art.ARTDomain;
import cpa.art.ARTElement;
import cpa.common.Path;
import cpa.common.ReachedElements;
import cpa.common.RefinementOutcome;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.RefinementManager;
import exceptions.CPATransferException;

public class PredicateAbstractionRefinementManager implements RefinementManager {

  private PredicateAbstractionCPA cpa;
  private PredicateAbstractionAbstractFormulaManager amgr;
  private Map<Vector<Integer>, Integer> abstractCex;

  private int numAbstractStates = 0; // for statistics

  // this is used for deciding how much of the ART to undo after refinement
  // private Deque<ExplicitAbstractElement> lastErrorPath;
  // private int samePathAlready = 0;

  private boolean notEnoughPredicatesFlag = false;

  public PredicateAbstractionRefinementManager(PredicateAbstractionCPA pCpa) {
    cpa = pCpa;
    amgr = cpa.getAbstractFormulaManager();
    abstractCex = new HashMap<Vector<Integer>, Integer>();
  }

  @Override
  public RefinementOutcome performRefinement(ReachedElements pReached, Path pPath) {

    Pair<ARTElement, CFAEdge>[] pathArray;

    pathArray = getPathArray(pPath);

    CounterexampleTraceInfo info =
      amgr.buildCounterexampleTrace(
          cpa.getFormulaManager(), pathArray);

    assert(info != null);

    if (info.isSpurious()) {
      LazyLogger.log(CustomLogLevel.SpecificCPALevel,
          "Found spurious error trace, refining the ",
      "abstraction");
      try {
        return performRefinement(pReached, pPath, pathArray, info);
      } catch (CPATransferException e) {
        e.printStackTrace();
      }
      return null;
    } else {
//    LazyLogger.log(CustomLogLevel.SpecificCPALevel,
//    "REACHED ERROR LOCATION!: ", succ,
//    " RETURNING BOTTOM!");
      CPAMain.cpaStats.setErrorReached(true);
      return new RefinementOutcome();
    }
  }


  public RefinementOutcome performRefinement(ReachedElements pReached, Path pPath,
      Pair<ARTElement, CFAEdge>[] pPathArray,
      CounterexampleTraceInfo pInfo) throws CPATransferException {
    LazyLogger.log(LazyLogger.DEBUG_1, "STARTING REFINEMENT");
    UpdateablePredicateMap curpmap =
      (UpdateablePredicateMap)cpa.getPredicateMap();

    assert(pReached.getLastElement() instanceof ARTElement);
    ARTElement lastElem = (ARTElement)pReached.getLastElement();
    ARTCPA artCpa = (ARTCPA)((ARTDomain)lastElem.getDomain()).getCpa();

    ARTElement root = null;
    ARTElement cur = null;
    ARTElement firstInterpolant = null;

    for (Pair<ARTElement, CFAEdge> p : pPathArray) {
      ARTElement e = p.getFirst();
      CFAEdge edge = p.getSecond();
      Collection<Predicate> newpreds = pInfo.getPredicatesForRefinement(e);
      if (firstInterpolant == null && newpreds.size() > 0) {
        firstInterpolant = e;
      }
      // TODO check
      assert(edge != null);
      if (curpmap.update(edge.getSuccessor(), newpreds)) {
        if (root == null) {
          cur = e;
          root = e;
        }
      }
    }
    // TODO check
//  Path pth = new Path(path);
    Vector<Integer> pth = arrayToVector(pPathArray);
    int alreadySeen = 0;
    if (abstractCex.containsKey(pth)) {
      alreadySeen = abstractCex.get(pth);
    }
    abstractCex.put(pth, alreadySeen+1);
    if (root == null) {
      if (alreadySeen != 0) {//samePath(path, lastErrorPath)) {
        if (alreadySeen > 1) {//samePathAlready == 1) {
          // we have not enough predicates to rule out this path, and
          // we can't find any new, we are forced to exit :-(
          notEnoughPredicatesFlag = true;
          assert(false);
          System.exit(1);
        } else {
          root = firstInterpolant;
        }
      } else {
        assert(firstInterpolant != null);
        LazyLogger.log(CustomLogLevel.SpecificCPALevel,
        "Restarting ART from scratch");
        root = artCpa.getRoot();
      }
    } else {
      //samePathAlready  = 0;
    }
    if (CPAMain.cpaConfig.getBooleanValue("analysis.bfs")) {
      // TODO When using bfs traversal, we would have to traverse the ART
      // computed so far, and check for each leaf whether to re-add it
      // to the waiting list or not, similarly to what Blast does
      // (file psrc/be/modelChecker/lazyModelChecker.ml, function
      // update_tree_after_refinment). But for now, for simplicity we
      // just restart from scratch
      root = artCpa.getRoot();
    }
    assert(root != null);
    Collection<ARTElement> toWaitlist = new HashSet<ARTElement>();
    toWaitlist.add(root);
    Collection<ARTElement> toUnreach = root.getSubtree();
    if (cur != null) {
      // we don't want to unreach elements that were covered before
      // reaching the error!
      for (Iterator<ARTElement> it = toUnreach.iterator();
      it.hasNext(); ) {
        ARTElement e = it.next();
        if (e.isCovered() && e.getMark() < cur.getMark()) {
          LazyLogger.log(LazyLogger.DEBUG_1, "NOT unreaching ", e,
              " because it was covered before ", cur);
          it.remove();
        }
      }
    }

//  ARTCPA cpa = domain.getCPA();
    for (ARTElement ae : toUnreach) {
      if (ae.isCovered()) {
        ae.setCovered(false);
        artCpa.setUncovered(ae);
      }
    }
    if (root != artCpa.getRoot()) {
      // then, we have to unmark some nodes
      Collection<ARTElement> tmp =
        artCpa.getCovered();
      for (Iterator<ARTElement> i = tmp.iterator(); 
      i.hasNext(); ) {
        ARTElement e = i.next();
        assert(e.isCovered());
        if (e.getMark() > root.getMark()) {
          e.setCovered(false);
          i.remove();
          // TODO adding all parents? check this
          toWaitlist.addAll(e.getParents());
          toUnreach.add(e);
        }
      }
    }

    return new RefinementOutcome(true, toUnreach, toWaitlist, root);

//  LazyLogger.log(LazyLogger.DEBUG_1, "REFINEMENT - toWaitlist: ",
//  toWaitlist);
//  LazyLogger.log(LazyLogger.DEBUG_1, "REFINEMENT - toUnreach: ",
//  toUnreach);
//  throw new RefinementNeededException(toUnreach, toWaitlist);
  }

  private Vector<Integer> arrayToVector(
      Pair<ARTElement, CFAEdge>[] pPathArray) {

    Vector<Integer> r = new Vector<Integer>();

    for(Pair<ARTElement, CFAEdge> p: pPathArray){
      int i = p.getSecond().getSuccessor().getNodeNumber();
      r.add(i);
    }

    return r;

  }

  private Pair<ARTElement, CFAEdge>[] getPathArray(
      Path pPath) {

    Pair<ARTElement, CFAEdge>[] array = 
      new Pair[pPath.size()];

    Pair<AbstractElement, CFAEdge> pathElement = pPath.getElementAt(0);
    assert(pathElement != null);
    AbstractElement absElement = pathElement.getFirst();

    assert(absElement instanceof ARTElement);

    for(int i=0; i<pPath.size(); i++){
      Pair<AbstractElement, CFAEdge> p = pPath.getElementAt(i);
      array[i] = new Pair<ARTElement, CFAEdge>((ARTElement) p.getFirst(), p.getSecond());
    }

    return array;

//  AbstractElement wrappedElement = artElement.getAbstractElementOnArtNode();
//  int idxOfPredAbsElem = -1;

//  if(wrappedElement instanceof CompositeElement){
//  CompositeElement compositeElement = (CompositeElement) wrappedElement;
//  for(int i=0; i<compositeElement.getNumberofElements(); i++){
//  AbstractElement abstElement = compositeElement.get(i);
//  if(abstElement instanceof PredicateAbstractionAbstractElement){
//  idxOfPredAbsElem = i;
//  break;
//  }
//  }

//  assert(idxOfPredAbsElem != -1);

//  for(int i=0; i<pPath.size(); i++){
//  Pair<AbstractElement, CFAEdge> p = pPath.getElementAt(i);
//  CompositeElement compElement = (CompositeElement)p.getFirst();
//  CFAEdge edge = p.getSecond();
//  PredicateAbstractionAbstractElement predAbsElem = (PredicateAbstractionAbstractElement)compElement.get(idxOfPredAbsElem);
//  array[i] = new Pair<PredicateAbstractionAbstractElement, CFAEdge>(predAbsElem, edge);
//  }

//  }
//  else{
//  assert(wrappedElement instanceof PredicateAbstractionAbstractElement);
//  for(int i=0; i<pPath.size(); i++){
//  Pair<AbstractElement, CFAEdge> p = pPath.getElementAt(i);
//  PredicateAbstractionAbstractElement predAbsElem = (PredicateAbstractionAbstractElement)p.getFirst();
//  CFAEdge edge = p.getSecond();
//  array[i] = new Pair<PredicateAbstractionAbstractElement, CFAEdge>(predAbsElem, edge);
//  }
//  }

//  return array;

  }

}
