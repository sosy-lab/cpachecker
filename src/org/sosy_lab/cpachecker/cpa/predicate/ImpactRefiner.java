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
import static org.sosy_lab.cpachecker.util.AbstractStates.toState;

import java.io.PrintStream;
import java.util.Collection;
import java.util.List;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.predicates.Solver;
import org.sosy_lab.cpachecker.util.predicates.SymbolicRegionManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Region;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interpolation.AbstractInterpolationBasedRefiner;
import org.sosy_lab.cpachecker.util.predicates.interpolation.CounterexampleTraceInfo;
import org.sosy_lab.cpachecker.util.predicates.interpolation.InterpolationManager;

import com.google.common.base.Function;
import com.google.common.base.Predicates;

public class ImpactRefiner extends AbstractInterpolationBasedRefiner<BooleanFormula> implements StatisticsProvider {

  private final RefinementStrategy strategy;

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
    FormulaManagerView fmgr = predicateCpa.getFormulaManager();
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
      final FormulaManagerView pFmgr, final Solver pSolver) throws InvalidConfigurationException, CPAException {

    super(config, logger, pCpa, pInterpolationManager);

    strategy = new ImpactRefinementStrategy(config, logger, pFmgr, pSolver);
  }


  @Override
  protected List<ARGState> transformPath(ARGPath pPath) {
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
  protected List<BooleanFormula> getFormulasForPath(List<ARGState> pPath, ARGState pInitialState) {
    return from(pPath)
            .transform(toState(PredicateAbstractState.class))
            .transform(new Function<PredicateAbstractState, BooleanFormula>() {
                @Override
                public BooleanFormula apply(PredicateAbstractState s) {
                  return s.getAbstractionFormula().getBlockFormula();
                }
              })
            .toImmutableList();
  }

  @Override
  protected void performRefinement(ARGReachedSet pReached, List<ARGState> pPath,
      CounterexampleTraceInfo<BooleanFormula> pCounterexample, boolean pRepeatedCounterexample) throws CPAException {
    strategy.performRefinement(pReached, pPath, pCounterexample, pRepeatedCounterexample);
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(new Statistics() {

      private final Statistics statistics = strategy.getStatistics();

      @Override
      public void printStatistics(PrintStream pOut, Result pResult, ReachedSet pReached) {
        ImpactRefiner.this.printStatistics(pOut, pResult, pReached);
        statistics.printStatistics(pOut, pResult, pReached);
      }

      @Override
      public String getName() {
        return strategy.getStatistics().getName();
      }
    });
  }
}