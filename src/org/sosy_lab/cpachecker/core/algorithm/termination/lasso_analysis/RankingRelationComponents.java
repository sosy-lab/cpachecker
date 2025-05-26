// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.termination.lasso_analysis;



import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.IntegerFormulaManager;
import org.sosy_lab.java_smt.api.NumeralFormula;
import org.sosy_lab.java_smt.api.NumeralFormula.IntegerFormula;

//RankingRelationBuilder
public class RankingRelationComponents {

  private final Optional<CExpression> unprimedExpression;
  private final Optional<CExpression> primedExpression;
  private final List<NumeralFormula> unprimedFormulaSummands;
  private final List<NumeralFormula> primedFormulaSummands;
  private final IntegerFormulaManager integerFormulaManager;

  public RankingRelationComponents(
      Optional<CExpression> pUnprimedFunction,
      Optional<CExpression> pPrimedFunction,
      List<NumeralFormula> pUnprimedFormulaSummands,
      List<NumeralFormula> pPrimedFormulaSummands,
      IntegerFormulaManager pIntegerFormulaManager) {
    unprimedExpression = pUnprimedFunction;
    primedExpression = pPrimedFunction;
    unprimedFormulaSummands = pUnprimedFormulaSummands;
    primedFormulaSummands = pPrimedFormulaSummands;
    integerFormulaManager = pIntegerFormulaManager;
  }

  public Optional<CExpression> getPrimedExpression() {
    return primedExpression;
  }

  public Optional<CExpression> getUnprimedExpression() {
    return unprimedExpression;
  }

  public NumeralFormula getPrimedFormula() {
    return sum(primedFormulaSummands);
  }

  public NumeralFormula getUnprimedFormula() {
    return sum(unprimedFormulaSummands);
  }

  private NumeralFormula sum(List<NumeralFormula> operands) {
    if (operands.isEmpty()) {
      throw new IllegalArgumentException("EMPTY ERROR");
    }

    return operands.stream()
        .map(f -> (IntegerFormula) f)
        .reduce((a, b) -> integerFormulaManager.add(a, b))
        .orElseThrow();
  }

  public List<IntegerFormula> getUnprimedSummands() {
    return unprimedFormulaSummands.stream()
        .map(f -> (IntegerFormula) f)
        .toList();
  }

  public List<IntegerFormula> getPrimedSummands() {
    return primedFormulaSummands.stream()
        .map(f -> (IntegerFormula) f)
        .toList();
  }

  public static BooleanFormula computeIntegratedInvariantFormula(
      RankingRelationComponents components,
      FormulaManagerView fmgr,
      IntegerFormula zero) {

    IntegerFormulaManager ifmgr = fmgr.getIntegerFormulaManager();
    BooleanFormulaManager bfmgr = fmgr.getBooleanFormulaManager();

    IntegerFormula globalUnprimed = (IntegerFormula) components.getUnprimedFormula();
    IntegerFormula globalPrimed = (IntegerFormula) components.getPrimedFormula();

    BooleanFormula nonNegative = ifmgr.greaterOrEquals(globalUnprimed, zero);
    BooleanFormula decreaseCondition = ifmgr.lessThan(globalPrimed, globalUnprimed);

    return bfmgr.and(nonNegative, decreaseCondition);
  }

  public static RankingRelationComponents createComponentsFromSSAMap(
      SSAMap ssaMap,
      FormulaManagerView fmgr,
      IntegerFormula zero) {

    IntegerFormulaManager ifmgr = fmgr.getIntegerFormulaManager();

    List<NumeralFormula> unprimedSummands = new ArrayList<>();
    List<NumeralFormula> primedSummands = new ArrayList<>();

    for (String var : ssaMap.allVariables()) {
      int idx = ssaMap.getIndex(var);

      String renameUnprimed = renameIndex(var, idx);
      String renamePrimed = renameIndex(var, idx + 1);

      NumeralFormula unprimed = ifmgr.makeVariable(renameUnprimed);
      NumeralFormula primed = ifmgr.makeVariable(renamePrimed);
      unprimedSummands.add(unprimed);
      primedSummands.add(primed);

    }

    Optional<CExpression> unprimedExpression = Optional.empty();
    Optional<CExpression> primedExpression = Optional.empty();

    return new RankingRelationComponents(unprimedExpression, primedExpression, unprimedSummands, primedSummands,
        fmgr.getIntegerFormulaManager());
  }

  private static String renameIndex(String var, int index) {
    return var.replace("@", "_") + "_" + index;
  }

}