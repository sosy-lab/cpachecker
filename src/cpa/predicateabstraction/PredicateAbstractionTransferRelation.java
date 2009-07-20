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
package cpa.predicateabstraction;

import java.util.Collection;
import java.util.List;
import java.util.logging.Level;

import logging.CPACheckerLogger;
import logging.CustomLogLevel;
import logging.LazyLogger;
import predicateabstraction.PredicateAbstractionAbstractFormulaManager;
import cfa.objectmodel.CFAEdge;
import cfa.objectmodel.CFAErrorNode;
import cpa.common.CPAAlgorithm;
import cpa.common.ErrorElement;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.AbstractElementWithLocation;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.TransferRelation;
import cpa.symbpredabs.AbstractFormula;
import cpa.symbpredabs.Predicate;
import cpa.symbpredabs.SymbolicFormulaManager;
import exceptions.CPAException;
import exceptions.CPATransferException;


/**
 * TransferRelation for explicit-state lazy abstraction. This is the most
 * complex of the CPA-related classes, and where the analysis is actually
 * performed.
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public class PredicateAbstractionTransferRelation implements TransferRelation {

//  class Path {
//    Vector<Integer> elemIds;
//
//    public Path(Deque<PredicateAbstractionAbstractElement> cex) {
//      elemIds = new Vector<Integer>();
//      elemIds.ensureCapacity(cex.size());
//      for (PredicateAbstractionAbstractElement e : cex) {
//        elemIds.add(e.getLocation().getNodeNumber());
//      }
//    }
//
//    @Override
//    public boolean equals(Object o) {
//      if (o == this) return true;
//      if (o instanceof Path) {
//        return elemIds.equals(((Path)o).elemIds);
//      }
//      return false;
//    }
//
//    @Override
//    public int hashCode() {
//      return elemIds.hashCode();
//    }
//  }

  private PredicateAbstractionAbstractDomain domain;
//private Map<Path, Integer> abstractCex;

  private int numAbstractStates = 0; // for statistics

  // this is used for deciding how much of the ART to undo after refinement
  // private Deque<ExplicitAbstractElement> lastErrorPath;
  // private int samePathAlready = 0;

  private boolean notEnoughPredicatesFlag = false;

  public PredicateAbstractionTransferRelation(PredicateAbstractionAbstractDomain d) {
    domain = d;
//  abstractTree = new AbstractReachabilityTree();
    // lastErrorPath = null;
//  abstractCex = new HashMap<Path, Integer>();
  }

  public boolean notEnoughPredicates() { return notEnoughPredicatesFlag; }

  public int getNumAbstractStates() { return numAbstractStates; }

  // isFunctionStart and isFunctionEnd are used to manage the call stack
//private boolean isFunctionStart(PredicateAbstractionAbstractElement elem) {
//return (elem.getLocation() instanceof FunctionDefinitionNode);
//}

//private boolean isFunctionEnd(PredicateAbstractionAbstractElement elem) {
//CFANode n = elem.getLocation();
//return (n.getNumLeavingEdges() > 0 &&
//n.getLeavingEdge(0) instanceof ReturnEdge);
//}

  // performs the abstract post operation
  private AbstractElement buildSuccessor(PredicateAbstractionAbstractElement e,
      CFAEdge edge) throws CPATransferException {
    PredicateAbstractionCPA cpa = domain.getCPA();
//    CFANode succLoc = edge.getSuccessor();

    // check whether the successor is an error location: if so, we want
    // to check for feasibility of the path...

    Collection<Predicate> predicates =
      cpa.getPredicateMap().getRelevantPredicates(
          edge.getSuccessor());
//  if (predicates.isEmpty() && e.getParent() != null) {
//  predicates = cpa.getPredicateMap().getRelevantPredicates(
//  e.getParent().getLocation());
//  }

    PredicateAbstractionAbstractElement succ = new PredicateAbstractionAbstractElement();

    // if e is the end of a function, we must find the correct return
    // location
//  if (isFunctionEnd(e)) {
//  CFANode retNode = e.topContextLocation();
//  if (!succLoc.equals(retNode)) {
//  LazyLogger.log(LazyLogger.DEBUG_1,
//  "Return node for this call is: ", retNode,
//  ", but edge leads to: ", succLoc, ", returning BOTTOM");
//  return domain.getBottomElement();
//  }
//  }

//  succ.setContext(e.getContext(), false);
//  if (isFunctionEnd(e)) {
//  succ.popContext();
//  }

    PredicateAbstractionAbstractFormulaManager amgr = cpa.getAbstractFormulaManager();
    AbstractFormula abstraction = amgr.buildAbstraction(
        cpa.getFormulaManager(), e, succ, edge, predicates);
    succ.setAbstraction(abstraction);
//  succ.setParent(e);

    Level lvl = LazyLogger.DEBUG_1;
    if (CPACheckerLogger.getLevel() <= lvl.intValue()) {
      SymbolicFormulaManager mgr = cpa.getFormulaManager();
      LazyLogger.log(lvl, "COMPUTED ABSTRACTION: ",
          amgr.toConcrete(mgr, abstraction));
    }

    if (amgr.isFalse(abstraction)) {
      return domain.getBottomElement();
    } else {
      //++numAbstractStates;
      // if we reach an error state, we want to log this...
//      if (succ.getLocation() instanceof CFAErrorNode) {
//        if (CPAMain.cpaConfig.getBooleanValue(
//        "cpas.symbpredabs.abstraction.norefinement")) {
//          CPAMain.cpaStats.setErrorReached(true);
//          throw new ErrorReachedException(
//          "Reached error location, but refinement disabled");
//        }
//        // oh oh, reached error location. Let's check whether the
//        // trace is feasible or spurious, and in case refine the
//        // abstraction
//        //
//        // first we build the abstract path
//        Deque<PredicateAbstractionAbstractElement> path =
//          new LinkedList<PredicateAbstractionAbstractElement>();
//        path.addFirst(succ);
//        PredicateAbstractionAbstractElement parent = succ.getParent();
//        while (parent != null) {
//          path.addFirst(parent);
//          parent = parent.getParent();
//        }
//        CounterexampleTraceInfo info =
//          amgr.buildCounterexampleTrace(
//              cpa.getFormulaManager(), path);
//        if (info.isSpurious()) {
//          LazyLogger.log(CustomLogLevel.SpecificCPALevel,
//              "Found spurious error trace, refining the ",
//          "abstraction");
//          performRefinement(path, info);
//        } else {
//          LazyLogger.log(CustomLogLevel.SpecificCPALevel,
//              "REACHED ERROR LOCATION!: ", succ,
//          " RETURNING BOTTOM!");
//          CPAMain.cpaStats.setErrorReached(true);
//          throw new ErrorReachedException(
//              info.getConcreteTrace().toString());
//        }
//        return domain.getBottomElement();
//      }

//      if (isFunctionStart(succ)) {
//        // we push into the context the return location, which is
//        // the successor location of the summary edge
//        assert(e.getLocation().getLeavingSummaryEdge() != null);
//        CFANode retNode = null;
//        retNode =
//          e.getLocation().getLeavingSummaryEdge().getSuccessor();
//        succ.pushContext(e.getAbstraction(), retNode);
////      for (CFANode l : e.getLeaves()) {
////      if (l instanceof FunctionDefinitionNode) {
////      assert(l.getNumLeavingEdges() == 1);
////      assert(l.getNumEnteringEdges() == 1);
//
////      CFAEdge ee = l.getLeavingEdge(0);
////      InnerCFANode n = (InnerCFANode)ee.getSuccessor();
////      if (n.getSummaryNode().equals(succ.getLocation())) {
////      CFANode pr = l.getEnteringEdge(0).getPredecessor();
////      CallToReturnEdge ce = pr.getLeavingSummaryEdge();
////      //assert(ce != null);
////      if (ce != null) {
////      retNode = ((InnerCFANode)ce.getSuccessor()).
////      getSummaryNode();
////      break;
////      }
////      }
////      }
////      }
//        //assert(retNode != null);
//        if (retNode != null) {
////        LazyLogger.log(LazyLogger.DEBUG_3, "PUSHING CONTEXT TO ", succ,
////        ": ", cpa.getAbstractFormulaManager().toConcrete(
////        cpa.getFormulaManager(),
////        succ.getAbstraction()));
////        //succ.getContext().push(succ.getAbstraction());
////        succ.pushContext(succ.getAbstraction(), retNode);
//        }
//      }

      return succ;
    }
  }

  @Override
  public AbstractElement getAbstractSuccessor(AbstractElement element,
      CFAEdge cfaEdge, Precision prec) throws CPATransferException {
    LazyLogger.log(CustomLogLevel.SpecificCPALevel,
        "Getting Abstract Successor of element: ", element,
        " on edge: ", cfaEdge.getRawStatement());

//    System.out.println("---------------------------");
//    System.out.println(cfaEdge);
//    System.out.println("___________________________");
    
    if(cfaEdge.getSuccessor() instanceof CFAErrorNode){
      CPAAlgorithm.errorFound = true;
      return new ErrorElement(cfaEdge.getSuccessor());
    }
    
//  if (!abstractTree.contains((AbstractElementWithLocation)element)) {
//  ++numAbstractStates;
//  }

    // To get the successor, we compute the predicate abstraction of the
    // formula of element plus all the edges that connect any of the
    // inner nodes of the summary of element to any inner node of the
    // destination
    PredicateAbstractionAbstractElement e = (PredicateAbstractionAbstractElement)element;

    // bottom produces bottom
    if (domain.getBottomElement().equals(e)) {
      return e;
    }

//  CFANode src = e.getLocation();

//  for (int i = 0; i < src.getNumLeavingEdges(); ++i) {
//    CFAEdge edge = src.getLeavingEdge(i);
//  if (edge.equals(cfaEdge)) {
    AbstractElement ret = buildSuccessor(e, cfaEdge);

    LazyLogger.log(CustomLogLevel.SpecificCPALevel,
        "Successor is: ", ret);

//  if (ret != domain.getBottomElement()) {
//  abstractTree.addChild(e, ret);
//  }

    return ret;
//  }
//  }

//    LazyLogger.log(CustomLogLevel.SpecificCPALevel, "Successor is: BOTTOM");
//
//    return domain.getBottomElement();
  }

  @Override
  public List<AbstractElementWithLocation> getAllAbstractSuccessors(
      AbstractElementWithLocation element, Precision prec) throws CPAException, CPATransferException {
    LazyLogger.log(CustomLogLevel.SpecificCPALevel,
        "Getting ALL Abstract Successors of element: ",
        element);
    return null;
//  List<AbstractElementWithLocation> allSucc = new Vector<AbstractElementWithLocation>();
//  PredicateAbstractionAbstractElement e = (PredicateAbstractionAbstractElement)element;


//  assert(!e.isCovered());
//  CFANode src = e.getLocation();

//  for (int i = 0; i < src.getNumLeavingEdges(); ++i) {
//  AbstractElement newe =
//  getAbstractSuccessor(e, src.getLeavingEdge(i), prec);
//  if (newe != domain.getBottomElement()) {
//  allSucc.add((PredicateAbstractionAbstractElement)newe);
//  }
//  }

//  e.setMark();

//  LazyLogger.log(CustomLogLevel.SpecificCPALevel,
//  allSucc.size(), " successors found");

//  return allSucc;
  }

//  public void clearART() {
//    this.abstractTree.clear();
//  }

  @Override
  public void strengthen(AbstractElement element,
                         List<AbstractElement> otherElements, CFAEdge cfaEdge,
                         Precision precision) {    
  }
}
