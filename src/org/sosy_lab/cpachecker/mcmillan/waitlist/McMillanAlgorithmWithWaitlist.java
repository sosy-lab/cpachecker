/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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
package org.sosy_lab.cpachecker.mcmillan.waitlist;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.Timer;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.art.ARTCPA;
import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.cpa.impact.ImpactAbstractElement;
import org.sosy_lab.cpachecker.cpa.impact.ImpactCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractElements;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.predicates.ExtendedFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.Solver;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interpolation.CounterexampleTraceInfo;
import org.sosy_lab.cpachecker.util.predicates.interpolation.InterpolationManager;
import org.sosy_lab.cpachecker.util.predicates.interpolation.UninstantiatingInterpolationManager;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

public class McMillanAlgorithmWithWaitlist implements Algorithm, StatisticsProvider {

  private final LogManager logger;

  private final ARTCPA cpa;

  private final ExtendedFormulaManager fmgr;
  private final Solver solver;
  private final InterpolationManager<Formula> imgr;

  private final Timer expandTime = new Timer();
  private final Timer refinementTime = new Timer();
  private final Timer coverTime = new Timer();
  private final Timer closeTime = new Timer();

  private class Stats implements Statistics {

    @Override
    public String getName() {
      return "McMillan's algorithm";
    }

    @Override
    public void printStatistics(PrintStream out, Result pResult, ReachedSet pReached) {
      out.println("Time for expand:                    " + expandTime);
      out.println("Time for refinement:                " + refinementTime);
      out.println("Time for close:                     " + closeTime);
      out.println("  Time for cover:                   " + coverTime);
      out.println("Time spent by solver for reasoning: " + solver.solverTime);
      out.println();
      out.println("Number of implication checks:       " + solver.implicationChecks);
      out.println("  trivial:                          " + solver.trivialImplicationChecks);
      out.println("  cached:                           " + solver.cachedImplicationChecks);
      out.println("Number of refinements:              " + refinementTime.getNumberOfIntervals());
    }
  }

  public McMillanAlgorithmWithWaitlist(Configuration config, LogManager pLogger, ConfigurableProgramAnalysis pCpa) throws InvalidConfigurationException, CPAException {
    logger = pLogger;
    cpa = (ARTCPA)pCpa;

    ImpactCPA impactCpa = Iterables.getOnlyElement(Iterables.filter(CPAs.asIterable(pCpa), ImpactCPA.class));

    fmgr = impactCpa.getFormulaManager();
    solver = impactCpa.getSolver();
    imgr = new UninstantiatingInterpolationManager(fmgr, impactCpa.getPathFormulaManager(), impactCpa.getTheoremProver(), config, logger);
  }

  @Override
  public boolean run(ReachedSet pReachedSet) throws CPAException, InterruptedException {
    run2(pReachedSet);
    return true;
  }

  private void expand(ARTElement v, ReachedSet reached) throws CPAException, InterruptedException {
    expandTime.start();
    try {
      assert v.getChildren().isEmpty() && !isCovered(v);
      assert !v.wasExpanded();
      reached.removeOnlyFromWaitlist(v);

      Precision precision = reached.getPrecision(v);

      Collection<? extends AbstractElement> successors = cpa.getTransferRelation().getAbstractSuccessors(v, precision, null);

      for (AbstractElement successor : successors) {
        reached.add(successor, precision);

        if (close((ARTElement)successor, reached)) {
          reached.remove(successor);
          ((ARTElement)successor).removeFromART();
          continue; // no need to expand
        }

        if (((ARTElement)successor).isTarget()) {
          break;
        }
      }
    } finally {
      expandTime.stop();
    }
  }

  private boolean refine(final ARTElement v, ReachedSet reached) throws CPAException, InterruptedException {
    refinementTime.start();
    try {
      assert (v.isTarget() && !getStateFormula(v).isFalse());
      assert !isCovered(v);

      logger.log(Level.FINER, "Refinement on " + v);

      // build list of path elements in bottom-to-top order and reverse
      List<ARTElement> path = getPathFromRootTo(v);
      path = path.subList(1, path.size()); // skip root element, it has no formula

      // build list of formulas for edges
      List<Formula> pathFormulas = new ArrayList<Formula>(path.size());
      addPathFormulasToList(path, pathFormulas);

      CounterexampleTraceInfo<Formula> cex = imgr.buildCounterexampleTrace(pathFormulas, Collections.<ARTElement>emptySet());

      if (!cex.isSpurious()) {
        return false; // real counterexample
      }

      logger.log(Level.FINER, "Refinement successful");

      path = path.subList(0, path.size()-1); // skip last element, itp is always false there
      assert cex.getPredicatesForRefinement().size() ==  path.size();

      List<ARTElement> changedElements = new ArrayList<ARTElement>();
      ARTElement root = null;

      for (Pair<Formula, ARTElement> interpolationPoint : Pair.zipList(cex.getPredicatesForRefinement(), path)) {
        Formula itp = interpolationPoint.getFirst();
        ARTElement w = interpolationPoint.getSecond();

        if (itp.isTrue()) {
          continue;
        }

        if (itp.isFalse()) {
          root = w;
          break;
        }

        Formula stateFormula = getStateFormula(w);
        if (!solver.implies(stateFormula, itp)) {
          setStateFormula(w, fmgr.makeAnd(stateFormula, itp));
          removeCoverageOf(w, reached);
          changedElements.add(w);
        }
      }

      assert root != null;
      Set<ARTElement> subtree = root.getSubtree();
      assert subtree.contains(v);

      for (ARTElement removedNode : subtree) {
        setStateFormula(removedNode, fmgr.makeFalse());
        removeCoverageOf(removedNode, reached);
        removedNode.removeFromART();
      }
      reached.removeAll(subtree);

      // optimization: instead of closing all ancestors of v,
      // close only those that were strengthened during refine
      for (ARTElement w : changedElements) {
        if (close(w, reached)) {
          break; // all further elements are covered anyway
        }
      }

      return true; // refinement successful
    } finally {
      refinementTime.stop();
    }
  }

  /**
   * Check if a ARTElement v may potentially be covered by another ARTElement w.
   * It checks everything except their state formulas.
   */
  private boolean covers(ARTElement v, ARTElement w, Precision prec) throws CPAException {
    return (v != w)
        && !isCovered(w)
        && w.isOlderThan(v)
        && !isAncestorOf(v, w)
        && cpa.getStopOperator().stop(v, Collections.<AbstractElement>singleton(w), prec);
  }

  private boolean cover(ARTElement v, ARTElement w, Precision prec, ReachedSet reached) throws CPAException {
    coverTime.start();
    try {
      assert !isCovered(v);

      if (covers(v, w, prec)) {
        removeCoverageOf(v, reached);
        reached.removeOnlyFromWaitlist(v);

        Set<ARTElement> subtree = v.getSubtree();
        subtree.remove(v);

        for (ARTElement childOfV : subtree) {
          // each child of v is now not covered directly anymore
          if (childOfV.isCovered()) {
            childOfV.uncover();
          }

          reached.removeOnlyFromWaitlist(childOfV);

          childOfV.setNotCovering();
        }

        for (ARTElement childOfV : subtree) {
          // each child of v now doesn't cover anything anymore
          removeCoverageOf(childOfV, reached);
        }

        assert !reached.getWaitlist().contains(v.getSubtree());
        return true;
      }
      return false;

    } finally {
      coverTime.stop();
    }
  }

  /**
   * Remove all covering relations from a node so that this node does not cover
   * any other node anymore.
   * Also adds any now uncovered lead nodes to the waitlist.
   */
  private void removeCoverageOf(ARTElement v, ReachedSet reached) {
    for (ARTElement coveredByChildOfV : v.clearCoverage()) {

      // this is the subtree of elements which now becomes uncovered
      Set<ARTElement> uncoveredSubTree = coveredByChildOfV.getSubtree();

      for (ARTElement e : uncoveredSubTree) {
        assert !e.isCovered();

        e.setCovering();

        if (!e.wasExpanded()) {
          // its a leaf
          reached.reAddToWaitlist(e);
        }
      }
    }
  }

  private boolean close(ARTElement v, ReachedSet reached) throws CPAException {
    closeTime.start();
    try {
      if (isCovered(v)) {
        return true;
      }

      Precision prec = reached.getPrecision(v);
      for (AbstractElement ae : reached.getReached(v)) {
        ARTElement w = (ARTElement)ae;

        if (cover(v, w, prec, reached)) {
          return true; // v is now covered
        }
      }

      return false;

    } finally {
      closeTime.stop();
    }
  }


  private void dfs(ARTElement v, ReachedSet reached) throws CPAException, InterruptedException {
    if (close(v, reached)) {
      return;
    }

    expand(v, reached);

    for (ARTElement w : v.getChildren()) {
      if (AbstractElements.isTargetElement(reached.getLastElement())) {
        return;
      }

      dfs(w, reached);
    }

    return;
  }

  private void unwind(ReachedSet reached) throws CPAException, InterruptedException {

    outer:
    while (reached.hasWaitingElement()) {
      ARTElement v = (ARTElement)reached.popFromWaitlist();
      assert v.getChildren().isEmpty();
      assert !isCovered(v);

      // close parents of v
      List<ARTElement> path = getPathFromRootTo(v);
      path = path.subList(0, path.size()-1); // skip v itself
      for (ARTElement w : path) {
        if (close(w, reached)) {
          continue outer; // v is now covered
        }
      }

      dfs(v, reached);

      if (AbstractElements.isTargetElement(reached.getLastElement())) {
        if (!refine((ARTElement)reached.getLastElement(), reached)) {
          logger.log(Level.INFO, "Bug found");
          break outer;
        }
      }
    }
  }


  private void run2(ReachedSet reached) throws CPAException, InterruptedException {

    while (reached.hasWaitingElement()) {
      for (AbstractElement e : reached.getWaitlist()) {
        assert !((ARTElement)e).isCovered();
        assert !isCovered((ARTElement)e) : reached.size();
      }

      ARTElement v = (ARTElement)reached.popFromWaitlist();
      assert !v.isDestroyed();
      assert v.getChildren().isEmpty();
      assert !v.isCovered();
      assert !isCovered(v);
      assert !v.wasExpanded();
      assert !getStateFormula(v).isFalse();

      if (close(v, reached)) {
        continue;
      }

      Precision precision = reached.getPrecision(v);

      Collection<? extends AbstractElement> successors = cpa.getTransferRelation().getAbstractSuccessors(v, precision, null);

      for (AbstractElement successor : successors) {
        boolean stop = cpa.getStopOperator().stop(successor, reached.getReached(successor), precision);

        reached.add(successor, precision);

        if (stop) {
          reached.removeOnlyFromWaitlist(successor);
          continue;
        }

        if (AbstractElements.isTargetElement(successor)) {
          boolean safe = refine((ARTElement)successor, reached);
          if (!safe) {
            logger.log(Level.INFO, "Bug found");
            return;
          }
        }

      }
      assert v.wasExpanded();
    }
  }

  private void addPathFormulasToList(List<ARTElement> path, List<Formula> pathFormulas) throws CPATransferException {
    for (ARTElement w : path) {
      ImpactAbstractElement.AbstractionElement element = AbstractElements.extractElementByType(w, ImpactAbstractElement.AbstractionElement.class);
      pathFormulas.add(element.getBlockFormula().getFormula());
    }
  }

  private static List<ARTElement> getPathFromRootTo(ARTElement v) {
    List<ARTElement> path = new ArrayList<ARTElement>();

    ARTElement w = v;
    while (!w.getParents().isEmpty()) {
      path.add(w);
      w = Iterables.getOnlyElement(w.getParents());
    }
    path.add(w); // root element

    return Lists.reverse(path);
  }

  private static Formula getStateFormula(ARTElement pARTElement) {
    return AbstractElements.extractElementByType(pARTElement, ImpactAbstractElement.AbstractionElement.class).getStateFormula();
  }

  private static void setStateFormula(ARTElement pARTElement, Formula pFormula) {
    AbstractElements.extractElementByType(pARTElement, ImpactAbstractElement.AbstractionElement.class).setStateFormula(pFormula);
  }

  /**
   * Checks if the first element is ancestor of the second element.
   */
  private static boolean isAncestorOf(ARTElement ancestor, ARTElement v) {
    if (ancestor == v) {
      return true;
    }

    while (!v.getParents().isEmpty()) {
      v = Iterables.getOnlyElement(v.getParents());
      if (ancestor == v) {
        return true;
      }
    }
    return false;
  }

  private static boolean isCovered(ARTElement pArtElement) {
    if (pArtElement.isCovered()) {
      return true;
    }

    ARTElement e = pArtElement;
    while (!e.getParents().isEmpty()) {
      e = Iterables.getOnlyElement(e.getParents());

      if (e.isCovered()) {
        return true;
      }
    }

    return false;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(new Stats());
  }
}
