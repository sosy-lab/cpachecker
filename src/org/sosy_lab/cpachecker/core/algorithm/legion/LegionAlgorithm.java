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
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
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
        description = "How many passes to fuzz before asking the solver for the first time.")
    private int initialPasses = 3;

    @Option(secure = true, description = "fuzzingPasses = ⌈ fuzzingMultiplier * fuzzingSolutions ⌉")
    private double fuzzingMultiplier = 1;

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

    // Statistics
    private HashMap<String, Duration> timings;

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

        // Set selection Strategy, targetSolver and fuzzer
        this.selectionStrategy = buildSelectionStrategy();
        this.targetSolver = new TargetSolver(logger, solver, maxSolverAsks);
        this.fuzzer = new Fuzzer(logger, valueCpa, this.outputWriter, pShutdownNotifier);

        this.timings = new HashMap<>();
    }

    @Override
    public AlgorithmStatus run(ReachedSet reachedSet)
            throws CPAException, InterruptedException,
            CPAEnabledAnalysisPropertyViolationException {
        logger.log(Level.INFO, "Running legion.");
        AlgorithmStatus status = AlgorithmStatus.NO_PROPERTY_CHECKED;

        // Before asking a solver for path constraints, one initial pass through the program
        // has to be done to provide an initial set of states. This initial discovery
        // is meant to be cheap in resources and tries to establish easy to reach states.
        Instant init_start = Instant.now();
        ArrayList<ArrayList<ValueAssignment>> preloadedValues = new ArrayList<>();
        try {
            reachedSet = fuzzer.fuzz(reachedSet, initialPasses, algorithm, preloadedValues);
        } catch (PropertyViolationException ex) {
            logger.log(Level.WARNING, "Found violated property at preload.");
            outputWriter.writeTestCases(reachedSet);
            return AlgorithmStatus.SOUND_AND_PRECISE;
        }
        Instant init_end = Instant.now();
        timings.merge(
                "init",
                Duration.between(init_start, init_end),
                (oldValue, newValue) -> oldValue.plus(newValue));

        // In it's main iterations, ask the solver for new solutions every time.
        // This is done until a resource limit is reached or no new target states
        // are available.
        for (int i = 0; i < maxIterations; i++) {
            logger.log(Level.INFO, "Iteration", i + 1);

            // Check whether to shut down
            if (this.shutdownNotifier.shouldShutdown()){
                break;
            }

            // Phase Selection: Select non-deterministic variables for path solving
            Instant selection_start = Instant.now();
            PathFormula target;
            try {
                target = selectionStrategy.select(reachedSet);
            } catch (IllegalArgumentException e) {
                logger.log(Level.WARNING, "No target state found");
                outputWriter.writeTestCases(reachedSet);
                break;
            }
            if (target == null) {
                logger.log(Level.WARNING, "No target states left");
                break;
            }
            Instant selection_end = Instant.now();
            timings.merge(
                    "selection",
                    Duration.between(selection_start, selection_end),
                    (oldValue, newValue) -> oldValue.plus(newValue));

            // Check whether to shut down
            if (this.shutdownNotifier.shouldShutdown()){
                break;
            }

            // Phase Targetting: Solve for the target and produce a number of values
            // needed as input to reach this target.
            Instant targetting_start = Instant.now();
            ArrayList<ArrayList<ValueAssignment>> previousLoadedValues = preloadedValues;
            try {
                preloadedValues = this.targetSolver.target(target);
            } catch (SolverException ex) {
                // Re-Run with previous preloaded Values
                logger.log(Level.WARNING, "Running with previous preloaded values");
                preloadedValues = previousLoadedValues;
            }
            Instant targetting_end = Instant.now();
            timings.merge(
                    "targetting",
                    Duration.between(targetting_start, targetting_end),
                    (oldValue, newValue) -> oldValue.plus(newValue));

            // Check whether to shut down
            if (this.shutdownNotifier.shouldShutdown()){
                break;
            }

            // Phase Fuzzing: Run the configured number of fuzzingPasses to detect
            // new paths through the program.
            Instant fuzzing_start = Instant.now();
            int fuzzingPasses = (int) Math.ceil(fuzzingMultiplier * preloadedValues.size());
            try {
                reachedSet = fuzzer.fuzz(reachedSet, fuzzingPasses, algorithm, preloadedValues);
            } catch (PropertyViolationException ex) {
                logger.log(Level.WARNING, "Found violated property in iteration", i + 1);
                status = AlgorithmStatus.SOUND_AND_PRECISE;
                break;
            } finally {
                valueCpa.getTransferRelation().clearKnownValues();
            }
            Instant fuzzing_end = Instant.now();
            timings.merge(
                    "fuzzing",
                    Duration.between(fuzzing_start, fuzzing_end),
                    (oldValue, newValue) -> oldValue.plus(newValue));

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

        pOut.println("");
        pOut.println("settings:");
        pOut.println("  selection_strategy: " + selectionStrategyOption);

        pOut.println("");
        pOut.println("times:");
        String format = "  %-10s: %ss";
        for (Map.Entry<String, Duration> entry : timings.entrySet()) {
            String out =
                    String.format(
                            format,
                            entry.getKey(),
                            (float) entry.getValue().toMillis() / 1000);
            pOut.println(out);
        }
    }

    @Override
    public @Nullable String getName() {
        return "Legion Algorithm";
    }
}
