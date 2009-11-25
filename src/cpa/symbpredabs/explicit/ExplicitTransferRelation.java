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
package cpa.symbpredabs.explicit;

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

import symbpredabstraction.UpdateablePredicateMap;
import symbpredabstraction.interfaces.AbstractFormula;
import symbpredabstraction.interfaces.Predicate;
import symbpredabstraction.interfaces.SymbolicFormulaManager;
import symbpredabstraction.trace.CounterexampleTraceInfo;

import cfa.objectmodel.CFAEdge;
import cfa.objectmodel.CFAErrorNode;
import cfa.objectmodel.CFANode;
import cfa.objectmodel.c.FunctionDefinitionNode;
import cfa.objectmodel.c.ReturnEdge;
import cmdline.CPAMain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.AbstractElementWithLocation;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.TransferRelation;
import cpa.symbpredabs.AbstractReachabilityTree;
import exceptions.CPAException;
import exceptions.CPATransferException;
import exceptions.ErrorReachedException;
import exceptions.RefinementNeededException;


/**
 * TransferRelation for explicit-state lazy abstraction. This is the most
 * complex of the CPA-related classes, and where the analysis is actually
 * performed.
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public class ExplicitTransferRelation implements TransferRelation {

  class Path {
    Vector<Integer> elemIds;

    public Path(Deque<ExplicitAbstractElement> cex) {
      elemIds = new Vector<Integer>();
      elemIds.ensureCapacity(cex.size());
      for (ExplicitAbstractElement e : cex) {
        elemIds.add(e.getLocation().getNodeNumber());
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

  private ExplicitAbstractDomain domain;
  private AbstractReachabilityTree abstractTree;
  private Map<Path, Integer> abstractCex;

  private int numAbstractStates = 0; // for statistics

  // this is used for deciding how much of the ART to undo after refinement
  // private Deque<ExplicitAbstractElement> lastErrorPath;
  // private int samePathAlready = 0;

  private boolean notEnoughPredicatesFlag = false;

  public ExplicitTransferRelation(ExplicitAbstractDomain d) {
    domain = d;
    abstractTree = new AbstractReachabilityTree();
    // lastErrorPath = null;
    abstractCex = new HashMap<Path, Integer>();
  }

  public boolean notEnoughPredicates() { return notEnoughPredicatesFlag; }

  public int getNumAbstractStates() { return numAbstractStates; }

  // isFunctionStart and isFunctionEnd are used to manage the call stack
  private boolean isFunctionStart(ExplicitAbstractElement elem) {
    return (elem.getLocation() instanceof FunctionDefinitionNode);
  }

  private boolean isFunctionEnd(ExplicitAbstractElement elem) {
    CFANode n = elem.getLocation();
    return (n.getNumLeavingEdges() > 0 &&
        n.getLeavingEdge(0) instanceof ReturnEdge);
  }

  // performs the abstract post operation
  private AbstractElement buildSuccessor(ExplicitAbstractElement e,
                                         CFAEdge edge) throws CPATransferException {
    ExplicitCPA cpa = domain.getCPA();
    CFANode succLoc = edge.getSuccessor();

    // check whether the successor is an error location: if so, we want
    // to check for feasibility of the path...

    Collection<Predicate> predicates =
      cpa.getPredicateMap().getRelevantPredicates(
          e.getLocation());
//  if (predicates.isEmpty() && e.getParent() != null) {
//  predicates = cpa.getPredicateMap().getRelevantPredicates(
//  e.getParent().getLocation());
//  }

    ExplicitAbstractElement succ = new ExplicitAbstractElement(succLoc);

    // if e is the end of a function, we must find the correct return
    // location
    if (isFunctionEnd(e)) {
      CFANode retNode = e.topContextLocation();
      if (!succLoc.equals(retNode)) {
        CPAMain.logManager.log(Level.ALL, "DEBUG_1",
            "Return node for this call is: ", retNode,
            ", but edge leads to: ", succLoc, ", returning BOTTOM");
        return domain.getBottomElement();
      }
    }

    succ.setContext(e.getContext(), false);
    if (isFunctionEnd(e)) {
      succ.popContext();
    }

    ExplicitAbstractFormulaManager amgr = cpa.getAbstractFormulaManager();
    AbstractFormula abstraction = amgr.buildAbstraction(
        cpa.getFormulaManager(), e, succ, edge, predicates);
    succ.setAbstraction(abstraction);
    succ.setParent(e);

    if (CPAMain.logManager.getLogLevel().intValue() <= Level.ALL.intValue()) {
      SymbolicFormulaManager mgr = cpa.getFormulaManager();
      CPAMain.logManager.log(Level.ALL, "DEBUG_1", "COMPUTED ABSTRACTION:",
          amgr.toConcrete(mgr, abstraction));
    }

    if (amgr.isFalse(abstraction)) {
      return domain.getBottomElement();
    } else {
      //++numAbstractStates;
      // if we reach an error state, we want to log this...
      if (succ.getLocation() instanceof CFAErrorNode) {
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
        Deque<ExplicitAbstractElement> path =
          new LinkedList<ExplicitAbstractElement>();
        path.addFirst(succ);
        ExplicitAbstractElement parent = succ.getParent();
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
          throw new ErrorReachedException(
              info.getConcreteTrace().toString());
        }
        return domain.getBottomElement();
      }

      if (isFunctionStart(succ)) {
        // we push into the context the return location, which is
        // the successor location of the summary edge
        assert(e.getLocation().getLeavingSummaryEdge() != null);
        CFANode retNode = null;
        retNode =
          e.getLocation().getLeavingSummaryEdge().getSuccessor();
        succ.pushContext(e.getAbstraction(), retNode);
//      for (CFANode l : e.getLeaves()) {
//      if (l instanceof FunctionDefinitionNode) {
//      assert(l.getNumLeavingEdges() == 1);
//      assert(l.getNumEnteringEdges() == 1);

//      CFAEdge ee = l.getLeavingEdge(0);
//      InnerCFANode n = (InnerCFANode)ee.getSuccessor();
//      if (n.getSummaryNode().equals(succ.getLocation())) {
//      CFANode pr = l.getEnteringEdge(0).getPredecessor();
//      CallToReturnEdge ce = pr.getLeavingSummaryEdge();
//      //assert(ce != null);
//      if (ce != null) {
//      retNode = ((InnerCFANode)ce.getSuccessor()).
//      getSummaryNode();
//      break;
//      }
//      }
//      }
//      }
        //assert(retNode != null);
        if (retNode != null) {
//        CPAMain.logManager.log(Level.FINEST, "PUSHING CONTEXT TO", succ,
//        ": ", cpa.getAbstractFormulaManager().toConcrete(
//        cpa.getFormulaManager(),
//        succ.getAbstraction()));
//        //succ.getContext().push(succ.getAbstraction());
//        succ.pushContext(succ.getAbstraction(), retNode);
        }
      }

      return succ;
    }
  }

  // abstraction refinement is performed here
  public void performRefinement(Deque<ExplicitAbstractElement> path,
                                CounterexampleTraceInfo info) throws CPATransferException {
    CPAMain.logManager.log(Level.ALL, "DEBUG_1", "STARTING REFINEMENT");
    UpdateablePredicateMap curpmap =
      (UpdateablePredicateMap)domain.getCPA().getPredicateMap();
    ExplicitAbstractElement root = null;
    ExplicitAbstractElement cur = null;
    ExplicitAbstractElement firstInterpolant = null;
    for (ExplicitAbstractElement e : path) {
      Collection<Predicate> newpreds = info.getPredicatesForRefinement(e);
      if (firstInterpolant == null && newpreds.size() > 0) {
        firstInterpolant = e;
      }
      System.out.println(e.getLocation() + "  ..  " + newpreds);
      System.exit(0);
      
      if (curpmap.update(e.getLocation(), newpreds)) {
        CPAMain.logManager.log(Level.ALL, "DEBUG_1", "REFINING LOCATION:",
            e.getLocation());
        if (root == null) {
          cur = e;
          root = e;//.getParent();
        }
//      else if (root.getLocation().equals(e.getLocation())) {
//      root = e;
//      }
      }
    }
    Path pth = new Path(path);
    int alreadySeen = 0;
    if (abstractCex.containsKey(pth)) {
      alreadySeen = abstractCex.get(pth);
    }
    abstractCex.put(pth, alreadySeen+1);
    //root = (ExplicitAbstractElement)abstractTree.getRoot();
    if (root == null) {
      //root = firstInterpolant;//(ExplicitAbstractElement)abstractTree.getRoot();
      if (alreadySeen != 0) {//samePath(path, lastErrorPath)) {
        if (alreadySeen > 1) {//samePathAlready == 1) {
          // we have not enough predicates to rule out this path, and
          // we can't find any new, we are forced to exit :-(
          notEnoughPredicatesFlag = true;
          assert(false);
          System.exit(1);
//        } else if (samePathAlready == 1) {
//        root = (ExplicitAbstractElement)abstractTree.getRoot();
//        samePathAlready = 2;
        } else {
          //samePathAlready = 1;
          root = firstInterpolant;
          //root = (ExplicitAbstractElement)abstractTree.getRoot();
        }
      } else {
        assert(firstInterpolant != null);
        CPAMain.logManager.log(Level.FINEST,
            "Restarting ART from scratch");
        root = (ExplicitAbstractElement)abstractTree.getRoot();
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
      root = (ExplicitAbstractElement)abstractTree.getRoot();
    }
    assert(root != null);// || firstInterpolant == path.getFirst());
    //lastErrorPath = path;
    //root = firstInterpolant;
    //root = (ExplicitAbstractElement)abstractTree.getRoot();

//  Collection<AbstractElementWithLocation> roots = 
//  abstractTree.findAll(root.getLocationNode());
//  assert(root != null);
//  if (root.getParent() != null) {
//  root = root.getParent();
//  }

//  if (root == null) {
//  assert(firstInterpolant != null);
////assert(CPAMain.cpaConfig.getBooleanValue(
////"cpas.symbpredabs.refinement.addPredicatesGlobally"));
//  //root = abstractTree.getRoot();
//  root = firstInterpolant;
//  }
    assert(root != null);
    //root = path.getFirst();
    Collection<AbstractElementWithLocation> toWaitlist = new HashSet<AbstractElementWithLocation>();
//  Collection<AbstractElementWithLocation> toUnreach = null;
//  for (AbstractElementWithLocation e : roots) {
//  toWaitlist.add(e);
//  Collection<AbstractElementWithLocation> t =
//  abstractTree.getSubtree(e, true, false);
//  if (toUnreach == null) toUnreach = t;
//  else toUnreach.addAll(t);
//  }
    toWaitlist.add(root);
    Collection<AbstractElementWithLocation> toUnreach =
      abstractTree.getSubtree(root, true, false);
    if (cur != null) {
      // we don't want to unreach elements that were covered before
      // reaching the error!
      for (Iterator<AbstractElementWithLocation> it = toUnreach.iterator();
      it.hasNext(); ) {
        ExplicitAbstractElement e = (ExplicitAbstractElement)it.next();
        if (e.isCovered() && e.getMark() < cur.getMark()) {
          CPAMain.logManager.log(Level.ALL, "DEBUG_1", "NOT unreaching", e,
              "because it was covered before", cur);
          it.remove();
        }
      }
    }

    ExplicitCPA cpa = domain.getCPA();
    for (AbstractElementWithLocation ae : toUnreach) {
      ExplicitAbstractElement e = (ExplicitAbstractElement)ae;
      if (e.isCovered()) {
        e.setCovered(false);
        cpa.setUncovered(e);
      }
    }
    if (root != abstractTree.getRoot()) {
      // then, we have to unmark some nodes
      Collection<ExplicitAbstractElement> tmp =
        cpa.getCovered();
      for (Iterator<ExplicitAbstractElement> i = tmp.iterator(); 
      i.hasNext(); ) {
        ExplicitAbstractElement e = i.next();
        assert(e.isCovered());
        if (e.getMark() > root.getMark()) {
          e.setCovered(false);
          i.remove();
          toWaitlist.add(e.getParent());
          toUnreach.add(e);
        }
      }
    }

    CPAMain.logManager.log(Level.ALL, "DEBUG_1", "REFINEMENT - toWaitlist:",
        toWaitlist);
    CPAMain.logManager.log(Level.ALL, "DEBUG_1", "REFINEMENT - toUnreach:",
        toUnreach);
    throw new RefinementNeededException(toUnreach, toWaitlist);
  }    

  /*
    // checks whether the two paths are the same (in terms of locations)
    private boolean samePath(Deque<ExplicitAbstractElement> path1,
                             Deque<ExplicitAbstractElement> path2) {
        if (path1.size() == path2.size()) {
            Iterator<ExplicitAbstractElement> it1 = path1.iterator();
            Iterator<ExplicitAbstractElement> it2 = path2.iterator();
            while (it1.hasNext()) {
                ExplicitAbstractElement e1 = it1.next();
                ExplicitAbstractElement e2 = it2.next();
                if (!e1.getLocationNode().equals(e2.getLocationNode())) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }
   */

  @Override
  public AbstractElement getAbstractSuccessor(AbstractElement element,
                                              CFAEdge cfaEdge, Precision prec) throws CPATransferException {
    CPAMain.logManager.log(Level.FINEST,
        "Getting Abstract Successor of element: ", element,
        " on edge: ", cfaEdge.getRawStatement());

    if (!abstractTree.contains((AbstractElementWithLocation)element)) {
      ++numAbstractStates;
    }

    // To get the successor, we compute the predicate abstraction of the
    // formula of element plus all the edges that connect any of the
    // inner nodes of the summary of element to any inner node of the
    // destination
    ExplicitAbstractElement e = (ExplicitAbstractElement)element;

    // bottom produces bottom
    if (domain.getBottomElement().equals(e)) {
      return e;
    }

    CFANode src = e.getLocation();

    for (int i = 0; i < src.getNumLeavingEdges(); ++i) {
      CFAEdge edge = src.getLeavingEdge(i);
      if (edge.equals(cfaEdge)) {
        AbstractElementWithLocation ret = (ExplicitAbstractElement)buildSuccessor(e, edge);

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
  public List<AbstractElementWithLocation> getAllAbstractSuccessors(
      AbstractElementWithLocation element, Precision prec) throws CPAException, CPATransferException {
    CPAMain.logManager.log(Level.FINEST,
        "Getting ALL Abstract Successors of element: ",
        element);

    List<AbstractElementWithLocation> allSucc = new Vector<AbstractElementWithLocation>();
    ExplicitAbstractElement e = (ExplicitAbstractElement)element;


    assert(!e.isCovered());
    CFANode src = e.getLocation();

    for (int i = 0; i < src.getNumLeavingEdges(); ++i) {
      AbstractElement newe =
        getAbstractSuccessor(e, src.getLeavingEdge(i), prec);
      if (newe != domain.getBottomElement()) {
        allSucc.add((ExplicitAbstractElement)newe);
      }
    }

    e.setMark();

    CPAMain.logManager.log(Level.FINEST,
        allSucc.size(), " successors found");

    return allSucc;
  }

  public void clearART() {
    this.abstractTree.clear();
  }

  @Override
  public AbstractElement strengthen(AbstractElement element,
                         List<AbstractElement> otherElements, CFAEdge cfaEdge,
                         Precision precision) {    
    return null;
  }
}
