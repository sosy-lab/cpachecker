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

import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import logging.CustomLogLevel;
import logging.LazyLogger;
import symbpredabstraction.AbstractReachabilityTree;
import symbpredabstraction.BDDMathsatSymbPredAbstractionAbstractManager;
import symbpredabstraction.ParentsList;
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
 * Transfer relation for symbolic lazy abstraction with summaries
 *
 * @author Erkan
 */
public class SymbPredAbsTransferRelation implements TransferRelation {

  private int numAbstractStates = 0; // for statistics
  public static long totalTimeForPFCopmutation = 0;

  private SymbPredAbsAbstractDomain domain;
  // TODO maybe we should move these into CPA later
  // associate a Mathsat Formula Manager with the transfer relation
  private SymbolicFormulaManager symbolicFormulaManager;
  //private BDDMathsatSummaryAbstractManager
  private AbstractFormulaManager abstractFormulaManager;
  // private SymbAbsBDDMathsatAbstractFormulaManager bddMathsatMan;

  private AbstractReachabilityTree abstractTree;
  
  // this is for debugging purposes, also we can use it later when we use
  // mergeJoin for SummaryCPA, keeps line numbers not locations
  public static Set<Integer> extraAbstractionLocations = 
    new HashSet<Integer>();

  public SymbPredAbsTransferRelation(AbstractDomain d, SymbolicFormulaManager symFormMan, AbstractFormulaManager abstFormMan) {

    domain = (SymbPredAbsAbstractDomain) d;
    abstractFormulaManager = abstFormMan;
    symbolicFormulaManager = symFormMan;
    abstractTree = new AbstractReachabilityTree();

    // a set of lines given on property files to mark their successors as 
    // abstraction nodes for debugging purposes

    String lines[] = CPAMain.cpaConfig.getPropertiesArray("abstraction.extraLocations");
    if(lines != null && lines.length > 0){
      for(String line:lines){
        extraAbstractionLocations.add(Integer.getInteger(line));
      }
    }

    // setNamespace("");
    // globalVars = new HashSet<String>();
    // abstractTree = new ART();
  }

  public int getNumAbstractStates() {
    return numAbstractStates;
  }

  // abstract post operation
  private AbstractElement buildSuccessor (SymbPredAbsAbstractElement element,
                                          CFAEdge edge) throws CPATransferException {
    SymbPredAbsAbstractElement newElement = null;
    CFANode succLoc = edge.getSuccessor();

    // check if the successor is an abstraction location
    boolean b = isAbstractionLocation(succLoc);

    if (!b) {
      try {
        newElement = new SymbPredAbsAbstractElement(domain, false,element.getAbstractionLocation(), null, 
            element.getInitAbstractionSet(), element.getAbstraction(), 
            element.getParents(), element.getArtParent(), element.getPredicates());
        try {
          handleNonAbstractionLocation(element, newElement, edge);
        } catch (UnrecognizedCFAEdgeException e) {
          e.printStackTrace();
        }
      } catch (SymbPredAbstTransferException e) {
        e.printStackTrace();
      }
    }

    else {
      newElement = new SymbPredAbsAbstractElement(domain, true, succLoc, null, null, null, null, null, null);
      // register newElement as an abstraction node
      newElement.setAbstractionNode();
      try {
        handleAbstractionLocation(element, newElement, edge);
      } catch (UnrecognizedCFAEdgeException e) {
        e.printStackTrace();
      }
    }
    return newElement;

  }

  private void handleAbstractionLocation(SymbPredAbsAbstractElement element,
                                         SymbPredAbsAbstractElement newElement, CFAEdge edge) throws UnrecognizedCFAEdgeException, CPATransferException {

    BDDMathsatSymbPredAbstractionAbstractManager bddAbstractFormulaManager  = (BDDMathsatSymbPredAbstractionAbstractManager)abstractFormulaManager;
    
    
    SSAMap maxIndex = new SSAMap();
    newElement.setMaxIndex(maxIndex);
    
    SSAMap ssamap = new SSAMap();
    PathFormula pf = new PathFormula(symbolicFormulaManager.makeTrue(), ssamap);
    newElement.setPathFormula(pf);
    newElement.setMaxIndex(maxIndex);
    
    ParentsList parents = element.getParents();
    // add the parent to the list
    ParentsList newParents = new ParentsList();
    newParents.copyFromExisting(parents);
    newElement.setParents(newParents);
    newElement.addParent(edge.getSuccessor().getNodeNumber());

    PathFormula functionUpdatedFormula;
    AbstractFormula abs = element.getAbstraction();

    PredicateMap pmap = domain.getCPA().getPredicateMap();
    newElement.setPredicates(pmap);

    AbstractFormula abst;

    if(edge instanceof FunctionCallEdge){
      PathFormula functionInitFormula = toPathFormula(symbolicFormulaManager.makeAnd(element.getPathFormula().getSymbolicFormula(), 
          edge, element.getPathFormula().getSsa(), false, false));
      newElement.setInitAbstractionSet(functionInitFormula);
      abst = bddAbstractFormulaManager.buildAbstraction(symbolicFormulaManager, abs, functionInitFormula, pmap.getRelevantPredicates(edge.getSuccessor()), null);
    }
    else if(edge instanceof ReturnEdge){

      CallToReturnEdge summaryEdge = edge.getSuccessor().getEnteringSummaryEdge();
      pf = toPathFormula(symbolicFormulaManager.makeAnd(symbolicFormulaManager.makeTrue(), 
          edge, new SSAMap(), false, false));
      newElement.setPathFormula(pf);

      PathFormula functionInitFormula = toPathFormula(symbolicFormulaManager.makeAnd(element.getPathFormula().getSymbolicFormula(), 
          summaryEdge, element.getPathFormula().getSsa(), false, false));

      newElement.setInitAbstractionSet(functionInitFormula);

      // TODO fix later for returning from functions
//    SymbPredAbsAbstractElement previousElem = (SymbPredAbsAbstractElement)summaryEdge.extractAbstractElement("SymbPredAbsAbstractElement");
//    MathsatSymbolicFormulaManager mmgr = (MathsatSymbolicFormulaManager) symbolicFormulaManager;

//    AbstractFormula ctx = previousElem.getAbstraction();
//    MathsatSymbolicFormula fctx = (MathsatSymbolicFormula)mmgr.instantiate(abstractFormulaManager.toConcrete(mmgr, ctx), null);

      abst = bddAbstractFormulaManager.buildAbstraction(symbolicFormulaManager, abs, functionInitFormula, pmap.getRelevantPredicates(edge.getSuccessor()), null);
    }
    else{
      newElement.setInitAbstractionSet(element.getPathFormula());
      functionUpdatedFormula = newElement.getInitAbstractionSet();
      abst = bddAbstractFormulaManager.buildAbstraction(symbolicFormulaManager, abs, functionUpdatedFormula, pmap.getRelevantPredicates(edge.getSuccessor()), null);
    }

    // TODO cartesian abstraction
//  if (CPAMain.cpaConfig.getBooleanValue(
//  "cpas.symbpredabs.abstraction.cartesian")) {
//  abst = computeCartesianAbstraction(element, newElement, edge);
//  }
//  else{

//  abst = computeBooleanAbstraction(element, newElement, edge);
//  }

    newElement.setAbstraction(abst);

    if (abstractFormulaManager.isFalse(abst)) {
      newElement.isBottomElement = true;
      return;
    }
    else{
      newElement.setArtParent(element);
      if (!newElement.isBottomElement) {
        abstractTree.addChild(element, newElement);
      }
      ++numAbstractStates;
      // if we reach an error state, we want to log this...
      if (edge.getSuccessor() instanceof CFAErrorNode) {
        if (CPAMain.cpaConfig.getBooleanValue(
        "cpas.symbpredabs.abstraction.norefinement")) {
          CPAMain.cpaStats.setErrorReached(true);
          throw new ErrorReachedException(
          "Reached error location, but refinement disabled");
        }
        // oh oh, reached error location. Let's check whether the
        // trace is feasible or spurious, and in case refine the
        // abstraction
        //
        // first we build the abstract path
        Deque<SymbPredAbsAbstractElement> path = new LinkedList<SymbPredAbsAbstractElement>();
        path.addFirst(newElement);
        SymbPredAbsAbstractElement artParent = newElement.getArtParent();
        while (artParent != null) {
          path.addFirst(artParent);
          artParent = artParent.getArtParent();
        }
        // TODO traceInfo is a abstractElement -> PredicateList map
        //System.out.println("PATH::::::::::: "+ path);
        CounterexampleTraceInfo info = bddAbstractFormulaManager.buildCounterexampleTrace(
            symbolicFormulaManager, path);
        if (info.isSpurious()) {
          LazyLogger.log(CustomLogLevel.SpecificCPALevel,
              "Found spurious error trace, refining the ",
          "abstraction");
          performRefinement(path, info);
        } else {
          LazyLogger.log(CustomLogLevel.SpecificCPALevel,
              "REACHED ERROR LOCATION!: ", newElement,
          " RETURNING BOTTOM!");
          CPAMain.cpaStats.setErrorReached(true);
          throw new ErrorReachedException(
              info.getConcreteTrace().toString());
        }
        //return domain.getBottomElement();
      }
      //return succ;
    }
  }

  private void handleNonAbstractionLocation(
                                            SymbPredAbsAbstractElement element,
                                            SymbPredAbsAbstractElement newElement, CFAEdge edge)
  throws SymbPredAbstTransferException, UnrecognizedCFAEdgeException {
    // TODO check this (false, false is used when constructing pf for
    // summary nodes)
    newElement.setMaxIndex(element.getMaxIndex());
    PathFormula pf = null;
    long start = System.currentTimeMillis();
    pf = toPathFormula(symbolicFormulaManager.makeAnd(
        element.getPathFormula().getSymbolicFormula(),
        edge, element.getPathFormula().getSsa(), false, false));
    // TODO check these 3 lines
    // SymbolicFormula t1 = pf.getSymbolicFormula();
    long end = System.currentTimeMillis();
    totalTimeForPFCopmutation = totalTimeForPFCopmutation + (end-start);
    SSAMap ssa1 = pf.getSsa();
    assert(pf != null);
    newElement.setPathFormula(pf);
    // TODO check
    newElement.updateMaxIndex(ssa1);
  }

  @Override
  public AbstractElement getAbstractSuccessor(AbstractElement element,
                                              CFAEdge cfaEdge, Precision prec) throws CPATransferException {
    //System.out.println(cfaEdge);
    SymbPredAbsAbstractElement e = (SymbPredAbsAbstractElement)element;
    AbstractElement ret = buildSuccessor(e, cfaEdge);

    return ret;

    //LazyLogger.log(CustomLogLevel.SpecificCPALevel, "Successor is: BOTTOM");

    //return domain.getBottomElement();
  }

  private void performRefinement(Deque<SymbPredAbsAbstractElement> path,
                                 CounterexampleTraceInfo info) throws CPATransferException {
    // TODO we use Path here, check later
//  Path pth = new Path(path);
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
      } else {
        assert(numSeen <= 1);
      }
      CFANode loc = 
        ((SymbPredAbsAbstractElement)firstInterpolant).getAbstractionLocation(); 
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
//    SymbPredAbsCPA cpa = domain.getCPA();
    for (AbstractElement e : toUnreachTmp) {
      toUnreach.add(e);
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

  @Override
  public List<AbstractElementWithLocation> getAllAbstractSuccessors(
      AbstractElementWithLocation element, Precision prec) throws CPAException, CPATransferException {
    throw new CPAException ("Cannot get all abstract successors from non-location domain");
  }

  public boolean isAbstractionLocation(CFANode succLoc) {

    if (succLoc.isLoopStart() || succLoc instanceof CFAErrorNode
        || succLoc.getNumLeavingEdges() == 0) {
      return true;
    } else if (succLoc instanceof CFAFunctionDefinitionNode) {
      return true;
    } else if (succLoc.getEnteringSummaryEdge() != null) {
      return true;
    } else if (extraAbstractionLocations.contains(succLoc.getLineNumber())) {
      return true;
    }
    else {
      return false;
    }
  }

  private PathFormula toPathFormula(Pair<SymbolicFormula, SSAMap> pair) {
    return new PathFormula(pair.getFirst(), pair.getSecond());
  }
}
