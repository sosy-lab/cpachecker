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

import static org.junit.Assert.assertThat;
import static org.sosy_lab.java_smt.test.ProverEnvironmentSubject.assertThat;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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
import org.sosy_lab.cpachecker.core.algorithm.legion.selection.RandomSelectionStrategy;
import org.sosy_lab.cpachecker.core.algorithm.legion.selection.Selector;
import org.sosy_lab.cpachecker.core.algorithm.legion.selection.UnvisitedEdgesStrategy;
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
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.testcase.XMLTestCaseExport;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.Model;
import org.sosy_lab.java_smt.api.Model.ValueAssignment;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverContext.ProverOptions;
import org.sosy_lab.java_smt.api.SolverException;

@Options(prefix = "legion")
public class LegionAlgorithm implements Algorithm {
    private final Algorithm algorithm;
    private final LogManager logger;
    @SuppressWarnings("unused")
    private final ConfigurableProgramAnalysis cpa;
    private Solver solver;
    @SuppressWarnings("unused")
    private ShutdownNotifier shutdownNotifier;
    private ValueAnalysisCPA valCpa;
    final PredicateCPA predCpa;

    @Option(
        secure = true,
        name = "selectionStrategy",
        toUppercase = true,
        values = {"RAND", "UNVISITED"},
        description = "which selection strategy to use to get target states.")
    private String selectionStrategyOption = "RAND";
    private Selector selectionStrategy;

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
        this.cpa = cpa;

        pConfig.inject(this, LegionAlgorithm.class);

        // Fetch solver from predicate CPA
        this.predCpa = CPAs.retrieveCPAOrFail(cpa, PredicateCPA.class, LegionAlgorithm.class);
        this.solver = predCpa.getSolver();

        // Get value cpa
        valCpa = CPAs.retrieveCPAOrFail(cpa, ValueAnalysisCPA.class, LegionAlgorithm.class);

        // Set selection Strategy
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
        ArrayList<ArrayList<ValueAssignment>> preloadedValues = new ArrayList<>();
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
            PathFormula target;
            try {
                target = selectionStrategy.select(reachedSet);
            } catch (IllegalArgumentException e) {
                logger.log(Level.WARNING, "No target state found");
                break;
            }
            if (target == null) {
                logger.log(Level.WARNING, "No target states left");
                break;
            }

            // Phase Targetting: Solve and plug results to RVA as preload
            preloadedValues = target(solver, this.maxSolverAsks, target);
            int fuzzingPasses = (int) Math.ceil(fuzzingMultiplier * preloadedValues.size());

            // Phase Fuzzing: Run iterations to resource limit (m)
            try {
                reachedSet = fuzz(reachedSet, fuzzingPasses, algorithm, preloadedValues);
            } catch (PropertyViolationException ex) {
                logger.log(Level.WARNING, "Found violated property in iteration", i + 1);
                return AlgorithmStatus.SOUND_AND_PRECISE;
            }
            valCpa.getTransferRelation().clearKnownValues();
            try {
                writeTestCases(preloadedValues, String.valueOf(i));
            } catch (IOException exc){
                logger.log(Level.WARNING, "Could not write test output", exc);
            }
        }

        try {
            writeTestCases(preloadedValues, "last");
            writeTestMetadata();
        } catch (IOException exc){
            logger.log(Level.WARNING, "Could not write test output", exc);
        }

        return status;
    }

    private void writeTestMetadata() throws IOException {
        File outpath = new File("./output/testcases");
        outpath.mkdirs();

        try (FileWriter metadata = new FileWriter("./output/testcases/metadata.xml")){
            XMLTestCaseExport.writeXMLMetadata(
                metadata,
                this.predCpa.getCfa(),
                null,
                "legion"
                );
            metadata.flush();
        }
    }

    private void writeTestCases(ArrayList<ArrayList<ValueAssignment>> preloadedValues, String iteration) throws IOException {
        File outpath = new File("./output/testcases");
        outpath.mkdirs();

        int i = 0;
        for (ArrayList<ValueAssignment> lst : preloadedValues) {
            String filename = "testcase_it-" + iteration + "va-" + String.valueOf(i) + ".xml";
            logger.log(Level.WARNING, "Writing testcase", filename);
            try (FileWriter testcase = new FileWriter("./output/testcases/" + filename)){
                testcase.append("<testcase>\n");
                for (ValueAssignment va : lst){
                    logger.log(Level.WARNING, "Appending va");
                    String name = va.getName();
                    String value = String.valueOf(va.getValue());
                    String type = "int";
                    testcase.append(String.format("\t<input variable=\"%s\" type=\"%s\">%s</input>\n", name, type, value));
                }
                testcase.append("</testcase>\n");
                testcase.flush();
            }
        }
    }

    /**
     * Phase targetting Solve for the given targets and return matching values.
     * 
     * @param pSolver        The solver to use.
     * @param pMaxSolverAsks The maximum amount of times to bother the SMT-Solver.
     * @param pTarget        The target formula to solve for.
     */
    ArrayList<ArrayList<ValueAssignment>>
            target(Solver pSolver, int pMaxSolverAsks, PathFormula pTarget)
                    throws InterruptedException {
        // Phase Targetting: Solve and plug results to RVA as preload
        ArrayList<ArrayList<ValueAssignment>> preloadedValues = new ArrayList<>();

        try (ProverEnvironment prover =
                pSolver.newProverEnvironment(
                        ProverOptions.GENERATE_MODELS,
                        ProverOptions.GENERATE_UNSAT_CORE)) {

            FormulaManagerView fmgr = pSolver.getFormulaManager();
            BooleanFormulaManager bmgr = fmgr.getBooleanFormulaManager();

            // Ask solver for the first set of Values
            try (Model constraints = solvePathConstrains(pTarget.getFormula(), prover)) {
                preloadedValues.add(computePreloadValues(constraints));
            } catch (SolverException ex) {
                this.logger.log(Level.WARNING, "Could not solve formula.");
            }

            // Repeats the solving at most pMaxSolverAsks amount of times
            // or the size of preloadedValues
            for (int i = 0; i < Math.min(pMaxSolverAsks - 1, preloadedValues.get(0).size()); i++) {

                ValueAssignment assignment = preloadedValues.get(0).get(i);

                // Create negated assignment formula
                BooleanFormula f = assignment.getAssignmentAsFormula();
                BooleanFormula not_f = bmgr.not(f);

                try {
                    prover.push(not_f);
                    if (prover.isUnsat()) {
                        this.logger.log(Level.WARNING, "Is unsat.", i);
                        continue;
                    }
                    Model constraints = prover.getModel();
                    preloadedValues.add(computePreloadValues(constraints));
                } catch (SolverException ex) {
                    this.logger.log(Level.WARNING, "Could not solve formula.");
                } finally {
                    prover.pop();
                }
            }
        }

        return preloadedValues;
    }

    /**
     * Ask the SAT-solver to compute path constraints for the pTarget.
     * 
     * @param target  The formula leading to the selected state.
     * @param pProver The prover to use.
     * @throws InterruptedException, SolverException
     */
    private Model solvePathConstrains(BooleanFormula target, ProverEnvironment pProver)
            throws InterruptedException, SolverException {

        logger.log(Level.INFO, "Solve path constraints. ", target.toString());
        pProver.push(target);
        assertThat(pProver).isSatisfiable();
        return pProver.getModel();
    }

    /**
     * Pushes the values from the model into the value assigner. TODO may be moved to RVA
     * 
     * @param pConstraints The source of values to assign.
     */
    private ArrayList<ValueAssignment> computePreloadValues(Model pConstraints) {
        ArrayList<ValueAssignment> values = new ArrayList<>();
        for (ValueAssignment assignment : pConstraints.asList()) {
            String name = assignment.getName();

            if (!name.startsWith("__VERIFIER_nondet_")) {
                continue;
            }

            Value value = toValue(assignment.getValue());
            logger.log(Level.INFO, "Loaded Value", name, value);
            values.add(assignment);
        }
        // valCpa.getTransferRelation().setKnownValues(values);
        return values;
    }

    private Value toValue(Object value) {
        if (value instanceof Boolean) {
            return BooleanValue.valueOf((Boolean) value);
        } else if (value instanceof Integer) {
            return new NumericValue((Integer) value);
        } else if (value instanceof Character) {
            return new NumericValue((Integer) value);
        } else if (value instanceof Float) {
            return new NumericValue((Float) value);
        } else if (value instanceof Double) {
            return new NumericValue((Double) value);
        } else if (value instanceof BigInteger) {
            BigInteger v = (BigInteger) value;
            return new NumericValue(v);
        } else {
            throw new IllegalArgumentException(
                    String.format(
                            "Did not recognize value for loadedValues Map: %s.",
                            value.getClass()));
        }
    }

    /**
     * Run the fuzzing phase using pAlgorithm pPasses times on the states in pReachedSet.
     * 
     * To be discussed: Design decision --------------------------------
     * 
     */
    private ReachedSet fuzz(
            ReachedSet pReachedSet,
            int pPasses,
            Algorithm pAlgorithm,
            ArrayList<ArrayList<ValueAssignment>> pPreLoadedValues)
            throws CPAEnabledAnalysisPropertyViolationException, CPAException, InterruptedException,
            PropertyViolationException {

        for (int i = 0; i < pPasses; i++) {
            logger.log(Level.INFO, "Fuzzing pass", i + 1);

            // Preload values if they exist
            int size = pPreLoadedValues.size();
            if (size > 0) {
                int j = i % size;
                logger.log(Level.FINE, "pPreLoadedValues at", j, "/", size);
                // valCpa.getTransferRelation().setKnownValues(pPreLoadedValues.get(j));
                preloadValues(valCpa, pPreLoadedValues.get(j));
            }
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

    ArrayList<Value>
            preloadValues(ValueAnalysisCPA valueCpa, ArrayList<ValueAssignment> assignments) {
        ArrayList<Value> values = new ArrayList<>();
        for (ValueAssignment a : assignments) {
            values.add(toValue(a.getValue()));
        }

        valueCpa.getTransferRelation().setKnownValues(values);

        return values;
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
}
