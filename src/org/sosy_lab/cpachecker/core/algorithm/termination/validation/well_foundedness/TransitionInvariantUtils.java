// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.termination.validation.well_foundedness;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.parser.Scope;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.cwriter.FormulaToCExpressionConverter;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.SolverException;

public class TransitionInvariantUtils {

  public static boolean isPrevVariable(
      String pVariable, ImmutableMap<CSimpleDeclaration, CSimpleDeclaration> pMapPrevToCurrVars) {
    return pMapPrevToCurrVars.keySet().stream()
        .anyMatch(d -> d.getName().equals(removeFunctionFromVarsName(pVariable)));
  }

  public static CSimpleDeclaration getPrevDeclaration(
      String pVariable, ImmutableMap<CSimpleDeclaration, CSimpleDeclaration> pMapPrevToCurrVars) {
    return pMapPrevToCurrVars.keySet().stream()
        .filter(d -> d.getName().equals(removeFunctionFromVarsName(pVariable)))
        .findAny()
        .orElseThrow();
  }

  public static String transformFormulaToStringWithTrivialReplacement(
      BooleanFormula pFormula, BooleanFormulaManagerView bfmgr, FormulaManagerView fmgr)
      throws CPAException {
    FormulaToCExpressionConverter converter = new FormulaToCExpressionConverter(fmgr);
    if (bfmgr.isTrue(pFormula)) {
      return "1";
    } else if (bfmgr.isFalse(pFormula)) {
      return "0";
    }
    try {
      return converter.formulaToCExpression(pFormula);
    } catch (SolverException | InterruptedException e) {
      throw new CPAException("It was not possible to translate invariant to CExpression.");
    }
  }

  /**
   * Constructs a new SSAMap, where the __PREV variables and normal variables are instantiated
   * differently.
   *
   * @param pFormula given on input
   * @param prevIndex to which should the __PREV variables be instantiated
   * @param currIndex to which should the normal variables be instantiated
   * @return instantiated ssaMap
   */
  public static SSAMap setIndicesToDifferentValues(
      Formula pFormula,
      int prevIndex,
      int currIndex,
      FormulaManagerView fmgr,
      Scope scope,
      ImmutableMap<CSimpleDeclaration, CSimpleDeclaration> pMapPrevToCurrVars) {
    SSAMapBuilder builder = SSAMap.emptySSAMap().builder();
    for (String var : fmgr.extractVariableNames(pFormula)) {
      if (currIndex < 0 && !isPrevVariable(var, pMapPrevToCurrVars)) {
        continue;
      }
      builder.setIndex(
          var,
          scope.lookupVariable(var).getType(),
          isPrevVariable(var, pMapPrevToCurrVars) ? prevIndex : currIndex);
    }
    return builder.build();
  }

  /**
   * Constructs formulas to make some states equivalent. For example, s' occurs in the formulas both
   * in T(.,s') and T(s',.), so we have to make the corresponding variables equivalent.
   *
   * @param pPrevFormula with the previous variables (i.e. variables like x__PREV)
   * @param pCurrFormula with the current variables (i.e. variables like x)
   * @param prevIndex the index of the variables from the previous state
   * @param currIndex the index of the current variables
   * @return the formula with terms like x__PREV@1 <==> x@2
   */
  public static BooleanFormula makeStatesEquivalent(
      BooleanFormula pPrevFormula,
      BooleanFormula pCurrFormula,
      int prevIndex,
      int currIndex,
      BooleanFormulaManagerView bfmgr,
      FormulaManagerView fmgr) {
    BooleanFormula equivalence = bfmgr.makeTrue();
    Map<String, Formula> prevMapNamesToVars = fmgr.extractVariables(pPrevFormula);
    Map<String, Formula> currMapNamesToVars = fmgr.extractVariables(pCurrFormula);

    for (Map.Entry<String, Formula> entry : prevMapNamesToVars.entrySet()) {
      String prevVar = entry.getKey();
      if (prevVar.contains("__PREV") && prevVar.contains("@" + prevIndex)) {
        String prevVarPure = prevVar.replace("__PREV", "");
        prevVarPure = prevVarPure.replace("@" + prevIndex, "");
        prevVarPure = removeFunctionFromVarsName(prevVarPure);
        String currVar = "";
        for (String var : currMapNamesToVars.keySet()) {
          if (removeFunctionFromVarsName(var.replace("@" + currIndex, "")).equals(prevVarPure)) {
            currVar = var;
            break;
          }
        }
        if (!currVar.isEmpty()) {
          equivalence =
              fmgr.makeAnd(
                  equivalence,
                  fmgr.makeEqual(prevMapNamesToVars.get(prevVar), currMapNamesToVars.get(currVar)));
        }
      }
    }
    return equivalence;
  }

  public static String removeFunctionFromVarsName(String pFormula) {
    return pFormula.replaceAll("\\b\\w+::", "");
  }
}
