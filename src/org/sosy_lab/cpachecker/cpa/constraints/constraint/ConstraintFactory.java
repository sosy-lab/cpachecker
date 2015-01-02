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

import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JBinaryExpression;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.expressions.ConstraintExpression;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.expressions.ConstraintExpressionFactory;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

import com.google.common.base.Optional;

/**
 * Factory for creating {@link org.sosy_lab.cpachecker.cpa.constraints.constraint.Constraint} objects.
 */
public class ConstraintFactory {

  private String functionName;
  private Optional<ValueAnalysisState> valueState;
  private boolean missingInformation = false;
  private ConstraintExpressionFactory expressionFactory;


  private ConstraintFactory(String pFunctionName, Optional<ValueAnalysisState> pValueState) {
    functionName = pFunctionName;
    valueState = pValueState;
    expressionFactory = ConstraintExpressionFactory.getInstance();
  }

  public static ConstraintFactory getInstance(String pFunctionName, Optional<ValueAnalysisState> pValueState) {
    return new ConstraintFactory(pFunctionName, pValueState);
  }

  /**
   * Returns whether information was missing while creating the last constraint.
   *
   * <p>This method always resets after one call. So when calling this method after the creation of a constraint,
   * it will only return <code>true</code> at the first call, if at all.</p>
   *
   * @return <code>true</code> if information was missing, <code>false</code> otherwise
   */
  public boolean hasMissingInformation() {
    boolean hasMissingInformation = missingInformation;
    missingInformation = false;

    return hasMissingInformation;
  }

  public Constraint createNegativeConstraint(CBinaryExpression pExpression) throws UnrecognizedCodeException {
    Constraint positiveConstraint = createPositiveConstraint(pExpression);

    if (positiveConstraint == null) {
      return null;
    } else {
      return createNot(positiveConstraint);
    }
  }

  public Constraint createNegativeConstraint(JBinaryExpression pExpression) throws UnrecognizedCodeException {
    Constraint positiveConstraint = createPositiveConstraint(pExpression);

    if (positiveConstraint == null) {
      return null;
    } else {
      return createNot(positiveConstraint);
    }
  }

  public Constraint createPositiveConstraint(CBinaryExpression pExpression) throws UnrecognizedCodeException {
    final CBinaryExpression.BinaryOperator operator = pExpression.getOperator();
    final Type expressionType = pExpression.getExpressionType();
    final Type calculationType = pExpression.getCalculationType();

    final CExpressionTransformer transformer = new CExpressionTransformer(functionName, valueState);

    ConstraintExpression leftOperand = pExpression.getOperand1().accept(transformer);

    checkForMissingInfo(transformer);
    if (leftOperand == null) {
      return null;
    }

    ConstraintExpression rightOperand = pExpression.getOperand2().accept(transformer);

    checkForMissingInfo(transformer);
    if (rightOperand == null) {
      return null;
    }

    switch (operator) {
      case EQUALS:
        return createEqual(leftOperand, rightOperand, expressionType, calculationType);
      case NOT_EQUALS:
        return createNotEqual(leftOperand, rightOperand, expressionType, calculationType);
      case GREATER_EQUAL: {
        ConstraintExpression swap = leftOperand;
        leftOperand = rightOperand;
        rightOperand = swap;
      }
      // $FALL-THROUGH$
      case LESS_EQUAL:
        return createLessOrEqual(leftOperand, rightOperand, expressionType, calculationType);

      case GREATER_THAN: {
        ConstraintExpression swap = leftOperand;
        leftOperand = rightOperand;
        rightOperand = swap;
      }
      // $FALL-THROUGH$
      case LESS_THAN:
        return createLess(leftOperand, rightOperand, expressionType, calculationType);

      default:
        throw new AssertionError("Operation " + operator + " not a constraint.");
    }
  }

  public Constraint createPositiveConstraint(JBinaryExpression pExpression) throws UnrecognizedCodeException {
    final JBinaryExpression.BinaryOperator operator = pExpression.getOperator();
    final Type expressionType = pExpression.getExpressionType();

    final JExpressionTransformer transformer = new JExpressionTransformer(functionName, valueState);

    ConstraintExpression leftOperand = pExpression.getOperand1().accept(transformer);

    checkForMissingInfo(transformer);
    if (leftOperand == null) {
      return null;
    }

    ConstraintExpression rightOperand = pExpression.getOperand2().accept(transformer);

    checkForMissingInfo(transformer);
    if (rightOperand == null) {
      return null;
    }

    switch (operator) {
      case EQUALS:
        return createEqual(leftOperand, rightOperand, expressionType, expressionType);
      case NOT_EQUALS:
        return createNotEqual(leftOperand, rightOperand, expressionType, expressionType);
      case GREATER_EQUAL: {
        ConstraintExpression swap = leftOperand;
        leftOperand = rightOperand;
        rightOperand = swap;
      }
      // $FALL-THROUGH$
      case LESS_EQUAL:
        return createLessOrEqual(leftOperand, rightOperand, expressionType, expressionType);

      case GREATER_THAN: {
        ConstraintExpression swap = leftOperand;
        leftOperand = rightOperand;
        rightOperand = swap;
      }
      // $FALL-THROUGH$
      case LESS_THAN:
        return createLess(leftOperand, rightOperand, expressionType, expressionType);

      default:
        throw new AssertionError("Operation " + operator + " not a constraint.");
    }
  }

  private void checkForMissingInfo(ExpressionTransformer pTransformer) {
    missingInformation |= pTransformer.hasMissingInformation();
  }

  private UnaryConstraint createNot(Constraint pConstraint) {
    // We use ConstraintExpression as Constraints, so this should be possible
    return createNot((ConstraintExpression) pConstraint);
  }

  private UnaryConstraint createNot(ConstraintExpression pConstraintExpression) {
    return (UnaryConstraint)
        expressionFactory.logicalNot(pConstraintExpression, pConstraintExpression.getExpressionType());
  }

  private Constraint createLess(ConstraintExpression pLeftOperand, ConstraintExpression pRightOperand,
      Type pExpressionType, Type pCalculationType) {

    return (Constraint)expressionFactory.lessThan(pLeftOperand, pRightOperand, pExpressionType, pCalculationType);
  }

  private Constraint createLessOrEqual(ConstraintExpression pLeftOperand, ConstraintExpression pRightOperand,
      Type pExpressionType, Type pCalculationType) {

    return (Constraint)expressionFactory.lessThanOrEqual(pLeftOperand, pRightOperand, pExpressionType,
        pCalculationType);
  }

  private Constraint createNotEqual(ConstraintExpression pLeftOperand, ConstraintExpression pRightOperand,
      Type pExpressionType, Type pCalculationType) {

    return (Constraint)expressionFactory.notEqual(pLeftOperand, pRightOperand, pExpressionType, pCalculationType);
  }

  private Constraint createEqual(ConstraintExpression pLeftOperand, ConstraintExpression pRightOperand,
      Type pExpressionType, Type pCalculationType) {

    return (Constraint) expressionFactory.equal(pLeftOperand, pRightOperand, pExpressionType, pCalculationType);
  }
}
