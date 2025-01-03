// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.preciseErrorCondition;

import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.Model.ValueAssignment;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverException;

import java.util.logging.Level;

public class GenerateModelRefiner implements Refiner {

  private final FormulaContext context;
  private PathFormula exclusionFormula;

  public GenerateModelRefiner(FormulaContext pContext) {
    context = pContext;
    exclusionFormula = context.getManager().makeEmptyPathFormula();
  }

  @Override
  public PathFormula refine(CounterexampleInfo cex)
      throws SolverException, InterruptedException, CPATransferException {
    BooleanFormulaManager bmgr = context.getSolver().getFormulaManager().getBooleanFormulaManager();
    BooleanFormula precond = bmgr.makeTrue();

    try (ProverEnvironment prover = context.getProver()) {
      BooleanFormula formula =
          context.getManager().makeFormulaForPath(cex.getTargetPath().getFullPath()).getFormula();
      prover.push(formula);

      if (prover.isUnsat()) {
        context.getLogger().log(Level.WARNING, "Counterexample is infeasible.");
        return exclusionFormula;
      }

      for (ValueAssignment assignment : prover.getModelAssignments()) {
        if (assignment.getName().startsWith("__VERIFIER_nondet_")) {
          precond = bmgr.and(precond, assignment.getAssignmentAsFormula());
        }
      }

      // Update exclusion formula
      exclusionFormula = context.getManager().makeAnd(exclusionFormula, bmgr.not(precond));
      context.getLogger().log(Level.INFO,
          "Updated exclusion formula with precondition: " + exclusionFormula.getFormula());
    }

    return exclusionFormula;
  }
}
