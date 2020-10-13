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

import static org.sosy_lab.java_smt.test.ProverEnvironmentSubject.assertThat;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Random;
import java.util.logging.Level;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Property;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.cpa.value.RandomValueAssigner;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisCPA;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.exceptions.CPAEnabledAnalysisPropertyViolationException;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Model;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverContext.ProverOptions;
import org.sosy_lab.java_smt.api.SolverException;

@Options(prefix = "cpa.value.legion")
public class LegionAlgorithm implements Algorithm {
    private final Algorithm algorithm;
    private final LogManager logger;
    private final int maxIterations;
    @SuppressWarnings("unused")
    private final ConfigurableProgramAnalysis cpa;
    private RandomValueAssigner unknownValueHandler;
    private Solver solver;
    private int initialPasses;
    private int fuzzingPasses;
    @SuppressWarnings("unused")
    private ShutdownNotifier shutdownNotifier;

    public LegionAlgorithm(
            final Algorithm algorithm,
            final LogManager pLogger,
            Configuration pConfig,
            ShutdownNotifier shutdownNotifier,
            ConfigurableProgramAnalysis cpa)
            throws InvalidConfigurationException {
        this.algorithm = algorithm;
        this.logger = pLogger;
        this.shutdownNotifier = shutdownNotifier;
        this.initialPasses = 1;
        this.fuzzingPasses = 2;
        this.maxIterations = 10;
        this.cpa = cpa;

        pConfig.inject(this);

        // Fetch sovler from predicate CPA
        PredicateCPA predCpa =
                CPAs.retrieveCPAOrFail(cpa, PredicateCPA.class, LegionAlgorithm.class);
        this.solver = predCpa.getSolver();

        // Get UVA from Value Analysis
        ValueAnalysisCPA valCpa =
                CPAs.retrieveCPAOrFail(cpa, ValueAnalysisCPA.class, LegionAlgorithm.class);
        this.unknownValueHandler = (RandomValueAssigner) valCpa.getUnknownValueHandler();

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
        try {
            reachedSet = fuzz(reachedSet, initialPasses, algorithm);
        } catch (PropertyViolationException ex) {
            logger.log(Level.WARNING, "Found violated property at preload.");
            return AlgorithmStatus.SOUND_AND_PRECISE;
        }

        // Now iterate until maxIterations is reached
        for (int i = 0; i < maxIterations; i++) {
            logger.log(Level.INFO, "Iteration", i + 1);
            // Phase Selection: Select non_det for path solving
            AbstractState target = selectTarget(reachedSet, SelectionStrategy.RANDOM);

            // Phase Targetting: Solve and plug results to RVA as preload
            try (Model constraints = solvePathConstrains(target, solver)) {
                preloadValueAssigner(constraints, unknownValueHandler);
            }

            // Phase Fuzzing: Run iterations to resource limit (m)
            try {
                reachedSet = fuzz(reachedSet, fuzzingPasses, algorithm);
            } catch (PropertyViolationException ex) {
                logger.log(Level.WARNING, "Found violated property in iteration", i + 1);
                return AlgorithmStatus.SOUND_AND_PRECISE;
            }
        }
        return status;
    }

    /**
     * Select a state to target based on the targetting strategy.
     * 
     * @param pReachedSet A set of states to choose targets from.
     * @param pStrategy   How to select a target.
     */
    private AbstractState selectTarget(ReachedSet pReachedSet, SelectionStrategy pStrategy) {

        if (pStrategy == SelectionStrategy.RANDOM) {
            LinkedList<AbstractState> nonDetStates = new LinkedList<>();
            for (AbstractState state : pReachedSet.asCollection()) {
                ValueAnalysisState vs =
                        AbstractStates.extractStateByType(state, ValueAnalysisState.class);
                if (vs.nonDeterministicMark) {
                    nonDetStates.add(state);
                }
            }
            int rnd = new Random().nextInt(nonDetStates.size());
            return nonDetStates.get(rnd);
        }
        return null;
    }

    LinkedList<AbstractState> getNondetStates(ReachedSet pReachedSet) {
        LinkedList<AbstractState> nonDetStates = new LinkedList<>();
        for (AbstractState state : pReachedSet.asCollection()) {
            ValueAnalysisState vs =
                    AbstractStates.extractStateByType(state, ValueAnalysisState.class);
            if (vs.nonDeterministicMark) {
                nonDetStates.add(state);
            }
        }
        return nonDetStates;
    }

    /**
     * Ask the SAT-solver to compute path constraints for the pTarget.
     * 
     * @param pTarget State to solve path constraints for.
     * @param pSolver The solver to use.
     */
    private Model solvePathConstrains(AbstractState pTarget, Solver pSolver)
            throws InterruptedException {
        logger.log(Level.INFO, "Solve path constraints.");
        try (ProverEnvironment prover =
                solver.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
            PredicateAbstractState ps =
                    AbstractStates.extractStateByType(pTarget, PredicateAbstractState.class);
            BooleanFormula f = ps.getPathFormula().getFormula();
            prover.push(f);
            assertThat(prover).isSatisfiable();
            return prover.getModel();
        } catch (SolverException ex) {
            this.logger.log(Level.WARNING, "Could not solve formula.");
        }
        return null;
    }

    /**
     * Pushes the values from the model into the value assigner. TODO may be moved to RVA
     * 
     * @param pConstraints The source of values to assign.
     */
    private void preloadValueAssigner(Model pConstraints, RandomValueAssigner pValueAssigner) {
        logger.log(Level.INFO, "Preloading value assigner !not implemented.");
        // todo
    }

    /**
     * Run the fuzzing phase using pAlgorithm pPasses times on the states in pReachedSet.
     */
    private ReachedSet fuzz(ReachedSet pReachedSet, int pPasses, Algorithm pAlgorithm)
            throws CPAEnabledAnalysisPropertyViolationException, CPAException, InterruptedException,
            PropertyViolationException {

        logger.log(Level.INFO, "Fuzzing target.");
        for (int i = 0; i < pPasses; i++) {
            // Run algorithm and collect result
            pAlgorithm.run(pReachedSet);

            // If an error was found, stop execution
            Collection<Property> violatedProperties = pReachedSet.getViolatedProperties();
            if (!violatedProperties.isEmpty()) {
                throw new PropertyViolationException(violatedProperties);
            }

            // Otherwise, start with nondet States again
            for (AbstractState state : getNondetStates(pReachedSet)) {
                pReachedSet.reAddToWaitlist(state);
            }
        }

        return pReachedSet;
    }
}
