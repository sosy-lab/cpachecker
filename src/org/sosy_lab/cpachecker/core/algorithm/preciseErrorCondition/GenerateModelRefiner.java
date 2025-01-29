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
import org.sosy_lab.cpachecker.util.cwriter.FormulaToCExpressionConverter;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaToCVisitor;
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
  private int currentRefinementIteration = 0;
  private final SSAMapBuilder ssaBuilder;

  public GenerateModelRefiner(FormulaContext pContext) throws InvalidConfigurationException {
    context = pContext;
    exclusionModelFormula = context.getManager().makeEmptyPathFormula();
    solver = pContext.getSolver();
    ssaBuilder = SSAMap.emptySSAMap().builder();
  }

  @Override
  public PathFormula refine(CounterexampleInfo cex)
      throws SolverException, InterruptedException, CPATransferException {
    BooleanFormulaManager bmgr = solver.getFormulaManager().getBooleanFormulaManager();
    BooleanFormula nondetModel = bmgr.makeTrue();
    ImmutableSet.Builder<String> nondetVariables = ImmutableSet.builder();

    try (ProverEnvironment prover = solver.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
      BooleanFormula formula =
          context.getManager().makeFormulaForPath(cex.getTargetPath().getFullPath()).getFormula();
      prover.push(formula);

      if (prover.isUnsat()) {
        context.getLogger()
            .log(Level.WARNING, "Counterexample is infeasible. Returning an empty formula.");
        return exclusionModelFormula; // empty
      }

      PathFormula cexFormula =
          context.getManager().makeFormulaForPath(cex.getTargetPath().getFullPath());
      context.getLogger().log(Level.INFO,
          String.format("Iteration %d: Current CEX FORMULA: %s \n", currentRefinementIteration,
              cexFormula.getFormula()));

      setupSSAMap(cexFormula);

      for (ValueAssignment assignment : prover.getModelAssignments()) {
        if (assignment.getName().contains("_nondet")) {
          nondetModel = bmgr.and(nondetModel, assignment.getAssignmentAsFormula());
          nondetVariables.add(assignment.getName());
        }
      }

      context.getLogger().log(Level.INFO,
          String.format("Iteration %d: Non-Det Model in current iteration:\n%s.",
              currentRefinementIteration, nondetModel));

      // Update exclusion formula
      exclusionModelFormula =
          context.getManager()
              .makeAnd(exclusionModelFormula,
                  bmgr.not(nondetModel))
              .withContext(ssaBuilder.build(), cexFormula.getPointerTargetSet());
      System.out.printf(nondetVariables.build() + "\n");
      context.getLogger().log(Level.INFO,
          String.format(
              "Iteration %d: Updated exclusion formula with precondition: \n%s",
              currentRefinementIteration, exclusionModelFormula.getFormula()));
    }
    formatErrorCondition(exclusionModelFormula.getFormula(), solver);

    BooleanFormula uninstantiated =
        solver.getFormulaManager().uninstantiate(exclusionModelFormula.getFormula());

    context.getLogger().log(Level.INFO,
        String.format(
            "Iteration %d: instantiated: \n%s",
            currentRefinementIteration, uninstantiated));

    currentRefinementIteration++;
    return exclusionModelFormula;
  }

  private void setupSSAMap(PathFormula cexFormula) {
    SSAMap cexSSA = cexFormula.getSsa();
    for (String variable : cexSSA.allVariables()) {
      // Preserve the original SSA index from the counterexample's formula
      int index = cexSSA.getIndex(variable);
      ssaBuilder.setIndex(variable, cexSSA.getType(variable), index);
    }
  }

//  private void setupSSAMap(PathFormula cexFormula) {
//    for (String variable : cexFormula.getSsa().allVariables()) {
//      if (!ssaBuilder.build().containsVariable(variable)) {
//        ssaBuilder.setIndex(variable, cexFormula.getSsa().getType(variable), 1);
//      }
//    }
//  }


  private void formatErrorCondition(BooleanFormula pFormula, Solver pSolver)
      throws InterruptedException {
    FormulaToCExpressionConverter exprConverter =
        new FormulaToCExpressionConverter(pSolver.getFormulaManager());
    String c_expr = exprConverter.formulaToCExpression(pFormula);

    FormulaToCVisitor visitor = new FormulaToCVisitor(pSolver.getFormulaManager(), id -> id);
    pSolver.getFormulaManager().visit(pFormula, visitor);
    String visitedFormula = visitor.getString();


    context.getLogger().log(Level.INFO,
        String.format("Iteration %d: Converted To C Expression : \n%s \n",
            currentRefinementIteration,
            c_expr));

    context.getLogger().log(Level.INFO,
        String.format("Iteration %d: Error Condition in current iteration: \n%s \n",
            currentRefinementIteration,
            visitedFormula));
  }
}