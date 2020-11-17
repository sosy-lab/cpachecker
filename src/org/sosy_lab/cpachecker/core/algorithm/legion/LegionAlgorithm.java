/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2020  Dirk Beyer
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
 */
package org.sosy_lab.cpachecker.core.algorithm.legion;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
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
import org.sosy_lab.cpachecker.core.algorithm.legion.selection.RandomSelectionStrategy;
import org.sosy_lab.cpachecker.core.algorithm.legion.selection.Selector;
import org.sosy_lab.cpachecker.core.algorithm.legion.selection.UnvisitedEdgesStrategy;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisCPA;
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
        description = "How many total iterations of [select, target, fuzz] to perform.")
    private int maxIterations = 5;

    @Option(
        secure = true,
        description = "The maximum number of times to ask the solver for a solution per iteration.")
    private int maxSolverAsks = 5;

    // General fields
    private final Algorithm algorithm;
    private final LogManager logger;
    private Configuration config;
    private ShutdownNotifier shutdownNotifier;

    // CPAs + components
    private ValueAnalysisCPA valueCpa;
    private Solver solver;
    final PredicateCPA predCpa;

    // Legion Specific
    private OutputWriter outputWriter;
    private Selector selectionStrategy;
    private TargetSolver targetSolver;
    private Fuzzer fuzzer;
    private Fuzzer init_fuzzer;

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

        this.config = pConfig;
        pConfig.inject(this, LegionAlgorithm.class);
        this.shutdownNotifier = pShutdownNotifier;

        // Fetch solver from predicate CPA and valueCpa (used in fuzzer)
        this.predCpa = CPAs.retrieveCPAOrFail(cpa, PredicateCPA.class, LegionAlgorithm.class);
        this.solver = predCpa.getSolver();
        this.valueCpa = CPAs.retrieveCPAOrFail(cpa, ValueAnalysisCPA.class, LegionAlgorithm.class);

        // Configure Output
        this.outputWriter = new OutputWriter(logger, predCpa, "./output/testcases");

        // Set selection Strategy, targetSolver and fuzzers
        this.selectionStrategy = buildSelectionStrategy();
        this.targetSolver = new TargetSolver(logger, solver, maxSolverAsks);
        this.init_fuzzer =
                new Fuzzer(
                        "init",
                        logger,
                        valueCpa,
                        this.outputWriter,
                        pShutdownNotifier,
                        pConfig);
        this.fuzzer =
                new Fuzzer(
                        "fuzzer",
                        logger,
                        valueCpa,
                        this.outputWriter,
                        pShutdownNotifier,
                        pConfig);
    }

    @Override
    public AlgorithmStatus run(ReachedSet reachedSet)
            throws CPAException, InterruptedException,
            CPAEnabledAnalysisPropertyViolationException {
        AlgorithmStatus status = AlgorithmStatus.SOUND_AND_PRECISE;

        // Before asking a solver for path constraints, one initial pass through the program
        // has to be done to provide an initial set of states. This initial discovery
        // is meant to be cheap in resources and tries to establish easy to reach states.
        logger.log(Level.INFO, "Initial fuzzing ...");
        ArrayList<ArrayList<ValueAssignment>> preloadedValues = new ArrayList<>();
        try {
            reachedSet = init_fuzzer.fuzz(reachedSet, algorithm, preloadedValues);
        } catch (PropertyViolationException ex) {
            logger.log(Level.WARNING, "Found violated property at preload.");
            return status;
        } finally {
            outputWriter.writeTestCases(reachedSet);
        }

        // In it's main iterations, ask the solver for new solutions every time.
        // This is done until a resource limit is reached or no new target states
        // are available.
        for (int i = 0; i < maxIterations; i++) {
            logger.log(Level.INFO, "Iteration", i + 1);

            // Check whether to shut down
            if (this.shutdownNotifier.shouldShutdown()) {
                break;
            }

            // Phase Selection: Select non-deterministic variables for path solving
            PathFormula target;
            logger.log(Level.INFO, "Selection ...");
            try {
                target = selectionStrategy.select(reachedSet);
            } catch (IllegalArgumentException e) {
                logger.log(Level.WARNING, "No target state found");
                break;
            } finally {
                outputWriter.writeTestCases(reachedSet);
            }

            if (target == null) {
                logger.log(Level.WARNING, "No target states left");
                break;
            }

            // Check whether to shut down
            if (this.shutdownNotifier.shouldShutdown()) {
                break;
            }

            // Phase Targetting: Solve for the target and produce a number of values
            // needed as input to reach this target.
            ArrayList<ArrayList<ValueAssignment>> previousLoadedValues = preloadedValues;
            logger.log(Level.INFO, "Targetting ...");
            try {
                preloadedValues = this.targetSolver.target(target);
            } catch (SolverException ex) {
                // Re-Run with previous preloaded Values
                preloadedValues = previousLoadedValues;
            }

            // Check whether to shut down
            if (this.shutdownNotifier.shouldShutdown()) {
                break;
            }

            // Phase Fuzzing: Run the configured number of fuzzingPasses to detect
            // new paths through the program.
            logger.log(Level.INFO, "Fuzzing ...");
            fuzzer.computePasses(preloadedValues.size());
            try {
                reachedSet = fuzzer.fuzz(reachedSet, algorithm, preloadedValues);
            } catch (PropertyViolationException ex) {
                logger.log(Level.WARNING, "Found violated property in iteration", i + 1);
                break;
            } finally {
                valueCpa.getTransferRelation().clearKnownValues();
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

        pOut.println("phases:");

        pOut.println(this.init_fuzzer.stats.collect());
        pOut.println(this.selectionStrategy.getStats().collect());
        pOut.println(this.targetSolver.stats.collect());
        pOut.println(this.fuzzer.stats.collect());
    }

    @Override
    public @Nullable String getName() {
        return "Legion Algorithm";
    }
}
