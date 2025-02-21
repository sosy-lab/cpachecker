// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.preciseErrorCondition;

import java.util.logging.Level;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.Model.ValueAssignment;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverContext.ProverOptions;
import org.sosy_lab.java_smt.api.SolverException;

public class GenerateModelRefiner implements Refiner {

  private final FormulaContext context;
  private final Solver solver;
  private final ErrorConditionFormatter formatter;
  private PathFormula exclusionModelFormula;
  private int currentRefinementIteration = 0;

  public GenerateModelRefiner(FormulaContext pContext) throws InvalidConfigurationException {
    context = pContext;
    exclusionModelFormula = context.getManager().makeEmptyPathFormula();
    solver = pContext.getSolver();
    formatter = new ErrorConditionFormatter(pContext);
  }

  @Override
  public PathFormula refine(CounterexampleInfo cex)
      throws SolverException, InterruptedException, CPATransferException {
    BooleanFormulaManager bmgr = solver.getFormulaManager().getBooleanFormulaManager();
    BooleanFormula nondetModel = bmgr.makeTrue();

    try (ProverEnvironment prover = solver.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
      PathFormula cexFormula = exclusionModelFormula;
      for (CFAEdge cfaEdge : cex.getTargetPath().getFullPath()) {
        cexFormula = context.getManager().makeAnd(cexFormula, cfaEdge);
      }

      prover.push(cexFormula.getFormula());

      if (prover.isUnsat()) {
        formatter.loggingWithIteration(currentRefinementIteration,
            Level.WARNING, "Counterexample Is Infeasible. Returning An Empty Formula.");
        return exclusionModelFormula; // empty
      }

      formatter.loggingWithIteration(currentRefinementIteration,
          Level.INFO, String.format("Current CEX FORMULA:\n%s", cexFormula.getFormula()));


      for (ValueAssignment assignment : prover.getModelAssignments()) {
        if (assignment.getName().contains("_nondet")) {
          nondetModel = bmgr.and(nondetModel, assignment.getAssignmentAsFormula());
        }
      }

      formatter.loggingWithIteration(currentRefinementIteration,
          Level.INFO, String.format("Non-Det Model In Current Iteration:\n%s", nondetModel));

      formatter.setupSSAMap(cexFormula);
      // Update exclusion formula
      exclusionModelFormula = context.getManager()
          .makeAnd(exclusionModelFormula, bmgr.not(nondetModel))
          .withContext(formatter.getSsaBuilder().build(), cexFormula.getPointerTargetSet());


      formatter.loggingWithIteration(currentRefinementIteration,
          Level.INFO, String.format("Updated Exclusion Formula With Precondition: \n%s",
              exclusionModelFormula.getFormula()));

      formatter.reformat(cexFormula, exclusionModelFormula.getFormula(),
          currentRefinementIteration);
    }
    currentRefinementIteration++;
    return exclusionModelFormula;
  }
}