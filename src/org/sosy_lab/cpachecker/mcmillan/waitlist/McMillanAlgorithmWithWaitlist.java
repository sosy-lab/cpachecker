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

import static org.sosy_lab.cpachecker.util.AbstractElements.extractLocation;
import static org.sosy_lab.cpachecker.util.CFAUtils.leavingEdges;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.Timer;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
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
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.mcmillan.waitlist.cpa.McMillanAbstractElement;
import org.sosy_lab.cpachecker.mcmillan.waitlist.cpa.McMillanCPA;
import org.sosy_lab.cpachecker.util.AbstractElements;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.predicates.CachingPathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.ExtendedFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interpolation.CounterexampleTraceInfo;
import org.sosy_lab.cpachecker.util.predicates.interpolation.InterpolationManager;
import org.sosy_lab.cpachecker.util.predicates.interpolation.UninstantiatingInterpolationManager;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

public class McMillanAlgorithmWithWaitlist implements Algorithm, StatisticsProvider {

  private final LogManager logger;

  private final ARTCPA cpa;

  private final ExtendedFormulaManager fmgr;
  private final PathFormulaManager pfmgr;
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

    McMillanCPA mcmillanCpa = Iterables.getOnlyElement(Iterables.filter(CPAs.asIterable(pCpa), McMillanCPA.class));

    fmgr = mcmillanCpa.getFormulaManager();
    solver = mcmillanCpa.getSolver();
    pfmgr = new CachingPathFormulaManager(new PathFormulaManagerImpl(fmgr, config, logger));
    imgr = new UninstantiatingInterpolationManager(fmgr, pfmgr, mcmillanCpa.getTheoremProver(), config, logger);
  }

  @Override
  public boolean run(ReachedSet pReachedSet) throws CPAException, InterruptedException {
    unwind(pReachedSet);
    return true;
  }

  private void expand(ARTElement v, ReachedSet reached) throws CPAException, InterruptedException {
    expandTime.start();
    try {
      assert v.getChildren().isEmpty() && !v.isCovered();
      reached.removeOnlyFromWaitlist(v);

      Precision precision = reached.getPrecision(v);

      CFANode loc = extractLocation(v);
      for (CFAEdge edge : leavingEdges(loc)) {

        Collection<ARTElement> successors = cpa.getTransferRelation().getAbstractSuccessors(v, precision, edge);
        if (successors.isEmpty()) {
          // edge not feasible
          continue;
        }
        assert successors.size() == 1;

        ARTElement w = Iterables.getOnlyElement(successors);
        reached.add(w, precision);
      }
    } finally {
      expandTime.stop();
    }
  }

  private List<ARTElement> refine(final ARTElement v, ReachedSet reached) throws CPAException, InterruptedException {
    refinementTime.start();
    try {
      assert (v.isTarget() && !getStateFormula(v).isFalse());

      logger.log(Level.FINER, "Refinement on " + v);

      // build list of path elements in bottom-to-top order and reverse
      List<ARTElement> path = getPathFromRootTo(v);
      path = path.subList(1, path.size()); // skip root element, it has no formula

      // build list of formulas for edges
      List<Formula> pathFormulas = new ArrayList<Formula>(path.size());
      addPathFormulasToList(path, pathFormulas);

      CounterexampleTraceInfo<Formula> cex = imgr.buildCounterexampleTrace(pathFormulas, Collections.<ARTElement>emptySet());

      if (!cex.isSpurious()) {
        return Collections.emptyList(); // real counterexample
      }

      logger.log(Level.FINER, "Refinement successful");

      path = path.subList(0, path.size()-1); // skip last element, itp is always false there
      assert cex.getPredicatesForRefinement().size() ==  path.size();

      List<ARTElement> changedElements = new ArrayList<ARTElement>();

      for (Pair<Formula, ARTElement> interpolationPoint : Pair.zipList(cex.getPredicatesForRefinement(), path)) {
        Formula itp = interpolationPoint.getFirst();
        ARTElement w = interpolationPoint.getSecond();

        if (itp.isTrue()) {
          continue;
        }

        Formula stateFormula = getStateFormula(w);
        if (!solver.implies(stateFormula, itp)) {
          setStateFormula(w, fmgr.makeAnd(stateFormula, itp));
          removeCoverageOf(w, reached);
          changedElements.add(w);
        }
      }

      // itp of last element is always false, set it
      if (!getStateFormula(v).isFalse()) {
        setStateFormula(v, fmgr.makeFalse());
        removeCoverageOf(v, reached);
        changedElements.add(v);
      }

      return changedElements;
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
        && !w.isCovered() // ???
        && w.isOlderThan(v)
        && !isAncestorOf(v, w)
        && cpa.getStopOperator().stop(v, Collections.<AbstractElement>singleton(w), prec);
  }

  private boolean cover(ARTElement v, ARTElement w, Precision prec, ReachedSet reached) throws CPAException {
    coverTime.start();
    try {
      assert !v.isCovered();

      if (covers(v, w, prec)) {

        for (ARTElement childOfV : v.getSubtree()) {
          // each child of v is now covered
          reached.removeOnlyFromWaitlist(childOfV);

          // each child of v now doesn't cover anything anymore
          removeCoverageOf(childOfV, reached);
        }

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
  private static void removeCoverageOf(ARTElement v, ReachedSet reached) {
    for (ARTElement coveredByChildOfV : v.clearCoverage()) {
      for (ARTElement leaf : findLeafChildrenOf(coveredByChildOfV)) {
        reached.reAddToWaitlist(leaf);
      }
    }
  }

  private static Iterable<ARTElement> findLeafChildrenOf(ARTElement v) {
    return filterLeafs(v.getSubtree());
  }

  private static Iterable<ARTElement> filterLeafs(Iterable<ARTElement> vertices) {
    return Iterables.filter(vertices, new Predicate<ARTElement>() {
      @Override
      public boolean apply(ARTElement pInput) {
        // TODO don't count nodes at sink nodes
        return pInput.getChildren().isEmpty() && !getStateFormula(pInput).isFalse();
      }
    });
  }


  private boolean close(ARTElement v, ReachedSet reached) throws CPAException {
    closeTime.start();
    try {
      if (v.isCovered()) {
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

  private boolean dfs(ARTElement v, ReachedSet reached) throws CPAException, InterruptedException {
    if (close(v, reached)) {
      return true; // no need to expand
    }

    if (v.isTarget()) {
      List<ARTElement> changedElements = refine(v, reached);
      if (changedElements.isEmpty()) {
        return false; // real counterexample
      }

      // optimization: instead of closing all ancestors of v,
      // close only those that were strengthened during refine
      for (ARTElement w : changedElements) {
        if (close(w, reached)) {
          break; // all further elements are covered anyway
        }
      }

      assert getStateFormula(v).isFalse();
      reached.remove(v);
      return true; // no need to expand further
    }

    expand(v, reached);
    for (ARTElement w : v.getChildren()) {
      if (!McMillanAlgorithmWithWaitlist.getStateFormula(w).isFalse()) {
        dfs(w, reached);
      }
    }

    return true;
  }

  private void unwind(ReachedSet reached) throws CPAException, InterruptedException {

    outer:
    while (reached.hasWaitingElement()) {
      ARTElement v = (ARTElement)reached.popFromWaitlist();
      assert v.getChildren().isEmpty();
      assert !v.isCovered();

      // close parents of v
      List<ARTElement> path = getPathFromRootTo(v);
      path = path.subList(0, path.size()-1); // skip v itself
      for (ARTElement w : path) {
        if (close(w, reached)) {
          continue outer; // v is now covered
        }
      }

      if (!dfs(v, reached)) {
        logger.log(Level.INFO, "Bug found");
        break outer;
      }
    }
  }

  private void addPathFormulasToList(List<ARTElement> path, List<Formula> pathFormulas) throws CPATransferException {
    PathFormula pf = pfmgr.makeEmptyPathFormula();
    for (ARTElement w : path) {
      ARTElement parent = Iterables.getOnlyElement(w.getParents());

      pf = pfmgr.makeAnd(pf, parent.getEdgeToChild(w));
      pathFormulas.add(pf.getFormula());
      pf = pfmgr.makeEmptyPathFormula(pf); // reset formula, keep SSAMap
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
    return AbstractElements.extractElementByType(pARTElement, McMillanAbstractElement.class).getStateFormula();
  }

  private static void setStateFormula(ARTElement pARTElement, Formula pFormula) {
    AbstractElements.extractElementByType(pARTElement, McMillanAbstractElement.class).setStateFormula(pFormula);
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

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(new Stats());
  }
}
