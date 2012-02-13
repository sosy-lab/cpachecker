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
package org.sosy_lab.cpachecker.mcmillan;

import static org.sosy_lab.cpachecker.util.AbstractElements.extractLocation;
import static org.sosy_lab.cpachecker.util.CFAUtils.leavingEdges;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.CachingPathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.ExtendedFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.TheoremProver;
import org.sosy_lab.cpachecker.util.predicates.interpolation.CounterexampleTraceInfo;
import org.sosy_lab.cpachecker.util.predicates.interpolation.InterpolationManager;
import org.sosy_lab.cpachecker.util.predicates.interpolation.UninstantiatingInterpolationManager;
import org.sosy_lab.cpachecker.util.predicates.mathsat.MathsatFactory;
import org.sosy_lab.cpachecker.util.predicates.mathsat.MathsatFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.mathsat.MathsatTheoremProver;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class McMillanAlgorithm implements Algorithm, StatisticsProvider {

  private final LogManager logger;

  private final ConfigurableProgramAnalysis cpa;

  private final ExtendedFormulaManager fmgr;
  private final PathFormulaManager pfmgr;
  private final TheoremProver prover;
  private final InterpolationManager<Formula> imgr;

  private final Map<Formula, Boolean> implicationCache = Maps.newHashMap();


  private final Timer expandTime = new Timer();
  private final Timer forceCoverTime = new Timer();
  private final Timer refinementTime = new Timer();
  private final Timer coverTime = new Timer();
  private final Timer closeTime = new Timer();
  private final Timer solverTime = new Timer();
  private int implicationChecks = 0;
  private int trivialImplicationChecks = 0;
  private int cachedImplicationChecks = 0;
  private int successfulForcedCovering = 0;

  private class Stats implements Statistics {

    @Override
    public String getName() {
      return "McMillan's algorithm";
    }

    @Override
    public void printStatistics(PrintStream out, Result pResult, ReachedSet pReached) {
      out.println("Time for expand:                    " + expandTime);
      out.println("  Time for forced covering:         " + forceCoverTime);
      out.println("Time for refinement:                " + refinementTime);
      out.println("Time for close:                     " + closeTime);
      out.println("  Time for cover:                   " + coverTime);
      out.println("Time spent by solver for reasoning: " + solverTime);
      out.println();
      out.println("Number of implication checks:       " + implicationChecks);
      out.println("  trivial:                          " + trivialImplicationChecks);
      out.println("  cached:                           " + cachedImplicationChecks);
      out.println("Number of refinements:              " + refinementTime.getNumberOfIntervals());
      out.println("Number of forced coverings:         " + forceCoverTime.getNumberOfIntervals());
      out.println("  Successful:                       " + successfulForcedCovering);
    }
  }


  public McMillanAlgorithm(Configuration config, LogManager pLogger, ConfigurableProgramAnalysis pCpa) throws InvalidConfigurationException, CPAException {
    logger = pLogger;
    cpa = pCpa;

    MathsatFormulaManager mfmgr = MathsatFactory.createFormulaManager(config, logger);
    fmgr = new ExtendedFormulaManager(mfmgr, config, logger);
    pfmgr = new CachingPathFormulaManager(new PathFormulaManagerImpl(fmgr, config, logger));
    prover = new MathsatTheoremProver(mfmgr);
    imgr = new UninstantiatingInterpolationManager(fmgr, pfmgr, prover, config, logger);

    prover.init();
  }

  public AbstractElement getInitialElement(CFANode location) {
    return new Vertex(fmgr.makeTrue(), cpa.getInitialElement(location));
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

      AbstractElement predecessor = v.getWrappedElement();
      Precision precision = reached.getPrecision(v);

      CFANode loc = extractLocation(v);
      for (CFAEdge edge : leavingEdges(loc)) {

        Collection<? extends AbstractElement> successors = cpa.getTransferRelation().getAbstractSuccessors(predecessor, precision, edge);
        if (successors.isEmpty()) {
          // edge not feasible
          // create fake vertex
          new Vertex(v, fmgr.makeFalse(), edge, null);
          continue;
        }
        assert successors.size() == 1;

        Vertex w = new Vertex(v, fmgr.makeTrue(), edge, Iterables.getOnlyElement(successors));
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

      CounterexampleTraceInfo<Formula> cex = imgr.buildCounterexampleTrace(pathFormulas, Collections.<ARTElement>emptySet());

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
        if (!implies(stateFormula, itp)) {
          w.setStateFormula(fmgr.makeAnd(stateFormula, itp)); // automatically uncovers nodes as needed
          changedElements.add(w);
        }
      }

      // itp of last element is always false, set it
      if (!v.getStateFormula().isFalse()) {
        v.setStateFormula(fmgr.makeFalse());
        changedElements.add(v);
      }

      return changedElements;
    } finally {
      refinementTime.stop();
    }
  }

  private void cover(Vertex v, Vertex w, Precision prec) throws CPAException {
    coverTime.start();

    if (   !v.isCovered()
        && !w.isCovered() // ???
        && !v.isAncestorOf(w)) {

      if (cpa.getStopOperator().stop(v.getWrappedElement(), Collections.singleton(w.getWrappedElement()), prec)) {

        if (implies(v.getStateFormula(), w.getStateFormula())) {
          for (Vertex y : v.getSubtree()) {
            y.cleanCoverage();
          }
          v.setCoveredBy(w);
        }
      }
    }
    coverTime.stop();
  }

  /**
   * Preconditions:
   * v is not covered, w is not covered
   * v is a leaf
   * v is not coverable by w in its current state
   *
   * @param v
   * @param w
   * @return
   * @throws CPAException
   * @throws InterruptedException
   */
  private boolean forceCover(Vertex v, Vertex w, Precision prec) throws CPAException, InterruptedException {
    if (!cpa.getStopOperator().stop(v.getWrappedElement(), Collections.singleton(w.getWrappedElement()), prec)) {
      // w is not a potential candidate, it would never cover v due to the other elements' contents
      return false;
    }

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
      formulas.add(fmgr.instantiate(x.getStateFormula(), pf.getSsa()));

      for (Vertex w1 : path) {
        pf = pfmgr.makeAnd(pf, w1.getIncomingEdge());
        formulas.add(pf.getFormula());
        pf = pfmgr.makeEmptyPathFormula(pf); // reset formula, keep SSAMap
      }

      formulas.add(fmgr.makeNot(fmgr.instantiate(w.getStateFormula(), pf.getSsa())));
    }

    path.add(0, x); // now path is [x; v] (including x and v)
    assert formulas.size() == path.size() + 1;

    CounterexampleTraceInfo<Formula> interpolantInfo = imgr.buildCounterexampleTrace(formulas, Collections.<ARTElement>emptySet());

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
      if (!implies(stateFormula, itp)) {
        p.setStateFormula(fmgr.makeAnd(stateFormula, itp)); // automatically uncovers nodes as needed
        changedElements.add(p);
      }
    }

    assert !changedElements.contains(x);
    assert changedElements.contains(v);

    assert implies(v.getStateFormula(), w.getStateFormula());
    cover(v, w, prec);
    assert v.isCovered();

    return true;
  }

  private void close(Vertex v, ReachedSet reached) throws CPAException {
    if (!v.isCovered()) {
      closeTime.start();

      Precision prec = reached.getPrecision(v);
      for (AbstractElement ae : reached.getReached(v)) {
        Vertex w = (Vertex)ae;

        if (w.isOlderThan(v)) {
          cover(v, w, prec);
        }
      }

      closeTime.stop();
    }
  }

  private boolean dfs(Vertex v, ReachedSet reached) throws CPAException, InterruptedException {
    close(v, reached);
    if (v.isCovered()) {
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
        close(w, reached);
      }

      assert v.getStateFormula().isFalse();
      return true; // no need to expand further
    }

    if (!v.isLeaf()) {
      return true; // no need to expand
    }

    forceCoverTime.start();
    try {
      Precision prec = reached.getPrecision(v);
      for (AbstractElement ae : reached.getReached(v)) {
        Vertex w = (Vertex)ae;
        if (v != w && w.isOlderThan(v) && !w.isCovered() && forceCover(v, w, prec)) {
          assert v.isCovered();
          return true; // no need to expand
        }
      }
    } finally {
      forceCoverTime.stop();
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
      for (AbstractElement ae : reached) {
        Vertex v = (Vertex)ae;
        if (v.isLeaf() && !v.isCovered()) {

          // close parents of v
          List<Vertex> path = getPathFromRootTo(v);
          path = path.subList(0, path.size()-1); // skip v itself
          for (Vertex w : path) {
            close(w, reached);
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

  private boolean implies(Formula a, Formula b) {
    implicationChecks++;

    if (a.isFalse() || b.isTrue()) {
      trivialImplicationChecks++;
      return true;
    }
    if (a.isTrue() || b.isFalse()) {
      // "true" implies only "true", but b is not "true"
      // "false" is implied only by "false", but a is not "false"
      trivialImplicationChecks++;
      return false;
    }
    if (a.equals(b)) {
      trivialImplicationChecks++;
      return true;
    }

    Formula f = fmgr.makeNot(fmgr.makeImplication(a, b));

    Boolean result = implicationCache.get(f);
    if (result != null) {
      cachedImplicationChecks++;
      return result;
    }

    solverTime.start();
    try {
      result = prover.isUnsat(f);
    } finally {
      solverTime.stop();
    }
    implicationCache.put(f, result);
    return result;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(new Stats());
  }
}
