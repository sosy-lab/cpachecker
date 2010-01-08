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
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import symbpredabstraction.interfaces.AbstractFormula;
import symbpredabstraction.interfaces.Predicate;
import cfa.objectmodel.CFAEdge;
import cmdline.CPAMain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.TransferRelation;
import exceptions.CPATransferException;
/**
 * TransferRelation for explicit-state lazy abstraction. This is the most
 * complex of the CPA-related classes, and where the analysis is actually
 * performed.
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public class PredicateAbstractionTransferRelation implements TransferRelation {

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

    PredicateAbstractionAbstractElement succ = new PredicateAbstractionAbstractElement(cpa);

    // if e is the end of a function, we must find the correct return
    // location
    //  if (isFunctionEnd(e)) {
    //  CFANode retNode = e.topContextLocation();
    //  if (!succLoc.equals(retNode)) {
    //  CPAMain.logManager.log(Level.ALL, "DEBUG_1",
    //  "Return node for this call is: ", retNode,
    //  ", but edge leads to: ", succLoc, ", returning BOTTOM");
    //  return domain.getBottomElement();
    //  }
    //  }

    //  succ.setContext(e.getContext(), false);
    //  if (isFunctionEnd(e)) {
    //  succ.popContext();
    //  }

    PredicateAbstractionAbstractFormulaManager amgr = cpa.getPredAbsFormulaManager();
    AbstractFormula abstraction = amgr.buildAbstraction(
        e, succ, edge, predicates);
    succ.setAbstraction(abstraction);
    //  succ.setParent(e);

    if (CPAMain.logManager.getLogLevel().intValue() <= Level.ALL.intValue()) {
      CPAMain.logManager.log(Level.ALL, "DEBUG_1", "COMPUTED ABSTRACTION:",
          amgr.toConcrete(abstraction));
    }

    if (cpa.getAbstractFormulaManager().isFalse(abstraction)) {
      return domain.getBottomElement();
    } else {
      return succ;
    }
  }

  private AbstractElement getAbstractSuccessor(AbstractElement element,
      CFAEdge cfaEdge, Precision prec) throws CPATransferException {
    CPAMain.logManager.log(Level.FINEST, 
        "Getting Abstract Successor of element: ", element,
        " on edge: ", cfaEdge.getRawStatement());

    // To get the successor, we compute the predicate abstraction of the
    // formula of element plus all the edges that connect any of the
    // inner nodes of the summary of element to any inner node of the
    // destination
    PredicateAbstractionAbstractElement e = (PredicateAbstractionAbstractElement)element;

    // bottom produces bottom
    if (domain.getBottomElement().equals(e)) {
      return e;
    }

    AbstractElement ret = buildSuccessor(e, cfaEdge);

    CPAMain.logManager.log(Level.FINEST,
        "Successor is: ", ret);

    return ret;
  }

  @Override
  public Collection<AbstractElement> getAbstractSuccessors(
      AbstractElement element, Precision prec, CFAEdge cfaEdge) throws CPATransferException {
    return Collections.singleton(getAbstractSuccessor(element, cfaEdge, prec));
  }

  @Override
  public AbstractElement strengthen(AbstractElement element,
      List<AbstractElement> otherElements, CFAEdge cfaEdge,
      Precision precision) {    
    return null;
  }
}
