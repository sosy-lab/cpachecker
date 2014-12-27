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
import org.sosy_lab.cpachecker.cpa.constraints.constraint.expressions.ConstraintExpression;
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

  private ConstraintFactory(String pFunctionName) {
    functionName = pFunctionName;
    valueState = Optional.absent();
  }

  private ConstraintFactory(String pFunctionName, ValueAnalysisState pValueState) {
    this(pFunctionName);
    valueState = Optional.of(pValueState);
  }

  public static ConstraintFactory getInstance(String pFunctionName) {
    return new ConstraintFactory(pFunctionName);
  }

  public static ConstraintFactory getInstance(String pFunctionName, ValueAnalysisState pValueState) {
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
    boolean isPositive = pIsPositive;

    leftOperand = createOperand(pLeftExpression);
    rightOperand = createOperand(pRightExpression);


    if (leftOperand == null || rightOperand == null) {
      return null;
    }

    switch (pOperator) {
      case NOT_EQUALS:
        isPositive = !isPositive;
        // $FALL-THROUGH$
      case EQUALS:
        return new EqualConstraint(leftOperand, rightOperand, isPositive);

      case GREATER_EQUAL: {
        ConstraintOperand swap = leftOperand;
        leftOperand = rightOperand;
        rightOperand = swap;
      }
      // $FALL-THROUGH$
      case LESS_EQUAL:
        return new LessOrEqualConstraint(leftOperand, rightOperand, isPositive);

      case GREATER_THAN: {
        ConstraintOperand swap = leftOperand;
        leftOperand = rightOperand;
        rightOperand = swap;
      }
      // $FALL-THROUGH$
      case LESS_THAN:
        return new LessConstraint(leftOperand, rightOperand, isPositive);

      default:
        // TODO: Change to correct exception type (UnrecognizedCCodeException, probably)
        throw new AssertionError("Operation " + pOperator + " not allowed as assume.");
    }
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
    boolean isPositive = pIsPositive;

    leftOperand = createOperand(pLeftExpression);
    rightOperand = createOperand(pRightExpression);

    switch (pOperator) {
      case NOT_EQUALS:
        isPositive = !isPositive;
        // $FALL-THROUGH$
      case EQUALS:
        return new EqualConstraint(leftOperand, rightOperand, isPositive);

      case GREATER_EQUAL: {
        ConstraintOperand swap = leftOperand;
        leftOperand = rightOperand;
        rightOperand = swap;
      }
      // $FALL-THROUGH$
      case LESS_EQUAL:
        return new LessOrEqualConstraint(leftOperand, rightOperand, isPositive);

      case GREATER_THAN: {
        ConstraintOperand swap = leftOperand;
        leftOperand = rightOperand;
        rightOperand = swap;
      }
      // $FALL-THROUGH$
      case LESS_THAN:
        return new LessConstraint(leftOperand, rightOperand, isPositive);

      default:
        // TODO: Change to correct exception type (UnrecognizedCCodeException, probably)
        throw new AssertionError("Operation " + pOperator + " not allowed as assume.");
    }
  }

  private ConstraintOperand createOperand(AExpression pExpression) throws UnrecognizedCodeException {
    ConstraintExpression operandFormula;

    if (pExpression instanceof CExpression) {
      operandFormula = transformExpression((CExpression) pExpression);
    } else {
      operandFormula = transformExpression((JExpression) pExpression);
    }

    return operandFormula == null ? null : new ConstraintOperand(operandFormula);
  }

  private ConstraintExpression transformExpression(CExpression pExpression) throws UnrecognizedCodeException {
    CExpressionTransformer formulaTransformer = getCTransformer();
    ConstraintExpression expressionFormula = formulaTransformer.transform(pExpression);

    if (expressionFormula == null && formulaTransformer.hasMissingInformation()) {
      missingInformation = true;
    }

    return expressionFormula;
  }

  private CExpressionTransformer getCTransformer() {
    if (valueState.isPresent()) {
      return new CExpressionTransformer(functionName, valueState.get());
    } else {
      return new CExpressionTransformer(functionName);
    }
  }

  private ConstraintExpression transformExpression(JExpression pExpression) throws UnrecognizedCodeException {
    JExpressionTransformer formulaTransformer = getJavaTransformer();
    ConstraintExpression expressionFormula = formulaTransformer.transform(pExpression);

    if (expressionFormula == null && formulaTransformer.hasMissingInformation()) {
      missingInformation = true;
    }

    return formulaTransformer.transform(pExpression);
  }


  private JExpressionTransformer getJavaTransformer() {
    if (valueState.isPresent()) {
      return new JExpressionTransformer(functionName, valueState.get());
    } else {
      return new JExpressionTransformer(functionName);
    }
  }
}
