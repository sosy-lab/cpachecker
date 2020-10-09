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

public class TAGlobalImplicitTime extends TAAbstractVariables {
  private static final String DELAY_VARIABLE_NAME = "#delay";

  public TAGlobalImplicitTime(FormulaManagerView pFmgr, boolean pAllowZeroDelay) {
    super(pFmgr, pAllowZeroDelay);
  }

  @Override
  protected BooleanFormula makeVariableExpression(
      TaDeclaration pAutomaton, int pVariableIndex, TaVariableExpression expression) {
    var variableFormula =
        fmgr.makeVariable(CLOCK_VARIABLE_TYPE, expression.getVariable().getName(), pVariableIndex);
    var constantFormula = makeRealNumber(expression.getConstant());

    return makeVariableExpression(variableFormula, constantFormula, expression.getOperator());
  }

  @Override
  protected BooleanFormula makeEqualsZeroFormula(
      TaDeclaration pAutomaton, int pVariableIndex, TaVariable pVariable) {
    var variable = fmgr.makeVariable(CLOCK_VARIABLE_TYPE, pVariable.getName(), pVariableIndex);
    var zero = fmgr.makeNumber(CLOCK_VARIABLE_TYPE, 0);
    return fmgr.makeEqual(variable, zero);
  }

  @Override
  public BooleanFormula makeTimeElapseFormula(TaDeclaration pAutomaton, int pIndexBefore) {
    var delayVariable =
        fmgr.makeVariable(CLOCK_VARIABLE_TYPE, DELAY_VARIABLE_NAME, pIndexBefore + 1);
    var clockUpdateFormulas =
        from(pAutomaton.getClocks())
            .transform(clock -> makeTimeUpdateFormula(clock, delayVariable, pIndexBefore));
    var clockUpdatesFormula = bFmgr.and(clockUpdateFormulas.toSet());
    BooleanFormula delayLowerBound = makeDelayLowerBound(delayVariable);
    return bFmgr.and(delayLowerBound, clockUpdatesFormula);
  }

  private BooleanFormula makeDelayLowerBound(Formula delayVariable) {
    var zero = fmgr.makeNumber(CLOCK_VARIABLE_TYPE, 0);
    if (allowZeroDelay) {
      return fmgr.makeGreaterOrEqual(delayVariable, zero, false);
    } else {
      return fmgr.makeGreaterThan(delayVariable, zero, false);
    }
  }

  private BooleanFormula makeTimeUpdateFormula(
      TaVariable clock, Formula delayVariable, int pIndexBefore) {
    var newVariable = fmgr.makeVariable(CLOCK_VARIABLE_TYPE, clock.getName(), pIndexBefore + 1);
    var oldVariable = fmgr.makeVariable(CLOCK_VARIABLE_TYPE, clock.getName(), pIndexBefore);
    var updatedVariable = fmgr.makePlus(oldVariable, delayVariable);
    return fmgr.makeEqual(newVariable, updatedVariable);
  }

  @Override
  public BooleanFormula makeTimeDoesNotAdvanceFormula(TaDeclaration pAutomaton, int pIndexBefore) {
    return bFmgr.makeTrue();
  }
}
