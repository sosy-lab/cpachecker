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

import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CImaginaryLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.expressions.ConstraintExpression;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.expressions.ConstraintExpressionFactory;
import org.sosy_lab.cpachecker.cpa.invariants.formula.InvariantsFormula;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

import com.google.common.base.Optional;

/**
 * Class for transforming {@link CExpression} objects into their {@link InvariantsFormula} representation.
 *
 * <p>Always use {@link #transform} to create correct representations. Otherwise, correctness can't be assured.</p>
 */
public class CExpressionTransformer extends ExpressionTransformer
    implements CRightHandSideVisitor<ConstraintExpression, UnrecognizedCodeException> {

  public CExpressionTransformer(String pFunctionName, Optional<ValueAnalysisState> pValueState) {
    super(pFunctionName, pValueState);
  }

  public ConstraintExpression transform(CExpression pExpression) throws UnrecognizedCodeException {
    return pExpression.accept(this);
  }

  @Override
  public ConstraintExpression visit(CBinaryExpression pIastBinaryExpression) throws UnrecognizedCodeException {
    ConstraintExpression operand1Expression = pIastBinaryExpression.getOperand1().accept(this);

    if (operand1Expression == null) {
      return null;
    }

    ConstraintExpression operand2Expression = pIastBinaryExpression.getOperand2().accept(this);

    if (operand2Expression == null) {
      return null;
    }
        final Type expressionType = pIastBinaryExpression.getExpressionType();
    final Type calculationType = pIastBinaryExpression.getCalculationType();

    final ConstraintExpressionFactory factory = ConstraintExpressionFactory.getInstance();

    switch (pIastBinaryExpression.getOperator()) {
      case PLUS:
        return factory.add(operand1Expression, operand2Expression, calculationType, calculationType);
      case MINUS:
        return factory.minus(operand1Expression, operand2Expression, expressionType, calculationType);
      case MULTIPLY:
        return factory.multiply(operand1Expression, operand2Expression, calculationType, calculationType);
      case DIVIDE:
        return factory.divide(operand1Expression, operand2Expression, calculationType, calculationType);
      case MODULO:
        return factory.modulo(operand1Expression, operand2Expression, calculationType, calculationType);
      case SHIFT_LEFT:
        return factory.shiftLeft(operand1Expression, operand2Expression, calculationType, calculationType);
      case SHIFT_RIGHT:
        return factory.shiftRight(operand1Expression, operand2Expression, calculationType, calculationType);
      case BINARY_AND:
        return factory.binaryAnd(operand1Expression, operand2Expression, calculationType, calculationType);
      case BINARY_OR:
        return factory.binaryOr(operand1Expression, operand2Expression, calculationType, calculationType);
      case BINARY_XOR:
        return factory.binaryXor(operand1Expression, operand2Expression, calculationType, calculationType);
      case EQUALS:
        return factory.equal(operand1Expression, operand2Expression, calculationType, calculationType);
      case NOT_EQUALS:
        return factory.notEqual(operand1Expression, operand2Expression, calculationType, calculationType);
      case LESS_THAN:
        return factory.lessThan(operand1Expression, operand2Expression, calculationType, calculationType);
      case LESS_EQUAL:
        return factory.lessThanOrEqual(operand1Expression, operand2Expression, calculationType, calculationType);
      case GREATER_THAN:
        return factory.greaterThan(operand1Expression, operand2Expression, calculationType, calculationType);
      case GREATER_EQUAL:
        return factory.greaterThanOrEqual(operand1Expression, operand2Expression, calculationType, calculationType);
      default:
        throw new AssertionError("Unhandled binary operation " + pIastBinaryExpression.getOperator());
    }
  }

  @Override
  public ConstraintExpression visit(CUnaryExpression pIastUnaryExpression) throws UnrecognizedCodeException {
    final CUnaryExpression.UnaryOperator operator = pIastUnaryExpression.getOperator();
    final Type expressionType = pIastUnaryExpression.getExpressionType();

    switch (operator) {
      case MINUS:
      case TILDE: {
        ConstraintExpression operand = pIastUnaryExpression.getOperand().accept(this);

        if (operand == null) {
          return null;
        } else {
          return transformUnaryArithmetic(operator, operand, expressionType);
        }
      }

      default:
        return null; // TODO: amper, alignof, sizeof with own expressions
    }
  }

  private ConstraintExpression transformUnaryArithmetic(CUnaryExpression.UnaryOperator pOperator,
      ConstraintExpression pOperand, Type pExpressionType) {
    switch (pOperator) {
      case MINUS:
        return ConstraintExpressionFactory.getInstance().negate(pOperand, pExpressionType);
      case TILDE:
        return ConstraintExpressionFactory.getInstance().binaryNot(pOperand, pExpressionType);
      default:
        throw new AssertionError("No arithmetic operator: " + pOperator);
    }
  }

  @Override
  public ConstraintExpression visit(CIdExpression pIastIdExpression) throws UnrecognizedCodeException {
    return super.visit(pIastIdExpression);
  }

  @Override
  public ConstraintExpression visit(CCharLiteralExpression pIastCharLiteralExpression)
      throws UnrecognizedCodeException {
    return super.visit(pIastCharLiteralExpression);
  }

  @Override
  public ConstraintExpression visit(CFloatLiteralExpression pIastFloatLiteralExpression)
      throws UnrecognizedCodeException {
    return super.visit(pIastFloatLiteralExpression);
  }

  @Override
  public ConstraintExpression visit(CIntegerLiteralExpression pIastIntegerLiteralExpression)
      throws UnrecognizedCodeException {
    return super.visit(pIastIntegerLiteralExpression);
  }

  @Override
  public ConstraintExpression visit(CStringLiteralExpression pIastStringLiteralExpression)
      throws UnrecognizedCodeException {
    return null;
  }

  @Override
  public ConstraintExpression visit(CTypeIdExpression pIastTypeIdExpression) throws UnrecognizedCodeException {
    return null;
  }

  @Override
  public ConstraintExpression visit(CImaginaryLiteralExpression pIastLiteralExpression)
      throws UnrecognizedCodeException {
    assert false : "Imaginary literal";
    return null;
  }

  @Override
  public ConstraintExpression visit(CArraySubscriptExpression pIastArraySubscriptExpression)
      throws UnrecognizedCodeException {
    return null;
  }

  @Override
  public ConstraintExpression visit(CFieldReference pIastFieldReference) throws UnrecognizedCodeException {
    return null;
  }

  @Override
  public ConstraintExpression visit(CPointerExpression pointerExpression) throws UnrecognizedCodeException {
    return null;
  }


  @Override
  public ConstraintExpression visit(CFunctionCallExpression pIastFunctionCallExpression)
      throws UnrecognizedCodeException {
    return null;
  }

  @Override
  public ConstraintExpression visit(CCastExpression pIastCastExpression) throws UnrecognizedCodeException {
    return null;
  }

  @Override
  public ConstraintExpression visit(CComplexCastExpression complexCastExpression) throws UnrecognizedCodeException {
    return null;
  }
}
