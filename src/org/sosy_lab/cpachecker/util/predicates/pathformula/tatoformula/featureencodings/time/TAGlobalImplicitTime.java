// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings.time;

import static com.google.common.collect.FluentIterable.from;

import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaVariable;
import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaVariableExpression;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;

public class TAGlobalImplicitTime extends TAAbstractTime {
  private static final String DELAY_VARIABLE_NAME = "#delay";

  public TAGlobalImplicitTime(FormulaManagerView pFmgr) {
    super(pFmgr);
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
  protected BooleanFormula makeResetFormula(
      TaDeclaration pAutomaton, int pVariableIndex, TaVariable pVariable) {
    var variable = fmgr.makeVariable(CLOCK_VARIABLE_TYPE, pVariable.getName(), pVariableIndex);
    var zero = fmgr.makeNumber(CLOCK_VARIABLE_TYPE, 0);
    return fmgr.makeEqual(variable, zero);
  }

  @Override
  public BooleanFormula makeInitiallyZeroFormula(TaDeclaration pAutomaton, int pVariableIndex) {
    var allClocksZero =
        from(pAutomaton.getClocks())
            .transform(clock -> makeResetFormula(pAutomaton, pVariableIndex, clock));
    return bFmgr.and(allClocksZero.toSet());
  }

  @Override
  public BooleanFormula makeTimeUpdateFormula(TaDeclaration pAutomaton, int pIndexBefore) {
    var delayVariable =
        fmgr.makeVariable(CLOCK_VARIABLE_TYPE, DELAY_VARIABLE_NAME, pIndexBefore + 1);
    var clockUpdateFormulas =
        from(pAutomaton.getClocks())
            .transform(clock -> makeTimeUpdateFormula(clock, delayVariable, pIndexBefore));
    var clockUpdatesFormula = bFmgr.and(clockUpdateFormulas.toSet());

    var zero = fmgr.makeNumber(CLOCK_VARIABLE_TYPE, 0);
    var delayLowerBound = fmgr.makeGreaterOrEqual(delayVariable, zero, false);

    return bFmgr.and(delayLowerBound, clockUpdatesFormula);
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
