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
package org.sosy_lab.cpachecker.cpa.value.symbolic.refiner.delegation;

import java.io.PrintStream;
import java.util.Collection;
import java.util.logging.Level;

import javax.annotation.Nullable;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Refiner;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.constraints.ConstraintsCPA;
import org.sosy_lab.cpachecker.cpa.constraints.refiner.precision.RefinableConstraintsPrecision;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisCPA;
import org.sosy_lab.cpachecker.cpa.value.symbolic.refiner.ElementTestingSymbolicEdgeInterpolator;
import org.sosy_lab.cpachecker.cpa.value.symbolic.refiner.ForgettingCompositeState;
import org.sosy_lab.cpachecker.cpa.value.symbolic.refiner.SymbolicFeasibilityChecker;
import org.sosy_lab.cpachecker.cpa.value.symbolic.refiner.SymbolicPathInterpolator;
import org.sosy_lab.cpachecker.cpa.value.symbolic.refiner.SymbolicStrongestPostOperator;
import org.sosy_lab.cpachecker.cpa.value.symbolic.refiner.SymbolicValueAnalysisFeasibilityChecker;
import org.sosy_lab.cpachecker.cpa.value.symbolic.refiner.SymbolicValueAnalysisRefiner;
import org.sosy_lab.cpachecker.cpa.value.symbolic.refiner.ValueTransferBasedStrongestPostOperator;
import org.sosy_lab.cpachecker.cpa.value.symbolic.refiner.interpolant.SymbolicInterpolant;
import org.sosy_lab.cpachecker.cpa.value.symbolic.refiner.interpolant.SymbolicInterpolantManager;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.refinement.EdgeInterpolator;
import org.sosy_lab.cpachecker.util.refinement.FeasibilityChecker;
import org.sosy_lab.cpachecker.util.refinement.GenericEdgeInterpolator;
import org.sosy_lab.cpachecker.util.refinement.GenericFeasibilityChecker;
import org.sosy_lab.cpachecker.util.refinement.GenericPathInterpolator;
import org.sosy_lab.cpachecker.util.refinement.GenericPrefixProvider;
import org.sosy_lab.cpachecker.util.refinement.PathExtractor;
import org.sosy_lab.cpachecker.util.refinement.PathInterpolator;

/**
 * Refiner for {@link ValueAnalysisCPA} using symbolic values and
 * {@link org.sosy_lab.cpachecker.cpa.constraints.ConstraintsCPA ConstraintsCPA}
 * that tries to refine precision using only the {@link ValueAnalysisCPA}, first.
 */
public class SymbolicDelegatingRefiner implements Refiner, StatisticsProvider {

  private final SymbolicValueAnalysisRefiner explicitRefiner;
  private final SymbolicValueAnalysisRefiner symbolicRefiner;

  private final LogManager logger;

  // Statistics
  private int explicitRefinements = 0;
  private int successfulExplicitRefinements = 0;
  private int symbolicRefinements = 0;
  private int successfulSymbolicRefinements = 0;
  private final Timer explicitRefinementTime = new Timer();
  private final Timer symbolicRefinementTime = new Timer();

  public static SymbolicDelegatingRefiner create(final ConfigurableProgramAnalysis pCpa)
      throws InvalidConfigurationException {

    final ARGCPA argCpa = CPAs.retrieveCPA(pCpa, ARGCPA.class);
    final ValueAnalysisCPA valueAnalysisCpa = CPAs.retrieveCPA(pCpa, ValueAnalysisCPA.class);
    final ConstraintsCPA constraintsCpa = CPAs.retrieveCPA(pCpa, ConstraintsCPA.class);

    if (argCpa == null) {
      throw new InvalidConfigurationException(SymbolicValueAnalysisRefiner.class.getSimpleName() + " needs to be wrapped in an ARGCPA");
    }

    if (valueAnalysisCpa == null) {
      throw new InvalidConfigurationException(SymbolicValueAnalysisRefiner.class.getSimpleName()
          + " needs a ValueAnalysisCPA");
    }

    if (constraintsCpa == null) {
      throw new InvalidConfigurationException(SymbolicValueAnalysisRefiner.class.getSimpleName()
          + " needs a ConstraintsCPA");
    }

    final Configuration config = valueAnalysisCpa.getConfiguration();

    valueAnalysisCpa.injectRefinablePrecision();
    constraintsCpa.injectRefinablePrecision(new RefinableConstraintsPrecision(config));

    final LogManager logger = valueAnalysisCpa.getLogger();
    final CFA cfa = valueAnalysisCpa.getCFA();
    final ShutdownNotifier shutdownNotifier = valueAnalysisCpa.getShutdownNotifier();

    final Solver solver = Solver.create(config, logger, shutdownNotifier);

    final SymbolicStrongestPostOperator symbolicStrongestPost =
        new ValueTransferBasedStrongestPostOperator(solver, logger, config, cfa, shutdownNotifier);

    final SymbolicFeasibilityChecker feasibilityChecker =
        new SymbolicValueAnalysisFeasibilityChecker(symbolicStrongestPost,
            config,
            logger,
            cfa);

    final GenericPrefixProvider<ForgettingCompositeState> symbolicPrefixProvider =
        new GenericPrefixProvider<>(
            symbolicStrongestPost,
            ForgettingCompositeState.getInitialState(cfa.getMachineModel()),
            logger,
            cfa,
            config,
            ValueAnalysisCPA.class);

    final ElementTestingSymbolicEdgeInterpolator symbolicEdgeInterpolator =
        new ElementTestingSymbolicEdgeInterpolator(feasibilityChecker,
            symbolicStrongestPost,
            SymbolicInterpolantManager.getInstance(),
            config,
            shutdownNotifier,
            cfa);

    final SymbolicPathInterpolator pathInterpolator =
        new SymbolicPathInterpolator(
            symbolicEdgeInterpolator,
            feasibilityChecker,
            symbolicPrefixProvider,
            config,
            logger,
            shutdownNotifier,
            cfa);

    final SymbolicStrongestPostOperator explicitStrongestPost =
        new DelegatingStrongestPost(logger, config, cfa);

    final FeasibilityChecker<ForgettingCompositeState> explicitFeasibilityChecker =
        new GenericFeasibilityChecker<>(
            explicitStrongestPost,
            ForgettingCompositeState.getInitialState(cfa.getMachineModel()),
            ValueAnalysisCPA.class, // we want to work on the ValueAnalysisCPA only
            logger,
            config,
            cfa);

    final EdgeInterpolator<ForgettingCompositeState, SymbolicInterpolant> explicitEdgeInterpolator =
        new GenericEdgeInterpolator<>(
            explicitStrongestPost,
            explicitFeasibilityChecker,
            SymbolicInterpolantManager.getInstance(),
            ForgettingCompositeState.getInitialState(cfa.getMachineModel()),
            ValueAnalysisCPA.class, // we want to work on the ValueAnalysisCPA only
            config,
            shutdownNotifier,
            cfa);

    final GenericPrefixProvider<ForgettingCompositeState> explicitPrefixProvider =
        new GenericPrefixProvider<>(
            explicitStrongestPost,
            ForgettingCompositeState.getInitialState(cfa.getMachineModel()),
            logger,
            cfa,
            config,
            ValueAnalysisCPA.class);

    final PathInterpolator<SymbolicInterpolant> explicitPathInterpolator =
        new GenericPathInterpolator<>(
            explicitEdgeInterpolator,
            explicitFeasibilityChecker,
            explicitPrefixProvider,
            SymbolicInterpolantManager.getInstance(),
            config, logger, shutdownNotifier, cfa);

    return new SymbolicDelegatingRefiner(argCpa,
        feasibilityChecker,
        pathInterpolator,
        explicitFeasibilityChecker,
        explicitPathInterpolator,
        config,
        logger);
  }


  public SymbolicDelegatingRefiner(final ARGCPA pArgCPA,
      final SymbolicFeasibilityChecker pSymbolicFeasibilityChecker,
      final SymbolicPathInterpolator pSymbolicInterpolator,
      final FeasibilityChecker<ForgettingCompositeState> pExplicitFeasibilityChecker,
      final PathInterpolator<SymbolicInterpolant> pExplicitInterpolator,
      final Configuration pConfig,
      final LogManager pLogger) throws InvalidConfigurationException {

    // Two different instances of PathExtractor have to be used, otherwise,
    // RepeatedCounterexample error will occur when symbolicRefiner starts refinement.
    symbolicRefiner = new SymbolicValueAnalysisRefiner(pArgCPA,
                                                       pSymbolicFeasibilityChecker,
                                                       pSymbolicInterpolator,
                                                       new PathExtractor(pLogger, pConfig),
                                                       pConfig,
                                                       pLogger);

    explicitRefiner = new SymbolicValueAnalysisRefiner(pArgCPA,
                                                       pExplicitFeasibilityChecker,
                                                       pExplicitInterpolator,
                                                       new PathExtractor(pLogger, pConfig),
                                                       pConfig,
                                                       pLogger);
    logger = pLogger;
  }

  @Override
  public boolean performRefinement(ReachedSet pReached) throws CPAException, InterruptedException {
    logger.log(Level.FINER, "Trying to refine using explicit refiner only");
    explicitRefinements++;
    explicitRefinementTime.start();

    boolean refinementSuccessful = explicitRefiner.performRefinement(pReached);

    explicitRefinementTime.stop();

    if (!refinementSuccessful) {
      logger.log(Level.FINER, "Refinement using explicit refiner only failed");
      logger.log(Level.FINER, "Trying to refine using symbolic refiner");
      symbolicRefinements++;
      symbolicRefinementTime.start();

      refinementSuccessful = symbolicRefiner.performRefinement(pReached);

      symbolicRefinementTime.stop();
      logger.logf(Level.FINER,
          "Refinement using symbolic refiner finished with status %s", refinementSuccessful);

      if (refinementSuccessful) {
        successfulSymbolicRefinements++;
      }
    } else {
      logger.log(Level.FINER, "Refinement using explicit refiner only successful");
      successfulExplicitRefinements++;
    }

    return refinementSuccessful;
  }

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {
    statsCollection.add(new Statistics() {
      @Override
      public void printStatistics(PrintStream out, Result result, ReachedSet reached) {
        out.println("Explicit refinements: " + explicitRefinements);
        out.println("Successful explicit refinements: " + successfulExplicitRefinements);
        out.println("Symbolic refinements: " + symbolicRefinements);
        out.println("Successful symbolic refinements: " + successfulSymbolicRefinements);
        out.println("Overall explicit refinement time: " + explicitRefinementTime.getSumTime());
        out.println("Average explicit refinement time: " + explicitRefinementTime.getAvgTime());
        out.println("Overall symbolic refinement time: " + symbolicRefinementTime.getSumTime());
        out.println("Average symbolic refinement time: " + symbolicRefinementTime.getAvgTime());
      }

      @Nullable
      @Override
      public String getName() {
        return SymbolicDelegatingRefiner.class.getSimpleName();
      }
    });
  }
}
