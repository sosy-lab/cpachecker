// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0
package org.sosy_lab.cpachecker.core.algorithm.legion;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.Model;
import org.sosy_lab.java_smt.api.Model.ValueAssignment;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverContext.ProverOptions;
import org.sosy_lab.java_smt.api.SolverException;

public class TargetSolver {

    private LogManager logger;
    private Solver solver;
    private int maxSolverAsks;
    private int successfull_primary_solves = 0;
    private int successfull_secondary_solves = 0;
    private int unsuccessfull_solves = 0;
    private LegionComponentStatistics stats;

    /**
     * @param pLogger        The logging instance to use.
     * @param pSolver        The solver to use.
     * @param pMaxSolverAsks The maximum amount of times to bother the SMT-Solver.
     */
    public TargetSolver(LogManager pLogger, Solver pSolver, int pMaxSolverAsks) {
        logger = pLogger;
        solver = pSolver;
        maxSolverAsks = pMaxSolverAsks;

        this.stats = new LegionComponentStatistics("targeting");
    }


    /**
     * Phase targeting Solve for the given targets and return matching values.
     * 
     * @param pTarget The target formula to solve for.
     */
    List<List<ValueAssignment>> target(PathFormula pTarget)
            throws InterruptedException, SolverException {
                        
        this.stats.start();
        List<List<ValueAssignment>> preloadedValues = new ArrayList<>();

        try (ProverEnvironment prover =
                this.solver.newProverEnvironment(
                        ProverOptions.GENERATE_MODELS,
                        ProverOptions.GENERATE_UNSAT_CORE)) {

            FormulaManagerView fmgr = this.solver.getFormulaManager();
            BooleanFormulaManager bmgr = fmgr.getBooleanFormulaManager();

            // Ask solver for the first set of Values
            try (Model constraints = solvePathConstrains(pTarget.getFormula(), prover)) {
                preloadedValues.add(computePreloadValues(constraints));
                this.successfull_primary_solves += 1;
            } catch (SolverException ex) {
                this.unsuccessfull_solves += 1;
                this.logger.log(Level.WARNING, "Could not solve even once formula.");
                this.stats.finish();
                throw ex;
            }

            // Repeats the solving at most pMaxSolverAsks amount of times
            // or the size of preloadedValues
            for (int i = 0; i < Math.min(this.maxSolverAsks - 1, preloadedValues.get(0).size()); i++) {

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
                    try (Model constraints = prover.getModel()){
                        preloadedValues.add(computePreloadValues(constraints));
                        this.successfull_secondary_solves += 1;
                    }
                    
                } catch (SolverException ex) {
                    // If this is not solvable, just skip
                    this.unsuccessfull_solves += 1;
                    this.logger.log(Level.INFO, "Could not solve for more solutions.");
                    continue;
                } finally {
                    prover.pop();
                }
            }
        }
        this.stats.finish();
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

        logger.log(Level.FINE, "Solve path constraints. ");
        logger.log(Level.FINER, "Formula is ", target.toString());
        pProver.push(target);
        boolean isUnsat = pProver.isUnsat();
        if (isUnsat){
            throw new SolverException("Formula is unsat");
        }
        return pProver.getModel();
    }

    /**
     * Pushes the values from the model into the value assigner.
     * 
     * @param pConstraints The source of values to assign.
     */
    private List<ValueAssignment> computePreloadValues(Model pConstraints) {
        List<ValueAssignment> values = new ArrayList<>();
        for (ValueAssignment assignment : pConstraints.asList()) {
            String name = assignment.getName();

            if (!name.startsWith("__VERIFIER_nondet_")) {
                continue;
            }

            Value value = Utils.toValue(assignment.getValue());
            logger.log(Level.FINE, "Loaded Value", name, value);
            values.add(assignment);
        }
        return values;
    }

    public LegionComponentStatistics getStats(){
        this.stats.set_other("successfull_primary_solves", (double)this.successfull_primary_solves);
        this.stats.set_other("successfull_secondary_solves", (double)this.successfull_secondary_solves);
        this.stats.set_other("unsuccessfull_solves", (double)this.unsuccessfull_solves);

        return this.stats;
    }
}
