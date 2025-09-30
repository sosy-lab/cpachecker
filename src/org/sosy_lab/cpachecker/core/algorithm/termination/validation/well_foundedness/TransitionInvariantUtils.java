// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.termination.validation.well_foundedness;

import java.util.List;
import java.util.Map;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.parser.Scope;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;

public class TransitionInvariantUtils {

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
      Formula pFormula, int prevIndex, int currIndex, FormulaManagerView fmgr, Scope scope) {
    SSAMapBuilder builder = SSAMap.emptySSAMap().builder();
    for (String var : fmgr.extractVariableNames(pFormula)) {
      if (currIndex < 0 && !var.contains("__PREV")) {
        continue;
      }
      builder.setIndex(
          var, scope.lookupVariable(var).getType(), var.contains("__PREV") ? prevIndex : currIndex);
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

  public static PathFormula makeLoopFormulaWithInitialSSAIndex(
      List<CFAEdge> pLoop, PathFormulaManager pfmgr)
      throws InterruptedException, CPATransferException {
    PathFormula loopFormula = pfmgr.makeEmptyPathFormula();
    loopFormula =
        loopFormula.withContext(
            loopFormula.getSsa().withDefault(2), PointerTargetSet.emptyPointerTargetSet());
    for (CFAEdge edge : pLoop) {
      loopFormula = pfmgr.makeAnd(loopFormula, edge);
    }
    return loopFormula;
  }
}
