// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings.timeencodings;

import static com.google.common.collect.FluentIterable.from;

import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaVariable;
import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaVariableExpression;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;

public class ExplicitTimeEncoding extends AbstractTimeEncoding {
  private static final String TIME_VARIABLE_NAME = "#time";
  private final boolean localEncoding;

  public ExplicitTimeEncoding(FormulaManagerView pFmgr, boolean pLocalEncoding) {
    super(pFmgr);
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
  protected BooleanFormula makeResetFormula(
      TaDeclaration pAutomaton, int pVariableIndex, TaVariable pVariable) {
    var timeVariableFormula = makeTimeVariableFormula(pAutomaton, pVariableIndex);
    var variable = fmgr.makeVariable(CLOCK_VARIABLE_TYPE, pVariable.getName(), pVariableIndex);
    return fmgr.makeEqual(variable, timeVariableFormula);
  }

  @Override
  public BooleanFormula makeInitiallyZeroFormula(TaDeclaration pAutomaton, int pVariableIndex) {
    var allClocksZero =
        from(pAutomaton.getClocks())
            .transform(clock -> makeResetFormula(pAutomaton, pVariableIndex, clock));
    var timeVariableFormula = makeTimeVariableFormula(pAutomaton, pVariableIndex);
    var zero = fmgr.makeNumber(CLOCK_VARIABLE_TYPE, 0);
    var timeZero = fmgr.makeEqual(timeVariableFormula, zero);

    return bFmgr.and(bFmgr.and(allClocksZero.toSet()), timeZero);
  }

  @Override
  public BooleanFormula makeTimeUpdateFormula(TaDeclaration pAutomaton, int pIndexBefore) {
    var timeVariableBeforeFormula = makeTimeVariableFormula(pAutomaton, pIndexBefore);
    var timeVariableAfterFormula = makeTimeVariableFormula(pAutomaton, pIndexBefore + 1);

    return fmgr.makeGreaterOrEqual(timeVariableAfterFormula, timeVariableBeforeFormula, true);
  }

  @Override
  public BooleanFormula makeTimeDoesNotAdvanceFormula(TaDeclaration pAutomaton, int pIndexBefore) {
    var timeVariableBeforeFormula = makeTimeVariableFormula(pAutomaton, pIndexBefore);
    var timeVariableAfterFormula = makeTimeVariableFormula(pAutomaton, pIndexBefore + 1);

    return fmgr.makeEqual(timeVariableAfterFormula, timeVariableBeforeFormula);
  }
}
