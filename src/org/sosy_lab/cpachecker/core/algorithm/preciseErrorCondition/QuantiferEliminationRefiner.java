// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.preciseErrorCondition;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.logging.Level;
import javax.annotation.Nonnull;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView.FormulaTransformationVisitor;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.SolverContextFactory.Solvers;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FunctionDeclaration;
import org.sosy_lab.java_smt.api.QuantifiedFormulaManager.Quantifier;
import org.sosy_lab.java_smt.api.SolverException;

public class QuantiferEliminationRefiner implements Refiner {
  private final FormulaContext context;
  private final Solver solver;
  private final Solver quantifierSolver;
  private final ErrorConditionFormatter formatter;
  private final Boolean withFormatter;
  private PathFormula exclusionFormula;
  private int currentRefinementIteration = 0;

  public QuantiferEliminationRefiner(
      FormulaContext pContext, Solvers pQuantifierSolver, Boolean pWithFormatter)
      throws InvalidConfigurationException, CPATransferException, InterruptedException {
    context = pContext;
    solver = pContext.getSolver();
    quantifierSolver = Solver.create(
        Configuration.builder().copyFrom(context.getConfiguration())
            .setOption("solver.solver", pQuantifierSolver.name())
            .build(), context.getLogger(), context.getShutdownNotifier());
    exclusionFormula = context.getManager().makeEmptyPathFormula();
    withFormatter = pWithFormatter;
    formatter = new ErrorConditionFormatter(pContext);
  }

  @Override
  public PathFormula refine(CounterexampleInfo cex)
      throws CPATransferException, InterruptedException, SolverException {

    PathFormula cexFormula =
        context.getManager().makeFormulaForPath(cex.getTargetPath().getFullPath());

    ECUtilities.logWithIteration(currentRefinementIteration,
        Level.FINE, context,
        String.format("Current CEX FORMULA: \n%s", cexFormula.getFormula()));

    // translate to Z3
    BooleanFormula translatedFormula =
        translateFormula(cexFormula.getFormula(), solver, quantifierSolver);

    // Quantifier Elimination
    BooleanFormula quantifierEliminationResult = eliminateVariables(
        translatedFormula,
        // this predicate filters out irrelevant variables
        entry -> !entry.getKey().contains("_nondet"),
        // this predicate keeps the non-det variables
        entry -> entry.getKey().contains("_nondet"));

    // translate back to MATHSAT
    quantifierEliminationResult =
        translateFormula(quantifierEliminationResult, quantifierSolver, solver);

    updateExclusionFormula(quantifierEliminationResult, cexFormula);

    if (withFormatter) {
      formatter.reformat(cexFormula, exclusionFormula.getFormula(), currentRefinementIteration);
    }
    currentRefinementIteration++;
    return exclusionFormula;
  }

  private BooleanFormula translateFormula(
      BooleanFormula formula,
      Solver thisSolver,
      Solver otherSolver) {
    // formula translation between solvers, e.g. MATHSAT5 to Z3 or vice versa.
    ECUtilities.logWithIteration(currentRefinementIteration,
        Level.INFO, context,
        String.format("Translating Formula From %s To %s.", thisSolver.getSolverName(),
            otherSolver.getSolverName()));

    BooleanFormula translatedFormula = otherSolver.getFormulaManager().translateFrom(
        formula, thisSolver.getFormulaManager());
    ECUtilities.logWithIteration(currentRefinementIteration,
        Level.FINE, context,
        String.format("Translated Formula:\n%s", translatedFormula));

    return translatedFormula;
  }


  // eliminate variables (Quantifier Elimination)
  private BooleanFormula eliminateVariables(
      BooleanFormula translatedFormula,
      Predicate<Entry<String, Formula>> deterministicVariablesPredicate,
      Predicate<Entry<String, Formula>> nonDetVariablesPredicate
  ) throws InterruptedException, SolverException {

    context.getLogger().log(Level.INFO,
        "******************************** Quantifier Elimination ********************************");

    Map<String, Formula> formulaNameToFormulaMap =
        quantifierSolver.getFormulaManager().extractVariables(translatedFormula);

    ImmutableList<Formula> irrelevantVariables =
        FluentIterable.from(formulaNameToFormulaMap.entrySet())
            .filter(deterministicVariablesPredicate::test)
            .transform(Entry::getValue)
            .toList();

    ImmutableList<Formula> nondetVariables = FluentIterable.from(formulaNameToFormulaMap.entrySet())
        .filter(nonDetVariablesPredicate::test)
        .transform(Entry::getValue)
        .toList();

    ECUtilities.logWithIteration(currentRefinementIteration,
        Level.FINER, context,
        String.format("Deterministic Variables:\n%s", irrelevantVariables));
    ECUtilities.logWithIteration(currentRefinementIteration,
        Level.FINER, context,
        String.format("Non-Deterministic Variables:\n%s", nondetVariables));

    ECUtilities.logWithIteration(currentRefinementIteration,
        Level.INFO, context, "Quantifying Formula...");

    BooleanFormula quantifiedFormula = quantifierSolver
        .getFormulaManager()
        .getQuantifiedFormulaManager()
        .mkQuantifier(Quantifier.EXISTS, irrelevantVariables, translatedFormula);

    ECUtilities.logWithIteration(currentRefinementIteration,
        Level.FINE, context,
        String.format("Quantified Formula:\n%s", quantifiedFormula));

    ECUtilities.logWithIteration(currentRefinementIteration,
        Level.FINER, context,
        String.format("Visited Quantified Formula:\n%s",
            ECUtilities.visit(new ArrayList<>(List.of(quantifiedFormula)), solver, context)
                .get(0)));

    ECUtilities.logWithIteration(currentRefinementIteration,
        Level.INFO, context, "Eliminating Quantifiers...");

    BooleanFormula eliminationResult = quantifierSolver
        .getFormulaManager()
        .getQuantifiedFormulaManager()
        .eliminateQuantifiers(quantifiedFormula);

    ECUtilities.logWithIteration(currentRefinementIteration,
        Level.FINE, context,
        String.format("Quantifier Elimination Result: \n%s", eliminationResult));

    ECUtilities.logWithIteration(currentRefinementIteration,
        Level.FINER, context,
        String.format("Visited Elimination Result:\n%s",
            ECUtilities.visit(new ArrayList<>(List.of(eliminationResult)), solver, context)
                .get(0)));


    ECUtilities.logWithIteration(currentRefinementIteration,
        Level.INFO, context, "Handling Modulo Operator...");
    // handle modulo operation before translation back to MATHSAT5
    BooleanFormula fixedEliminationResult = handleModuloOp(eliminationResult);

    ECUtilities.logWithIteration(currentRefinementIteration,
        Level.FINE, context,
        String.format("Modified Result After Modulo Fix: \n%s", fixedEliminationResult));

    ECUtilities.logWithIteration(currentRefinementIteration,
        Level.FINER, context,
        String.format("Visited Result After Modulo Fix:\n%s",
            ECUtilities.visit(new ArrayList<>(List.of(fixedEliminationResult)), solver, context)
                .get(0)));

    return fixedEliminationResult;
  }

  /*
  Traverse the entire formula, and replace encountered modulo op 'bvsrem_i' with 'bvsrem_32'
   while keeping the same arguments
   */
  @Nonnull
  private BooleanFormula handleModuloOp(BooleanFormula formula) {
    FormulaManagerView formulaManager = quantifierSolver.getFormulaManager();

    FormulaTransformationVisitor formulaVisitor =
        new FormulaTransformationVisitor(formulaManager) {
          @Override
          public Formula visitFunction(
              Formula f, List<Formula> args, FunctionDeclaration<?> functionDeclaration) {
            // check if the function name is "bvsrem_i"
            if ("bvsrem_i".equals(functionDeclaration.getName())) {
              // replace "bvsrem_i" with "bvsrem" while keeping the arguments unchanged
              FunctionDeclaration<?> newFunctionDeclaration =
                  formulaManager.getFunctionFormulaManager().declareUF(
                      "bvsrem_32", functionDeclaration.getType(),
                      functionDeclaration.getArgumentTypes());
              return formulaManager.getFunctionFormulaManager()
                  .callUF(newFunctionDeclaration, args);
            }
            return super.visitFunction(f, args, functionDeclaration);
          }
        };

    // transform the formula recursively using the visitor
    return formulaManager.transformRecursively(formula, formulaVisitor);
  }

  private void updateExclusionFormula(
      BooleanFormula quantifierEliminationResult,
      PathFormula cexFormula) {
    formatter.setupSSAMap(cexFormula);

    ECUtilities.logWithIteration(currentRefinementIteration,
        Level.INFO, context, "Updating Exclusion Formula...");

    //update exclusion formula with the new quantified variables from this iteration.
    exclusionFormula = context.getManager()
        .makeAnd(exclusionFormula,
            solver.getFormulaManager().getBooleanFormulaManager().not(quantifierEliminationResult))
        .withContext(formatter.getSsaBuilder().build(), cexFormula.getPointerTargetSet());

    ECUtilities.logWithIteration(currentRefinementIteration,
        Level.FINE, context,
        String.format("Updated Exclusion Formula: \n%s", exclusionFormula.getFormula()));

    ECUtilities.logWithIteration(currentRefinementIteration,
        Level.FINER, context,
        String.format("Visited Updated Exclusion Formula: \n%s",
            ECUtilities.visit(new ArrayList<>(List.of(exclusionFormula.getFormula())), solver,
                    context)
                .get(0)));
  }

}