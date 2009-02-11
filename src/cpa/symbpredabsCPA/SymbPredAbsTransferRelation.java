/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker. 
 *
 *  Copyright (C) 2007-2008  Dirk Beyer and Erkan Keremoglu.
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
 *    http://www.cs.sfu.ca/~dbeyer/CPAchecker/
 */
package cpa.symbpredabsCPA;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import logging.CustomLogLevel;
import logging.LazyLogger;
import symbpredabstraction.AbstractReachabilityTree;
import symbpredabstraction.AbstractionPathList;
import symbpredabstraction.BDDMathsatSymbPredAbstractionAbstractManager;
import symbpredabstraction.PathFormula;
import cfa.objectmodel.CFAEdge;
import cfa.objectmodel.CFAErrorNode;
import cfa.objectmodel.CFAFunctionDefinitionNode;
import cfa.objectmodel.CFANode;
import cfa.objectmodel.c.CallToReturnEdge;
import cfa.objectmodel.c.FunctionCallEdge;
import cfa.objectmodel.c.ReturnEdge;
import cmdline.CPAMain;

import common.Pair;

import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.AbstractElementWithLocation;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.TransferRelation;
import cpa.symbpredabs.AbstractFormula;
import cpa.symbpredabs.AbstractFormulaManager;
import cpa.symbpredabs.CounterexampleTraceInfo;
import cpa.symbpredabs.Predicate;
import cpa.symbpredabs.PredicateMap;
import cpa.symbpredabs.SSAMap;
import cpa.symbpredabs.SymbolicFormula;
import cpa.symbpredabs.SymbolicFormulaManager;
import cpa.symbpredabs.UpdateablePredicateMap;
import exceptions.CPAException;
import exceptions.CPATransferException;
import exceptions.ErrorReachedException;
import exceptions.RefinementNeededException;
import exceptions.SymbPredAbstTransferException;
import exceptions.UnrecognizedCFAEdgeException;

/**
 * Transfer relation for symbolic predicate abstraction. It makes a case
 * split to compute the abstract state. If the new abstract state is 
 * computed for an abstraction location we compute the abstraction and 
 * on the given set of predicates, otherwise we just update the path formula
 * and do not compute the abstraction.
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it> and Erkan
 */
public class SymbPredAbsTransferRelation implements TransferRelation {

  private int numAbstractStates = 0; // for statistics
  public static long totalTimeForPFCopmutation = 0;

  private SymbPredAbsAbstractDomain domain;
  // formula managers
  private SymbolicFormulaManager symbolicFormulaManager;
  private AbstractFormulaManager abstractFormulaManager;

  /** ART to construct counter-examples */
  private AbstractReachabilityTree abstractTree;

  public SymbPredAbsTransferRelation(AbstractDomain d, SymbolicFormulaManager symFormMan, AbstractFormulaManager abstFormMan) {
    domain = (SymbPredAbsAbstractDomain) d;
    abstractFormulaManager = abstFormMan;
    symbolicFormulaManager = symFormMan;
    abstractTree = new AbstractReachabilityTree();
  }

  @Override
  public List<AbstractElementWithLocation> getAllAbstractSuccessors(
      AbstractElementWithLocation element, Precision prec) throws CPAException, CPATransferException {
    throw new CPAException ("Cannot get all abstract successors from non-location domain");
  }

  @Override
  public AbstractElement getAbstractSuccessor(AbstractElement element,
                                              CFAEdge cfaEdge, Precision prec) throws CPATransferException {
    //System.out.println(cfaEdge);
    AbstractElement ret = buildSuccessor(element, cfaEdge);
    return ret;
  }

  // abstract post operation
  private AbstractElement buildSuccessor (AbstractElement element,
                                          CFAEdge edge) throws CPATransferException {

    AbstractElement newElement = null;
    CFANode succLoc = edge.getSuccessor();

    // if the successor is not an abstraction location
    if (!isAbstractionLocation(succLoc)) {
      try {
        try {
          // compute new abstract state for non-abstraction location
          newElement = handleNonAbstractionLocation(element, edge);
        } catch (UnrecognizedCFAEdgeException e) {
          e.printStackTrace();
        }
      } catch (SymbPredAbstTransferException e) {
        e.printStackTrace();
      }
    }
    // this is an abstraction location
    else {
      try {
        newElement = handleAbstractionLocation(element, edge);
      } catch (UnrecognizedCFAEdgeException e) {
        e.printStackTrace();
      }
    }
    assert(newElement != null);
    return newElement;

  }

  /**
   * Computes element -(op)-> newElement where edge = (l1 -(op)-> l2) and l2 
   * is not an abstraction location. 
   * Only pfParents and pathFormula are updated and set as returned element's
   * instances in this method, all other values for the previous abstract
   * element is copied.
   * 
   * @param pElement is the last abstract element
   * @param edge edge of the operation
   * @return computed abstract element
   * @throws UnrecognizedCFAEdgeException if edge is not recognized
   * @throws CPATransferException if we don't handle this operation
   */
  private AbstractElement handleNonAbstractionLocation(AbstractElement pElement, CFAEdge edge)
  throws SymbPredAbstTransferException, UnrecognizedCFAEdgeException {

    SymbPredAbsAbstractElement element = (SymbPredAbsAbstractElement) pElement;

    long start = System.currentTimeMillis();
    // compute new pathFormula with the operation on the edge
    PathFormula pf = toPathFormula(symbolicFormulaManager.makeAnd(
        element.getPathFormula().getSymbolicFormula(),
        edge, element.getPathFormula().getSsa(), false, false));
    long end = System.currentTimeMillis();
    totalTimeForPFCopmutation = totalTimeForPFCopmutation + (end-start);
    assert(pf != null);

    // update pfParents list
    List<Integer> newPfParents = new ArrayList<Integer>();
    newPfParents.add(edge.getPredecessor().getNodeNumber());

    // create the new abstract element for non-abstraction location
    SymbPredAbsAbstractElement newElement = new SymbPredAbsAbstractElement(
        // set 'domain' to domain
        // set 'isAbstractionLocation' to false
        // set 'abstractionLocation' to last element's abstractionLocation since they are same
        // set 'pathFormula' to pf - the updated pathFormula -
        // set 'pfParents' to newPfParents - the updated list of nodes that constructed the pathFormula -
        domain, false, element.getAbstractionLocation(), pf, newPfParents,
        // set 'initAbstractionFormula' and 'abstraction' to last element's values, they don't change
        element.getInitAbstractionFormula(), element.getAbstraction(),
        // set 'abstractionPathList', 'artParent', and 'predicates' to last element's values, they don't change
        element.getAbstractionPathList(), element.getArtParent(),  element.getPredicates());

    // set and update maxIndex for ssa
    newElement.setMaxIndex(element.getMaxIndex());
    SSAMap ssa1 = pf.getSsa();
    newElement.updateMaxIndex(ssa1);

    return newElement;
  }

  /**
   * Computes element -(op)-> newElement where edge = (l1 -(op)-> l2) and l2 
   * is an abstraction location. 
   * We set newElement's 'abstractionLocation' to edge.successor(), 
   * its newElement's 'pathFormula' to true, its 'initAbstractionFormula' to
   * the 'pathFormula' of element, its 'abstraction' to newly computed abstraction
   * over predicates we get from {@link PredicateMap#getRelevantPredicates(CFANode newElement)},
   * its 'abstractionPathList' to edge.successor() concatenated to element's 'abstractionPathList',
   * and its 'artParent' to element.
   * 
   * @param pElement is the last abstract element
   * @param edge edge of the operation
   * @return computed abstract element
   * @throws UnrecognizedCFAEdgeException if edge is not recognized
   * @throws CPATransferException if we don't handle this operation
   */
  private AbstractElement handleAbstractionLocation(AbstractElement pElement, CFAEdge edge) 
  throws UnrecognizedCFAEdgeException, CPATransferException {

    SymbPredAbsAbstractElement element = (SymbPredAbsAbstractElement) pElement;

    BDDMathsatSymbPredAbstractionAbstractManager bddAbstractFormulaManager  = (BDDMathsatSymbPredAbstractionAbstractManager)abstractFormulaManager;

    // set a new path formula and initialize it as true 
    SSAMap ssamap = new SSAMap();
    PathFormula pf = new PathFormula(symbolicFormulaManager.makeTrue(), ssamap);

    // add the new abstaction location to the abstractionPath
    AbstractionPathList newAbstractionPath = new AbstractionPathList();
    newAbstractionPath.copyFromExisting(element.getAbstractionPathList());
    newAbstractionPath.addToList(edge.getSuccessor().getNodeNumber());

    // this will be the initial abstraction formula that we will use 
    // to compute the abstraction. Say this formula is pf, and the abstraction
    // from last element is af, then we use "pf AND af" to compute the new
    // abstraction
    PathFormula initAbstractionFormula;
    // abstraction from last element
    AbstractFormula abstraction = element.getAbstraction();

    // get predicate map
    PredicateMap pmap = domain.getCPA().getPredicateMap();

    // we do a case split here because initAbstractionFormula and pathFormula for a function call
    // node, function return node and other nodes might be different
    if(edge instanceof FunctionCallEdge){
      // compute the initAbstractionFormula for function calls
      initAbstractionFormula = toPathFormula(symbolicFormulaManager.makeAnd(
          element.getPathFormula().getSymbolicFormula(), 
          edge, element.getPathFormula().getSsa(), false, false));
    }
    else if(edge instanceof ReturnEdge){
      // compute the initAbstractionFormula for return edges
      CallToReturnEdge summaryEdge = edge.getSuccessor().getEnteringSummaryEdge();
      pf = toPathFormula(symbolicFormulaManager.makeAnd(symbolicFormulaManager.makeTrue(), 
          edge, new SSAMap(), false, false));

      initAbstractionFormula = toPathFormula(symbolicFormulaManager.makeAnd(
          element.getPathFormula().getSymbolicFormula(), 
          summaryEdge, element.getPathFormula().getSsa(), false, false));

      // TODO handle returning from functions
//    SymbPredAbsAbstractElement previousElem = (SymbPredAbsAbstractElement)summaryEdge.extractAbstractElement("SymbPredAbsAbstractElement");
//    MathsatSymbolicFormulaManager mmgr = (MathsatSymbolicFormulaManager) symbolicFormulaManager;
//    AbstractFormula ctx = previousElem.getAbstraction();
//    MathsatSymbolicFormula fctx = (MathsatSymbolicFormula)mmgr.instantiate(abstractFormulaManager.toConcrete(mmgr, ctx), null);
    }
    else{
      initAbstractionFormula = element.getPathFormula();
    }

    // TODO add cartesian abstraction
    // compute boolean abstraction
    AbstractFormula newAbstraction = bddAbstractFormulaManager.buildAbstraction(
        symbolicFormulaManager, abstraction, initAbstractionFormula, 
        pmap.getRelevantPredicates(edge.getSuccessor()), null);

    SymbPredAbsAbstractElement newElement = new SymbPredAbsAbstractElement(
        // set 'domain' to domain
        // set 'isAbstractionNode' to true, this is an abstraction node
        // set 'abstractionLocation' to edge.getSuccessor()
        // set 'pathFormula' to pf which computed above
        domain, true, edge.getSuccessor(), pf, 
        // 'pfParents' is not instantiated for abstraction locations
        // set 'initAbstractionFormula' to  initAbstractionFormula computed above
        // set 'abstraction' to newly computed abstraction
        // set 'abstractionPathList' to updated pathList
        null, initAbstractionFormula, newAbstraction, newAbstractionPath,
        // we don't set 'artParent' here, we'll update it below
        // set 'pmap' to the global predicate map to get local predicates
        null, pmap);

    SSAMap maxIndex = new SSAMap();
    newElement.setMaxIndex(maxIndex);
    newElement.setMaxIndex(maxIndex);  

    // if the abstraction is false, return bottom element
    if (abstractFormulaManager.isFalse(newAbstraction)) {
      return domain.getBottomElement();
    }
    else{
      newElement.setArtParent(element);
      // TODO we can remove this check I think
      // add to ART if this is not bottom
      if (newElement != domain.getBottomElement()) {
        abstractTree.addChild(element, newElement);
      }
      ++numAbstractStates;
      // we reach error state
      if (edge.getSuccessor() instanceof CFAErrorNode) {
        if (CPAMain.cpaConfig.getBooleanValue(
        "cpas.symbpredabs.abstraction.norefinement")) {
          CPAMain.cpaStats.setErrorReached(true);
          throw new ErrorReachedException(
          "Reached error location, but refinement disabled");
        }
        // hit the error location
        // first we build the abstract path
        Deque<SymbPredAbsAbstractElement> path = new LinkedList<SymbPredAbsAbstractElement>();
        path.addFirst(newElement);
        SymbPredAbsAbstractElement artParent = newElement.getArtParent();
        while (artParent != null) {
          path.addFirst(artParent);
          artParent = artParent.getArtParent();
        }
        // build the counterexample
        CounterexampleTraceInfo info = bddAbstractFormulaManager.buildCounterexampleTrace(
            symbolicFormulaManager, path);
        // if error is spurious refine
        if (info.isSpurious()) {
          LazyLogger.log(CustomLogLevel.SpecificCPALevel,
              "Found spurious error trace, refining the ",
          "abstraction");
          performRefinement(path, info);
        }
        // we have a real error
        else {
          LazyLogger.log(CustomLogLevel.SpecificCPALevel,
              "REACHED ERROR LOCATION!: ", newElement,
          " RETURNING BOTTOM!");
          CPAMain.cpaStats.setErrorReached(true);
          throw new ErrorReachedException(
              info.getConcreteTrace().toString());
        }
        return domain.getBottomElement();
      }
      // if this is not an error location, return newElement
      return newElement;
    }
  }

  private void performRefinement(Deque<SymbPredAbsAbstractElement> path,
                                 CounterexampleTraceInfo info) throws CPATransferException {
    int numSeen = 0;
//  if (seenAbstractCounterexamples.containsKey(pth)) {
//  numSeen = seenAbstractCounterexamples.get(pth);
//  }
//  seenAbstractCounterexamples.put(pth, numSeen+1);

    UpdateablePredicateMap curpmap =
      (UpdateablePredicateMap)domain.getCPA().getPredicateMap();
    AbstractElement root = null;
    AbstractElement firstInterpolant = null;
    for (SymbPredAbsAbstractElement e : path) {
      Collection<Predicate> newpreds = info.getPredicatesForRefinement(e);
      if (firstInterpolant == null && newpreds.size() > 0) {
        firstInterpolant = e;
      }
      if (curpmap.update(e.getAbstractionLocation(), newpreds)) {
        if (root == null) {
          root = e.getArtParent();
        }
      }
    }
    if (root == null) {
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
      root = abstractTree.findHighest(loc);
    }
    assert(root != null);
    if (CPAMain.cpaConfig.getBooleanValue("analysis.bfs")) {
      // TODO When using bfs traversal, we would have to traverse the ART
      // computed so far, and check for each leaf whether to re-add it
      // to the waiting list or not, similarly to what Blast does
      // (file psrc/be/modelChecker/lazyModelChecker.ml, function
      // update_tree_after_refinment). But for now, for simplicity we
      // just restart from scratch
      root = path.getFirst();
    }

    assert(root != null);
    //root = path.getFirst();
    Collection<AbstractElement> toWaitlist = new HashSet<AbstractElement>();
    toWaitlist.add(root);
    Collection<SymbPredAbsAbstractElement> toUnreachTmp =
      abstractTree.getSubtree(root, true, false);
    Vector<AbstractElement> toUnreach = new Vector<AbstractElement>();
    toUnreach.ensureCapacity(toUnreachTmp.size());
//  SymbPredAbsCPA cpa = domain.getCPA();
    for (AbstractElement e : toUnreachTmp) {
      toUnreach.add(e);
      // TODO handle later
//    Set<SymbPredAbsAbstractElement> cov = cpa.getCoveredBy(
//    (SymbPredAbsAbstractElement)e);
//    for (AbstractElement c : cov) {
//    if (!((SymbPredAbsAbstractElement)c).isDescendant(
//    (SymbPredAbsAbstractElement)root)) {
//    toWaitlist.add(c);
//    }
//    }
//    cpa.uncoverAll((SymbPredAbsAbstractElement)e);
    }
//  Collection<AbstractElement> toUnreach = new Vector<AbstractElement>();
//  boolean add = false;
//  for (AbstractElement e : path) {
//  if (add) {
//  toUnreach.add(e);
//  } else if (e == root) {
//  add = true;
//  }
//  }
    LazyLogger.log(LazyLogger.DEBUG_1, "REFINEMENT - toWaitlist: ", root);
    LazyLogger.log(LazyLogger.DEBUG_1, "REFINEMENT - toUnreach: ",
        toUnreach);
    throw new RefinementNeededException(null, null);
  }

  /**
   * @param succLoc successor CFA location.
   * @return true if succLoc is an abstraction location. For now a location is 
   * an abstraction location if it has an incoming loop-back edge, if it is
   * an error node, if it is the start node of a function or if it is the call
   * site from a function call.
   */
  public boolean isAbstractionLocation(CFANode succLoc) {
    if (succLoc.isLoopStart() || succLoc instanceof CFAErrorNode
        || succLoc.getNumLeavingEdges() == 0) {
      return true;
    } else if (succLoc instanceof CFAFunctionDefinitionNode) {
      return true;
    } else if (succLoc.getEnteringSummaryEdge() != null) {
      return true;
    }
    else {
      return false;
    }
  }

  /**
   * Takes a {@link Pair} of {@link SymbolicFormula} and {@link SSAMap}, and
   * converts it to a {@link PathFormula}
   * @param pair
   * @return
   */
  private PathFormula toPathFormula(Pair<SymbolicFormula, SSAMap> pair) {
    return new PathFormula(pair.getFirst(), pair.getSecond());
  }

  public int getNumAbstractStates() {
    return numAbstractStates;
  }
}
