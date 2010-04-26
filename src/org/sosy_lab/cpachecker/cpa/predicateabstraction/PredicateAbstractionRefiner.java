/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.predicateabstraction;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Level;

import org.sosy_lab.cpachecker.util.symbpredabstraction.UpdateablePredicateMap;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.Predicate;
import org.sosy_lab.cpachecker.util.symbpredabstraction.trace.CounterexampleTraceInfo;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;

import org.sosy_lab.common.Pair;

import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.cpa.art.ARTReachedSet;
import org.sosy_lab.cpachecker.cpa.art.AbstractARTBasedRefiner;
import org.sosy_lab.cpachecker.cpa.art.Path;
import org.sosy_lab.cpachecker.core.CPAchecker;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class PredicateAbstractionRefiner extends AbstractARTBasedRefiner {

  private PredicateAbstractionCPA mCpa;
  private PredicateAbstractionFormulaManager amgr;
  private Map<Vector<Integer>, Integer> abstractCex;

//  private int numAbstractStates = 0; // for statistics

  // this is used for deciding how much of the ART to undo after refinement
  // private Deque<ExplicitAbstractElement> lastErrorPath;
  // private int samePathAlready = 0;

//  private boolean notEnoughPredicatesFlag = false;

  public PredicateAbstractionRefiner(ConfigurableProgramAnalysis pCpa) throws CPAException {
    super(pCpa);

    mCpa = this.getArtCpa().retrieveWrappedCpa(PredicateAbstractionCPA.class);
    if (mCpa == null) {
      throw new CPAException(getClass().getSimpleName() + " needs a PredicateAbstractionCPA");
    }
    amgr = mCpa.getPredAbsFormulaManager();
    abstractCex = new HashMap<Vector<Integer>, Integer>();
  }

  @Override
  public boolean performRefinement(ARTReachedSet pReached, Path pPath) {

    Pair<ARTElement, CFAEdge>[] pathArray;

    pathArray = getPathArray(pPath);

    CounterexampleTraceInfo info =
      amgr.buildCounterexampleTrace(pathArray);

    assert(info != null);

    if (info.isSpurious()) {
      CPAchecker.logger.log(Level.FINEST,
      "Found spurious error trace, refining the abstraction");

      ARTElement refinementRoot = performRefinement(pReached, pPath, pathArray, info);
      assert refinementRoot != null;

      pReached.removeSubtree(refinementRoot);
      return true;
    } else {
      // we have a real error
      CPAchecker.logger.log(Level.FINEST, "Error trace is not spurious");
      return false;
    }
  }


  private ARTElement performRefinement(ARTReachedSet pReached, Path pPath,
      Pair<ARTElement, CFAEdge>[] pPathArray,
      CounterexampleTraceInfo pInfo) {
    CPAchecker.logger.log(Level.ALL, "DEBUG_1", "STARTING REFINEMENT");
    UpdateablePredicateMap curpmap =
      (UpdateablePredicateMap)mCpa.getPredicateMap();


    ARTElement root = null;
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
//          notEnoughPredicatesFlag = true;
          assert(false);
          System.exit(1);
        } else {
          root = firstInterpolant;
        }
      } else {
        assert(firstInterpolant != null);
        CPAchecker.logger.log(Level.FINEST,
        "Restarting ART from scratch");
        // if the root is theinitial element, we add
        // its child as the root so that refinement algorithm
        // does not try to add initial element's parent
        // which is null
        ARTElement initialElement = pReached.getFirstElement();
        assert(initialElement.getChildren().size() == 1);
        root = (ARTElement)initialElement.getChildren().toArray()[0];
      }
    } else {
      //samePathAlready  = 0;
    }

    assert root != null;
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

  @SuppressWarnings("unchecked")
  private Pair<ARTElement, CFAEdge>[] getPathArray(
      Path pPath) {

    Pair<ARTElement, CFAEdge>[] array = pPath.toArray(new Pair[pPath.size()]);
    /*
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
     */

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
