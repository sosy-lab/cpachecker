package cpa.predicateabstraction;

import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;

import logging.LazyLogger;
import cmdline.CPAMain;
import cpa.art.ARTElement;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.AbstractElementWithLocation;
import cpa.common.interfaces.RefinementManager;
import cpa.predicateabstraction.PredicateAbstractionTransferRelation.Path;
import cpa.symbpredabs.Predicate;
import exceptions.CPATransferException;
import exceptions.RefinementNeededException;

public class PredicateAbstractionRefinementManager implements RefinementManager {

  @Override
  public boolean performRefinement(AbstractElement pElement) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean performRefinement(AbstractElement pElement,
      ARTElement pARTElement) {
  }

  // abstraction refinement is performed here
  public void performRefinement(Deque<PredicateAbstractionAbstractElement> path,
      CounterexampleTraceInfo info) throws CPATransferException {
    LazyLogger.log(LazyLogger.DEBUG_1, "STARTING REFINEMENT");
    UpdateablePredicateMap curpmap =
      (UpdateablePredicateMap)domain.getCPA().getPredicateMap();
    PredicateAbstractionAbstractElement root = null;
    PredicateAbstractionAbstractElement cur = null;
    PredicateAbstractionAbstractElement firstInterpolant = null;
    for (PredicateAbstractionAbstractElement e : path) {
      Collection<Predicate> newpreds = info.getPredicatesForRefinement(e);
      if (firstInterpolant == null && newpreds.size() > 0) {
        firstInterpolant = e;
      }
      if (curpmap.update(e.getLocation(), newpreds)) {
        LazyLogger.log(LazyLogger.DEBUG_1, "REFINING LOCATION: ",
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
        LazyLogger.log(CustomLogLevel.SpecificCPALevel,
            "Restarting ART from scratch");
        root = (PredicateAbstractionAbstractElement)abstractTree.getRoot();
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
      root = (PredicateAbstractionAbstractElement)abstractTree.getRoot();
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
        PredicateAbstractionAbstractElement e = (PredicateAbstractionAbstractElement)it.next();
        if (e.isCovered() && e.getMark() < cur.getMark()) {
          LazyLogger.log(LazyLogger.DEBUG_1, "NOT unreaching ", e,
              " because it was covered before ", cur);
          it.remove();
        }
      }
    }

    PredicateAbstractionCPA cpa = domain.getCPA();
    for (AbstractElementWithLocation ae : toUnreach) {
      PredicateAbstractionAbstractElement e = (PredicateAbstractionAbstractElement)ae;
      if (e.isCovered()) {
        e.setCovered(false);
        cpa.setUncovered(e);
      }
    }
    if (root != abstractTree.getRoot()) {
      // then, we have to unmark some nodes
      Collection<PredicateAbstractionAbstractElement> tmp =
        cpa.getCovered();
      for (Iterator<PredicateAbstractionAbstractElement> i = tmp.iterator(); 
      i.hasNext(); ) {
        PredicateAbstractionAbstractElement e = i.next();
        assert(e.isCovered());
        if (e.getMark() > root.getMark()) {
          e.setCovered(false);
          i.remove();
          toWaitlist.add(e.getParent());
          toUnreach.add(e);
        }
      }
    }

    LazyLogger.log(LazyLogger.DEBUG_1, "REFINEMENT - toWaitlist: ",
        toWaitlist);
    LazyLogger.log(LazyLogger.DEBUG_1, "REFINEMENT - toUnreach: ",
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

}
