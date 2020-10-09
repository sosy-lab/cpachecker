// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings.variables;

import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaVariable;
import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaVariableExpression;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;

public class TAExplicitTime extends TAAbstractVariables {
  private static final String TIME_VARIABLE_NAME = "#time";
  private final boolean localEncoding;

  public TAExplicitTime(FormulaManagerView pFmgr, boolean pLocalEncoding, boolean pAllowZeroDelay) {
    super(pFmgr, pAllowZeroDelay);
    localEncoding = pLocalEncoding;
  }

  private Formula makeTimeVariableFormula(TaDeclaration pAutomaton, int pVariableIndex) {
    var variableName = TIME_VARIABLE_NAME;
    if (localEncoding) {
      variableName += "#" + pAutomaton.getName();
    }
    return fmgr.makeVariable(CLOCK_VARIABLE_TYPE, variableName, pVariableIndex);
  }

  @Override
  protected BooleanFormula makeVariableExpression(
      TaDeclaration pAutomaton, int pVariableIndex, TaVariableExpression expression) {
    var variableFormula =
        fmgr.makeVariable(CLOCK_VARIABLE_TYPE, expression.getVariable().getName(), pVariableIndex);
    var timeVariableFormula = makeTimeVariableFormula(pAutomaton, pVariableIndex);
    var differenceFormula = fmgr.makeMinus(timeVariableFormula, variableFormula);
    var constantFormula = makeRealNumber(expression.getConstant());

    return makeVariableExpression(differenceFormula, constantFormula, expression.getOperator());
  }

  @Override
  protected BooleanFormula makeEqualsZeroFormula(
      TaDeclaration pAutomaton, int pVariableIndex, TaVariable pVariable) {
    var timeVariableFormula = makeTimeVariableFormula(pAutomaton, pVariableIndex);
    var variable = fmgr.makeVariable(CLOCK_VARIABLE_TYPE, pVariable.getName(), pVariableIndex);
    return fmgr.makeEqual(variable, timeVariableFormula);
  }

  @Override
  public BooleanFormula makeTimeElapseFormula(TaDeclaration pAutomaton, int pIndexBefore) {
    var timeVariableUpdate = makeTimeVariableUpdateFormula(pAutomaton, pIndexBefore);
    var clocksUnchanged =
        makeVariablesDoNotChangeFormula(pAutomaton, pIndexBefore, pAutomaton.getClocks());

    return bFmgr.and(timeVariableUpdate, clocksUnchanged);
  }

  private BooleanFormula makeTimeVariableUpdateFormula(TaDeclaration pAutomaton, int pIndexBefore) {
    var timeVariableBeforeFormula = makeTimeVariableFormula(pAutomaton, pIndexBefore);
    var timeVariableAfterFormula = makeTimeVariableFormula(pAutomaton, pIndexBefore + 1);

    BooleanFormula timeVariableRelation;
    if (allowZeroDelay) {
      timeVariableRelation =
          fmgr.makeGreaterOrEqual(timeVariableAfterFormula, timeVariableBeforeFormula, true);
    } else {
      timeVariableRelation =
          fmgr.makeGreaterThan(timeVariableAfterFormula, timeVariableBeforeFormula, true);
    }

    return timeVariableRelation;
  }

  @Override
  protected BooleanFormula makeTimeDoesNotAdvanceFormula(
      TaDeclaration pAutomaton, int pIndexBefore) {
    var timeVariableBeforeFormula = makeTimeVariableFormula(pAutomaton, pIndexBefore);
    var timeVariableAfterFormula = makeTimeVariableFormula(pAutomaton, pIndexBefore + 1);

    return fmgr.makeEqual(timeVariableAfterFormula, timeVariableBeforeFormula);
  }
}
