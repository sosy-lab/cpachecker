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
import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaVariableExpression.Operator;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaType;
import org.sosy_lab.java_smt.api.FormulaType.FloatingPointType;

public abstract class AbstractTimeEncoding implements TimeEncoding {
  protected static final FormulaType<?> CLOCK_VARIABLE_TYPE =
      FormulaType.getFloatingPointType(15, 112);
  protected final FormulaManagerView fmgr;
  protected final BooleanFormulaManagerView bFmgr;

  public AbstractTimeEncoding(FormulaManagerView pFmgr) {
    fmgr = pFmgr;
    bFmgr = pFmgr.getBooleanFormulaManager();
  }

  @Override
  public BooleanFormula makeConditionFormula(
      TaDeclaration pAutomaton, int pVariableIndex, TaVariableCondition pCondition) {
    var expressions =
        from(pCondition.getExpressions())
            .transform(expr -> makeVariableExpression(pAutomaton, pVariableIndex, expr))
            .toList();
    return bFmgr.and(expressions);
  }

  protected abstract BooleanFormula makeVariableExpression(
      TaDeclaration pAutomaton, int pVariableIndex, TaVariableExpression expression);

  protected BooleanFormula makeVariableExpression(Formula lhs, Formula rhs, Operator op) {
    switch (op) {
      case GREATER:
        return fmgr.makeGreaterThan(lhs, rhs, true);
      case GREATER_EQUAL:
        return fmgr.makeGreaterOrEqual(lhs, rhs, true);
      case LESS:
        return fmgr.makeLessThan(lhs, rhs, true);
      case LESS_EQUAL:
        return fmgr.makeLessOrEqual(lhs, rhs, true);
      case EQUAL:
        return fmgr.makeEqual(lhs, rhs);
      default:
        throw new AssertionError();
    }
  }

  protected Formula makeRealNumber(Number pNumber) {
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
    var resets =
        from(pClocks).transform(clock -> makeResetFormula(pAutomaton, pVariableIndex, clock));
    return bFmgr.and(resets.toSet());
  }

  protected abstract BooleanFormula makeResetFormula(
      TaDeclaration pAutomaton, int pVariableIndex, TaVariable pVariable);

  @Override
  public FormulaType<?> getTimeFormulaType() {
    return CLOCK_VARIABLE_TYPE;
  }
}
