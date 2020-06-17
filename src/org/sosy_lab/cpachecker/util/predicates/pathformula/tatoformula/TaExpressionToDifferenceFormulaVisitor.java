// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
// 
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
// 
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula;

import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaVariableCondition;
import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaVariableConditionVisitor;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.Constraints;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaConverter;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.ExpressionToFormulaVisitor;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.Formula;

public class TaExpressionToDifferenceFormulaVisitor extends ExpressionToFormulaVisitor
    implements TaVariableConditionVisitor<Formula, UnrecognizedCodeException> {

  private final Formula referenceVariable;

  public TaExpressionToDifferenceFormulaVisitor(
      CtoFormulaConverter pCtoFormulaConverter,
      FormulaManagerView pFmgr,
      CFAEdge pEdge,
      String pFunction,
      SSAMapBuilder pSsa,
      Constraints pConstraints,
      Formula pRefenceVariable) {
    super(pCtoFormulaConverter, pFmgr, pEdge, pFunction, pSsa, pConstraints);
    referenceVariable = pRefenceVariable;
  }

  @Override
  public Formula visit(TaVariableCondition pTaVariableCondition) throws UnrecognizedCodeException {
    var numberOfExpressions = pTaVariableCondition.getExpressions().size();
    if (numberOfExpressions == 0) {
      return getBooleanFormulaManagerView().makeTrue();
    }

    List<Formula> expressionFormulas = new ArrayList<>(numberOfExpressions);
    for (var expression : pTaVariableCondition.getExpressions()) {
      var differenceExpression = getDifferenceExpressionFormula(expression);
      expressionFormulas.add(differenceExpression);
    }

    var result = expressionFormulas.get(0);
    for (var expressionFormula : Iterables.skip(expressionFormulas, 1)) {
      result = mgr.makeAnd(result, expressionFormula);
    }

    return result;
  }

  /**
   * Converts an expression of the form variable operator constant to (referenceVariable - variable)
   * operator constant. Expressions which are not of the required form are returned unmodified.
   */
  private Formula getDifferenceExpressionFormula(CExpression originalExpression)
      throws UnrecognizedCodeException {

    if (!(originalExpression instanceof CBinaryExpression)) {
      return toFormula(originalExpression);
    }

    var binaryExpression = (CBinaryExpression) originalExpression;
    var variable = toFormula(binaryExpression.getOperand1());
    var differenceFormula = mgr.makeMinus(referenceVariable, variable);
    var constant = toFormula(binaryExpression.getOperand2());

    return handleBinaryExpression(binaryExpression, differenceFormula, constant);
  }
}
