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

import org.sosy_lab.cpachecker.cfa.ast.java.JArrayCreationExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JArrayInitializer;
import org.sosy_lab.cpachecker.cfa.ast.java.JArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JBooleanLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JClassInstanceCreation;
import org.sosy_lab.cpachecker.cfa.ast.java.JEnumConstantExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JMethodInvocationExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JNullLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.java.JRunTimeTypeEqualsType;
import org.sosy_lab.cpachecker.cfa.ast.java.JStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JThisExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JVariableRunTimeType;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.expressions.ConstraintExpression;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.expressions.ConstraintExpressionFactory;
import org.sosy_lab.cpachecker.cpa.invariants.formula.InvariantsFormula;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.type.BooleanValue;
import org.sosy_lab.cpachecker.cpa.value.type.EnumConstantValue;
import org.sosy_lab.cpachecker.cpa.value.type.NullValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

/**
 * Class for transforming {@link JExpression} objects into their {@link InvariantsFormula} representation.
 *
 * <p>Always use {@link #transform} to create correct representations. Otherwise, correctness can't be assured.</p>
 */
public class JExpressionTransformer extends ExpressionTransformer
    implements JRightHandSideVisitor<ConstraintExpression, UnrecognizedCodeException> {

  public JExpressionTransformer(String pFunctionName) {
    super(pFunctionName);
  }

  public JExpressionTransformer(String pFunctionName, ValueAnalysisState pValueState) {
    super(pFunctionName, pValueState);
  }

  public ConstraintExpression transform(JExpression pExpression) throws UnrecognizedCodeException {
    return pExpression.accept(this);
  }

  @Override
  public ConstraintExpression visit(JBinaryExpression paBinaryExpression) throws UnrecognizedCodeException {
    ConstraintExpression operand1Expression = paBinaryExpression.getOperand1().accept(this);
    ConstraintExpression operand2Expression = paBinaryExpression.getOperand2().accept(this);
    final JBinaryExpression.BinaryOperator operator = paBinaryExpression.getOperator();
    final Type expressionType = paBinaryExpression.getExpressionType();

    final ConstraintExpressionFactory factory = ConstraintExpressionFactory.getInstance();

    if (operand1Expression == null) {
      return operand2Expression;
    } else if (operand2Expression == null) {
      return operand1Expression;
    }

    switch (operator) {
      case PLUS:
        return factory.add(operand1Expression, operand2Expression, expressionType);
      case MINUS:
        return factory.minus(operand1Expression, operand2Expression, expressionType);
      case MULTIPLY:
        return factory.multiply(operand1Expression, operand2Expression, expressionType);
      case DIVIDE:
        return factory.divide(operand1Expression, operand2Expression, expressionType);
      case MODULO:
        return factory.modulo(operand1Expression, operand2Expression, expressionType);
      case SHIFT_LEFT:
        return factory.shiftLeft(operand1Expression, operand2Expression, expressionType);
      case SHIFT_RIGHT_SIGNED:
        return factory.shiftRight(operand1Expression, operand2Expression, expressionType);
      case SHIFT_RIGHT_UNSIGNED:
        throw new AssertionError("Shift right unsigned not implemented"); // TODO implement
      case BINARY_AND:
        return factory.binaryAnd(operand1Expression, operand2Expression, expressionType);
      case BINARY_OR:
        return factory.binaryOr(operand1Expression, operand2Expression, expressionType);
      case BINARY_XOR:
        return factory.binaryXor(operand1Expression, operand2Expression, expressionType);
      case EQUALS:
        return factory.equal(operand1Expression, operand2Expression, expressionType);
      case NOT_EQUALS:
        return factory.notEqual(operand1Expression, operand2Expression, expressionType);
      case LESS_THAN:
        return factory.lessThan(operand1Expression, operand2Expression, expressionType);
      case LESS_EQUAL:
        return factory.lessThanOrEqual(operand1Expression, operand2Expression, expressionType);
      case GREATER_THAN:
        return factory.greaterThan(operand1Expression, operand2Expression, expressionType);
      case GREATER_EQUAL:
        return factory.greaterThanOrEqual(operand1Expression, operand2Expression, expressionType);
      case LOGICAL_AND:
      case CONDITIONAL_AND:
        return factory.logicalAnd(operand1Expression, operand2Expression, expressionType);
      case LOGICAL_OR:
      case CONDITIONAL_OR:
        return factory.logicalOr(operand1Expression, operand2Expression, expressionType);
      case LOGICAL_XOR:
        return factory.binaryXor(operand1Expression, operand2Expression, expressionType);
      default:
        throw new AssertionError("Unhandled operator " + operator);
    }
  }

  @Override
  public ConstraintExpression visit(JUnaryExpression pAUnaryExpression) throws UnrecognizedCodeException {
    ConstraintExpression operand = pAUnaryExpression.getOperand().accept(this);

    if (operand == null) {
      return null;
    } else {
      final ConstraintExpressionFactory factory = ConstraintExpressionFactory.getInstance();
      final JUnaryExpression.UnaryOperator operator = pAUnaryExpression.getOperator();
      final Type expressionType = pAUnaryExpression.getExpressionType();

      switch (operator) {
        case PLUS:
          return operand;
        case MINUS:
          return factory.negate(operand, expressionType);
        case NOT:
          return factory.logicalNot(operand, expressionType);
        case COMPLEMENT:
          return factory.binaryNot(operand, expressionType);
        default:
          throw new AssertionError("Unhandled operation " + operator);
      }
    }
  }

  @Override
  public ConstraintExpression visit(JCharLiteralExpression paCharLiteralExpression)
      throws UnrecognizedCodeException {
    return super.visit(paCharLiteralExpression);
  }

  @Override
  public ConstraintExpression visit(JStringLiteralExpression paStringLiteralExpression)
      throws UnrecognizedCodeException {
    return null;
  }

  @Override
  public ConstraintExpression visit(JIntegerLiteralExpression pJIntegerLiteralExpression)
      throws UnrecognizedCodeException {
    return super.visit(pJIntegerLiteralExpression);
  }

  @Override
  public ConstraintExpression visit(JBooleanLiteralExpression pJBooleanLiteralExpression)
      throws UnrecognizedCodeException {
    final boolean value = pJBooleanLiteralExpression.getValue();
    final Type booleanType = pJBooleanLiteralExpression.getExpressionType();

    return ConstraintExpressionFactory.getInstance().asConstant(createBooleanValue(value), booleanType);
  }

  private Value createBooleanValue(boolean pValue) {
    return BooleanValue.valueOf(pValue);
  }

  @Override
  public ConstraintExpression visit(JFloatLiteralExpression pJFloatLiteralExpression)
      throws UnrecognizedCodeException {
    return super.visit(pJFloatLiteralExpression);
  }

  @Override
  public ConstraintExpression visit(JNullLiteralExpression pJNullLiteralExpression)
      throws UnrecognizedCodeException {

    final Type nullType = pJNullLiteralExpression.getExpressionType();

    return ConstraintExpressionFactory.getInstance().asConstant(getNullValue(), nullType);
  }

  private Value getNullValue() {
    return NullValue.getInstance();
  }

  @Override
  public ConstraintExpression visit(JEnumConstantExpression pJEnumConstantExpression)
      throws UnrecognizedCodeException {
    String enumConstant = pJEnumConstantExpression.getConstantName();
    Type enumType = pJEnumConstantExpression.getExpressionType();
    return ConstraintExpressionFactory.getInstance().asConstant(createEnumValue(enumType, enumConstant), enumType);
  }

  private Value createEnumValue(Type pType, String pConstant) {
    return new EnumConstantValue(pType, pConstant);
  }

  @Override
  public ConstraintExpression visit(JIdExpression pJIdExpression) throws UnrecognizedCodeException {
    return super.visit(pJIdExpression);
  }

  @Override
  public ConstraintExpression visit(JMethodInvocationExpression pAFunctionCallExpression)
      throws UnrecognizedCodeException {
    return null;
  }

  @Override
  public ConstraintExpression visit(JClassInstanceCreation pJClassInstanceCreation)
      throws UnrecognizedCodeException {
    return null;
  }

  @Override
  public ConstraintExpression visit(JArrayCreationExpression pJBooleanLiteralExpression)
      throws UnrecognizedCodeException {
    return null;
  }

  @Override
  public ConstraintExpression visit(JArrayInitializer pJArrayInitializer) throws UnrecognizedCodeException {
    return null;
  }

  @Override
  public ConstraintExpression visit(JVariableRunTimeType pJThisRunTimeType) throws UnrecognizedCodeException {
    return null;
  }

  @Override
  public ConstraintExpression visit(JRunTimeTypeEqualsType pJRunTimeTypeEqualsType)
      throws UnrecognizedCodeException {
    return null;
  }

  @Override
  public ConstraintExpression visit(JCastExpression pJCastExpression) throws UnrecognizedCodeException {
    return null;
  }

  @Override
  public ConstraintExpression visit(JThisExpression pThisExpression) throws UnrecognizedCodeException {
    return null;
  }

  @Override
  public ConstraintExpression visit(JArraySubscriptExpression pAArraySubscriptExpression)
      throws UnrecognizedCodeException {
    return null;
  }
}
