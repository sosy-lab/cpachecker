/*
 * CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.cpa.constraints.constraint;

import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JExpression;
import org.sosy_lab.cpachecker.cpa.invariants.formula.InvariantsFormula;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

/**
 * Factory for creating {@link org.sosy_lab.cpachecker.cpa.constraints.constraint.Constraint} objects.
 */
public class ConstraintFactory {

  private String functionName;

  private ConstraintFactory(String pFunctionName) {
    functionName = pFunctionName;
  }

  public static ConstraintFactory getInstance(String pFunctionName) {
    return new ConstraintFactory(pFunctionName);
  }

  public Constraint createPositiveConstraint(CExpression pLeftExpression, CBinaryExpression.BinaryOperator pOperator,
      CExpression pRightExpression) throws UnrecognizedCodeException {
    return createConstraint(pLeftExpression, pOperator, pRightExpression, true);
  }

  public Constraint createNegativeConstraint(CExpression pLeftExpression, CBinaryExpression.BinaryOperator pOperator,
      CExpression pRightExpression) throws UnrecognizedCodeException {
    return createConstraint(pLeftExpression, pOperator, pRightExpression, false);
  }

  private Constraint createConstraint(CExpression pLeftExpression, CBinaryExpression.BinaryOperator pOperator,
      CExpression pRightExpression, boolean pIsPositive) throws UnrecognizedCodeException {
    ConstraintOperand leftOperand;
    ConstraintOperand rightOperand;
    Constraint.Operator operator;
    boolean isPositive = pIsPositive;

    leftOperand = createOperand(pLeftExpression);
    rightOperand = createOperand(pRightExpression);


    if (leftOperand == null || rightOperand == null) {
      return null;
    }

    switch (pOperator) {
      case NOT_EQUALS:
        isPositive = !isPositive;
      case EQUALS:
        operator = Constraint.Operator.EQUAL;
        break;
      case GREATER_EQUAL: {
        ConstraintOperand swap = leftOperand;
        leftOperand = rightOperand;
        rightOperand = swap;
      }
      // $FALL-THROUGH$
      case LESS_EQUAL:
        operator = Constraint.Operator.LESS_EQUAL;
        break;
      case GREATER_THAN: {
        ConstraintOperand swap = leftOperand;
        leftOperand = rightOperand;
        rightOperand = swap;
      }
      // $FALL-THROUGH$
      case LESS_THAN:
        operator = Constraint.Operator.LESS;
        break;
      default:
        // TODO: Change to correct exception type (UnrecognizedCCodeException, probably)
        throw new AssertionError("Operation " + pOperator + " not allowed as assume.");
    }

    return new Constraint(leftOperand, operator, rightOperand, isPositive);
  }

  public Constraint createPositiveConstraint(JExpression pLeftExpression, JBinaryExpression.BinaryOperator pOperator,
      JExpression pRightExpression) throws UnrecognizedCodeException {
    return createConstraint(pLeftExpression, pOperator, pRightExpression, true);
  }

  public Constraint createNegativeConstraint(JExpression pLeftExpression, JBinaryExpression.BinaryOperator pOperator,
      JExpression pRightExpression) throws UnrecognizedCodeException {
    return createConstraint(pLeftExpression, pOperator, pRightExpression, false);
  }

  private Constraint createConstraint(JExpression pLeftExpression, JBinaryExpression.BinaryOperator pOperator,
      JExpression pRightExpression, boolean pIsPositive) throws UnrecognizedCodeException {
    ConstraintOperand leftOperand;
    ConstraintOperand rightOperand;
    Constraint.Operator operator;
    boolean isPositive = pIsPositive;

    leftOperand = createOperand(pLeftExpression);
    rightOperand = createOperand(pRightExpression);

    if (leftOperand == null || rightOperand == null) {
      return null;
    }

    switch (pOperator) {
      case NOT_EQUALS:
        isPositive = !isPositive;
      case EQUALS:
        operator = Constraint.Operator.EQUAL;
        break;
      case GREATER_EQUAL: {
        ConstraintOperand swap = leftOperand;
        leftOperand = rightOperand;
        rightOperand = swap;
      }
      // $FALL-THROUGH$
      case LESS_EQUAL:
        operator = Constraint.Operator.LESS_EQUAL;
        break;
      case GREATER_THAN: {
        ConstraintOperand swap = leftOperand;
        leftOperand = rightOperand;
        rightOperand = swap;
      }
      // $FALL-THROUGH$
      case LESS_THAN:
        operator = Constraint.Operator.LESS;
        break;
      default:
        // TODO: Change to correct exception type (UnrecognizedCCodeException, probably)
        throw new AssertionError("Operation " + pOperator + " not allowed as assume.");
    }


    return new Constraint(leftOperand, operator, rightOperand, isPositive);
  }

  private ConstraintOperand createOperand(AExpression pExpression) throws UnrecognizedCodeException {
    if (pExpression instanceof CExpression) {
      return new ConstraintOperand(transformExpression((CExpression) pExpression));
    } else {
      return new ConstraintOperand(transformExpression((JExpression) pExpression));
    }
  }

  private InvariantsFormula<Value> transformExpression(CExpression pExpression) throws UnrecognizedCodeException {
    CExpressionToFormulaVisitor formulaTransformer = new CExpressionToFormulaVisitor(functionName);

    return formulaTransformer.transform(pExpression);
  }

  private InvariantsFormula<Value> transformExpression(JExpression pExpression) throws UnrecognizedCodeException {
    JExpressionToFormulaVisitor formulaTransformer = new JExpressionToFormulaVisitor(functionName);

    return formulaTransformer.transform(pExpression);
  }
}
