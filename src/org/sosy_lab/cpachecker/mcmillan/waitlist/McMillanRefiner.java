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
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Refiner;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.cpa.art.ARTUtils;
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

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

public class McMillanRefiner implements Refiner, StatisticsProvider {

  private final LogManager logger;

  private final ConfigurableProgramAnalysis cpa;

  private final ExtendedFormulaManager fmgr;
  private final Solver solver;
  private final InterpolationManager<Formula> imgr;

  private final Timer refinementTime = new Timer();
  private final Timer coverTime = new Timer();
  private final Timer closeTime = new Timer();

  private class Stats implements Statistics {

    @Override
    public String getName() {
      return "McMillan Refiner";
    }

    @Override
    public void printStatistics(PrintStream out, Result pResult, ReachedSet pReached) {
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

  public static McMillanRefiner create(ConfigurableProgramAnalysis pCpa) throws CPAException, InvalidConfigurationException {
    return new McMillanRefiner(pCpa);
  }

  public McMillanRefiner(ConfigurableProgramAnalysis pCpa) throws InvalidConfigurationException, CPAException {
    cpa = pCpa;

    ImpactCPA impactCpa = Iterables.getOnlyElement(Iterables.filter(CPAs.asIterable(pCpa), ImpactCPA.class));

    Configuration config = impactCpa.getConfiguration();
    logger = impactCpa.getLogManager();
    fmgr = impactCpa.getFormulaManager();
    solver = impactCpa.getSolver();
    imgr = new UninstantiatingInterpolationManager(fmgr, impactCpa.getPathFormulaManager(), impactCpa.getTheoremProver(), config, logger);
  }

  @Override
  public boolean performRefinement(ReachedSet pReached) throws CPAException, InterruptedException {
    ARTElement lastElement = (ARTElement)pReached.getLastElement();
    assert lastElement.isTarget();
    return refine(lastElement, pReached);
  }

  private boolean refine(final ARTElement v, ReachedSet reached) throws CPAException, InterruptedException {
    refinementTime.start();
    try {

      assert (v.isTarget() && !getStateFormula(v).isFalse());
      assert v.mayCover();

      logger.log(Level.FINER, "Refinement on " + v);

      // build list of path elements in bottom-to-top order and reverse
      List<ARTElement> path = getPathFromRootTo(v);
      path = path.subList(1, path.size()); // skip root element, it has no formula
      path = ImmutableList.copyOf(Iterables.filter(path, new Predicate<AbstractElement>() {
          @Override
          public boolean apply(AbstractElement pInput) {
            return AbstractElements.extractElementByType(pInput, ImpactAbstractElement.class).isAbstractionElement();
          }
        }));

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

      uncover(subtree, reached);

      for (ARTElement removedNode : subtree) {
        removedNode.removeFromART();
      }
      reached.removeAll(subtree);

      // optimization: instead of closing all ancestors of v,
      // close only those that were strengthened during refine
      for (ARTElement w : changedElements) {
        if (cover(w, reached)) {
          break; // all further elements are covered anyway
        }
      }

      return true; // refinement successful
    } finally {
      refinementTime.stop();
    }
  }

  private boolean cover(ARTElement v, ReachedSet reached) throws CPAException {
    coverTime.start();
    try {
      assert v.mayCover();

      cpa.getStopOperator().stop(v, reached.getReached(v), reached.getPrecision(v));
      // ignore return value of stop, because it will always be false

      if (v.isCovered()) {
        reached.removeOnlyFromWaitlist(v);

        Set<ARTElement> subtree = v.getSubtree();

        // first, uncover all necessary states

        uncover(subtree, reached);

        // second, clean subtree of covered element
        subtree.remove(v); // but no not clean v itself

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
          assert childOfV.getCoveredByThis().isEmpty();
          assert !childOfV.mayCover();
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
    for (ARTElement coveredByChildOfV : ImmutableList.copyOf(v.getCoveredByThis())) {
      uncover(coveredByChildOfV, reached);
    }
    assert v.getCoveredByThis().isEmpty();
  }

  private void uncover(Set<ARTElement> subtree, ReachedSet reached) {
    Set<ARTElement> coveredStates = ARTUtils.getCoveredBy(subtree);
    for (ARTElement coveredState : coveredStates) {
      // uncover each previously covered state
      uncover(coveredState, reached);
    }
    assert ARTUtils.getCoveredBy(subtree).isEmpty() : "Subtree of covered node still covers other elements";
  }

  private void uncover(ARTElement v, ReachedSet reached) {
    v.uncover();

    // this is the subtree of elements which now become uncovered
    Set<ARTElement> uncoveredSubTree = v.getSubtree();

    for (ARTElement e : uncoveredSubTree) {
      assert !e.isCovered();

      e.setCovering();

      if (!e.wasExpanded()) {
        // its a leaf
        reached.reAddToWaitlist(e);
      }
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

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(new Stats());
  }
}
