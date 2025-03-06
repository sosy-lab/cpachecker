// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.preciseErrorCondition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.sosy_lab.cpachecker.util.cwriter.FormulaToCExpressionConverter;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaToCVisitor;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class ErrorConditionFormatter {
  private final FormulaContext context;
  private final Solver solver;
  private final Map<String, String> variableMapping = new HashMap<>();
  private final SSAMapBuilder ssaBuilder;

  public ErrorConditionFormatter(FormulaContext pContext) {
    context = pContext;
    solver = pContext.getSolver();
    ssaBuilder = SSAMap.emptySSAMap().builder();

  }


  private void mapNonDetToOriginalNames(PathFormula cexFormula, int currentRefinementIteration) {
    List<String> cexVarNames = new ArrayList<>(
        solver.getFormulaManager().extractVariableNames(cexFormula.getFormula()));

    // Map SSA variable names to original names (e.g., `__VERIFIER_int!2@` -> `x`)
    for (String cexVarName : cexVarNames) {
      if (cexVarName.contains("_nondet") && !variableMapping.containsKey(cexVarName)) {
        int index = cexVarNames.indexOf(cexVarName);
        variableMapping.put(cexVarName, cexVarNames.get(index - 1));
      }
    }

    Utility.logWithIteration(currentRefinementIteration,
        Level.FINE, context, String.format("CEX Non-Det Variables Mapping:\n%s", variableMapping));
  }

  private void formatErrorCondition(BooleanFormula exclusionFormula, int currentRefinementIteration)
      throws InterruptedException {
    FormulaToCExpressionConverter exprConverter =
        new FormulaToCExpressionConverter(solver.getFormulaManager());
    String cExpr = exprConverter.formulaToCExpression(exclusionFormula);

    FormulaToCVisitor visitor = new FormulaToCVisitor(solver.getFormulaManager(), id -> id);
    solver.getFormulaManager().visit(exclusionFormula, visitor);
    String visitedFormula = visitor.getString();

    // Clean up nondet annotations
    if (!variableMapping.isEmpty()) {
      for (Map.Entry<String, String> entry : variableMapping.entrySet()) {
        String ssaVariable = entry.getKey();
        String originalName = entry.getValue().replace("main::", "");
        visitedFormula = visitedFormula.replace(ssaVariable, originalName);
        cExpr = cExpr.replace(ssaVariable, originalName);
      }
    }

    Utility.logWithIteration(currentRefinementIteration,
        Level.INFO, context,
        String.format("Formatted Exclusion Formula: %s", visitedFormula));
    Utility.logWithIteration(currentRefinementIteration,
        Level.INFO, context,
        String.format("Formatted Exclusion Formula Represented As C Expression : \n%s", cExpr));

  }

  public void reformat(
      PathFormula cexFormula,
      BooleanFormula exclusionFormula,
      int currentRefinementIteration)
      throws InterruptedException {
    mapNonDetToOriginalNames(cexFormula, currentRefinementIteration);
    formatErrorCondition(exclusionFormula, currentRefinementIteration);
  }

  public List<String> visitBooleanFormulaList(
      List<BooleanFormula> pAtoms) {
    FormulaToCVisitor visitor =
        new FormulaToCVisitor(context.getSolver().getFormulaManager(), id -> id);
    ArrayList<String> atomsAsStrings = new ArrayList<>(pAtoms.size());
    for (BooleanFormula atom : pAtoms) {
      PathFormula newPath = context.getManager().makeEmptyPathFormula();
      context.getSolver().getFormulaManager()
          .visit(newPath.withFormula(atom).getFormula(), visitor);
      String visitedFormula = visitor.getString();
      atomsAsStrings.add(visitedFormula);
    }
    return atomsAsStrings;
  }

  public String visitBooleanFormula(BooleanFormula bFormula) {
    FormulaToCVisitor visitor =
        new FormulaToCVisitor(context.getSolver().getFormulaManager(), id -> id);
    PathFormula newPath = context.getManager().makeEmptyPathFormula();
    context.getSolver().getFormulaManager()
        .visit(newPath.withFormula(bFormula).getFormula(), visitor);
    return visitor.getString();
  }

  public SSAMapBuilder getSsaBuilder() {
    return ssaBuilder;
  }

  public void setupSSAMap(PathFormula cexFormula) {
    for (String variable : cexFormula.getSsa().allVariables()) {
      if (!ssaBuilder.build().containsVariable(variable)) {
        ssaBuilder.setIndex(variable, cexFormula.getSsa().getType(variable), 1);
      }
    }
  }
}

