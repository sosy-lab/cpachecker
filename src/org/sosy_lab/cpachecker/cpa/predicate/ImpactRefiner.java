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
package org.sosy_lab.cpachecker.cpa.predicate;

import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.cpa.predicate.ImpactUtils.*;
import static org.sosy_lab.cpachecker.util.AbstractStates.toState;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.Path;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.predicates.ExtendedFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.Solver;
import org.sosy_lab.cpachecker.util.predicates.SymbolicRegionManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Region;
import org.sosy_lab.cpachecker.util.predicates.interpolation.AbstractInterpolationBasedRefiner;
import org.sosy_lab.cpachecker.util.predicates.interpolation.CounterexampleTraceInfo;
import org.sosy_lab.cpachecker.util.predicates.interpolation.InterpolationManager;

import com.google.common.base.Function;
import com.google.common.base.Predicates;

public class ImpactRefiner extends AbstractInterpolationBasedRefiner<Formula> implements StatisticsProvider {

  private class Stats implements Statistics {

    private final Timer itpCheck  = new Timer();
    private final Timer coverTime = new Timer();
    private final Timer argUpdate = new Timer();

    @Override
    public String getName() {
      return "Impact Refiner";
    }

    @Override
    public void printStatistics(PrintStream out, Result pResult, ReachedSet pReached) {
      ImpactRefiner.this.printStatistics(out, pResult, pReached);
      out.println("  Checking whether itp is new:        " + itpCheck);
      out.println("  Coverage checks:                    " + coverTime);
      out.println("  ARG update:                         " + argUpdate);
    }
  }

  protected final ExtendedFormulaManager fmgr;
  protected final Solver solver;

  private final Stats stats = new Stats();

  public static ImpactRefiner create(ConfigurableProgramAnalysis pCpa) throws CPAException, InvalidConfigurationException {
    PredicateCPA predicateCpa = CPAs.retrieveCPA(pCpa, PredicateCPA.class);
    if (predicateCpa == null) {
      throw new InvalidConfigurationException(ImpactRefiner.class.getSimpleName() + " needs a PredicateCPA");
    }

    Region initialRegion = predicateCpa.getInitialState(null).getAbstractionFormula().asRegion();
    if (!(initialRegion instanceof SymbolicRegionManager.SymbolicRegion)) {
      throw new InvalidConfigurationException(ImpactRefiner.class.getSimpleName() + " works only with a PredicateCPA configured to store abstractions as formulas (cpa.predicate.abstraction.type=FORMULA)");
    }

    Configuration config = predicateCpa.getConfiguration();
    LogManager logger = predicateCpa.getLogger();
    ExtendedFormulaManager fmgr = predicateCpa.getFormulaManager();
    Solver solver = predicateCpa.getSolver();

    InterpolationManager manager = new InterpolationManager(
                                                  fmgr,
                                                  predicateCpa.getPathFormulaManager(),
                                                  solver,
                                                  predicateCpa.getFormulaManagerFactory(),
                                                  config, logger);

    return new ImpactRefiner(config, logger, pCpa, manager, fmgr, solver);
  }


  protected ImpactRefiner(final Configuration config, final LogManager logger,
      final ConfigurableProgramAnalysis pCpa,
      final InterpolationManager pInterpolationManager,
      final ExtendedFormulaManager pFmgr, final Solver pSolver) throws InvalidConfigurationException, CPAException {

    super(config, logger, pCpa, pInterpolationManager);

    solver = pSolver;
    fmgr = pFmgr;
  }


  @Override
  protected List<ARGState> transformPath(Path pPath) {
    // filter abstraction states

    List<ARGState> result = from(pPath)
                               .skip(1)
                               .transform(Pair.<ARGState>getProjectionToFirst())
                               .filter(Predicates.compose(PredicateAbstractState.FILTER_ABSTRACTION_STATES,
                                                          toState(PredicateAbstractState.class)))
                               .toImmutableList();

    assert pPath.getLast().getFirst() == result.get(result.size()-1);
    return result;
  }

  @Override
  protected List<Formula> getFormulasForPath(List<ARGState> pPath, ARGState pInitialState) {
    return from(pPath)
            .transform(toState(PredicateAbstractState.class))
            .transform(new Function<PredicateAbstractState, Formula>() {
                @Override
                public Formula apply(PredicateAbstractState s) {
                  return s.getAbstractionFormula().getBlockFormula();
                }
              })
            .toImmutableList();
  }

  @Override
  protected void performRefinement(ARGReachedSet pReached, List<ARGState> path,
      CounterexampleTraceInfo<Formula> cex, boolean pRepeatedCounterexample) throws CPAException {

    ReachedSet reached = pReached.asReachedSet();
    ARGState lastElement = path.get(path.size()-1);
    assert lastElement.isTarget();

    path = path.subList(0, path.size()-1); // skip last element, itp is always false there
    assert cex.getInterpolants().size() ==  path.size();

    List<ARGState> changedElements = new ArrayList<ARGState>();
    ARGState infeasiblePartOfART = lastElement;

    for (Pair<Formula, ARGState> interpolationPoint : Pair.zipList(cex.getInterpolants(), path)) {
      Formula itp = interpolationPoint.getFirst();
      ARGState w = interpolationPoint.getSecond();

      if (itp.isTrue()) {
        // do nothing
        totalUnchangedPrefixLength++;
        continue;
      }

      if (itp.isFalse()) {
        // we have reached the part of the path that is infeasible
        infeasiblePartOfART = w;
        break;
      }
      totalNumberOfStatesWithNonTrivialInterpolant++;

      itp = fmgr.uninstantiate(itp);
      Formula stateFormula = getStateFormula(w);

      stats.itpCheck.start();
      boolean isNewItp = !solver.implies(stateFormula, itp);
      stats.itpCheck.stop();

      if (isNewItp) {
        addFormulaToState(itp, w, fmgr);
        changedElements.add(w);
      }
    }
    totalNumberOfAffectedStates += changedElements.size();

    if (changedElements.isEmpty() && pRepeatedCounterexample) {
      // TODO One cause for this exception is that the CPAAlgorithm sometimes
      // re-adds the parent of the error element to the waitlist, and thus the
      // error element would get re-discovered immediately again.
      // Currently the CPAAlgorithm does this only when there are siblings of
      // the target state, which should rarely happen.
      // We still need a better handling for this situation.
      throw new RefinementFailedException(RefinementFailedException.Reason.RepeatedCounterexample, null);
    }

    stats.argUpdate.start();
    for (ARGState w : changedElements) {
      pReached.removeCoverageOf(w);
    }

    removeInfeasiblePartofARG(infeasiblePartOfART, pReached);
    stats.argUpdate.stop();

    // optimization: instead of closing all ancestors of v,
    // close only those that were strengthened during refine
    stats.coverTime.start();
    try {
      for (ARGState w : changedElements) {
        if (cover(w, pReached, getArtCpa())) {
          break; // all further elements are covered anyway
        }
      }
    } finally {
      stats.coverTime.stop();
    }

    assert !reached.contains(lastElement);
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(stats);
  }
}