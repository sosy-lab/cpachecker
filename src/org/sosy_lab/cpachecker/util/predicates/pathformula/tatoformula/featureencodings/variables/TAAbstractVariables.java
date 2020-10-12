// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings.variables;

import static com.google.common.collect.FluentIterable.from;

import java.math.BigInteger;
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
import org.sosy_lab.java_smt.api.FormulaType.BitvectorType;

public abstract class TAAbstractVariables implements TAVariables {
  protected final FormulaType<?> clockVariableType;
  protected final FormulaManagerView fmgr;
  protected final BooleanFormulaManagerView bFmgr;
  protected final boolean allowZeroDelay;

  public TAAbstractVariables(
      FormulaManagerView pFmgr, boolean pAllowZeroDelay, FormulaType<?> pClockVariableType) {
    fmgr = pFmgr;
    bFmgr = pFmgr.getBooleanFormulaManager();
    allowZeroDelay = pAllowZeroDelay;
    clockVariableType = pClockVariableType;
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

  protected Formula makeClockTypeNumber(Number pNumber) {
    assert (pNumber instanceof BigInteger);
    var number = (BigInteger) pNumber;
    if (clockVariableType.isBitvectorType()) {
      return fmgr.getBitvectorFormulaManager()
          .makeBitvector((BitvectorType) clockVariableType, number);
    }

    if (clockVariableType.isIntegerType()) {
      return fmgr.getIntegerFormulaManager().makeNumber(number);
    }

    if (clockVariableType.isRationalType()) {
      return fmgr.getRationalFormulaManager().makeNumber(number);
    }

    throw new AssertionError("Unsupported clock data type");
  }

  @Override
  public BooleanFormula makeVariablesDoNotChangeFormula(
      TaDeclaration pAutomaton, int pVariableIndexBefore, Iterable<TaVariable> pClocks) {
    var noChangesFormula = makeUnchangedFormulas(pVariableIndexBefore, pClocks);
    var timeDoesNotAdvanceFormula = makeTimeDoesNotAdvanceFormula(pAutomaton, pVariableIndexBefore);

    return bFmgr.and(noChangesFormula, timeDoesNotAdvanceFormula);
  }

  protected BooleanFormula makeUnchangedFormulas(int pVariableIndexBefore, Iterable<TaVariable> pClocks) {
    var unchangedFormulas = from(pClocks).transform(clock -> makeUnchangedFormula(clock, pVariableIndexBefore));
    return bFmgr.and(unchangedFormulas.toSet());
  }

  protected abstract BooleanFormula makeTimeDoesNotAdvanceFormula(
      TaDeclaration pAutomaton, int pIndexBefore);

  protected BooleanFormula makeUnchangedFormula(TaVariable pVariable, int pVariableIndexBefore) {
    var newVariable =
        fmgr.makeVariable(clockVariableType, pVariable.getName(), pVariableIndexBefore + 1);
    var oldVariable =
        fmgr.makeVariable(clockVariableType, pVariable.getName(), pVariableIndexBefore);
    return fmgr.makeEqual(newVariable, oldVariable);
  }

  @Override
  public BooleanFormula makeEqualsZeroFormula(
      TaDeclaration pAutomaton, int pVariableIndex, Iterable<TaVariable> pClocks, boolean indexVariables) {
    var resets =
        from(pClocks).transform(clock -> makeEqualsZeroFormula(pAutomaton, pVariableIndex, clock, indexVariables));
    return bFmgr.and(resets.toSet());
  }

  protected abstract BooleanFormula makeEqualsZeroFormula(
      TaDeclaration pAutomaton, int pVariableIndex, TaVariable pVariable, boolean indexVariable);
}
