// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import java.io.PrintStream;
import java.util.Collection;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.collect.PersistentMap;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.preciseErrorCondition.CompositeRefiner;
import org.sosy_lab.cpachecker.core.algorithm.preciseErrorCondition.FormulaContext;
import org.sosy_lab.cpachecker.core.algorithm.preciseErrorCondition.ReachabilityAnalyzer;
import org.sosy_lab.cpachecker.core.algorithm.preciseErrorCondition.RefinementStrategy;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;
import org.sosy_lab.java_smt.SolverContextFactory.Solvers;
import org.sosy_lab.java_smt.api.SolverException;

@Options(prefix = "findErrorCondition")
public class FindErrorCondition implements Algorithm, StatisticsProvider, Statistics {

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

  private final Algorithm algorithm;
  private final ConfigurableProgramAnalysis cpa;
  private final LogManager logger;
  private final StatTimer totalTime =
      new StatTimer("Total time for finding precise error condition");
  private int currentIteration = 0;
  private final FormulaContext context;

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
    logger.log(Level.INFO, "Finding error condition...");
    totalTime.start();
    try {

      // Initialize variables
      boolean foundNewCounterexamples;
      ReachabilityAnalyzer analyzer = new ReachabilityAnalyzer(cpa, context, currentIteration);
      AbstractState initialState = analyzer.getInitialState();
      CompositeRefiner refiner =
          new CompositeRefiner(context, refiners, qSolver, parallelRefinement);

      do {
        logger.log(Level.INFO, "Iteration: " + currentIteration);
        foundNewCounterexamples = false;

        // Run reachability analysis
        reachedSet = analyzer.updateReachedSet(reachedSet, initialState);
        status = algorithm.run(reachedSet);

        // Collect counterexamples
        FluentIterable<CounterexampleInfo> counterExamples =
            analyzer.getCounterexamples(reachedSet);
        logger.log(Level.INFO,
            String.format("Iteration %d: Found %d Counterexamples", currentIteration,
                counterExamples.size()));

        if (!counterExamples.isEmpty()) {
          foundNewCounterexamples = true;
          logger.log(Level.INFO,
              String.format("Iteration %d: Entering For Loop...", currentIteration));
          for (CounterexampleInfo cex : counterExamples) {
            // Refinement
            PathFormula exclusionFormula = refiner.refine(cex);
            System.out.printf("IGNORE. " + exclusionFormula.getLength());
          }
          // update initial state with the exclusion formula
          initialState =
              updateInitialStateWithExclusions(initialState, refiner.getExclusionFormula());
        }

      } while (foundNewCounterexamples && (++currentIteration < maxIterations
          || maxIterations == -1));
      if (parallelRefinement) {
        refiner.shutdown();
      }
      return status;

    } catch (InvalidConfigurationException | SolverException ex) {
      throw new CPAException("Error during the execution of FindErrorCondition", ex);
    } finally {
      totalTime.stop();
    }
  }

  // Update the initial state with exclusion formulas for the next run
  private AbstractState updateInitialStateWithExclusions(
      AbstractState initialState,
      PathFormula exclusionFormula) {
    Builder<AbstractState> initialAbstractStates = ImmutableList.builder();
    for (AbstractState abstractState : AbstractStates.asIterable(initialState)) {
      if (abstractState instanceof ARGState) {
        // TODO handle ARGState instances
        continue;
      }
      if (abstractState instanceof CompositeState) {
        // TODO handle CompositeState instances
        continue;
      }
      if (abstractState instanceof PredicateAbstractState predicateState) {
        PersistentMap<CFANode, Integer> locations =
            predicateState.getAbstractionLocationsOnPath();
        initialAbstractStates.add(PredicateAbstractState.mkAbstractionState(exclusionFormula,
            predicateState.getAbstractionFormula(), locations));
      } else {
        initialAbstractStates.add(abstractState);
      }
    }
    logger.log(Level.INFO,
        String.format(
            "Iteration %d: Updated initial state with the exclusion formula for next iteration.",
            currentIteration));
    logger.log(Level.FINE, String.format("Iteration %s: Updated initial state: ", initialState));
    return new ARGState(new CompositeState(initialAbstractStates.build()), null);
  }


  // TODO minimize Error condition remove redundant expressions

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