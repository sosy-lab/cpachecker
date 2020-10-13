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

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Property;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisCPA;
import org.sosy_lab.cpachecker.cpa.value.type.BooleanValue;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.exceptions.CPAEnabledAnalysisPropertyViolationException;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Model;
import org.sosy_lab.java_smt.api.Model.ValueAssignment;
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
    private Solver solver;
    private int initialPasses;
    private int fuzzingPasses;
    @SuppressWarnings("unused")
    private ShutdownNotifier shutdownNotifier;
    private ValueAnalysisCPA valCpa;


    @Option(secure=true, name="merge", toUppercase=true, values={"RAND"},
      description="which selection strategy to use to get target states.")
    private String selectionStrategyOption = "RAND";
    private Selector selectionStrategy;

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
        this.initialPasses = 3;
        this.fuzzingPasses = 5;
        this.maxIterations = 5;
        this.cpa = cpa;

        pConfig.inject(this);

        // Fetch sovler from predicate CPA
        PredicateCPA predCpa =
                CPAs.retrieveCPAOrFail(cpa, PredicateCPA.class, LegionAlgorithm.class);
        this.solver = predCpa.getSolver();

        // Get UVA from Value Analysis
        valCpa = CPAs.retrieveCPAOrFail(cpa, ValueAnalysisCPA.class, LegionAlgorithm.class);

        selectionStrategy = buildSelectionStrategy();
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
        ArrayList<Value> preloadedValues = new ArrayList<Value>();
        try {
            reachedSet = fuzz(reachedSet, initialPasses, algorithm, preloadedValues);
        } catch (PropertyViolationException ex) {
            logger.log(Level.WARNING, "Found violated property at preload.");
            return AlgorithmStatus.SOUND_AND_PRECISE;
        }

        // Now iterate until maxIterations is reached
        for (int i = 0; i < maxIterations; i++) {
            logger.log(Level.INFO, "Iteration", i + 1);
            // Phase Selection: Select non_det for path solving
            BooleanFormula target;
            try {
                target = selectionStrategy.select(reachedSet);
            } catch (IllegalArgumentException e) {
                logger.log(Level.WARNING, "No target state found");
                return status;
            }

            // Phase Targetting: Solve and plug results to RVA as preload
            preloadedValues = new ArrayList<Value>();
            try (ProverEnvironment prover =
                    solver.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
                try (Model constraints = solvePathConstrains(target, prover)) {
                    preloadedValues = computePreloadValues(constraints);
                }
            } catch (SolverException ex) {
                this.logger.log(Level.WARNING, "Could not solve formula.");
            }

            // Phase Fuzzing: Run iterations to resource limit (m)
            try {
                reachedSet = fuzz(reachedSet, fuzzingPasses, algorithm, preloadedValues);
            } catch (PropertyViolationException ex) {
                logger.log(Level.WARNING, "Found violated property in iteration", i + 1);
                return AlgorithmStatus.SOUND_AND_PRECISE;
            }
            valCpa.getTransferRelation().clearKnownValues();
        }
        return status;
    }

    /**
     * Ask the SAT-solver to compute path constraints for the pTarget.
     * 
     * @param target The formula leading to the selected state.
     * @param pProver The prover to use.
     * @throws InterruptedException, SolverException
     */
    private Model solvePathConstrains(BooleanFormula target, ProverEnvironment pProver)
            throws InterruptedException, SolverException {
        logger.log(Level.INFO, "Solve path constraints.");
        pProver.push(target);
        assertThat(pProver).isSatisfiable();
        return pProver.getModel();
    }

    /**
     * Pushes the values from the model into the value assigner. TODO may be moved to RVA
     * 
     * @param pConstraints The source of values to assign.
     */
    private ArrayList<Value> computePreloadValues(Model pConstraints) {
        ArrayList<Value> values = new ArrayList<Value>();
        for (ValueAssignment assignment : pConstraints.asList()) {
            String name = assignment.getName();
            Value value = toValue(assignment.getValue());
            logger.log(Level.INFO, "Loaded Value", name, value);
            values.add(value);
        }
        // valCpa.getTransferRelation().setKnownValues(values);
        return values;
    }

    private Value toValue(Object value){
        if (value instanceof Boolean){
          return BooleanValue.valueOf((Boolean)value);
        } else if (value instanceof Integer){
          return new NumericValue((Integer)value);
        } else if (value instanceof Character){
          return new NumericValue((Integer)value);
        } else if (value instanceof Float) {
          return new NumericValue((Float)value);
        } else if (value instanceof Double) {
          return new NumericValue((Double)value);
        } else if (value instanceof BigInteger) {
          BigInteger v = (BigInteger)value;
          return new NumericValue(v);
        } else {
          throw new IllegalArgumentException(String.format("Did not recognize value for loadedValues Map: %s.", value.getClass()));
        }
      }

    /**
     * Run the fuzzing phase using pAlgorithm pPasses times on the states in pReachedSet.
     * 
     * To be discussed: Design decision --------------------------------
     * 
     */
    private ReachedSet fuzz(ReachedSet pReachedSet, int pPasses, Algorithm pAlgorithm, ArrayList<Value> pPreLoadedValues)
            throws CPAEnabledAnalysisPropertyViolationException, CPAException, InterruptedException,
            PropertyViolationException {

        logger.log(Level.INFO, "Fuzzing target.");
        for (int i = 0; i < pPasses; i++) {
            logger.log(Level.INFO, "Fuzzing pass", i + 1);
            valCpa.getTransferRelation().setKnownValues(pPreLoadedValues);
            // Run algorithm and collect result
            pAlgorithm.run(pReachedSet);

            // If an error was found, stop execution
            Collection<Property> violatedProperties = pReachedSet.getViolatedProperties();
            if (!violatedProperties.isEmpty()) {
                throw new PropertyViolationException(violatedProperties);
            }

            // Otherwise, start from the beginning again
            pReachedSet.reAddToWaitlist(pReachedSet.getFirstState());
        }

        return pReachedSet;
    }

    Selector buildSelectionStrategy(){
        if (selectionStrategyOption == "RAND"){
            return new RandomSelectionStrategy(logger);
        }
        throw new IllegalArgumentException(
            "Selection strategy " + selectionStrategyOption + " unknown"
            );
    }
}
