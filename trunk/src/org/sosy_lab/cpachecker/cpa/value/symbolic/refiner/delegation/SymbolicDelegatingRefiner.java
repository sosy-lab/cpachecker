// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.value.symbolic.refiner.delegation;

import java.io.PrintStream;
import java.util.Collection;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGBasedRefiner;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
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
import org.sosy_lab.cpachecker.util.refinement.EdgeInterpolator;
import org.sosy_lab.cpachecker.util.refinement.FeasibilityChecker;
import org.sosy_lab.cpachecker.util.refinement.GenericEdgeInterpolator;
import org.sosy_lab.cpachecker.util.refinement.GenericFeasibilityChecker;
import org.sosy_lab.cpachecker.util.refinement.GenericPathInterpolator;
import org.sosy_lab.cpachecker.util.refinement.GenericPrefixProvider;
import org.sosy_lab.cpachecker.util.refinement.PathExtractor;
import org.sosy_lab.cpachecker.util.refinement.PathInterpolator;

/**
 * Refiner for {@link ValueAnalysisCPA} using symbolic values and {@link
 * org.sosy_lab.cpachecker.cpa.constraints.ConstraintsCPA ConstraintsCPA} that tries to refine
 * precision using only the {@link ValueAnalysisCPA}, first.
 */
public class SymbolicDelegatingRefiner implements ARGBasedRefiner, StatisticsProvider {

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

    final ValueAnalysisCPA valueAnalysisCpa =
        CPAs.retrieveCPAOrFail(pCpa, ValueAnalysisCPA.class, SymbolicValueAnalysisRefiner.class);
    final ConstraintsCPA constraintsCpa =
        CPAs.retrieveCPAOrFail(pCpa, ConstraintsCPA.class, SymbolicValueAnalysisRefiner.class);

    final Configuration config = valueAnalysisCpa.getConfiguration();

    valueAnalysisCpa.injectRefinablePrecision();
    constraintsCpa.injectRefinablePrecision(new RefinableConstraintsPrecision(config));

    final LogManager logger = valueAnalysisCpa.getLogger();
    final CFA cfa = valueAnalysisCpa.getCFA();
    final ShutdownNotifier shutdownNotifier = valueAnalysisCpa.getShutdownNotifier();

    final SymbolicStrongestPostOperator symbolicStrongestPost =
        new ValueTransferBasedStrongestPostOperator(
            constraintsCpa.getSolver(), logger, config, cfa);

    final SymbolicFeasibilityChecker feasibilityChecker =
        new SymbolicValueAnalysisFeasibilityChecker(symbolicStrongestPost, config, logger, cfa);

    final GenericPrefixProvider<ForgettingCompositeState> symbolicPrefixProvider =
        new GenericPrefixProvider<>(
            symbolicStrongestPost,
            ForgettingCompositeState.getInitialState(cfa.getMachineModel()),
            logger,
            cfa,
            config,
            ValueAnalysisCPA.class,
            shutdownNotifier);

    final ElementTestingSymbolicEdgeInterpolator symbolicEdgeInterpolator =
        new ElementTestingSymbolicEdgeInterpolator(
            feasibilityChecker,
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
            ValueAnalysisCPA.class,
            shutdownNotifier);

    final PathInterpolator<SymbolicInterpolant> explicitPathInterpolator =
        new GenericPathInterpolator<>(
            explicitEdgeInterpolator,
            explicitFeasibilityChecker,
            explicitPrefixProvider,
            SymbolicInterpolantManager.getInstance(),
            config,
            logger,
            shutdownNotifier,
            cfa);

    return new SymbolicDelegatingRefiner(
        feasibilityChecker,
        cfa,
        pathInterpolator,
        explicitFeasibilityChecker,
        symbolicStrongestPost,
        explicitPathInterpolator,
        config,
        logger);
  }

  private SymbolicDelegatingRefiner(
      final SymbolicFeasibilityChecker pSymbolicFeasibilityChecker,
      final CFA pCfa,
      final SymbolicPathInterpolator pSymbolicInterpolator,
      final FeasibilityChecker<ForgettingCompositeState> pExplicitFeasibilityChecker,
      final SymbolicStrongestPostOperator pSymbolicStrongestPost,
      final PathInterpolator<SymbolicInterpolant> pExplicitInterpolator,
      final Configuration pConfig,
      final LogManager pLogger)
      throws InvalidConfigurationException {

    // Two different instances of PathExtractor have to be used, otherwise,
    // RepeatedCounterexample error will occur when symbolicRefiner starts refinement.
    symbolicRefiner =
        new SymbolicValueAnalysisRefiner(
            pCfa,
            pSymbolicFeasibilityChecker,
            pSymbolicStrongestPost,
            pSymbolicInterpolator,
            new PathExtractor(pLogger, pConfig),
            pConfig,
            pLogger);

    explicitRefiner =
        new SymbolicValueAnalysisRefiner(
            pCfa,
            pExplicitFeasibilityChecker,
            pSymbolicStrongestPost,
            pExplicitInterpolator,
            new PathExtractor(pLogger, pConfig),
            pConfig,
            pLogger);
    logger = pLogger;
  }

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {
    statsCollection.add(
        new Statistics() {
          @Override
          public void printStatistics(
              PrintStream out, Result result, UnmodifiableReachedSet reached) {
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

    symbolicRefiner.collectStatistics(statsCollection);
  }

  @Override
  public CounterexampleInfo performRefinementForPath(ARGReachedSet pReached, ARGPath pPath)
      throws CPAException, InterruptedException {
    logger.log(Level.FINER, "Trying to refine using explicit refiner only");
    explicitRefinements++;
    explicitRefinementTime.start();

    CounterexampleInfo cex = explicitRefiner.performRefinementForPath(pReached, pPath);

    explicitRefinementTime.stop();

    if (!cex.isSpurious()) {
      logger.log(Level.FINER, "Refinement using explicit refiner only failed");
      logger.log(Level.FINER, "Trying to refine using symbolic refiner");
      symbolicRefinements++;
      symbolicRefinementTime.start();

      cex = symbolicRefiner.performRefinementForPath(pReached, pPath);

      symbolicRefinementTime.stop();
      logger.logf(
          Level.FINER,
          "Refinement using symbolic refiner finished with status %s",
          cex.isSpurious());

      if (cex.isSpurious()) {
        successfulSymbolicRefinements++;
      }
    } else {
      logger.log(Level.FINER, "Refinement using explicit refiner only successful");
      successfulExplicitRefinements++;
    }

    return cex;
  }
}
