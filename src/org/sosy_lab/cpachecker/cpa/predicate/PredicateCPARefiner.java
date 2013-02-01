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

import javax.annotation.Nullable;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interpolation.AbstractInterpolationBasedRefiner;
import org.sosy_lab.cpachecker.util.predicates.interpolation.CounterexampleTraceInfo;
import org.sosy_lab.cpachecker.util.predicates.interpolation.InterpolationManager;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

@Options(prefix="cpa.predicate.refinement")
public class PredicateCPARefiner extends AbstractInterpolationBasedRefiner implements StatisticsProvider {

  @Option(description="slice block formulas, experimental feature!")
  private boolean sliceBlockFormulas = false;

  private final PathFormulaManager pfmgr;

  private final RefinementStrategy strategy;

  protected PredicateCPARefiner(final Configuration config, final LogManager logger,
      final ConfigurableProgramAnalysis pCpa,
      final InterpolationManager pInterpolationManager,
      final PathFormulaManager pPathFormulaManager,
      final RefinementStrategy pStrategy) throws CPAException, InvalidConfigurationException {

    super(config, logger, pCpa, pInterpolationManager);

    config.inject(this, PredicateCPARefiner.class);

    pfmgr = pPathFormulaManager;
    strategy = pStrategy;
  }

  @Override
  protected final List<ARGState> transformPath(ARGPath pPath) {
    List<ARGState> result = from(pPath)
      .skip(1)
      .transform(Pair.<ARGState>getProjectionToFirst())
      .filter(Predicates.compose(PredicateAbstractState.FILTER_ABSTRACTION_STATES,
                                 toState(PredicateAbstractState.class)))
      .toImmutableList();

    assert from(result).allMatch(new Predicate<ARGState>() {
      @Override
      public boolean apply(@Nullable ARGState pInput) {
        boolean correct = pInput.getParents().size() <= 1;
        assert correct : "PredicateCPARefiner expects abstraction states to have only one parent, but this state has more:" + pInput;
        return correct;
      }
    });

    assert pPath.getLast().getFirst() == result.get(result.size()-1);
    return result;
  }

  private static final Function<PredicateAbstractState, BooleanFormula> GET_BLOCK_FORMULA
                = new Function<PredicateAbstractState, BooleanFormula>() {
                    @Override
                    public BooleanFormula apply(PredicateAbstractState e) {
                      assert e.isAbstractionState();
                      return e.getAbstractionFormula().getBlockFormula();
                    }
                  };

  @Override
  protected List<BooleanFormula> getFormulasForPath(List<ARGState> path, ARGState initialState)
      throws CPATransferException {
    if (sliceBlockFormulas) {
      BlockFormulaSlicer bfs = new BlockFormulaSlicer(pfmgr);
      return bfs.sliceFormulasForPath(path, initialState);
    } else {
      return from(path)
          .transform(toState(PredicateAbstractState.class))
          .transform(GET_BLOCK_FORMULA)
          .toImmutableList();
    }
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
        PredicateCPARefiner.this.printStatistics(pOut, pResult, pReached);
        statistics.printStatistics(pOut, pResult, pReached);
      }

      @Override
      public String getName() {
        return strategy.getStatistics().getName();
      }
    });
  }
}
