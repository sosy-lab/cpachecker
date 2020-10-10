// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings.variables;

import static com.google.common.collect.FluentIterable.from;

import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaVariable;
import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaVariableExpression;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaType;

public class TAGlobalImplicitTime extends TAAbstractVariables {
  private static final String DELAY_VARIABLE_NAME = "#delay";

  public TAGlobalImplicitTime(
      FormulaManagerView pFmgr, boolean pAllowZeroDelay, FormulaType<?> pClockVariableType) {
    super(pFmgr, pAllowZeroDelay, pClockVariableType);
  }

  @Override
  protected BooleanFormula makeVariableExpression(
      TaDeclaration pAutomaton, int pVariableIndex, TaVariableExpression expression) {
    var variableFormula =
        fmgr.makeVariable(clockVariableType, expression.getVariable().getName(), pVariableIndex);
    var constantFormula = makeRealNumber(expression.getConstant());

    return makeVariableExpression(variableFormula, constantFormula, expression.getOperator());
  }

  @Override
  protected BooleanFormula makeEqualsZeroFormula(
      TaDeclaration pAutomaton, int pVariableIndex, TaVariable pVariable, boolean indexVariable) {
    Formula variable;
    if(indexVariable) {
      variable = fmgr.makeVariable(clockVariableType, pVariable.getName(), pVariableIndex);
    } else {
      variable = fmgr.makeVariable(clockVariableType, pVariable.getName());
    }
    var zero = fmgr.makeNumber(clockVariableType, 0);
    return fmgr.makeEqual(variable, zero);
  }

  @Override
  public BooleanFormula makeTimeElapseFormula(TaDeclaration pAutomaton, int pIndexBefore) {
    var delayVariable = fmgr.makeVariable(clockVariableType, DELAY_VARIABLE_NAME, pIndexBefore + 1);
    var clockUpdateFormulas =
        from(pAutomaton.getClocks())
            .transform(clock -> makeTimeUpdateFormula(clock, delayVariable, pIndexBefore));
    var clockUpdatesFormula = bFmgr.and(clockUpdateFormulas.toSet());
    BooleanFormula delayLowerBound = makeDelayLowerBound(delayVariable);
    return bFmgr.and(delayLowerBound, clockUpdatesFormula);
  }

  private BooleanFormula makeDelayLowerBound(Formula delayVariable) {
    var zero = fmgr.makeNumber(clockVariableType, 0);
    if (allowZeroDelay) {
      return fmgr.makeGreaterOrEqual(delayVariable, zero, false);
    } else {
      return fmgr.makeGreaterThan(delayVariable, zero, false);
    }
  }

  private BooleanFormula makeTimeUpdateFormula(
      TaVariable clock, Formula delayVariable, int pIndexBefore) {
    var newVariable = fmgr.makeVariable(clockVariableType, clock.getName(), pIndexBefore + 1);
    var oldVariable = fmgr.makeVariable(clockVariableType, clock.getName(), pIndexBefore);
    var updatedVariable = fmgr.makePlus(oldVariable, delayVariable);
    return fmgr.makeEqual(newVariable, updatedVariable);
  }

  @Override
  public BooleanFormula makeTimeDoesNotAdvanceFormula(TaDeclaration pAutomaton, int pIndexBefore) {
    return bFmgr.makeTrue();
  }
}
