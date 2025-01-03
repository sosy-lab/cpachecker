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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaToCVisitor;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.SolverContextFactory.Solvers;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.QuantifiedFormulaManager.Quantifier;
import org.sosy_lab.java_smt.api.SolverException;

public class QuantiferEliminationRefiner implements Refiner {
  private final FormulaContext context;
  private PathFormula exclusionFormula;
  private final Solver quantifierSolver;
  private int currentRefinementIteration = 0;
  private final SSAMapBuilder ssaBuilder;

  public QuantiferEliminationRefiner(
      FormulaContext pContext, SSAMapBuilder pSsaBuilder)
      throws InvalidConfigurationException, CPATransferException, InterruptedException {
    context = pContext;
    exclusionFormula = context.getManager().makeEmptyPathFormula();
    quantifierSolver = Solver.create(
        Configuration.builder().copyFrom(context.getConfiguration())
            .setOption("solver.solver", Solvers.Z3.name())
            .build()
        ,
        context.getLogger(), context.getShutdownNotifier());
    ssaBuilder = pSsaBuilder;

  }

  @Override
  public PathFormula refine(CounterexampleInfo cex)
      throws CPATransferException, InterruptedException, SolverException {

    context.getLogger().log(Level.INFO,
        "******************************** Refinement ********************************");

    PathFormula cexFormula =
        context.getManager().makeFormulaForPath(cex.getTargetPath().getFullPath());
    context.getLogger().log(Level.INFO,
        String.format("Iteration %d: Current CEX FORMULA: %s \n", currentRefinementIteration,
            cexFormula.getFormula()));

    mapNonDetToOriginalNames(cexFormula, context.getSolver());

    setupSSAMap(cexFormula);

    // translate to Z3
    BooleanFormula translatedFormula =
        translateFormula(cexFormula.getFormula(), context.getSolver(), quantifierSolver);

    // TODO handle modulo operations through string manipulation

    BooleanFormula eliminatedByQuantifier = eliminateVariables(
        translatedFormula,
        // this predicate filters out variables (keys) that include "_nondet" in their names
        entry -> !entry.getKey().contains("_nondet"),
        // this predicate keeps the non-det variables
        entry -> entry.getKey().contains("_nondet"));

    // translate back to MATHSAT
    eliminatedByQuantifier =
        translateFormula(eliminatedByQuantifier, quantifierSolver, context.getSolver());

    updateExclusionFormula(eliminatedByQuantifier, cexFormula);

    String formattedErrorCondition =
        formatErrorCondition(exclusionFormula.getFormula(), context.getSolver());
    context.getLogger().log(Level.INFO,
        String.format("Iteration %d: Error Condition in this iteration: %s \n",
            currentRefinementIteration,
            formattedErrorCondition));
    return exclusionFormula;
  }

  private void updateExclusionFormula(
      BooleanFormula eliminatedByQuantifier,
      PathFormula cexFormula) {
    //update exclusion formula with the new quantified variables from this iteration.
    exclusionFormula = context.getManager().makeAnd(exclusionFormula,
            context.getSolver().getFormulaManager().getBooleanFormulaManager().not(
                eliminatedByQuantifier))
        .withContext(ssaBuilder.build(), cexFormula.getPointerTargetSet());

    context.getLogger().log(Level.INFO,
        String.format("Iteration %d: Updated Exclusion Formula: %s \n", currentRefinementIteration,
            exclusionFormula.getFormula()));
  }

  private String formatErrorCondition(BooleanFormula formula, Solver solver)
      throws InterruptedException {
    FormulaToCVisitor visitor = new FormulaToCVisitor(solver.getFormulaManager(), id -> id);
    BooleanFormula simplifiedFormula = solver.getFormulaManager().simplify(formula);
    solver.getFormulaManager().visit(simplifiedFormula, visitor);

    String rawFormula = visitor.getString();

    // Clean up nondet annotations
    for (Map.Entry<String, String> entry : variableMapping.entrySet()) {
      String ssaVariable = entry.getKey();
      String originalName = entry.getValue().replace("main::", "");
      rawFormula = rawFormula.replace(ssaVariable, originalName);
    }

    return rawFormula;
  }

  private void setupSSAMap(PathFormula cexFormula) {
    for (String variable : cexFormula.getSsa().allVariables()) {
      if (!ssaBuilder.build().containsVariable(variable)) {
        ssaBuilder.setIndex(variable, cexFormula.getSsa().getType(variable), 1);
      }
    }
  }

  private BooleanFormula translateFormula(
      BooleanFormula formula,
      Solver thisSolver,
      Solver otherSolver) {
    // formula translation between solvers, e.g. MATHSAT5 to Z3 or vice versa.
    BooleanFormula translatedFormula = otherSolver.getFormulaManager().translateFrom(
        formula, thisSolver.getFormulaManager());
    context.getLogger().log(Level.INFO,
        String.format("Iteration %d: Translated Formula:\n%s \n", currentRefinementIteration,
            translatedFormula));
    return translatedFormula;
  }

  // eliminate variables matching a predicate (Quantifier Elimination)
  private BooleanFormula eliminateVariables(
      BooleanFormula translatedFormula,
      Predicate<Entry<String, Formula>> deterministicVariablesPredicate,
      Predicate<Entry<String, Formula>> nonDetVariablesPredicate
  ) throws InterruptedException, SolverException {

    context.getLogger().log(Level.INFO,
        "******************************** Quantifier Elimination ********************************");

    Map<String, Formula> formulaNameToFormulaMap =
        quantifierSolver.getFormulaManager().extractVariables(translatedFormula);
    context.getLogger().log(Level.INFO,
        String.format("Iteration %d: formulaNameToFormulaMap: %s", currentRefinementIteration,
            formulaNameToFormulaMap.entrySet()));

    ImmutableList<Formula> eliminate = FluentIterable.from(formulaNameToFormulaMap.entrySet())
        .filter(deterministicVariablesPredicate::test)
        .transform(Entry::getValue)
        .toList();

    ImmutableList<Formula> nonDetVariables = FluentIterable.from(formulaNameToFormulaMap.entrySet())
        .filter(nonDetVariablesPredicate::test)
        .transform(Entry::getValue)
        .toList();


    context.getLogger().log(Level.INFO,
        String.format("Iteration %d: Deterministic variables to eliminate: %s",
            currentRefinementIteration,
            eliminate));
    context.getLogger().log(Level.INFO,
        String.format("Iteration %d: Non-Deterministic variables : %s", currentRefinementIteration,
            nonDetVariables));

    BooleanFormula quantified = quantifierSolver.getFormulaManager().getQuantifiedFormulaManager()
        .mkQuantifier(Quantifier.EXISTS, eliminate, translatedFormula);
    context.getLogger().log(Level.INFO,
        String.format("Iteration %d: Quantified non-deterministic variables: \n%s",
            currentRefinementIteration,
            quantified));

    return quantifierSolver.getFormulaManager().getQuantifiedFormulaManager()
        .eliminateQuantifiers(quantified);
  }


  private final Map<String, String> variableMapping = new HashMap<>();

  private void mapNonDetToOriginalNames(
      PathFormula cexFormula,
      Solver solver) {
    List<String> cexVarNames = new ArrayList<>(
        solver.getFormulaManager().extractVariableNames(cexFormula.getFormula()));

    // Map SSA variable names to original names (e.g., `__VERIFIER_int!2@` -> `x`)
    for (String cexVarName : cexVarNames) {
      if (cexVarName.contains("_nondet") && !variableMapping.containsKey(cexVarName)) {
        int index = cexVarNames.indexOf(cexVarName);
        variableMapping.put(cexVarName, cexVarNames.get(index - 1));
      }
    }
    context.getLogger()
        .log(Level.INFO, String.format("Iteration %d: CEX ALL VARS: %s", currentRefinementIteration,
            cexFormula.getSsa().allVariables()));
    context.getLogger().log(Level.INFO,
        String.format("Iteration %d: CEX EXTRACTED VAR NAMES: %s", currentRefinementIteration,
            solver.getFormulaManager().extractVariableNames(
                cexFormula.getFormula())));
    context.getLogger().log(Level.INFO,
        String.format("Iteration %d: CEX Non-Det Variables Mapping: %s", currentRefinementIteration,
            variableMapping));
  }

  public Map<String, String> getVariableMapping() {
    return variableMapping;
  }

  public PathFormula getExclusionFormula() {
    return exclusionFormula;
  }

  public void increaseRefinementIteration() {
    currentRefinementIteration++;
  }
}
