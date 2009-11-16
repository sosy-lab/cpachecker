package cpa.predicateabstraction;

import java.util.Collection;
import java.util.HashMap;
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
import compositeCPA.CompositeCPA;

import cpa.art.ARTElement;
import cpa.art.AbstractARTBasedRefiner;
import cpa.common.Path;
import cpa.common.ReachedElements;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import exceptions.CPAException;

public class PredicateAbstractionRefiner extends AbstractARTBasedRefiner {

  private PredicateAbstractionCPA mCpa;
  private PredicateAbstractionAbstractFormulaManager amgr;
  private Map<Vector<Integer>, Integer> abstractCex;

  private int numAbstractStates = 0; // for statistics

  // this is used for deciding how much of the ART to undo after refinement
  // private Deque<ExplicitAbstractElement> lastErrorPath;
  // private int samePathAlready = 0;

  private boolean notEnoughPredicatesFlag = false;

  public PredicateAbstractionRefiner(final ConfigurableProgramAnalysis pCpa) throws CPAException {
    super(pCpa);
    
    ConfigurableProgramAnalysis cpa = this.getArtCpa().getWrappedCPA();
    
    if (cpa instanceof PredicateAbstractionCPA) {
      mCpa = (PredicateAbstractionCPA)cpa;
    
    } else {
      PredicateAbstractionCPA predabsCPA = null;
      if (pCpa instanceof CompositeCPA) {
        for (ConfigurableProgramAnalysis compCPA : ((CompositeCPA)cpa).getComponentCPAs()) {
          if (compCPA instanceof PredicateAbstractionCPA) {
            predabsCPA = (PredicateAbstractionCPA)cpa;
            break;
          }
        }
      }
      if (predabsCPA == null) {
        throw new CPAException(getClass().getSimpleName() + " needs a PredicateAbstractionCPA");
      }
    }
    
    amgr = mCpa.getAbstractFormulaManager();
    abstractCex = new HashMap<Vector<Integer>, Integer>();
  }

  @Override
  public ARTElement performRefinement(ReachedElements pReached, Path pPath) {

    Pair<ARTElement, CFAEdge>[] pathArray;

    pathArray = getPathArray(pPath);

    CounterexampleTraceInfo info =
      amgr.buildCounterexampleTrace(
          mCpa.getFormulaManager(), pathArray);

    assert(info != null);

    if (info.isSpurious()) {
      LazyLogger.log(CustomLogLevel.SpecificCPALevel,
          "Found spurious error trace, refining the abstraction");
      return performRefinement(pReached, pPath, pathArray, info);

    } else {
      CPAMain.cpaStats.setErrorReached(true);
      return null;
    }
  }


  private ARTElement performRefinement(ReachedElements pReached, Path pPath,
      Pair<ARTElement, CFAEdge>[] pPathArray,
      CounterexampleTraceInfo pInfo) {
    LazyLogger.log(LazyLogger.DEBUG_1, "STARTING REFINEMENT");
    UpdateablePredicateMap curpmap =
      (UpdateablePredicateMap)mCpa.getPredicateMap();

    assert(pReached.getLastElement() instanceof ARTElement);
    ARTElement lastElem = (ARTElement)pReached.getLastElement();

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
        root = (ARTElement)pReached.getFirstElement();
      }
    } else {
      //samePathAlready  = 0;
    }

    return root;
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
