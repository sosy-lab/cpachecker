// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0
package org.sosy_lab.cpachecker.core.algorithm.legion;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.CPAAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.legion.selection.RandomSelectionStrategy;
import org.sosy_lab.cpachecker.core.algorithm.legion.selection.Selector;
import org.sosy_lab.cpachecker.core.algorithm.legion.selection.UnvisitedEdgesStrategy;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGTransferRelation;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.cpa.value.NondeterministicValueProvider;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisTransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPAEnabledAnalysisPropertyViolationException;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.Model.ValueAssignment;
import org.sosy_lab.java_smt.api.SolverException;

@Options(prefix = "legion")
public class LegionAlgorithm implements Algorithm, StatisticsProvider, Statistics {

  // Configuration Options
  @Option(
      secure = true,
      name = "selectionStrategy",
      toUppercase = true,
      values = {"RAND", "UNVISITED"},
      description = "which selection strategy to use to get target states.")
  private String selectionStrategyOption = "RAND";

  @Option(
      secure = true,
      description = "The maximum number of times to ask the solver for a solution per iteration.")
  private int maxSolverAsks = 5;

  // General fields
  private final Algorithm algorithm;
  private final LogManager logger;
  private final Configuration config;
  private final ShutdownNotifier shutdownNotifier;

  // CPAs + components
  private final Solver solver;
  private final PredicateCPA predCpa;

  // Legion Specific
  private final TestcaseWriter outputWriter;
  private final Selector selectionStrategy;
  private final TargetSolver targetSolver;
  private final Fuzzer fuzzer;
  private final Fuzzer initFuzzer;
  private final NondeterministicValueProvider nonDetValueProvider;

  public LegionAlgorithm(
      final Algorithm algorithm,
      final LogManager pLogger,
      Configuration pConfig,
      ConfigurableProgramAnalysis cpa,
      ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {

    // General fields
    this.algorithm = algorithm;
    this.logger = pLogger;

    ValueAnalysisTransferRelation valueAnalysisTransferRelation =
        getCurrentValueTransferRelation(algorithm);
    this.nonDetValueProvider = valueAnalysisTransferRelation.getNonDetValueProvider();

    this.config = pConfig;
    pConfig.inject(this, LegionAlgorithm.class);
    this.shutdownNotifier = pShutdownNotifier;

    // Fetch solver from predicate CPA and valueCpa (used in fuzzer)

    this.predCpa = CPAs.retrieveCPAOrFail(cpa, PredicateCPA.class, LegionAlgorithm.class);
    this.solver = predCpa.getSolver();

    // Configure Output
    this.outputWriter = new TestcaseWriter(logger, predCpa, pConfig);

    // Set selection Strategy, targetSolver and fuzzers
    this.selectionStrategy = buildSelectionStrategy();
    this.targetSolver = new TargetSolver(logger, solver, maxSolverAsks);
    this.initFuzzer =
        new Fuzzer(
            "init", logger, this.outputWriter, pShutdownNotifier, nonDetValueProvider, pConfig);
    this.fuzzer =
        new Fuzzer(
            "fuzzer", logger, this.outputWriter, pShutdownNotifier, nonDetValueProvider, pConfig);
  }

  @Override
  public AlgorithmStatus run(ReachedSet reachedSet)
      throws CPAException, InterruptedException, CPAEnabledAnalysisPropertyViolationException {
    AlgorithmStatus status = AlgorithmStatus.SOUND_AND_PRECISE;

    // Before asking a solver for path constraints, one initial pass through the program
    // has to be done to provide an initial set of states. This initial discovery
    // is meant to be cheap in resources and tries to establish easy to reach states.
    logger.log(Level.INFO, "Initial fuzzing ...");
    List<List<ValueAssignment>> preloadedValues = new ArrayList<>();
    try {
      reachedSet = initFuzzer.fuzz(reachedSet, algorithm, preloadedValues);
    } finally {
      outputWriter.writeTestCases(reachedSet);
    }

    // In it's main iterations, ask the solver for new solutions every time.
    // This is done until a resource limit is reached or no new target states
    // are available.
    int i = 0;
    while (true) {
      logger.log(Level.INFO, "Iteration", i + 1);
      i += 1;

      // Check whether to shut down
      shutdownNotifier.shutdownIfNecessary();

      // Phase Selection: Select non-deterministic variables for path solving
      PathFormula target;
      logger.log(Level.INFO, "Selection ...");
      try {
        target = selectionStrategy.select(reachedSet);
      } catch (IllegalArgumentException e) {
        logger.logUserException(Level.WARNING, e, "No target state found");
        break;
      } finally {
        outputWriter.writeTestCases(reachedSet);
      }

      if (target == null) {
        logger.log(Level.INFO, "No target states left");
        break;
      }

      // Check whether to shut down
      shutdownNotifier.shutdownIfNecessary();

      // Phase Targeting: Solve for the target and produce a number of values
      // needed as input to reach this target as well as give feedback to selection.
      List<List<ValueAssignment>> previousLoadedValues = preloadedValues;
      logger.log(Level.INFO, "Targeting ...");

      try {
        preloadedValues = this.targetSolver.target(target);
      } catch (SolverException exc) {
        logger.logException(Level.SEVERE, exc, "SAT solver failed to compute values");
        return status;
      }

      // If no values have been found, use previous ones and feedback it
      if (preloadedValues.isEmpty()) {
        preloadedValues = previousLoadedValues;
        selectionStrategy.feedback(target, -1);
      } else {
        selectionStrategy.feedback(target, preloadedValues.size());
      }

      // Check whether to shut down
      shutdownNotifier.shutdownIfNecessary();

      // Phase Fuzzing: Run the configured number of fuzzingPasses to detect
      // new paths through the program.
      logger.log(Level.INFO, "Fuzzing ...");
      fuzzer.computePasses(preloadedValues.size());
      try {
        reachedSet = fuzzer.fuzz(reachedSet, algorithm, preloadedValues);
      } finally {
        this.nonDetValueProvider.clearKnownValues();
      }
    }
    return status;
  }

  Selector buildSelectionStrategy() {
    if (selectionStrategyOption.equals("RAND")) {
      return new RandomSelectionStrategy(logger);
    }
    if (selectionStrategyOption.equals("UNVISITED")) {
      return new UnvisitedEdgesStrategy(logger, predCpa.getPathFormulaManager());
    }
    throw new IllegalArgumentException(
        "Selection strategy " + selectionStrategyOption + " unknown");
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(this);
  }

  @Override
  public void printStatistics(PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {
    @SuppressWarnings("deprecation")
    String programName = config.getProperty("analysis.programNames");
    pOut.println("program: " + programName);

    pOut.println("settings:");
    pOut.println("  selection_strategy: " + selectionStrategyOption);

    pOut.println("components:");

    pOut.println(this.initFuzzer.getStats().collect());
    pOut.println(this.selectionStrategy.getStats().collect());
    pOut.println(this.targetSolver.getStats().collect());
    pOut.println(this.fuzzer.getStats().collect());
    pOut.println(this.outputWriter.getStats().collect());
  }

  @Override
  public @Nullable String getName() {
    return "Legion Algorithm";
  }

  private ValueAnalysisTransferRelation getCurrentValueTransferRelation(Algorithm algo)
      throws InvalidConfigurationException {
    if (!(algo instanceof CPAAlgorithm)) {
      throw new InvalidConfigurationException(
          "Cannot run concolic execution with anything but CPAAlgorithm.");
    }

    TransferRelation transferRelation = ((CPAAlgorithm) algorithm).getTransferRelation();
    if (!(transferRelation instanceof ARGTransferRelation)) {
      throw new InvalidConfigurationException(
          "Can only run with arg transfer relation in algorithm.");
    }
    ARGTransferRelation argTransferRelation = (ARGTransferRelation) transferRelation;
    return argTransferRelation.retrieveWrappedTransferRelation(ValueAnalysisTransferRelation.class);
  }
}
