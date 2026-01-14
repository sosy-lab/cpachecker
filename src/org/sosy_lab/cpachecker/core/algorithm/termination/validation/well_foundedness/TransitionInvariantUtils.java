// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.termination.validation.well_foundedness;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
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

  /**
   * Enum representing the SSA indices of the current states that we use for different states when
   * constructing formulas. The indices are used for renaming only with instantiation so the
   * concrete value will not have an impact on the instantiation, as all of them are bigger than 1.
   * (except for -1) The special enum value INDEX_LATEST is used to get the latest SSA index from
   * path formula. The names of the enum values correspond to the names of the states described in
   * the documentation of DecreasingCardinalityChecker and TerminationWitnessValidator.
   */
  public enum CurrStateIndices {
    INDEX_LATEST(-1), // Used to get the latest indices from the path formula
    INDEX_MIDDLE(1),
    INDEX_S(2),
    INDEX_S_PRIME(3),
    INDEX_S1(4),
    INDEX_S2(5);

    private final int index;

    CurrStateIndices(int pIndex) {
      index = pIndex;
    }

    public int getIndex() {
      return index;
    }
  }

  /**
   * Enum representing the SSA indices of the previous states that we use for different states when
   * constructing formulas. The names of the enum values correspond to the names of the states
   * described in the documentation of DecreasingCardinalityChecker and TerminationWitnessValidator.
   */
  public enum PrevStateIndices {
    INDEX_FIRST(1),
    INDEX_S(6),
    INDEX_S_PRIME(7),
    INDEX_S1(8),
    INDEX_S2(9);

    private final int index;

    PrevStateIndices(int pIndex) {
      index = pIndex;
    }

    public int getIndex() {
      return index;
    }
  }

  public static boolean isPrevVariable(
      String pVariable, ImmutableMap<CSimpleDeclaration, CSimpleDeclaration> pMapPrevToCurrVars) {
    return pMapPrevToCurrVars.keySet().stream()
        .anyMatch(d -> d.getName().equals(removeFunctionFromVarsName(pVariable)));
  }

  public static CSimpleDeclaration getPrevDeclaration(
      String pVariable, ImmutableMap<CSimpleDeclaration, CSimpleDeclaration> pMapPrevToCurrVars) {
    return Iterables.getOnlyElement(
        pMapPrevToCurrVars.keySet().stream()
            .filter(d -> d.getName().equals(removeFunctionFromVarsName(pVariable)))
            .collect(ImmutableSet.toImmutableSet()));
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
   * @param prevStateIndex to which should the __PREV variables be instantiated
   * @param currStateIndex to which should the normal variables be instantiated
   * @return instantiated ssaMap
   */
  public static SSAMap setIndicesToDifferentValues(
      Formula pFormula,
      PrevStateIndices prevStateIndex,
      CurrStateIndices currStateIndex,
      FormulaManagerView fmgr,
      Scope scope,
      ImmutableMap<CSimpleDeclaration, CSimpleDeclaration> pMapPrevToCurrVars) {
    SSAMapBuilder builder = SSAMap.emptySSAMap().builder();
    int currIndex = currStateIndex.getIndex();
    int prevIndex = prevStateIndex.getIndex();

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
   * @return the formula with terms like x__PREV@1 <==> x@2
   */
  public static BooleanFormula makeStatesEquivalent(
      BooleanFormula pPrevFormula,
      BooleanFormula pCurrFormula,
      BooleanFormulaManagerView bfmgr,
      FormulaManagerView fmgr,
      ImmutableMap<CSimpleDeclaration, CSimpleDeclaration> pMapPrevVarsToCurr) {

    BooleanFormula equivalence = bfmgr.makeTrue();
    Map<String, Formula> prevMapNamesToVars = fmgr.extractVariables(pPrevFormula);
    Map<String, Formula> currMapNamesToVars = fmgr.extractVariables(pCurrFormula);

    for (Map.Entry<String, Formula> entry : prevMapNamesToVars.entrySet()) {
      String prevVarPure =
          removeFunctionFromVarsName(
              fmgr.extractVariables(fmgr.uninstantiate(entry.getValue())).keySet().stream()
                  .findAny()
                  .orElseThrow());
      String prevVar = entry.getKey();
      if (isPrevVariable(prevVarPure, pMapPrevVarsToCurr)) {
        String currVar =
            pMapPrevVarsToCurr.get(getPrevDeclaration(prevVarPure, pMapPrevVarsToCurr)).getName();
        for (Map.Entry<String, Formula> entry2 : currMapNamesToVars.entrySet()) {
          String currVarPure =
              removeFunctionFromVarsName(
                  fmgr.extractVariables(fmgr.uninstantiate(entry2.getValue())).keySet().stream()
                      .findAny()
                      .orElseThrow());
          if (currVar.equals(currVarPure)) {
            currVar = entry2.getKey();
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
