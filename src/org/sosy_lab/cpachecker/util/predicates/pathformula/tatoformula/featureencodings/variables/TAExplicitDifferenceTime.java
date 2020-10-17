// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings.variables;

import java.math.BigInteger;
import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaVariable;
import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaVariableExpression;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.TimedAutomatonView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaType;

public class TAExplicitDifferenceTime extends TAExplicitTime {
  private static final String ZERO_VARIABLE_NAME = "#global_zero_var";

  public TAExplicitDifferenceTime(
      FormulaManagerView pFmgr,
      boolean pLocalEncoding,
      boolean pAllowZeroDelay,
      FormulaType<?> pClockVariableType,
      TimedAutomatonView pAutomata) {
    super(pFmgr, pLocalEncoding, pAllowZeroDelay, pClockVariableType, pAutomata);
  }

  @Override
  protected BooleanFormula makeVariableExpression(
      TaDeclaration pAutomaton, int pVariableIndex, TaVariableExpression expression) {
    var variableFormula =
        fmgr.makeVariable(clockVariableType, expression.getVariable().getName(), pVariableIndex);
    var timeVariableFormula = makeTimeVariableFormula(pAutomaton, pVariableIndex);
    var differenceFormula = fmgr.makeMinus(timeVariableFormula, variableFormula);
    var inverseDifferenceFormula = fmgr.makeMinus(variableFormula, timeVariableFormula);
    var constantFormula = makeClockTypeNumber(expression.getConstant());
    var negativeConstantFormula =
        makeClockTypeNumber(((BigInteger) expression.getConstant()).negate());

    switch (expression.getOperator()) {
      case GREATER:
        return fmgr.makeLessThan(inverseDifferenceFormula, negativeConstantFormula, true);
      case GREATER_EQUAL:
        return fmgr.makeLessOrEqual(inverseDifferenceFormula, negativeConstantFormula, true);
      case LESS:
        return fmgr.makeLessThan(differenceFormula, constantFormula, true);
      case LESS_EQUAL:
        return fmgr.makeLessOrEqual(differenceFormula, constantFormula, true);
      case EQUAL:
        return bFmgr.and(
            fmgr.makeLessOrEqual(differenceFormula, constantFormula, true),
            fmgr.makeLessOrEqual(inverseDifferenceFormula, negativeConstantFormula, true));
      default:
        throw new AssertionError();
    }
  }

  @Override
  protected BooleanFormula makeEqualsZeroFormula(
      TaDeclaration pAutomaton, int pVariableIndex, TaVariable pVariable, boolean indexVariable) {
    var timeVariableFormula = makeTimeVariableFormula(pAutomaton, pVariableIndex);
    Formula variable;
    if (indexVariable) {
      variable = fmgr.makeVariable(clockVariableType, pVariable.getName(), pVariableIndex);
    } else {
      variable = fmgr.makeVariable(clockVariableType, pVariable.getName());
    }

    return makeVariablesEqualFormula(variable, timeVariableFormula);
  }

  @Override
  protected BooleanFormula makeTimeVariableUpdateFormula(
      TaDeclaration pAutomaton, int pIndexBefore) {
    var timeVariableBeforeFormula = makeTimeVariableFormula(pAutomaton, pIndexBefore);
    var timeVariableAfterFormula = makeTimeVariableFormula(pAutomaton, pIndexBefore + 1);
    var zero = fmgr.makeNumber(clockVariableType, 0);
    var differenceFormula = fmgr.makeMinus(timeVariableBeforeFormula, timeVariableAfterFormula);

    BooleanFormula timeVariableRelation;
    if (allowZeroDelay) {
      timeVariableRelation = fmgr.makeLessOrEqual(differenceFormula, zero, true);
    } else {
      timeVariableRelation = fmgr.makeLessThan(differenceFormula, zero, true);
    }

    return timeVariableRelation;
  }

  @Override
  protected BooleanFormula makeTimeDoesNotAdvanceFormula(
      TaDeclaration pAutomaton, int pIndexBefore) {
    var timeVariableBeforeFormula = makeTimeVariableFormula(pAutomaton, pIndexBefore);
    var timeVariableAfterFormula = makeTimeVariableFormula(pAutomaton, pIndexBefore + 1);

    return makeVariablesEqualFormula(timeVariableAfterFormula, timeVariableBeforeFormula);
  }

  @Override
  protected BooleanFormula makeUnchangedFormula(TaVariable pVariable, int pVariableIndexBefore) {
    var newVariable =
        fmgr.makeVariable(clockVariableType, pVariable.getName(), pVariableIndexBefore + 1);
    var oldVariable =
        fmgr.makeVariable(clockVariableType, pVariable.getName(), pVariableIndexBefore);

    return makeVariablesEqualFormula(newVariable, oldVariable);
  }

  private BooleanFormula makeVariablesEqualFormula(Formula variable1, Formula variable2) {
    var difference1 = fmgr.makeMinus(variable1, variable2);
    var difference2 = fmgr.makeMinus(variable2, variable1);
    var zero = fmgr.makeNumber(clockVariableType, 0);

    return bFmgr.and(
        fmgr.makeLessOrEqual(difference1, zero, true),
        fmgr.makeLessOrEqual(difference2, zero, true));
  }

  @Override
  public BooleanFormula makeTimeEqualsZeroFormula(TaDeclaration pAutomaton, int pVariableIndex) {
    var timeVariable = makeTimeVariableFormula(pAutomaton, pVariableIndex);
    var zeroVariable = fmgr.makeVariable(clockVariableType, ZERO_VARIABLE_NAME);

    return makeVariablesEqualFormula(timeVariable, zeroVariable);
  }
}
