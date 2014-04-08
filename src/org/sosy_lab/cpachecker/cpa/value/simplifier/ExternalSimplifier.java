/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.value.simplifier;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.matheclipse.core.eval.EvalUtilities;
import org.matheclipse.core.expression.F;
import org.matheclipse.core.interfaces.IExpr;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cpa.value.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.SymbolicValueFormula.BinaryExpression;
import org.sosy_lab.cpachecker.cpa.value.SymbolicValueFormula.ConstantValue;
import org.sosy_lab.cpachecker.cpa.value.SymbolicValueFormula.ExpressionBase;
import org.sosy_lab.cpachecker.cpa.value.SymbolicValueFormula.SymbolicValue;
import org.sosy_lab.cpachecker.cpa.value.Value;

/**
 * Uses an external library/application to simplify symbolic formulas.
 */
public class ExternalSimplifier {
  static private EvalUtilities util;

  public static void initialize() {
    if(util == null) {
      F.initSymbols(null);
      util = new EvalUtilities();
    }
  }

  public static ExpressionBase simplify(ExpressionBase expr, LogManagerWithoutDuplicates logger) {
    if(util == null) {
      initialize();
    }

    IExpr result;

    try {
      List<SymbolicValue> usedVariables = new ArrayList<>();
      String input = "Simplify[" +convertFormulaToString(expr, usedVariables) + "]";
      result = util.evaluate(input);
      return recursiveConvertExpressionToFormula(result, usedVariables);
    } catch (final Exception e) {
      logger.logf(Level.FINE, "Error simplifying formula %s: %s", expr.toString(), e.toString());
    }

    return expr;
  }

  /**
   * Exception thrown when a formula is simplified/solved that isn't yet supported.
   */
  static class UnsupportedFormulaException extends Exception {
    private static final long serialVersionUID = 1L;

    public UnsupportedFormulaException(String message) {
      super(message);
    }
  }

  public static Value solve(SymbolicValue value, ExpressionBase expr, LogManagerWithoutDuplicates logger) {
    if(util == null) {
      initialize();
    }

    IExpr result;

    try {
      List<SymbolicValue> usedVariables = new ArrayList<>();
      String formulaString = convertFormulaToString(expr, usedVariables);
      int toSolveIndex = usedVariables.indexOf(value);
      String variableToSolveFor = "X" + toSolveIndex;
      String input = "Solve[" + formulaString + ", " + variableToSolveFor + "]";
      result = util.evaluate(input);

      // We expect to get something of the form `{{Rule[X0, 1]}}`, which means `X0 := 1`

      // For some reason the result is wrapped in two lists.
      result = result.getAt(1).getAt(1);
      if(result.isRuleAST()) {
        if(result.getAt(1).toString().equals(variableToSolveFor)) {
          return new NumericValue(Integer.parseInt(result.getAt(2).toString()));
        }
      }

      return null;
    } catch (final Exception e) {
      logger.logf(Level.FINE, "Error solving formula %s: %s", expr.toString(), e.toString());
    }

    return null;
  }

  private static String convertFormulaToString(ExpressionBase expr, List<SymbolicValue> usedVariables) throws UnsupportedFormulaException {
    return recursiveConvertFormulaToString(expr, usedVariables);
  }

  private static String recursiveConvertFormulaToString(ExpressionBase formulaExpr, List<SymbolicValue> usedVariables) throws UnsupportedFormulaException {
    if(formulaExpr instanceof SymbolicValue) {
      int index = usedVariables.indexOf(formulaExpr);
      if(index == -1) {
        index = usedVariables.size();
        usedVariables.add((SymbolicValue) formulaExpr);
      }

      return "X" + index;
    } else if(formulaExpr instanceof BinaryExpression) {
      BinaryExpression expr = (BinaryExpression) formulaExpr;
      String leftHand = recursiveConvertFormulaToString(expr.getOperand1(), usedVariables);
      String rightHand = recursiveConvertFormulaToString(expr.getOperand2(), usedVariables);
      String operator = operatorIdentifierToOperator(expr.getOperator().toString());
      return "(" + leftHand + " " + operator + " " + rightHand + ")";
    } else if(formulaExpr instanceof ConstantValue) {
      return ((ConstantValue)formulaExpr).getValue().asNumericValue().getNumber().toString();
    } else {
      // This should never happen, as the above list should cover all symbolic value formulas.
      throw new UnsupportedFormulaException("Unsupported formula: "+formulaExpr);
    }
  }

  private static ExpressionBase recursiveConvertExpressionToFormula(IExpr expression, List<SymbolicValue> usedVariables) throws UnsupportedFormulaException {
    if(expression.isPlus() || expression.getAt(0).toString().equals("Plus")) {
      ExpressionBase leftHand = recursiveConvertExpressionToFormula(expression.getAt(1), usedVariables);
      ExpressionBase rightHand = recursiveConvertExpressionToFormula(expression.getAt(2), usedVariables);
      return new BinaryExpression(leftHand, rightHand, BinaryExpression.BinaryOperator.PLUS, CNumericTypes.INT, CNumericTypes.INT);
    } else if(expression.isTimes()) {
      ExpressionBase leftHand = recursiveConvertExpressionToFormula(expression.getAt(1), usedVariables);
      ExpressionBase rightHand = recursiveConvertExpressionToFormula(expression.getAt(2), usedVariables);
      return new BinaryExpression(leftHand, rightHand, BinaryExpression.BinaryOperator.MULTIPLY, CNumericTypes.INT, CNumericTypes.INT);
    } else if(expression.isPower()) {
      // TODO: find a way to tell symja not to generate power expressions
      ExpressionBase base = recursiveConvertExpressionToFormula(expression.getAt(1), usedVariables);
      ExpressionBase power = recursiveConvertExpressionToFormula(expression.getAt(2), usedVariables);

      // TODO: build an expression for any power, not just 2
      if(power instanceof ConstantValue && ((ConstantValue)power).getValue().asLong(CNumericTypes.INT) == 2) {
        return new BinaryExpression(base, base, BinaryExpression.BinaryOperator.MULTIPLY, CNumericTypes.INT, CNumericTypes.INT);
      } else {
        throw new RuntimeException("Power larger than 2 not yet supported.");
      }
    } else if(expression.isInteger()) {
      return new ConstantValue(new NumericValue(Integer.parseInt(expression.toString())));
    } else if(expression.toString().startsWith("X")) {
      String indexString = expression.toString().substring(1);
      int index = Integer.parseInt(indexString);
      return usedVariables.get(index);
    } else {
      throw new UnsupportedFormulaException("Unsupported formula: "+expression.toString());
    }
  }

  private static String operatorIdentifierToOperator(String identifier) throws UnsupportedFormulaException {
    switch(identifier) {
    case "PLUS":
      return "+";
    case "MINUS":
      return "-";
    case "MULTIPLY":
      return "*";
    case "EQUALS":
      return "==";
    default:
      // This should never happen, as we only use the external tools to simplify
      // integer arithmetic with +, -, *
      throw new UnsupportedFormulaException("Unsupported operand: "+identifier);
    }
  }
}
