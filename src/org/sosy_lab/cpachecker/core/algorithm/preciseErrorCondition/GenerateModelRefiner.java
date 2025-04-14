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
import org.sosy_lab.cpachecker.core.algorithm.preciseErrorCondition.RefinementResult.RefinementStatus;
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
  private final Boolean withFormatter;
  private final RefinementResult exclusionModelFormula;
  private int currentRefinementIteration = 0;

  public GenerateModelRefiner(FormulaContext pContext, Boolean pWithFormatter)
      throws InvalidConfigurationException {
    context = pContext;
    exclusionModelFormula =
        new RefinementResult(RefinementStatus.EMPTY, context.getManager().makeEmptyPathFormula());
    solver = pContext.getSolver();
    withFormatter = pWithFormatter;
    formatter = new ErrorConditionFormatter(pContext);
  }

  @Override
  public RefinementResult refine(CounterexampleInfo cex)
      throws SolverException, InterruptedException, CPATransferException {
    BooleanFormulaManager bmgr = solver.getFormulaManager().getBooleanFormulaManager();
    BooleanFormula nondetModel = bmgr.makeTrue();

    try (ProverEnvironment prover = solver.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
      PathFormula cexFormula = exclusionModelFormula.getFormula();
      for (CFAEdge cfaEdge : cex.getTargetPath().getFullPath()) {
        cexFormula = context.getManager().makeAnd(cexFormula, cfaEdge);
      }

      prover.push(cexFormula.getFormula());
      // this condition should always be false, since we work with concrete
      // satisfiable counterexamples
      if (prover.isUnsat()) {
        Utility.logWithIteration(currentRefinementIteration,
            Level.WARNING, context, "Counterexample Is Infeasible.");
        exclusionModelFormula.updateStatus(RefinementStatus.FAILURE);
        return exclusionModelFormula; // empty
      }

      Utility.logWithIteration(currentRefinementIteration,
          Level.FINE, context,
          String.format("Current CEX FORMULA:\n%s", cexFormula.getFormula()));


      for (ValueAssignment assignment : prover.getModelAssignments()) {
        if (assignment.getName().contains("_nondet")) {
          nondetModel = bmgr.and(nondetModel, assignment.getAssignmentAsFormula());
        }
      }

      Utility.logWithIteration(currentRefinementIteration,
          Level.INFO, context,
          String.format("Found Model In Current Iteration:\n%s", nondetModel));

      Utility.logWithIteration(currentRefinementIteration,
          Level.INFO, context, "Updating Exclusion Formula With Model...");

      formatter.setupSSAMap(cexFormula);
      // Update exclusion formula
      PathFormula updatedFormula = context.getManager()
          .makeAnd(exclusionModelFormula.getFormula(), bmgr.not(nondetModel))
          .withContext(formatter.getSsaBuilder().build(), cexFormula.getPointerTargetSet());

      exclusionModelFormula.updateFormula(updatedFormula);
      exclusionModelFormula.updateStatus(RefinementStatus.SUCCESS);

      Utility.logWithIteration(currentRefinementIteration,
          Level.INFO, context, String.format("Exclusion Formula In This Iteration: \n%s",
              exclusionModelFormula.getBooleanFormula()));

      if (withFormatter) {
        formatter.reformat(cexFormula, exclusionModelFormula.getBooleanFormula(),
            currentRefinementIteration);
      }
    }
    currentRefinementIteration++;
    return exclusionModelFormula;
  }
}