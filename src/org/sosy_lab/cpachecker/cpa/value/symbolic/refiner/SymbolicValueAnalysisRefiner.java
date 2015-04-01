/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.value.symbolic.refiner;

import java.util.Collection;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisCPA;
import org.sosy_lab.cpachecker.cpa.value.refiner.ValueAnalysisRefiner;
import org.sosy_lab.cpachecker.cpa.value.refiner.utils.ValueAnalysisFeasibilityChecker;
import org.sosy_lab.cpachecker.cpa.value.symbolic.refiner.interpolant.SymbolicInterpolant;
import org.sosy_lab.cpachecker.cpa.value.symbolic.refiner.interpolant.SymbolicInterpolantManager;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.refiner.GenericRefiner;

/**
 * Refiner for value analysis using symbolic values.
 */
@Options(prefix = "cpa.value.refinement")
public class SymbolicValueAnalysisRefiner
    extends GenericRefiner<ForgettingCompositeState, ForgettingCompositeState.MemoryLocationAssociation, SymbolicInterpolant> {

  private final SymbolicFeasibilityChecker symbolicChecker;
  private final ValueAnalysisFeasibilityChecker explicitOnlyChecker;

  private final SymbolicStrongestPostOperator strongestPostOp;

  private final ValueAnalysisRefiner explicitOnlyRefiner;
  //private final SymbolicInterpolator symbolicInterpolator;

  private final LogManager logger;

  public static SymbolicValueAnalysisRefiner create(final ConfigurableProgramAnalysis pCpa)
      throws InvalidConfigurationException {

    final ValueAnalysisCPA valueAnalysisCpa = CPAs.retrieveCPA(pCpa, ValueAnalysisCPA.class);
    if (valueAnalysisCpa == null) {
      throw new InvalidConfigurationException(ValueAnalysisRefiner.class.getSimpleName() + " needs a ValueAnalysisCPA");
    }

    valueAnalysisCpa.injectRefinablePrecision();

    final LogManager logger = valueAnalysisCpa.getLogger();
    final CFA cfa = valueAnalysisCpa.getCFA();
    final Configuration config = valueAnalysisCpa.getConfiguration();
    final ShutdownNotifier shutdownNotifier = valueAnalysisCpa.getShutdownNotifier();

    final SymbolicStrongestPostOperator strongestPostOperator =
        new ValueTransferBasedStrongestPostOperator(logger, config, cfa, shutdownNotifier);

    final SymbolicFeasibilityChecker feasibilityChecker =
        new SymbolicValueAnalysisFeasibilityChecker(strongestPostOperator,
                                                    config,
                                                    logger,
                                                    cfa,
                                                    shutdownNotifier);

    final  SymbolicEdgeInterpolator edgeInterpolator =
        new SymbolicEdgeInterpolator(strongestPostOperator,
                                     feasibilityChecker,
                                     SymbolicInterpolantManager.getInstance(),
                                     config, logger, shutdownNotifier, cfa);

    final SymbolicPathInterpolator pathInterpolator =
        new SymbolicPathInterpolator(edgeInterpolator,
                                     SymbolicInterpolantManager.getInstance(),
                                     feasibilityChecker,
                                     config,
                                     logger,
                                     shutdownNotifier,
                                     cfa);

    SymbolicValueAnalysisRefiner refiner = new SymbolicValueAnalysisRefiner(
        feasibilityChecker,
        pathInterpolator,
        strongestPostOperator,
        config,
        logger,
        shutdownNotifier,
        cfa,
        valueAnalysisCpa);

    return refiner;
  }

  protected SymbolicValueAnalysisRefiner(
      final SymbolicFeasibilityChecker pFeasibilityChecker,
      final SymbolicPathInterpolator pInterpolator,
      final SymbolicStrongestPostOperator pStrongestPostOperator,
      final Configuration pConfig,
      final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier,
      final CFA pCfa,
      final ValueAnalysisCPA pValueCpa
  ) throws InvalidConfigurationException {

    super(pFeasibilityChecker,
          pInterpolator,
          SymbolicInterpolantManager.getInstance(),
          ValueAnalysisCPA.class,
          pConfig,
          pLogger,
          pShutdownNotifier,
          pCfa);

    symbolicChecker =
        new SymbolicValueAnalysisFeasibilityChecker(pStrongestPostOperator,
                                                    pConfig, pLogger, pCfa,
                                                    pShutdownNotifier);

    explicitOnlyChecker =
        new ValueAnalysisFeasibilityChecker(pLogger, pCfa, pConfig);

    explicitOnlyRefiner = ValueAnalysisRefiner.create(pValueCpa);

    strongestPostOp = pStrongestPostOperator;
    logger = pLogger;
  }


  @Override
  public CounterexampleInfo performRefinement(
      final ARGReachedSet pReachedSet
  ) throws CPAException {

    try {
      // Perform refinement without symbolic values first.
      // We require a lot less resources if it is possible to identify the target states as
      // infeasible without using symbolic values (and as such, also without SAT checks).
      final CounterexampleInfo explicitOnlyCex = explicitOnlyRefiner.performRefinement(pReachedSet);

      if (!explicitOnlyCex.isSpurious()) {
        return super.performRefinement(pReachedSet);

      } else {
        return CounterexampleInfo.spurious();
      }
    } catch (InterruptedException e) {
      throw new CPAException("Error while performing refinement", e);
    }
  }

  @Override
  public void collectStatistics(final Collection<Statistics> pStatsCollection) {
    explicitOnlyRefiner.collectStatistics(pStatsCollection);
    super.collectStatistics(pStatsCollection);
  }
}
