// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm;

import com.google.common.collect.FluentIterable;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Optional;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.preciseErrorCondition.CompositeRefiner;
import org.sosy_lab.cpachecker.core.algorithm.preciseErrorCondition.FormulaContext;
import org.sosy_lab.cpachecker.core.algorithm.preciseErrorCondition.RefinementResult;
import org.sosy_lab.cpachecker.core.algorithm.preciseErrorCondition.RefinementResult.RefinementStatus;
import org.sosy_lab.cpachecker.core.algorithm.preciseErrorCondition.RefinementStrategy;
import org.sosy_lab.cpachecker.core.algorithm.preciseErrorCondition.Utility;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;
import org.sosy_lab.java_smt.SolverContextFactory.Solvers;
import org.sosy_lab.java_smt.api.SolverException;

@Options(prefix = "findErrorCondition")
public class FindErrorCondition implements Algorithm, StatisticsProvider, Statistics {

  private final Algorithm algorithm;
  private final ConfigurableProgramAnalysis cpa;
  private final LogManager logger;
  private final StatTimer totalTime =
      new StatTimer("Total time for finding precise error condition");
  private final FormulaContext context;
  @Option(
      secure = true,
      description = "Maximum iterations for error condition refinement.",
      name = "maxIterations")
  private int maxIterations = -1; // default: no iteration limit
  @Option(
      secure = true,
      description = "Solver that should perform quantifier Elimination.",
      name = "qSolver")
  private Solvers qSolver = Solvers.Z3; // default
  @Option(
      secure = true,
      description = "List of refiners to use",
      name = "refiners")
  private RefinementStrategy[] refiners =
      {RefinementStrategy.QUANTIFIER_ELIMINATION, RefinementStrategy.ALLSAT}; // default
  @Option(
      secure = true,
      description = "Enable parallel refinement. Only if at least two refiners are in use.",
      name = "parallel")
  private boolean parallelRefinement = true; // default
  @Option(
      secure = true,
      description = "Set the refiner timeout per iteration in seconds.",
      name = "timeout")
  private int refinerTimeOut = 180; // default
  @Option(
      secure = true,
      description = "Enable formatter for the error condition.",
      name = "withFormatter")
  private boolean withFormatter = true; // default
  private int currentIteration = 0;

  public FindErrorCondition(
      Algorithm pAlgorithm,
      ConfigurableProgramAnalysis pCpa,
      Configuration pConfig,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      CFA pCfa) throws InvalidConfigurationException {

    logger = pLogger;
    algorithm = pAlgorithm;
    cpa = pCpa;
    pConfig.inject(this);

    // set up formula context
    PredicateCPA predicateCPA = CPAs.retrieveCPAOrFail(cpa, PredicateCPA.class, getClass());
    Solver solver = predicateCPA.getSolver();
    PathFormulaManagerImpl manager = new PathFormulaManagerImpl(
        solver.getFormulaManager(),
        pConfig,
        pLogger,
        pShutdownNotifier,
        pCfa,
        AnalysisDirection.FORWARD);
    context = new FormulaContext(solver, manager, pCfa, logger, pConfig, pShutdownNotifier);
  }

  @Override
  public AlgorithmStatus run(ReachedSet reachedSet) throws CPAException, InterruptedException {

    AlgorithmStatus status;
    logger.log(Level.INFO, "Finding Error Condition...");
    totalTime.start();
    try {

      // Initialize variables
      boolean foundNewCounterexamples;
      AbstractState initialState = Utility.getInitialState(cpa, context);
      CompositeRefiner refiner =
          new CompositeRefiner(context, refiners, qSolver, parallelRefinement, withFormatter,
              refinerTimeOut);
      // initially empty
      RefinementResult errorCondition =
          new RefinementResult(RefinementStatus.EMPTY, Optional.empty());

      do {
        logger.log(Level.INFO, "Iteration: " + currentIteration);
        foundNewCounterexamples = false;

        // Run reachability analysis
        reachedSet =
            Utility.updateReachedSet(reachedSet, initialState, currentIteration, cpa, context);
        status = algorithm.run(reachedSet);

        // Collect counterexamples
        FluentIterable<CounterexampleInfo> counterExamples =
            Utility.getCounterexamples(reachedSet);

        if (!counterExamples.isEmpty()) {
          foundNewCounterexamples = true;
          logger.log(Level.INFO, "Found Counterexamples");
          // TODO: why is this a loop for counter examples? we get usually just one counter example
          //  per iteration
          for (CounterexampleInfo cex : counterExamples) {
            // Refinement
            errorCondition = refiner.refine(cex);
          }
          // do not continue if error condition is invalid (timed out or yielded an error)
          if (!errorCondition.isSuccessful()) {
            break;
          }
          // update initial state with the exclusion formula
          initialState = Utility.updateInitialStateWithExclusions(initialState,
              errorCondition.getOptionalFormula().get(),
              currentIteration, context);
        } else {
          logger.log(Level.INFO,
              String.format("Iteration %d: No Counterexamples Found. Finished Refinement.",
                  currentIteration));
        }

      } while (foundNewCounterexamples && (++currentIteration < maxIterations
          || maxIterations == -1));

      context.getLogger()
          .log(Level.INFO, "Final Error Condition:\n" + errorCondition);
      //TODO: can add a visitor to make final Error Condition more user friendly and easier to read.
      return status;

    } catch (InvalidConfigurationException | SolverException ex) {
      throw new CPAException("Error During The Execution Of FindErrorCondition", ex);
    } finally {
      totalTime.stop();
    }
  }


  @Override
  public void printStatistics(PrintStream out, Result result, UnmodifiableReachedSet reached) {
    StatisticsWriter.writingStatisticsTo(out).put(totalTime);
  }

  @Override
  public @Nullable String getName() {
    return "FindErrorCondition";
  }

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {
    //TODO add way for collecting statistics
  }

}