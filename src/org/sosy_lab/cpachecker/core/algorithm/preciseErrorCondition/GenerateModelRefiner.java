// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.preciseErrorCondition;

import com.google.common.collect.ImmutableSet;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
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

import java.util.logging.Level;

public class GenerateModelRefiner implements Refiner {

  private final FormulaContext context;
  private PathFormula exclusionModelFormula;
  private Solver solver;
  private ProverEnvironment proverEnv;

  public GenerateModelRefiner(FormulaContext pContext) throws InvalidConfigurationException {
    context = pContext;
    exclusionModelFormula = context.getManager().makeEmptyPathFormula();
    solver = Solver.create(context.getConfiguration(), context.getLogger(), context.getShutdownNotifier());
    proverEnv = solver.newProverEnvironment(ProverOptions.GENERATE_MODELS);
  }

  @Override
  public PathFormula refine(CounterexampleInfo cex)
      throws SolverException, InterruptedException, CPATransferException {
    BooleanFormulaManager bmgr = solver.getFormulaManager().getBooleanFormulaManager();
    BooleanFormula nondetModel = bmgr.makeTrue();
    ImmutableSet.Builder<String> nondetVariables = ImmutableSet.builder();

    try (ProverEnvironment prover = proverEnv) {
      BooleanFormula formula =
          context.getManager().makeFormulaForPath(cex.getTargetPath().getFullPath()).getFormula();
      prover.push(formula);

      if (prover.isUnsat()) {
        context.getLogger().log(Level.WARNING, "Counterexample is infeasible.");
        return exclusionModelFormula; // empty
      }

      for (ValueAssignment assignment : prover.getModelAssignments()) {
        if (assignment.getName().contains("_nondet")) {
          nondetModel = bmgr.and(nondetModel, assignment.getAssignmentAsFormula());
          nondetVariables.add(assignment.getName());
        }
      }

      // Update exclusion formula
      exclusionModelFormula =
          context.getManager().makeAnd(exclusionModelFormula, bmgr.not(nondetModel));
      System.out.printf(nondetVariables.build() + "\n");
      context.getLogger().log(Level.INFO,
          "Updated exclusion formula with precondition: " + exclusionModelFormula.getFormula());
    }

    return exclusionModelFormula;
  }
}
