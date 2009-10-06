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
package cpa.itpabs;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import logging.CustomLogLevel;
import logging.LazyLogger;
import cfa.objectmodel.CFAEdge;
import cfa.objectmodel.CFANode;
import cmdline.CPAMain;
import exceptions.CPATransferException;
import exceptions.ErrorReachedException;
import exceptions.RefinementNeededException;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.AbstractElementWithLocation;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.TransferRelation;
import cpa.symbpredabs.AbstractReachabilityTree;
import cpa.symbpredabs.SymbolicFormula;
import cpa.symbpredabs.SymbolicFormulaManager;
import exceptions.CPAException;


/**
 * Transfer relation for interpolation-based lazy abstraction.
 * STILL ON-GOING, NOT FINISHED, AND CURRENTLY BROKEN
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public class ItpTransferRelation implements TransferRelation {

  private ItpAbstractDomain domain;
  private AbstractReachabilityTree abstractTree;

  private int numAbstractStates = 0; // for statistics

  public class ForcedCoverStats {
    public int numForcedCoverChecks = 0;
    public int numForcedCoveredElements = 0;
    public int numForcedCoverChecksCached = 0;
  }

  // this is used for debugging only. Every time the successors of an
  // element are computed, the element is expanded
  private Set<AbstractElement> expanded;

  // Additional elements to put into the waiting list. When an element is
  // covered, it might uncover others, that need to be put on the wait list
  // again (see McMillan's paper for an explanation). This is uses for this
  // purpose
  private Set<AbstractElement> toProcess;

  // For each location, the set of reached elements corresponding to it
  private Map<CFANode, Set<AbstractElement>> reached;

  private Comparator<AbstractElement> reachedComp =
    new Comparator<AbstractElement>() {
    @Override
    public int compare(AbstractElement o1, AbstractElement o2) {
      return ((ItpAbstractElement)o1).compareTo(
          (ItpAbstractElement)o2);
    }
  };
  private ItpAbstractElement lastClosed = null;
  private ItpAbstractElement lastForcedCover = null;
  public ForcedCoverStats forcedCoverStats = new ForcedCoverStats();

  class ForcedCoverCacheKey {
    SymbolicFormula start;
    SymbolicFormula end;
    Deque<ItpAbstractElement> path;

    ForcedCoverCacheKey(SymbolicFormula s, SymbolicFormula e,
        Deque<ItpAbstractElement> p) {
      start = s;
      end = e;
      path = p;
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) return true;
      if (o instanceof ForcedCoverCacheKey) {
        ForcedCoverCacheKey oo = (ForcedCoverCacheKey)o;
        if (start.equals(oo.start) && end.equals(oo.end)) {
          if (path.size() != oo.path.size()) return false;
          Iterator<ItpAbstractElement> i1 = path.iterator();
          Iterator<ItpAbstractElement> i2 = oo.path.iterator();
          while (i1.hasNext()) {
            assert(i2.hasNext());
            ItpAbstractElement e1 = i1.next();
            ItpAbstractElement e2 = i2.next();
            if (!e1.getLocation().equals(e2.getLocation())) {
              return false;
            }
            if (!e1.getAbstraction().equals(e2.getAbstraction())) {
              return false;
            }
          }
          return true;
        }
      }
      return false;
    }

    @Override
    public int hashCode() {
      return start.hashCode() ^ end.hashCode() ^ path.hashCode();
    }
  }
  // cache for forced coverage checks
  private Map<ForcedCoverCacheKey, ItpCounterexampleTraceInfo>
  forcedCoverCache;
  private boolean useForcedCovering;

  public ItpTransferRelation(ItpAbstractDomain d) {
    domain = d;
    abstractTree = new AbstractReachabilityTree();
    toProcess = new HashSet<AbstractElement>();
    reached = new HashMap<CFANode, Set<AbstractElement>>();
    expanded = new HashSet<AbstractElement>();
    forcedCoverCache =
      new HashMap<ForcedCoverCacheKey, ItpCounterexampleTraceInfo>();
    useForcedCovering = CPAMain.cpaConfig.getBooleanValue(
        "cpas.itpabs.useForcedCovering");
  }

  public int getNumAbstractStates() { return numAbstractStates; }

  public AbstractReachabilityTree getART() { return abstractTree; }

//private boolean isFunctionStart(ItpAbstractElement elem) {
//return (elem.getLocation() instanceof FunctionDefinitionNode);
//}

//private boolean isFunctionEnd(ItpAbstractElement elem) {
//CFANode n = elem.getLocation();
//return (n.getNumLeavingEdges() == 1 &&
//n.getLeavingEdge(0) instanceof ReturnEdge);
//}

  /* abstract post computation. This is purely sintactical, except that if
   * an element is covered we don't expand its subtree. If the error
   * location is reached, refinement takes place
   */
  private ItpAbstractElement buildSuccessor(ItpAbstractElement e,
                                            CFAEdge edge) throws CPATransferException {
    ItpCPA cpa = domain.getCPA();

    ItpAbstractElementManager elemMgr = domain.getCPA().getElementCreator();

    if (e.isError()) {
      ItpCounterexampleRefiner refiner = cpa.getRefiner();
      // reached error location, we have to refine the abstraction
      Deque<ItpAbstractElement> path =
        new LinkedList<ItpAbstractElement>();
      path.addFirst(e);
      ItpAbstractElement parent = e.getParent();
      while (parent != null) {
        path.addFirst(parent);
        parent = parent.getParent();
      }
      ItpCounterexampleTraceInfo info =
        refiner.buildCounterexampleTrace(
            cpa.getFormulaManager(), path);
      if (info.isSpurious()) {
        LazyLogger.log(CustomLogLevel.SpecificCPALevel,
            "Found spurious error trace, refining the ",
            "abstraction");
        performRefinement(path, info);
      } else {
        // for debugging, we want to build the sequence of function
        // calls here...
        boolean retloc = false;
        for (ItpAbstractElement elem : path) {
          if (elemMgr.isFunctionStart(elem)) {
            System.out.println("CALLER: " + elem.getParent());
            System.out.println("CALLED FUNCTION: " + elem);
          } else if (elemMgr.isFunctionEnd(elem, null)) {
            retloc = true;
          } else if (retloc) {
            retloc = false;
            System.out.println("RETURNING TO: " + elem);
          }
        }
        LazyLogger.log(CustomLogLevel.SpecificCPALevel,
            "REACHED ERROR LOCATION!: ", e,
        " RETURNING BOTTOM!");
        CPAMain.cpaStats.setErrorReached(true);
        throw new ErrorReachedException(
            info.getConcreteTrace().toString());
      }
      return domain.getBottomElement();
    }

    if (cpa.isCovered(e)) {
      return domain.getBottomElement();
    }
    close(e);
    if (cpa.isCovered(e)) {
      return domain.getBottomElement();
    }

    if (forceCover(e)) {
      return domain.getBottomElement();
    }

    expanded.add(e);

    CFANode succLoc = edge.getSuccessor();

    // check whether the successor is an error location: if so, we want
    // to check for feasibility of the path...

    ItpAbstractElement succ = elemMgr.create(succLoc);

    // if e is the end of a function, we must find the correct return
    // location
    if (!elemMgr.isRightEdge(e, edge, succ)) {
      //if (isFunctionEnd(succ)) {
//    CFANode retNode = e.topContextLocation();
//    if (!succLoc.equals(retNode)) {
//    LazyLogger.log(LazyLogger.DEBUG_1,
//    "Return node for this call is: ", retNode,
//    ", but edge leads to: ", succLoc, ", returning BOTTOM");
      return domain.getBottomElement();
//    }
    }

    succ.setContext(e.getContext(), false);
    //if (isFunctionEnd(e)) {
    if (elemMgr.isFunctionEnd(e, succ)) {
      succ.popContext();
    }

    SymbolicFormulaManager mgr = cpa.getFormulaManager();
    succ.setAbstraction(mgr.makeTrue());
    succ.setParent(e);

//  if (succ.isErrorLocation()) {
//  ItpCounterexampleRefiner refiner = cpa.getRefiner();
//  // reached error location, we have to refine the abstraction
//  Deque<ItpAbstractElement> path =
//  new LinkedList<ItpAbstractElement>();
//  path.addFirst(succ);
//  ItpAbstractElement parent = succ.getParent();
//  while (parent != null) {
//  path.addFirst(parent);
//  parent = parent.getParent();
//  }
//  ItpCounterexampleTraceInfo info =
//  refiner.buildCounterexampleTrace(
//  cpa.getFormulaManager(), path);
//  if (info.isSpurious()) {
//  LazyLogger.log(CustomLogLevel.SpecificCPALevel,
//  "Found spurious error trace, refining the ",
//  "abstraction");
//  performRefinement(path, info);
//  } else {
//  // for debugging, we want to build the sequence of function
//  // calls here...
//  boolean retloc = false;
//  for (ItpAbstractElement elem : path) {
//  if (elemMgr.isFunctionStart(elem)) {
//  System.out.println("CALLER: " + elem.getParent());
//  System.out.println("CALLED FUNCTION: " + elem);
//  } else if (elemMgr.isFunctionEnd(elem)) {
//  retloc = true;
//  } else if (retloc) {
//  retloc = false;
//  System.out.println("RETURNING TO: " + elem);
//  }
//  }
//  LazyLogger.log(CustomLogLevel.SpecificCPALevel,
//  "REACHED ERROR LOCATION!: ", succ,
//  " RETURNING BOTTOM!");
//  errorReached = true;
//  throw new ErrorReachedException(
//  info.getConcreteTrace().toString());
//  }
//  return domain.getBottomElement();
//  }


    //if (isFunctionStart(succ)) {
    if (elemMgr.isFunctionStart(succ)) {
      // we push into the context the return location, which is
      // the successor location of the summary edge
      //succ.pushContextFindRetNode(e.getLocation());
      elemMgr.pushContextFindRetNode(e, succ);
    }
    return succ;
  }

  /*
   * abstraction refinement using interpolants directly
   */
  private void performRefinement(Deque<ItpAbstractElement> path,
                                 ItpCounterexampleTraceInfo info) throws CPATransferException {
    SymbolicFormulaManager mgr = domain.getCPA().getFormulaManager();
    // try {
    //     abstractTree.dump("/home/alb/tmp/art.dot");
    // } catch (IOException e1) {
    //     // TODO Auto-generated catch block
    //     e1.printStackTrace();
    // }
    Collection<AbstractElementWithLocation> maybeToWaitlist =
      new HashSet<AbstractElementWithLocation>();
    Collection<ItpAbstractElement> falseAbst =
      new Vector<ItpAbstractElement>();
    // we strengthen each element in the spurious path with the
    // corresponding interpolant
    for (ItpAbstractElement e : path) {
      SymbolicFormula newabs = info.getFormulaForRefinement(e);
      if (newabs.isFalse()) {
        falseAbst.add(e);
        LazyLogger.log(LazyLogger.DEBUG_1,
            "REFINING1 ", e, ", new abstraction: ", newabs);
        e.setAbstraction(newabs);
        for (AbstractElementWithLocation el : domain.getCPA().uncoverAll(e)) {
          maybeToWaitlist.add(el);
        }
      } else if (e.getAbstraction().isTrue() && !newabs.isTrue()) {
        LazyLogger.log(LazyLogger.DEBUG_1,
            "REFINING2 ", e, ", new abstraction: ", newabs);
        e.setAbstraction(newabs);
        //maybeToWaitlist.add(e);
        for (AbstractElementWithLocation el : domain.getCPA().uncoverAll(e)) {
          maybeToWaitlist.add(el);
        }
      } else if (!e.getAbstraction().isTrue() && !newabs.isTrue()) {
        if (!mgr.entails(e.getAbstraction(), newabs)) {
          e.setAbstraction(mgr.makeAnd(e.getAbstraction(), newabs));
          //maybeToWaitlist.add(e);
          for (AbstractElementWithLocation el : domain.getCPA().uncoverAll(e)) {
            maybeToWaitlist.add(el);
          }
          LazyLogger.log(LazyLogger.DEBUG_1,
              "REFINING3 ", e, ", new abstraction: ",
              e.getAbstraction());
        } else {
          LazyLogger.log(LazyLogger.DEBUG_1,
              "NOT REFINING ", e,
              " because new abstraction is entailed by old one! ",
              "OLD: ", e.getAbstraction(), ", NEW: ", newabs);
        }
      }
      // and we check whether the element is covered
      close(e);
    }
    assert(falseAbst.size() > 0);
    ItpCPA cpa = domain.getCPA();
    Collection<AbstractElementWithLocation> toUnreach = new HashSet<AbstractElementWithLocation>();
    for (ItpAbstractElement ie : falseAbst) {
//    maybeToWaitlist.addAll(cpa.removeDescendantsFromCovering(ie));
      Collection<AbstractElementWithLocation> tmp = 
        abstractTree.getSubtree(ie, true, false);
      for (AbstractElement el : tmp) {
        removeFromReached((ItpAbstractElement)el);
      }
      toUnreach.addAll(tmp);
    }
    toProcess.removeAll(toUnreach);
    // we re-add to the waiting list all the element that have been
    // uncovered as a consequence of refinement
    Collection<AbstractElementWithLocation> toWaitlist = new Vector<AbstractElementWithLocation>();
    for (AbstractElementWithLocation e : maybeToWaitlist) {
      if (((ItpAbstractElement)e).getParent() != null && !toUnreach.contains(e)) {
//      assert(!expanded.contains(e));
        toWaitlist.add(e);
      }
    }
    LazyLogger.log(LazyLogger.DEBUG_1, "REFINEMENT - toWaitlist: ",
        toWaitlist);
    LazyLogger.log(LazyLogger.DEBUG_1, "REFINEMENT - toUnreach: ",
        toUnreach);
    throw new RefinementNeededException(toUnreach, toWaitlist);
  }

  @Override
  public AbstractElement getAbstractSuccessor(AbstractElement element,
                                              CFAEdge cfaEdge, Precision prec) throws CPATransferException {

    removeObsoleteToProcess(element);

//  if (!toProcess.isEmpty()) {
//  // this is when an element is covered and uncovers others, which
//  // need to be re-processed...
//  // this is really a HACK!!
//  LazyLogger.log(LazyLogger.DEBUG_1,
//  "RE-ADDING uncovered to waitlist: ", toProcess);
//  Vector<AbstractElementWithLocation> toWaitlist =
//  new Vector<AbstractElementWithLocation>();
//  Collection<AbstractElement> tmp = toProcess;
//  toProcess = new HashSet<AbstractElement>();
//  for (AbstractElement e : tmp) {
//  ItpAbstractElement ie = (ItpAbstractElement)e;
////close(ie);
////if (!domain.getCPA().isCovered(ie)) {
//  toWaitlist.add(ie);
////}
//  }
//  if (!toWaitlist.isEmpty()) {
//  Collections.sort(toWaitlist, new Comparator<AbstractElement>() {
//  @Override
//  public int compare(AbstractElement arg0,
//  AbstractElement arg1) {
//  ItpAbstractElement a =
//  (ItpAbstractElement)arg0;
//  ItpAbstractElement b =
//  (ItpAbstractElement)arg1;
//  return b.getId() - a.getId();
//  }
//  });
//  toWaitlist.add((ItpAbstractElement)element);
//  throw new ToWaitListException(toWaitlist);
//  }
//  }


    LazyLogger.log(CustomLogLevel.SpecificCPALevel,
        "Getting Abstract Successor of element: ", element,
        " on edge: ", cfaEdge);

    if (!abstractTree.contains((AbstractElementWithLocation)element)) {
      ++numAbstractStates;
    }

    // To get the successor, we compute the predicate abstraction of the
    // formula of element plus all the edges that connect any of the
    // inner nodes of the summary of element to any inner node of the
    // destination
    ItpAbstractElement e = (ItpAbstractElement)element;
    CFANode src = e.getLocation();

    for (int i = 0; i < src.getNumLeavingEdges(); ++i) {
      CFAEdge edge = src.getLeavingEdge(i);
      if (edge.equals(cfaEdge)) {
        try {
          AbstractElementWithLocation ret = buildSuccessor(e, edge);

          LazyLogger.log(CustomLogLevel.SpecificCPALevel,
              "Successor is: ", ret);

          if (ret != domain.getBottomElement()) {
            abstractTree.addChild(
                e, ret);
            addToReached((ItpAbstractElement)ret);
          }
          return ret;
        } catch (RefinementNeededException exc) {
          for (int j = i+1; j < src.getNumLeavingEdges(); ++j) {
            AbstractElementWithLocation e2 =
              buildSuccessor(e, src.getLeavingEdge(j));
            if (e2 != domain.getBottomElement()) {
              abstractTree.addChild(
                  e, e2);
              addToReached((ItpAbstractElement)e2);
              exc.getToWaitlist().add(e2);
            }
          }
          throw exc;
        }
      }
    }

    LazyLogger.log(CustomLogLevel.SpecificCPALevel, "Successor is: BOTTOM");

    return domain.getBottomElement();
  }

  private void removeObsoleteToProcess(AbstractElement element) {
    ItpAbstractElement e = (ItpAbstractElement)element;
    Collection<AbstractElement> toRemove = new Vector<AbstractElement>();
    for (AbstractElement el : toProcess) {
      ItpAbstractElement iel = (ItpAbstractElement)el;
//    if (e.getLocation().equals(iel.getLocation()) &&
//    e.getAbstraction().equals(iel.getAbstraction())) {
      if (iel.getParent() == null) {
        toRemove.add(iel);
      }
    }
    toProcess.removeAll(toRemove);
  }

  @Override
  public List<AbstractElementWithLocation> getAllAbstractSuccessors(
      AbstractElementWithLocation element, Precision prec) throws CPAException, CPATransferException {
    LazyLogger.log(CustomLogLevel.SpecificCPALevel,
        "Getting ALL Abstract Successors of element: ",
        element);

    List<AbstractElementWithLocation> allSucc = new Vector<AbstractElementWithLocation>();
    ItpAbstractElement e = (ItpAbstractElement)element;
    CFANode src = e.getLocation();

    AbstractElementWithLocation r = abstractTree.getRoot();
    if (r == null || !(e != r && e.getParent() == null)) {            
      assert(abstractTree.getRoot() == null ||
          abstractTree.inTree(e));

      if (!e.isCovered()) {
        closeAncestors(e);

        for (int i = 0; i < src.getNumLeavingEdges(); ++i) {
          AbstractElement newe =
            getAbstractSuccessor(e, src.getLeavingEdge(i), prec);
          if (newe != domain.getBottomElement()) {
            allSucc.add((ItpAbstractElement)newe);
          }
        }
      }
      LazyLogger.log(CustomLogLevel.SpecificCPALevel,
          allSucc.size(), " successors found");
    }

    if (!toProcess.isEmpty()) {
      // this is when an element is covered and uncovers others, which
      // need to be re-processed...
      // this is really a HACK!!
      LazyLogger.log(LazyLogger.DEBUG_1,
          "RE-ADDING uncovered to waitlist: ", toProcess);
      Vector<AbstractElementWithLocation> toWaitlist =
        new Vector<AbstractElementWithLocation>();
      Collection<AbstractElement> tmp = toProcess;
      toProcess = new HashSet<AbstractElement>();
      for (AbstractElement el : tmp) {
        ItpAbstractElement ie = (ItpAbstractElement)el;
//      close(ie);
//if (!domain.getCPA().isCovered(ie)) {
        if (ie.getParent() != null) {
          toWaitlist.add(ie);
        }
//      }
      }
      if (!toWaitlist.isEmpty()) {
        Collections.sort(toWaitlist, new Comparator<AbstractElement>() {
          @Override
          public int compare(AbstractElement arg0,
                             AbstractElement arg1) {
            ItpAbstractElement a =
              (ItpAbstractElement)arg0;
            ItpAbstractElement b =
              (ItpAbstractElement)arg1;
            return b.getId() - a.getId();
          }
        });
//      toWaitlist.add((ItpAbstractElement)element);
//throw new ToWaitListException(toWaitlist);
        allSucc.addAll(toWaitlist);
      }
    }


    return allSucc;
  }

  public void addToProcess(Collection<ItpAbstractElement> elems) {
    toProcess.addAll(elems);
  }

  private void addToReached(ItpAbstractElement e) {
    CFANode n = e.getLocation();
    Set<AbstractElement> s = null;
    if (reached.containsKey(n)) {
      s = reached.get(n);
    } else {
      s = new TreeSet<AbstractElement>(reachedComp);
    }
    s.add(e);
    reached.put(n, s);
  }

  private void removeFromReached(ItpAbstractElement e) {
    CFANode n = e.getLocation();
    assert(reached.containsKey(n));
    reached.get(n).remove(e);
    e.setParent(null);
    if (e.isCovered()) {
      domain.getCPA().uncover(e);
    }
  }

  /*
   * Closes an element. That is, tries to see if the element is covered by
   * any of the previously-generated elements corresponding to the same
   * program location
   */
  private void close(ItpAbstractElement e) {
    if (lastClosed == e) {
      return;
    }
    lastClosed  = e;

    ItpCPA cpa = domain.getCPA();
    if (reached.containsKey(e.getLocation())) {
      ItpStopOperator s =
        (ItpStopOperator)domain.getCPA().getStopOperator();
      for (AbstractElement el : reached.get(e.getLocation())) {
        if (cpa.isCovered(e)) {
          return;
        }
        try {
          s.stop(e, el);
        } catch (CPAException e1) {
          e1.printStackTrace();
          System.exit(1);
        }
      }
    }
  }

  private void closeAncestors(ItpAbstractElement e) {
    ItpAbstractElement p = e.getParent();
    while (p != null) {
      close(p);
      p = p.getParent();
    }
  }

  /*
   * tries to force coverage of an element (see the paper)
   */
  private boolean forceCover(ItpAbstractElement e) {
    if (!useForcedCovering) {
      return false;
    }

    if (lastForcedCover == e) {
      return false;
    }
    lastForcedCover = e;

    final int MAX_FORCED_COVER = 3;
    final double DISTANCE_THRESHOLD = 0.5;

    ItpCPA cpa = domain.getCPA();
    ItpCounterexampleRefiner refiner = cpa.getRefiner();
    SymbolicFormulaManager mgr = cpa.getFormulaManager();

    if (reached.containsKey(e.getLocation())) {

      LazyLogger.log(LazyLogger.DEBUG_1, "Checking Forced Coverage of: ",
          e);

      Set<AbstractElement> path = new HashSet<AbstractElement>();
      ItpAbstractElement cur = e;
      while (cur != null) {
        path.add(cur);
        cur = cur.getParent();
      }

      NavigableSet<AbstractElement> s =
        (NavigableSet<AbstractElement>)reached.get(e.getLocation());
      int i = 0;
      for (Iterator<AbstractElement> it = s.descendingIterator();
      it.hasNext() && i < MAX_FORCED_COVER; ++i) {
        ItpAbstractElement el = (ItpAbstractElement)it.next();
        if (el != e && el.getId() < e.getId() &&
            !el.getAbstraction().isTrue() &&
            (double)(e.getId() - el.getId())/(double)e.getId() <
            DISTANCE_THRESHOLD) {
          // get the nearest common ancestor
          ItpAbstractElement nca = el;
          while (nca != null && !path.contains(nca)) {
            nca = nca.getParent();
          }
          assert(nca != null);

          Deque<ItpAbstractElement> path2 =
            new LinkedList<ItpAbstractElement>();
          cur = e;
          while (cur != nca) {
            assert(cur != null);
            path2.addFirst(cur);
            cur = cur.getParent();
          }
          path2.addFirst(nca);

          if (nca.getAbstraction().isFalse()) {
            // this might happen because of other forced covers
//          for (ItpAbstractElement e2 : path2) {
//if (!e2.getAbstraction().isFalse()) {
//          toProcess.addAll(cpa.uncoverAll(e2));
//          e2.setAbstraction(nca.getAbstraction());
//          }
//          }
//          cpa.setCoveredBy(e, el);
//          LazyLogger.log(LazyLogger.DEBUG_1,
//          "YES, Forcing coverage of: ", e, " by: ", el);
//          ++forcedCoverStats.numForcedCoveredElements;
//          return true;
            continue;
          }

          ItpCounterexampleTraceInfo info = null;
          ForcedCoverCacheKey key =
            new ForcedCoverCacheKey(
                nca.getAbstraction(), el.getAbstraction(),
                path2);
          if (forcedCoverCache.containsKey(key)) {
            info = forcedCoverCache.get(key);
            ++forcedCoverStats.numForcedCoverChecksCached ;
          } else {
            info = refiner.forceCover(mgr, nca, path2, el);
            ++forcedCoverStats.numForcedCoverChecks;
            forcedCoverCache.put(key, info);
          }
          if (info.isSpurious()) {
            // ok, e can be covered by el. Strengthen the formulas
            // in the path
            for (ItpAbstractElement e2 : path2) {
              SymbolicFormula newabs =
                info.getFormulaForRefinement(e2);
              if (!mgr.entails(e2.getAbstraction(), newabs)) {
                toProcess.addAll(cpa.uncoverAll(e2));
                e2.setAbstraction(mgr.makeAnd(
                    e2.getAbstraction(), newabs));
              }
            }
            cpa.setCoveredBy(e, el);
            LazyLogger.log(LazyLogger.DEBUG_1,
                "YES, Forcing coverage of: ", e, " by: ", el);
            ++forcedCoverStats.numForcedCoveredElements;
            return true;
          }
        }
      }
    }
    return false;
  }

  @Override
  public AbstractElement strengthen(AbstractElement element,
                         List<AbstractElement> otherElements, CFAEdge cfaEdge,
                         Precision precision) {    
    return null;
  }
}
