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
package org.sosy_lab.cpachecker.core.algorithm.impact;

import static org.sosy_lab.cpachecker.util.AbstractStates.extractLocation;
import static org.sosy_lab.cpachecker.util.CFAUtils.leavingEdges;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.Timer;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.CachingPathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.ExtendedFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.FormulaManagerFactory;
import org.sosy_lab.cpachecker.util.predicates.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.Solver;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.TheoremProver;
import org.sosy_lab.cpachecker.util.predicates.interpolation.CounterexampleTraceInfo;
import org.sosy_lab.cpachecker.util.predicates.interpolation.InterpolationManager;
import org.sosy_lab.cpachecker.util.predicates.interpolation.UninstantiatingInterpolationManager;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

/**
 * This is an implementation of McMillan's algorithm which was presented in the
 * paper "Lazy Abstraction with Interpolants" and implemented in the tool IMPACT.
 */
@Options(prefix="impact")
public class ImpactAlgorithm implements Algorithm, StatisticsProvider {

  private final LogManager logger;

  private final ConfigurableProgramAnalysis cpa;

  private final ExtendedFormulaManager fmgr;
  private final PathFormulaManager pfmgr;
  private final TheoremProver prover;
  private final Solver solver;
  private final InterpolationManager<Formula> imgr;

  private final Timer expandTime = new Timer();
  private final Timer forceCoverTime = new Timer();
  private final Timer refinementTime = new Timer();
  private final Timer coverTime = new Timer();
  private final Timer closeTime = new Timer();
  private int successfulForcedCovering = 0;

  private class Stats implements Statistics {

    @Override
    public String getName() {
      return "Impact Algorithm";
    }

    @Override
    public void printStatistics(PrintStream out, Result pResult, ReachedSet pReached) {
      out.println("Time for expand:                    " + expandTime);
      if (useForcedCovering) {
        out.println("  Time for forced covering:         " + forceCoverTime);
      }
      out.println("Time for refinement:                " + refinementTime);
      out.println("Time for close:                     " + closeTime);
      out.println("  Time for cover:                   " + coverTime);
      out.println("Time spent by solver for reasoning: " + solver.solverTime);
      out.println();
      out.println("Number of implication checks:       " + solver.implicationChecks);
      out.println("  trivial:                          " + solver.trivialImplicationChecks);
      out.println("  cached:                           " + solver.cachedImplicationChecks);
      out.println("Number of refinements:              " + refinementTime.getNumberOfIntervals());
      if (useForcedCovering) {
        out.println("Number of forced coverings:         " + forceCoverTime.getNumberOfIntervals());
        out.println("  Successful:                       " + successfulForcedCovering);
      }
    }
  }

  @Option(description="enable the Forced Covering optimization")
  private boolean useForcedCovering = true;


  public ImpactAlgorithm(Configuration config, LogManager pLogger, ConfigurableProgramAnalysis pCpa) throws InvalidConfigurationException, CPAException {
    config.inject(this);
    logger = pLogger;
    cpa = pCpa;

    FormulaManagerFactory factory = new FormulaManagerFactory(config, pLogger);
    fmgr = new ExtendedFormulaManager(factory.getFormulaManager(), config, logger);
    pfmgr = new CachingPathFormulaManager(new PathFormulaManagerImpl(fmgr, config, logger));
    prover = factory.createTheoremProver();
    solver = new Solver(fmgr, prover);
    imgr = new UninstantiatingInterpolationManager(fmgr, pfmgr, solver, factory, config, logger);
  }

  public AbstractState getInitialState(CFANode location) {
    return new Vertex(fmgr.makeTrue(), cpa.getInitialState(location));
  }

  public Precision getInitialPrecision(CFANode location) {
    return cpa.getInitialPrecision(location);
  }

  @Override
  public boolean run(ReachedSet pReachedSet) throws CPAException, InterruptedException {
    unwind(pReachedSet);
    pReachedSet.popFromWaitlist();
    return true;
  }

  private void expand(Vertex v, ReachedSet reached) throws CPAException, InterruptedException {
    expandTime.start();
    try {
      assert v.isLeaf() && !v.isCovered();

      AbstractState predecessor = v.getWrappedState();
      Precision precision = reached.getPrecision(v);

      CFANode loc = extractLocation(v);
      for (CFAEdge edge : leavingEdges(loc)) {

        Collection<? extends AbstractState> successors = cpa.getTransferRelation().getAbstractSuccessors(predecessor, precision, edge);
        if (successors.isEmpty()) {
          // edge not feasible
          // create fake vertex
          new Vertex(v, fmgr.makeFalse(), null);
          continue;
        }
        assert successors.size() == 1;

        Vertex w = new Vertex(v, fmgr.makeTrue(), Iterables.getOnlyElement(successors));
        reached.add(w, precision);
        reached.popFromWaitlist(); // we don't use the waitlist
      }
    } finally {
      expandTime.stop();
    }
  }

  private List<Vertex> refine(final Vertex v) throws CPAException, InterruptedException {
    refinementTime.start();
    try {
      assert (v.isTarget() && !v.getStateFormula().isFalse());

      logger.log(Level.FINER, "Refinement on " + v);

      // build list of path elements in bottom-to-top order and reverse
      List<Vertex> path = getPathFromRootTo(v);
      path = path.subList(1, path.size()); // skip root element, it has no formula

      // build list of formulas for edges
      List<Formula> pathFormulas = new ArrayList<Formula>(path.size());
      addPathFormulasToList(path, pathFormulas);

      CounterexampleTraceInfo<Formula> cex = imgr.buildCounterexampleTrace(pathFormulas, Collections.<ARGState>emptySet());

      if (!cex.isSpurious()) {
        return Collections.emptyList(); // real counterexample
      }

      logger.log(Level.FINER, "Refinement successful");

      path = path.subList(0, path.size()-1); // skip last element, itp is always false there
      assert cex.getPredicatesForRefinement().size() ==  path.size();

      List<Vertex> changedElements = new ArrayList<Vertex>();

      for (Pair<Formula, Vertex> interpolationPoint : Pair.zipList(cex.getPredicatesForRefinement(), path)) {
        Formula itp = interpolationPoint.getFirst();
        Vertex w = interpolationPoint.getSecond();

        if (itp.isTrue()) {
          continue;
        }

        Formula stateFormula = w.getStateFormula();
        if (!solver.implies(stateFormula, itp)) {
          w.setStateFormula(fmgr.makeAnd(stateFormula, itp));
          w.cleanCoverage();
          changedElements.add(w);
        }
      }

      // itp of last element is always false, set it
      if (!v.getStateFormula().isFalse()) {
        v.setStateFormula(fmgr.makeFalse());
        v.cleanCoverage();
        changedElements.add(v);
      }

      return changedElements;
    } finally {
      refinementTime.stop();
    }
  }

  /**
   * Check if a vertex v may potentially be covered by another vertex w.
   * It checks everything except their state formulas.
   */
  private boolean mayCover(Vertex v, Vertex w, Precision prec) throws CPAException {
    return (v != w)
        && !w.isCovered() // ???
        && w.isOlderThan(v)
        && !v.isAncestorOf(w)
        && cpa.getStopOperator().stop(v.getWrappedState(), Collections.singleton(w.getWrappedState()), prec);
  }

  private boolean cover(Vertex v, Vertex w, Precision prec) throws CPAException {
    coverTime.start();
    try {
      assert !v.isCovered();

      if (mayCover(v, w, prec)
          && solver.implies(v.getStateFormula(), w.getStateFormula())) {

        for (Vertex y : v.getSubtree()) {
          y.cleanCoverage();
        }
        v.setCoveredBy(w);

        return true;
      }
      return false;

    } finally {
      coverTime.stop();
    }
  }

  /**
   * Preconditions:
   * v may be covered by w ({@link #mayCover(Vertex, Vertex, Precision)}).
   * v is not coverable by w in its current state.
   *
   * @param v
   * @param w
   * @return
   * @throws CPAException
   * @throws InterruptedException
   */
  private boolean forceCover(Vertex v, Vertex w, Precision prec) throws CPAException, InterruptedException {
    List<Vertex> path = new ArrayList<Vertex>();
    Vertex x = v;
    {
      Set<Vertex> parentsOfW = new HashSet<Vertex>(getPathFromRootTo(w));

      while (!parentsOfW.contains(x)) {
        path.add(x);

        assert x.hasParent();
        x = x.getParent();
      }
    }
    path = Lists.reverse(path);

    // x is common ancestor
    // path is ]x; v] (path from x to v, excluding x, including v)

    List<Formula> formulas = new ArrayList<Formula>(path.size()+2);
    {
      PathFormula pf = pfmgr.makeEmptyPathFormula();
      formulas.add(fmgr.instantiate(x.getStateFormula(), SSAMap.emptySSAMap().withDefault(1)));

      for (Vertex w1 : path) {
        pf = pfmgr.makeAnd(pf, w1.getIncomingEdge());
        formulas.add(pf.getFormula());
        pf = pfmgr.makeEmptyPathFormula(pf); // reset formula, keep SSAMap
      }

      formulas.add(fmgr.makeNot(fmgr.instantiate(w.getStateFormula(), pf.getSsa().withDefault(1))));
    }

    path.add(0, x); // now path is [x; v] (including x and v)
    assert formulas.size() == path.size() + 1;

    CounterexampleTraceInfo<Formula> interpolantInfo = imgr.buildCounterexampleTrace(formulas, Collections.<ARGState>emptySet());

    if (!interpolantInfo.isSpurious()) {
      logger.log(Level.FINER, "Forced covering unsuccessful.");
      return false; // forced covering not possible
    }

    successfulForcedCovering++;
    logger.log(Level.FINER, "Forced covering successful.");


    List<Formula> interpolants = interpolantInfo.getPredicatesForRefinement();
    assert interpolants.size() == formulas.size() - 1;
    assert interpolants.size() ==  path.size();

    List<Vertex> changedElements = new ArrayList<Vertex>();

    for (Pair<Formula, Vertex> interpolationPoint : Pair.zipList(interpolants, path)) {
      Formula itp = interpolationPoint.getFirst();
      Vertex p = interpolationPoint.getSecond();

      if (itp.isTrue()) {
        continue;
      }

      Formula stateFormula = p.getStateFormula();
      if (!solver.implies(stateFormula, itp)) {
        p.setStateFormula(fmgr.makeAnd(stateFormula, itp));
        p.cleanCoverage();
        changedElements.add(p);
      }
    }

    boolean covered = cover(v, w, prec);
    assert covered;

    return true;
  }

  private boolean close(Vertex v, ReachedSet reached) throws CPAException {
    closeTime.start();
    try {
      if (v.isCovered()) {
        return true;
      }

      Precision prec = reached.getPrecision(v);
      for (AbstractState ae : reached.getReached(v)) {
        Vertex w = (Vertex)ae;

        if (cover(v, w, prec)) {
          return true; // v is now covered
        }
      }

      return false;

    } finally {
      closeTime.stop();
    }
  }

  private boolean dfs(Vertex v, ReachedSet reached) throws CPAException, InterruptedException {
    if (close(v, reached)) {
      return true; // no need to expand
    }

    if (v.isTarget()) {
      List<Vertex> changedElements = refine(v);
      if (changedElements.isEmpty()) {
        return false; // real counterexample
      }

      // optimization: instead of closing all ancestors of v,
      // close only those that were strengthened during refine
      for (Vertex w : changedElements) {
        if (close(w, reached)) {
          break; // all further elements are covered anyway
        }
      }

      assert v.getStateFormula().isFalse();
      return true; // no need to expand further
    }

    if (!v.isLeaf()) {
      return true; // no need to expand
    }

    if (useForcedCovering) {
      forceCoverTime.start();
      try {
        Precision prec = reached.getPrecision(v);
        for (AbstractState ae : reached.getReached(v)) {
          Vertex w = (Vertex)ae;
          if (mayCover(v, w, prec)) {
            if (forceCover(v, w, prec)) {
              assert v.isCovered();
              return true; // no need to expand
            }
          }
        }
      } finally {
        forceCoverTime.stop();
      }
    }

    expand(v, reached);
    for (Vertex w : v.getChildren()) {
      if (!w.getStateFormula().isFalse()) {
        dfs(w, reached);
      }
    }

    return true;
  }

  private void unwind(ReachedSet reached) throws CPAException, InterruptedException {

    outer:
    while (true) {
      for (AbstractState ae : reached) {
        Vertex v = (Vertex)ae;
        if (v.isLeaf() && !v.isCovered()) {

          // close parents of v
          List<Vertex> path = getPathFromRootTo(v);
          path = path.subList(0, path.size()-1); // skip v itself
          for (Vertex w : path) {
            if (close(w, reached)) {
              continue outer; // v is now covered
            }
          }

          if (!dfs(v, reached)) {
            logger.log(Level.INFO, "Bug found");
            break outer;
          }

          continue outer;
        }
      }
      break outer;
    }
  }

  private void addPathFormulasToList(List<Vertex> path, List<Formula> pathFormulas) throws CPATransferException {
    PathFormula pf = pfmgr.makeEmptyPathFormula();
    for (Vertex w : path) {
      pf = pfmgr.makeAnd(pf, w.getIncomingEdge());
      pathFormulas.add(pf.getFormula());
      pf = pfmgr.makeEmptyPathFormula(pf); // reset formula, keep SSAMap
    }
  }

  private List<Vertex> getPathFromRootTo(Vertex v) {
    List<Vertex> path = new ArrayList<Vertex>();

    Vertex w = v;
    while (w.hasParent()) {
      path.add(w);
      w = w.getParent();
    }
    path.add(w); // root element

    return Lists.reverse(path);
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(new Stats());
  }

  @Override
  public boolean reset() {
    return false;
  }
}
