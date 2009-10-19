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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

import common.Pair;
import common.Triple;

import cpa.common.CPAAlgorithm;
import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.AbstractElementWithLocation;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.TransferRelation;
import cpa.symbpredabs.AbstractFormula;
import cpa.symbpredabs.AbstractFormulaManager;
import cpa.symbpredabs.PredicateMap;
import cpa.symbpredabs.SSAMap;
import cpa.symbpredabs.SymbolicFormula;
import cpa.symbpredabs.SymbolicFormulaManager;
import exceptions.CPAException;
import exceptions.CPATransferException;
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

  public static long nonAbstractionTime = 0;
  public static long abstractionTime = 0;
  public static long abstTime1 = 0;
  public static long abstTime2 = 0;
  public static long abstTime3 = 0;
  public static long abstTime4 = 0;
  public static long abstTime5 = 0;
  
  
  private int numAbstractStates = 0; // for statistics
  public static long totalTimeForPFCopmutation = 0;
  public static long totalTimeForActualPfComputation = 0;
  public static long updateSSATime = 0;
  public static long newElementCreationTime = 0;

  private SymbPredAbsAbstractDomain domain;
  // formula managers
  private SymbolicFormulaManager symbolicFormulaManager;
  private AbstractFormulaManager abstractFormulaManager;

  // map from a node to path formula
  // used to not compute the formula again
  // the first integer in the key is parent element's node id
  // the second integer is current element's node id
  // the third is the sucessor element's node id
  private Map<Triple<Integer, Integer, Integer>, PathFormula> pathFormulaMapHash =
    new HashMap<Triple<Integer,Integer,Integer>, PathFormula>();

  public SymbPredAbsTransferRelation(AbstractDomain d, SymbolicFormulaManager symFormMan, AbstractFormulaManager abstFormMan) {
    domain = (SymbPredAbsAbstractDomain) d;
    abstractFormulaManager = abstFormMan;
    symbolicFormulaManager = symFormMan;
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
          long start = System.currentTimeMillis();
          // compute new abstract state for non-abstraction location
          newElement = handleNonAbstractionLocation(element, edge);
          long end = System.currentTimeMillis();
          nonAbstractionTime = nonAbstractionTime + (end - start);
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
        long start = System.currentTimeMillis();
        newElement = handleAbstractionLocation(element, edge);
        long end = System.currentTimeMillis();
        abstractionTime = abstractionTime + (end - start); 
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
    // id of parent
    int parentElementNodeId = element.getAbstractionLocation().getNodeNumber();
    // id of element's node
    int currentNodeId = edge.getPredecessor().getNodeNumber();
    // id of sucessor element's node
    int successorNodeId = edge.getSuccessor().getNodeNumber();
    PathFormula pf = null;

    long start = System.currentTimeMillis();
    Triple<Integer, Integer, Integer> tripleOfElementNodes = 
      new Triple<Integer, Integer, Integer>(parentElementNodeId, currentNodeId, successorNodeId);
    if(pathFormulaMapHash.containsKey(tripleOfElementNodes)){
      pf = pathFormulaMapHash.get(tripleOfElementNodes);
    }
    else{
      long startComp = System.currentTimeMillis();
      // compute new pathFormula with the operation on the edge
      pf = toPathFormula(symbolicFormulaManager.makeAnd(
          element.getPathFormula().getSymbolicFormula(),
          edge, element.getPathFormula().getSsa(), false, false));
      pathFormulaMapHash.put(tripleOfElementNodes, pf);
      long endComp = System.currentTimeMillis();
      totalTimeForActualPfComputation = totalTimeForActualPfComputation + (endComp-startComp);
    }
    assert(pf != null);
    long end = System.currentTimeMillis();
    totalTimeForPFCopmutation = totalTimeForPFCopmutation + (end-start);
    // update pfParents list
    List<Integer> newPfParents = new ArrayList<Integer>();
    newPfParents.add(currentNodeId);

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
    // TODO check
    //newElement.setMaxIndex(element.getMaxIndex());
//    SSAMap ssa1 = pf.getSsa();
//    newElement.updateMaxIndex(ssa1);
    
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

    long start1 = System.currentTimeMillis();
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
    long start2 = System.currentTimeMillis();
    abstTime1 = abstTime1 + (start2 - start1);
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
    long start3 = System.currentTimeMillis();
    abstTime2 = abstTime2 + (start3 - start2);
    // TODO add cartesian abstraction
    // compute boolean abstraction
    AbstractFormula newAbstraction = bddAbstractFormulaManager.buildAbstraction(
        symbolicFormulaManager, abstraction, initAbstractionFormula, 
        pmap.getRelevantPredicates(edge.getSuccessor()), null, edge.getSuccessor(), element.getAbstractionPathList());
    long start4 = System.currentTimeMillis();
    abstTime3 = abstTime3 + (start4 - start3);
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

//    SSAMap maxIndex = new SSAMap();
//    newElement.setMaxIndex(maxIndex);

    // if the abstraction is false, return bottom element
    if (abstractFormulaManager.isFalse(newAbstraction)) {
      return domain.getBottomElement();
    }
    else{
      newElement.setArtParent(element);
      // TODO we can remove this check I think
      // add to ART if this is not bottom
//    if (newElement != domain.getBottomElement()) {
//    abstractTree.addChild(element, newElement);
//    }
      ++numAbstractStates;
      // we reach error state
      if (edge.getSuccessor() instanceof CFAErrorNode) {
//        System.out.println("errorfound set to true by symbpredabs");
        CPAAlgorithm.errorFound = true;
        return newElement;
      }
      // if this is not an error location, return newElement
      return newElement;
    }
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

  @Override
  public AbstractElement strengthen(AbstractElement element,
      List<AbstractElement> otherElements, CFAEdge cfaEdge,
      Precision precision) {    
    return null;
  }
}
