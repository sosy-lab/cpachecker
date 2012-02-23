/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.impact;

import static com.google.common.collect.Iterables.*;
import static com.google.common.collect.Lists.transform;
import static org.sosy_lab.cpachecker.util.AbstractElements.extractElementByType;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.Timer;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.cpa.art.ARTReachedSet;
import org.sosy_lab.cpachecker.cpa.art.ARTUtils;
import org.sosy_lab.cpachecker.cpa.art.Path;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractElements;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.predicates.ExtendedFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.Solver;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interpolation.AbstractInterpolationBasedRefiner;
import org.sosy_lab.cpachecker.util.predicates.interpolation.CounterexampleTraceInfo;
import org.sosy_lab.cpachecker.util.predicates.interpolation.InterpolationManager;
import org.sosy_lab.cpachecker.util.predicates.interpolation.UninstantiatingInterpolationManager;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

public class ImpactRefiner extends AbstractInterpolationBasedRefiner<Formula, ARTElement> implements StatisticsProvider {

  private class Stats implements Statistics {

    private int newItpWasAdded = 0;

    private final Timer itpCheck  = new Timer();
    private final Timer coverTime = new Timer();
    private final Timer artUpdate = new Timer();

    @Override
    public String getName() {
      return "Impact Refiner";
    }

    @Override
    public void printStatistics(PrintStream out, Result pResult, ReachedSet pReached) {
      out.println("Time for checking whether Itp is new:" + itpCheck);
      out.println("Time for coverage checks:            " + coverTime);
      out.println("Time spent by solver for reasoning:  " + solver.solverTime);
      out.println("Time for ART update:                 " + artUpdate);
      out.println();
      out.println("Number of non-new interpolants:     " + (itpCheck.getNumberOfIntervals() - newItpWasAdded));
      out.println("Number of implication checks:       " + solver.implicationChecks);
      out.println("  trivial:                          " + solver.trivialImplicationChecks);
      out.println("  cached:                           " + solver.cachedImplicationChecks);
      out.println();
    }
  }

  protected final ExtendedFormulaManager fmgr;
  protected final Solver solver;

  private final Stats stats = new Stats();

  public static ImpactRefiner create(ConfigurableProgramAnalysis pCpa) throws CPAException, InvalidConfigurationException {
    ImpactCPA impactCpa = getFirst(filter(CPAs.asIterable(pCpa), ImpactCPA.class), null);
    if (impactCpa == null) {
      throw new InvalidConfigurationException(ImpactRefiner.class.getSimpleName() + " needs an ImpactCPA");
    }

    Configuration config = impactCpa.getConfiguration();
    LogManager logger = impactCpa.getLogManager();
    ExtendedFormulaManager fmgr = impactCpa.getFormulaManager();

    InterpolationManager<Formula> manager = new UninstantiatingInterpolationManager(
                                                  fmgr,
                                                  impactCpa.getPathFormulaManager(),
                                                  impactCpa.getTheoremProver(),
                                                  config, logger);

    return new ImpactRefiner(config, logger, pCpa, manager, fmgr, impactCpa.getSolver());
  }

  protected ImpactRefiner(final Configuration config, final LogManager logger,
      final ConfigurableProgramAnalysis pCpa,
      final InterpolationManager<Formula> pInterpolationManager,
      final ExtendedFormulaManager pFmgr, final Solver pSolver) throws InvalidConfigurationException, CPAException {

    super(config, logger, pCpa, pInterpolationManager);

    solver = pSolver;
    fmgr = pFmgr;
  }

  @Override
  protected List<ARTElement> transformPath(Path pPath) {
    // filter abstraction elements

    List<ARTElement> result = ImmutableList.copyOf(
        Iterables.filter(
            Iterables.transform(
                skip(pPath, 1),
                Pair.<ARTElement>getProjectionToFirst()),

            new Predicate<ARTElement>() {
                @Override
                public boolean apply(ARTElement pInput) {
                  return extractElementByType(pInput, ImpactAbstractElement.class).isAbstractionElement();
                }
            }));

    assert pPath.getLast().getFirst() == result.get(result.size()-1);
    return result;
  }

  @Override
  protected List<Formula> getFormulasForPath(List<ARTElement> pPath, ARTElement pInitialElement) {

    return transform(pPath,
        new Function<ARTElement, Formula>() {
          @Override
          public Formula apply(ARTElement e) {
            return extractElementByType(e, ImpactAbstractElement.AbstractionElement.class).getBlockFormula().getFormula();
          }
        });
  }

  @Override
  protected void performRefinement(ARTReachedSet pReached, List<ARTElement> path,
      CounterexampleTraceInfo<Formula> cex, boolean pRepeatedCounterexample) throws CPAException {

    ReachedSet reached = pReached.asReachedSet();
    ARTElement lastElement = path.get(path.size()-1);
    assert lastElement.isTarget();

    path = path.subList(0, path.size()-1); // skip last element, itp is always false there
    assert cex.getPredicatesForRefinement().size() ==  path.size();

    List<ARTElement> changedElements = new ArrayList<ARTElement>();
    ARTElement infeasiblePartOfART = lastElement;

    for (Pair<Formula, ARTElement> interpolationPoint : Pair.zipList(cex.getPredicatesForRefinement(), path)) {
      Formula itp = interpolationPoint.getFirst();
      ARTElement w = interpolationPoint.getSecond();

      if (itp.isTrue()) {
        // do nothing
        continue;
      }

      if (itp.isFalse()) {
        // we have reached the part of the path that is infeasible
        infeasiblePartOfART = w;
        break;
      }

      Formula stateFormula = getStateFormula(w);

      stats.itpCheck.start();
      boolean isNewItp = !solver.implies(stateFormula, itp);
      stats.itpCheck.stop();

      if (isNewItp) {
        stats.newItpWasAdded++;
        addFormulaToState(itp, w);
        changedElements.add(w);
      }
    }


    stats.artUpdate.start();
    for (ARTElement w : changedElements) {
      removeCoverageOf(w, reached);
    }

    Set<ARTElement> infeasibleSubtree = infeasiblePartOfART.getSubtree();
    assert infeasibleSubtree.contains(lastElement);

    uncover(infeasibleSubtree, reached);

    for (ARTElement removedNode : infeasibleSubtree) {
      removedNode.removeFromART();
    }
    reached.removeAll(infeasibleSubtree);
    stats.artUpdate.stop();

    // optimization: instead of closing all ancestors of v,
    // close only those that were strengthened during refine
    stats.coverTime.start();
    try {
      for (ARTElement w : changedElements) {
        if (cover(w, reached)) {
          break; // all further elements are covered anyway
        }
      }
    } finally {
      stats.coverTime.stop();
    }

    assert !reached.contains(lastElement);
  }


  private boolean cover(ARTElement v, ReachedSet reached) throws CPAException {
    assert v.mayCover();

    getArtCpa().getStopOperator().stop(v, reached.getReached(v), reached.getPrecision(v));
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

  protected Formula getStateFormula(ARTElement pARTElement) {
    return AbstractElements.extractElementByType(pARTElement, ImpactAbstractElement.AbstractionElement.class).getStateFormula();
  }

  protected void addFormulaToState(Formula f, ARTElement pARTElement) {
    ImpactAbstractElement.AbstractionElement e = AbstractElements.extractElementByType(pARTElement, ImpactAbstractElement.AbstractionElement.class);

    e.setStateFormula(fmgr.makeAnd(f, e.getStateFormula()));
  }


  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(stats);
  }
}