package org.sosy_lab.cpachecker.core.algorithm.legion;

import java.util.ArrayList;
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

    /**
     * @param pLogger        The logging instance to use.
     * @param pSolver        The solver to use.
     * @param pMaxSolverAsks The maximum amount of times to bother the SMT-Solver.
     */
    public TargetSolver(LogManager pLogger, Solver pSolver, int pMaxSolverAsks) {
        logger = pLogger;
        solver = pSolver;
        maxSolverAsks = pMaxSolverAsks;
    }


    /**
     * Phase targetting Solve for the given targets and return matching values.
     * 
     * @param pTarget        The target formula to solve for.
     */
    ArrayList<ArrayList<ValueAssignment>> target(PathFormula pTarget)
                    throws InterruptedException {
                        
        ArrayList<ArrayList<ValueAssignment>> preloadedValues = new ArrayList<>();

        try (ProverEnvironment prover =
                this.solver.newProverEnvironment(
                        ProverOptions.GENERATE_MODELS,
                        ProverOptions.GENERATE_UNSAT_CORE)) {

            FormulaManagerView fmgr = this.solver.getFormulaManager();
            BooleanFormulaManager bmgr = fmgr.getBooleanFormulaManager();

            // Ask solver for the first set of Values
            try (Model constraints = solvePathConstrains(pTarget.getFormula(), prover)) {
                preloadedValues.add(computePreloadValues(constraints));
            } catch (SolverException ex) {
                this.logger.log(Level.WARNING, "Could not solve formula.");
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
        boolean isUnsat = pProver.isUnsat();
        if (isUnsat){
            throw new SolverException("Formula is unsat");
        }
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

            // TODO this prevents values passed to functions beeing computed
            if (!name.startsWith("__VERIFIER_nondet_")) {
                continue;
            }

            Value value = utils.toValue(assignment.getValue());
            logger.log(Level.INFO, "Loaded Value", name, value);
            values.add(assignment);
        }
        return values;
    }
}
