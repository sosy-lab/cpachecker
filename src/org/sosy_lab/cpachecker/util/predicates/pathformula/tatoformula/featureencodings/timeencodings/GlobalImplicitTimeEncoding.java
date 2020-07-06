// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings.timeencodings;

import static com.google.common.collect.FluentIterable.from;

import java.math.BigDecimal;
import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaVariable;
import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaVariableCondition;
import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaVariableExpression;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaType;
import org.sosy_lab.java_smt.api.FormulaType.FloatingPointType;

public class GlobalImplicitTimeEncoding implements TimeEncoding {
  private static final String DELAY_VARIABLE_NAME = "#delay";
  private static final FormulaType<?> CLOCK_VARIABLE_TYPE =
      FormulaType.getFloatingPointType(15, 112);
  private final FormulaManagerView fmgr;
  private final BooleanFormulaManagerView bFmgr;

  public GlobalImplicitTimeEncoding(FormulaManagerView pFmgr) {
    fmgr = pFmgr;
    bFmgr = pFmgr.getBooleanFormulaManager();
  }

  @Override
  public BooleanFormula makeConditionFormula(
      TaDeclaration pAutomaton, int pVariableIndex, TaVariableCondition pCondition) {
    var expressions =
        from(pCondition.getExpressions())
            .transform(expr -> makeVariableExpression(expr, pVariableIndex))
            .toList();
    return bFmgr.and(expressions);
  }

  private BooleanFormula makeVariableExpression(
      TaVariableExpression expression, int pVariableIndex) {
    var variableFormula =
        fmgr.makeVariable(CLOCK_VARIABLE_TYPE, expression.getVariable().getName(), pVariableIndex);
    var constantFormula = makeRealNumber(expression.getConstant());

    switch (expression.getOperator()) {
      case GREATER:
        return fmgr.makeGreaterThan(variableFormula, constantFormula, true);
      case GREATER_EQUAL:
        return fmgr.makeGreaterOrEqual(variableFormula, constantFormula, true);
      case LESS:
        return fmgr.makeLessThan(variableFormula, constantFormula, true);
      case LESS_EQUAL:
        return fmgr.makeLessOrEqual(variableFormula, constantFormula, true);
      case EQUAL:
        return fmgr.makeEqual(variableFormula, constantFormula);
      default:
        throw new AssertionError();
    }
  }

  private Formula makeRealNumber(Number pNumber) {
    assert (pNumber instanceof BigDecimal);
    return fmgr.getFloatingPointFormulaManager()
        .makeNumber((BigDecimal) pNumber, (FloatingPointType) CLOCK_VARIABLE_TYPE);
  }

  @Override
  public BooleanFormula makeClockVariablesDoNotChangeFormula(
      TaDeclaration pAutomaton, int pVariableIndexBefore, Iterable<TaVariable> pClocks) {
    var noChange =
        from(pClocks).transform(clock -> makeUnchangedFormula(clock, pVariableIndexBefore));
    return bFmgr.and(noChange.toSet());
  }

  private BooleanFormula makeUnchangedFormula(TaVariable pVariable, int pVariableIndexBefore) {
    var newVariable =
        fmgr.makeVariable(CLOCK_VARIABLE_TYPE, pVariable.getName(), pVariableIndexBefore + 1);
    var oldVariable =
        fmgr.makeVariable(CLOCK_VARIABLE_TYPE, pVariable.getName(), pVariableIndexBefore);
    return fmgr.makeEqual(newVariable, oldVariable);
  }

  @Override
  public BooleanFormula makeResetToZeroFormula(
      TaDeclaration pAutomaton, int pVariableIndex, Iterable<TaVariable> pClocks) {
    var resets = from(pClocks).transform(clock -> makeResetFormula(clock, pVariableIndex));
    return bFmgr.and(resets.toSet());
  }

  private BooleanFormula makeResetFormula(TaVariable pVariable, int pVariableIndex) {
    var zero = fmgr.makeNumber(CLOCK_VARIABLE_TYPE, 0);
    var variable = fmgr.makeVariable(CLOCK_VARIABLE_TYPE, pVariable.getName(), pVariableIndex);
    return fmgr.makeEqual(variable, zero);
  }

  @Override
  public BooleanFormula makeInitiallyZeroFormula(TaDeclaration pAutomaton, int pVariableIndex) {
    var allClocksZero =
        from(pAutomaton.getClocks()).transform(clock -> makeResetFormula(clock, pVariableIndex));
    return bFmgr.and(allClocksZero.toSet());
  }

  @Override
  public BooleanFormula makeTimeUpdateFormula(TaDeclaration pAutomaton, int pIndexBefore) {
    var delayVariable =
        fmgr.makeVariable(CLOCK_VARIABLE_TYPE, DELAY_VARIABLE_NAME, pIndexBefore + 1);
    var clockUpdateFormulas =
        from(pAutomaton.getClocks())
            .transform(
                clock -> {
                  var newVariable =
                      fmgr.makeVariable(CLOCK_VARIABLE_TYPE, clock.getName(), pIndexBefore + 1);
                  var oldVariable =
                      fmgr.makeVariable(CLOCK_VARIABLE_TYPE, clock.getName(), pIndexBefore);
                  return fmgr.makeEqual(newVariable, fmgr.makePlus(oldVariable, delayVariable));
                });

    var zero = fmgr.makeNumber(CLOCK_VARIABLE_TYPE, 0);
    var delayLowerBound = fmgr.makeGreaterOrEqual(delayVariable, zero, true);

    return bFmgr.and(delayLowerBound, bFmgr.and(clockUpdateFormulas.toSet()));
  }

  @Override
  public BooleanFormula makeTimeDoesNotAdvanceFormula(TaDeclaration pAutomaton, int pIndexBefore) {
    return bFmgr.makeTrue();
  }
}
