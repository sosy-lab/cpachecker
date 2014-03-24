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

import org.matheclipse.core.eval.EvalUtilities;
import org.matheclipse.core.expression.F;
import org.matheclipse.core.interfaces.IExpr;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cpa.value.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.SymbolicValueFormula.BinaryExpression;
import org.sosy_lab.cpachecker.cpa.value.SymbolicValueFormula.ConstantValue;
import org.sosy_lab.cpachecker.cpa.value.SymbolicValueFormula.ExpressionBase;
import org.sosy_lab.cpachecker.cpa.value.SymbolicValueFormula.SymbolicValue;

import edu.jas.kern.ComputerThreads;

/**
 * Uses an external library/application to simplify symbolic formulas.
 */
public class ExternalSimplifier {

  public static ExpressionBase simplify(ExpressionBase expr) {
    // Static initialization of the MathEclipse engine instead of null
    // you can set a file name to overload the default initial
    // rules. This step should be called only once at program setup:
    F.initSymbols(null);
    EvalUtilities util = new EvalUtilities();

    IExpr result;

    try {
      List<SymbolicValue> usedVariables = new ArrayList<>();
      String input = "Simplify[" +convertFormulaToString(expr, usedVariables) + "]";
      System.out.println(input);
      result = util.evaluate(input);
      System.out.println(result.toString());
      return recursiveConvertExpressionToFormula(result, usedVariables);
    } catch (final Exception e) {
      e.printStackTrace();
    } finally {
      // Call terminate() only one time at the end of the program
      ComputerThreads.terminate();
    }

    return expr;
  }

  private static String convertFormulaToString(ExpressionBase expr, List<SymbolicValue> usedVariables) {
    return recursiveConvertFormulaToString(expr, usedVariables);
  }

  private static String recursiveConvertFormulaToString(ExpressionBase formulaExpr, List<SymbolicValue> usedVariables) {
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
      throw new RuntimeException("Unsupported formula.");
    }
  }

  private static ExpressionBase recursiveConvertExpressionToFormula(IExpr expression, List<SymbolicValue> usedVariables) {
    if(expression.isPlus()) {
      ExpressionBase leftHand = recursiveConvertExpressionToFormula(expression.getAt(0), usedVariables);
      ExpressionBase rightHand = recursiveConvertExpressionToFormula(expression.getAt(1), usedVariables);
      return new BinaryExpression(leftHand, rightHand, BinaryExpression.BinaryOperator.PLUS, CNumericTypes.INT, CNumericTypes.INT);
    } else if(expression.isTimes()) {
      ExpressionBase leftHand = recursiveConvertExpressionToFormula(expression.getAt(1), usedVariables);
      ExpressionBase rightHand = recursiveConvertExpressionToFormula(expression.getAt(2), usedVariables);
      return new BinaryExpression(leftHand, rightHand, BinaryExpression.BinaryOperator.MULTIPLY, CNumericTypes.INT, CNumericTypes.INT);
    } else if(expression.isPower()) {
      // TODO: find a way to tell symja not to generate power expressions
      ExpressionBase base = recursiveConvertExpressionToFormula(expression.getAt(0), usedVariables);
      ExpressionBase power = recursiveConvertExpressionToFormula(expression.getAt(1), usedVariables);

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
      throw new RuntimeException("Unsupported formula: "+expression.toString());
    }
  }

  private static String operatorIdentifierToOperator(String identifier) {
    switch(identifier) {
    case "PLUS":
      return "+";
    case "MINUS":
      return "-";
    case "MULTIPLY":
      return "*";
    default:
      // This should never happen, as we only use the external tools to simplify
      // integer arithmetic with +, -, *
      throw new RuntimeException("Unsupported formula.");
    }
  }
}
