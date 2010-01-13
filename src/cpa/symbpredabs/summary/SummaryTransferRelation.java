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
package cpa.symbpredabs.summary;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Level;

import symbpredabstraction.SSAMap;
import symbpredabstraction.UpdateablePredicateMap;
import symbpredabstraction.interfaces.AbstractFormula;
import symbpredabstraction.interfaces.Predicate;
import symbpredabstraction.interfaces.SymbolicFormula;
import symbpredabstraction.trace.CounterexampleTraceInfo;
import cfa.objectmodel.CFAEdge;
import cfa.objectmodel.CFAErrorNode;
import cfa.objectmodel.CFANode;
import cfa.objectmodel.c.CallToReturnEdge;
import cfa.objectmodel.c.FunctionDefinitionNode;
import cfa.objectmodel.c.ReturnEdge;
import cmdline.CPAMain;

import common.Pair;

import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.AbstractElementWithLocation;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.TransferRelation;
import cpa.symbpredabs.AbstractReachabilityTree;
import exceptions.CPATransferException;
import exceptions.ErrorReachedException;
import exceptions.RefinementNeededException;


/**
 * Transfer relation for symbolic lazy abstraction with summaries
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public class SummaryTransferRelation implements TransferRelation {

  class Path {
    Vector<Integer> elemIds;

    public Path(Deque<SummaryAbstractElement> cex) {
      elemIds = new Vector<Integer>();
      elemIds.ensureCapacity(cex.size());
      for (SummaryAbstractElement e : cex) {
        elemIds.add(((CFANode)e.getLocation()).getNodeNumber());
      }
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) return true;
      if (o instanceof Path) {
        return elemIds.equals(((Path)o).elemIds);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return elemIds.hashCode();
    }
  }

  private SummaryAbstractDomain domain;
  private AbstractReachabilityTree abstractTree;
  private Map<Path, Integer> seenAbstractCounterexamples;

  private int numAbstractStates = 0; // for statistics

  public SummaryTransferRelation(SummaryAbstractDomain d) {
    domain = d;
    abstractTree = new AbstractReachabilityTree();
    seenAbstractCounterexamples = new HashMap<Path, Integer>();
  }

  public int getNumAbstractStates() { return numAbstractStates; }

  // isFunctionStart and isFunctionEnd are used for managing the context,
  // needed for handling function calls

  private boolean isFunctionStart(SummaryAbstractElement elem) {
    return (elem.getLocation().getInnerNode() instanceof
        FunctionDefinitionNode);
  }

  private boolean isFunctionEnd(SummaryAbstractElement elem) {
    CFANode n = elem.getLocation().getInnerNode();
    return (n.getNumLeavingEdges() == 1 &&
        n.getLeavingEdge(0) instanceof ReturnEdge);
//  return (elem.getLocation().getInnerNode().getEnteringSummaryEdge()
//  != null);
  }

  // abstract post operation
  private SummaryAbstractElement buildSuccessor(SummaryAbstractElement e,
      CFAEdge edge) throws CPATransferException {
    SummaryCPA cpa = domain.getCPA();
    SummaryCFANode succLoc = (SummaryCFANode)edge.getSuccessor();
    // check whether the successor is an error location: if so, we want
    // to check for feasibility of the path...

    Collection<Predicate> predicates =
      cpa.getPredicateMap().getRelevantPredicates(
          edge.getSuccessor());

    SummaryAbstractElement succ = new SummaryAbstractElement(succLoc);
    Map<CFANode, Pair<SymbolicFormula, SSAMap>> p =
      cpa.getPathFormulas(succLoc);
    succ.setPathFormulas(p);

    // if e is the end of a function, we must find the correct return
    // location
    if (isFunctionEnd(succ)) {
      SummaryCFANode retNode = e.topContextLocation();
      if (!succLoc.equals(retNode)) {
        CPAMain.logManager.log(Level.ALL, "DEBUG_1",
            "Return node for this call is: ", retNode,
            ", but edge leads to: ", succLoc, ", returning BOTTOM");
        return domain.getBottomElement();
      }
    }

//  Stack<AbstractFormula> context =
//  (Stack<AbstractFormula>)e.getContext().clone();
//  if (isFunctionEnd(e)) {
//  context.pop();
//  }
//  succ.setContext(context);
    succ.setContext(e.getContext(), false);
    if (isFunctionEnd(succ)) {
      succ.popContext();
    }

    SummaryAbstractFormulaManager amgr = cpa.getSummaryFormulaManager();
    AbstractFormula abstraction = amgr.buildAbstraction(
        cpa.getFormulaManager(), e, succ, predicates);
    succ.setAbstraction(abstraction);
    succ.setParent(e);

    if (CPAMain.logManager.getLogLevel().intValue() <= Level.ALL.intValue()) {
      CPAMain.logManager.log(Level.ALL, "DEBUG_1", "COMPUTED ABSTRACTION:",
          amgr.toConcrete(abstraction));
    }

    if (cpa.getAbstractFormulaManager().isFalse(abstraction)) {
      return domain.getBottomElement();
    } else {
      ++numAbstractStates;
      // if we reach an error state, we want to log this...
      if (succ.getLocation().getInnerNode() instanceof CFAErrorNode) {
        if (CPAMain.cpaConfig.getBooleanValue(
        "cpas.symbpredabs.abstraction.norefinement")) {
          CPAMain.setErrorReached();
          throw new ErrorReachedException(
          "Reached error location, but refinement disabled");
        }
        // oh oh, reached error location. Let's check whether the
        // trace is feasible or spurious, and in case refine the
        // abstraction
        //
        // first we build the abstract path
        Deque<SummaryAbstractElement> path =
          new LinkedList<SummaryAbstractElement>();
        path.addFirst(succ);
        SummaryAbstractElement parent = succ.getParent();
        while (parent != null) {
          path.addFirst(parent);
          parent = parent.getParent();
        }
        CounterexampleTraceInfo info =
          amgr.buildCounterexampleTrace(
              cpa.getFormulaManager(), path);
        if (info.isSpurious()) {
          CPAMain.logManager.log(Level.FINEST,
              "Found spurious error trace, refining the ",
          "abstraction");
          performRefinement(path, info);
        } else {
          CPAMain.logManager.log(Level.FINEST,
              "REACHED ERROR LOCATION!: ", succ,
          " RETURNING BOTTOM!");
          CPAMain.setErrorReached();
          throw new ErrorReachedException("");
        }
        return domain.getBottomElement();
      }

      if (isFunctionStart(succ)) {
        // we push into the context the return location, which is
        // the successor location of the summary edge
        SummaryCFANode retNode = null;
        for (CFANode l : e.getLeaves()) {
          if (l instanceof FunctionDefinitionNode) {
            assert(l.getNumLeavingEdges() == 1);
            //assert(l.getNumEnteringEdges() == 1);

            CFAEdge ee = l.getLeavingEdge(0);
            InnerCFANode n = (InnerCFANode)ee.getSuccessor();
            if (n.getSummaryNode().equals(succ.getLocation())) {
              CFANode pr = l.getEnteringEdge(0).getPredecessor();
              CallToReturnEdge ce = pr.getLeavingSummaryEdge();
              //assert(ce != null);
              if (ce != null) {
                retNode = ((InnerCFANode)ce.getSuccessor()).
                getSummaryNode();
                break;
              }
            }
          }
        }
        //assert(retNode != null);
        if (retNode != null) {
          CPAMain.logManager.log(Level.ALL, "DEBUG_3", "PUSHING CONTEXT TO", succ,
              ": ", cpa.getSummaryFormulaManager().toConcrete(
                  succ.getAbstraction()));
          //succ.getContext().push(succ.getAbstraction());
          succ.pushContext(succ.getAbstraction(), retNode);
        }
      }

      return succ;
    }
  }


  // abstraction refinement and undoing of (part of) the ART
  private void performRefinement(Deque<SummaryAbstractElement> path,
      CounterexampleTraceInfo info) throws CPATransferException {
    Path pth = new Path(path);
    int numSeen = 0;
    if (seenAbstractCounterexamples.containsKey(pth)) {
      numSeen = seenAbstractCounterexamples.get(pth);
    }
    seenAbstractCounterexamples.put(pth, numSeen+1);

    UpdateablePredicateMap curpmap =
      (UpdateablePredicateMap)domain.getCPA().getPredicateMap();
    AbstractElementWithLocation root = null;
    AbstractElementWithLocation firstInterpolant = null;
    for (SummaryAbstractElement e : path) {
      Collection<Predicate> newpreds = info.getPredicatesForRefinement(e);
      if (firstInterpolant == null && newpreds.size() > 0) {
        firstInterpolant = e;
      }
      if (curpmap.update((CFANode)e.getLocation(), newpreds)) {
        if (root == null) {
          root = e.getParent();
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
      CFANode loc = 
        ((SummaryAbstractElement)firstInterpolant).getLocationNode(); 
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
    Collection<AbstractElementWithLocation> toWaitlist = new HashSet<AbstractElementWithLocation>();
    toWaitlist.add(root);
    Collection<AbstractElementWithLocation> toUnreach =
      abstractTree.getSubtree(root, true, false);

    SummaryCPA cpa = domain.getCPA();
    for (AbstractElementWithLocation ae : toUnreach) {
      SummaryAbstractElement e = (SummaryAbstractElement)ae;
      if (e.isCovered()) {
        e.setCovered(false);
        cpa.setUncovered(e);
      }
    }
    if (root != abstractTree.getRoot()) {
      // then, we have to unmark some nodes
      Collection<SummaryAbstractElement> tmp =
        cpa.getCovered();
      int m = ((SummaryAbstractElement)root).getMark();
      for (Iterator<SummaryAbstractElement> i = tmp.iterator(); 
      i.hasNext(); ) {
        SummaryAbstractElement e = i.next();
        assert(e.isCovered());
        if (e.getMark() > m) {
          e.setCovered(false);
          i.remove();
          toWaitlist.add(e.getParent());
          toUnreach.add(e);
        }
      }
    }

    CPAMain.logManager.log(Level.ALL, "DEBUG_1", "REFINEMENT - toWaitlist:", root);
    CPAMain.logManager.log(Level.ALL, "DEBUG_1", "REFINEMENT - toUnreach:",
        toUnreach);
    throw new RefinementNeededException(toUnreach, toWaitlist);
  }


  private AbstractElement getAbstractSuccessor(AbstractElement element,
      CFAEdge cfaEdge, Precision prec) throws CPATransferException {
    CPAMain.logManager.log(Level.FINEST,
        "Getting Abstract Successor of element: ", element,
        " on edge: ", cfaEdge);
    // To get the successor, we compute the predicate abstraction of the
    // formula of element plus all the edges that connect any of the
    // inner nodes of the summary of element to any inner node of the
    // destination
    SummaryAbstractElement e = (SummaryAbstractElement)element;
    CFANode src = (CFANode)e.getLocation();

    for (int i = 0; i < src.getNumLeavingEdges(); ++i) {
      CFAEdge edge = src.getLeavingEdge(i);
      if (edge.equals(cfaEdge)) {
        AbstractElementWithLocation ret = buildSuccessor(e, edge);

        CPAMain.logManager.log(Level.FINEST,
            "Successor is: ", ret);

        if (ret != domain.getBottomElement()) {
          abstractTree.addChild(e, ret);
        }

        return ret;
      }
    }

    CPAMain.logManager.log(Level.FINEST, "Successor is: BOTTOM");

    return domain.getBottomElement();
  }

  @Override
  public Collection<AbstractElement> getAbstractSuccessors(
      AbstractElement element, Precision prec, CFAEdge cfaEdge) throws CPATransferException {
    CPAMain.logManager.log(Level.FINEST,
        "Getting ALL Abstract Successors of element: ",
        element);

    List<AbstractElement> allSucc = new ArrayList<AbstractElement>();
    SummaryAbstractElement e = (SummaryAbstractElement)element;
    CFANode src = (CFANode)e.getLocation();

    for (int i = 0; i < src.getNumLeavingEdges(); ++i) {
      AbstractElement newe =
        getAbstractSuccessor(e, src.getLeavingEdge(i), prec);
      if (newe != domain.getBottomElement()) {
        allSucc.add(newe);
      }
    }

    e.setMark();

    CPAMain.logManager.log(Level.FINEST,
        allSucc.size(), " successors found");

    return allSucc;
  }

  @Override
  public Collection<? extends AbstractElement> strengthen(AbstractElement element,
      List<AbstractElement> otherElements, CFAEdge cfaEdge,
      Precision precision) {    
    return null;
  }
}
