// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.value.symbolic;

import org.sosy_lab.cpachecker.cfa.ast.java.JArrayCreationExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JArrayInitializer;
import org.sosy_lab.cpachecker.cfa.ast.java.JArrayLengthExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JBooleanLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JClassInstanceCreation;
import org.sosy_lab.cpachecker.cfa.ast.java.JClassLiteralExpression;
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
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.AdditionExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.BinaryAndExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.BinaryNotExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.BinaryOrExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.BinaryXorExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.CastExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.ConstantSymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.DivisionExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.EqualsExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.LessThanExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.LessThanOrEqualExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.LogicalAndExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.LogicalNotExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.LogicalOrExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.ModuloExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.MultiplicationExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.NegationExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.ShiftLeftExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.ShiftRightExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SubtractionExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValueFactory;
import org.sosy_lab.cpachecker.cpa.value.type.BooleanValue;
import org.sosy_lab.cpachecker.cpa.value.type.EnumConstantValue;
import org.sosy_lab.cpachecker.cpa.value.type.NullValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

/**
 * Class for transforming {@link JExpression} objects into their {@link SymbolicExpression}
 * representation.
 *
 * <p>Always use {@link #transform} to create correct representations. Otherwise, correctness can't
 * be assured.
 */
public class JExpressionTransformer extends ExpressionTransformer
    implements JRightHandSideVisitor<SymbolicExpression, UnrecognizedCodeException> {

  public JExpressionTransformer(String pFunctionName, ValueAnalysisState pValueState) {
    super(pFunctionName, pValueState);
  }

  public SymbolicExpression transform(JExpression pExpression) throws UnrecognizedCodeException {
    return pExpression.accept(this);
  }

  @Override
  public SymbolicExpression visit(JBinaryExpression paBinaryExpression)
      throws UnrecognizedCodeException {
    SymbolicExpression operand1Expression = paBinaryExpression.getOperand1().accept(this);

    if (operand1Expression == null) {
      return null;
    }

    SymbolicExpression operand2Expression = paBinaryExpression.getOperand2().accept(this);

    if (operand2Expression == null) {
      return null;
    }

    final JBinaryExpression.BinaryOperator operator = paBinaryExpression.getOperator();
    final Type expressionType = paBinaryExpression.getExpressionType();

    return switch (operator) {
      case PLUS ->
          AdditionExpression.of(
              operand1Expression, operand2Expression, expressionType, expressionType);
      case MINUS ->
          SubtractionExpression.of(
              operand1Expression, operand2Expression, expressionType, expressionType);
      case MULTIPLY ->
          MultiplicationExpression.of(
              operand1Expression, operand2Expression, expressionType, expressionType);
      case DIVIDE ->
          DivisionExpression.of(
              operand1Expression, operand2Expression, expressionType, expressionType);
      case MODULO ->
          ModuloExpression.of(
              operand1Expression, operand2Expression, expressionType, expressionType);
      case SHIFT_LEFT ->
          ShiftLeftExpression.of(
              operand1Expression, operand2Expression, expressionType, expressionType);
      case SHIFT_RIGHT_SIGNED ->
          ShiftRightExpression.ofSigned(
              operand1Expression, operand2Expression, expressionType, expressionType);
      case SHIFT_RIGHT_UNSIGNED ->
          ShiftRightExpression.ofUnsigned(
              operand1Expression, operand2Expression, expressionType, expressionType);
      case BINARY_AND ->
          BinaryAndExpression.of(
              operand1Expression, operand2Expression, expressionType, expressionType);
      case BINARY_OR ->
          BinaryOrExpression.of(
              operand1Expression, operand2Expression, expressionType, expressionType);
      case BINARY_XOR ->
          BinaryXorExpression.of(
              operand1Expression, operand2Expression, expressionType, expressionType);
      case EQUALS ->
          EqualsExpression.of(
              operand1Expression, operand2Expression, expressionType, expressionType);
      case NOT_EQUALS ->
          SymbolicValueFactory.notEqual(
              operand1Expression, operand2Expression, expressionType, expressionType);
      case LESS_THAN ->
          LessThanExpression.of(
              operand1Expression, operand2Expression, expressionType, expressionType);
      case LESS_EQUAL ->
          LessThanOrEqualExpression.of(
              operand1Expression, operand2Expression, expressionType, expressionType);
      case GREATER_THAN ->
          SymbolicValueFactory.greaterThan(
              operand1Expression, operand2Expression, expressionType, expressionType);
      case GREATER_EQUAL ->
          SymbolicValueFactory.greaterThanOrEqual(
              operand1Expression, operand2Expression, expressionType, expressionType);
      case LOGICAL_AND, CONDITIONAL_AND ->
          LogicalAndExpression.of(
              operand1Expression, operand2Expression, expressionType, expressionType);
      case LOGICAL_OR, CONDITIONAL_OR ->
          LogicalOrExpression.of(
              operand1Expression, operand2Expression, expressionType, expressionType);
      case LOGICAL_XOR ->
          BinaryXorExpression.of(
              operand1Expression, operand2Expression, expressionType, expressionType);
      default -> throw new AssertionError("Unhandled operator " + operator);
    };
  }

  @Override
  public SymbolicExpression visit(JUnaryExpression pAUnaryExpression)
      throws UnrecognizedCodeException {
    SymbolicExpression operand = pAUnaryExpression.getOperand().accept(this);

    if (operand == null) {
      return null;
    } else {
      final JUnaryExpression.UnaryOperator operator = pAUnaryExpression.getOperator();
      final Type expressionType = pAUnaryExpression.getExpressionType();

      return switch (operator) {
        case PLUS -> operand;
        case MINUS -> NegationExpression.of(operand, expressionType);
        case NOT -> LogicalNotExpression.of(operand, expressionType);
        case COMPLEMENT -> BinaryNotExpression.of(operand, expressionType);
      };
    }
  }

  @Override
  public SymbolicExpression visit(JCharLiteralExpression paCharLiteralExpression)
      throws UnrecognizedCodeException {
    return super.visit(paCharLiteralExpression);
  }

  @Override
  public SymbolicExpression visit(JStringLiteralExpression paStringLiteralExpression)
      throws UnrecognizedCodeException {
    return null;
  }

  @Override
  public SymbolicExpression visit(JIntegerLiteralExpression pJIntegerLiteralExpression)
      throws UnrecognizedCodeException {
    return super.visit(pJIntegerLiteralExpression);
  }

  @Override
  public SymbolicExpression visit(JBooleanLiteralExpression pJBooleanLiteralExpression)
      throws UnrecognizedCodeException {
    final Value value = BooleanValue.valueOf(pJBooleanLiteralExpression.getBoolean());
    final Type booleanType = pJBooleanLiteralExpression.getExpressionType();

    return ConstantSymbolicExpression.of(value, booleanType);
  }

  @Override
  public SymbolicExpression visit(JFloatLiteralExpression pJFloatLiteralExpression)
      throws UnrecognizedCodeException {
    return super.visit(pJFloatLiteralExpression);
  }

  @Override
  public SymbolicExpression visit(JNullLiteralExpression pJNullLiteralExpression)
      throws UnrecognizedCodeException {

    final Type nullType = pJNullLiteralExpression.getExpressionType();

    return ConstantSymbolicExpression.of(getNullValue(), nullType);
  }

  private Value getNullValue() {
    return NullValue.getInstance();
  }

  @Override
  public SymbolicExpression visit(JEnumConstantExpression pJEnumConstantExpression)
      throws UnrecognizedCodeException {
    String enumConstant = pJEnumConstantExpression.getConstantName();
    Type enumType = pJEnumConstantExpression.getExpressionType();
    return ConstantSymbolicExpression.of(createEnumValue(enumConstant), enumType);
  }

  private Value createEnumValue(String pConstant) {
    return new EnumConstantValue(pConstant);
  }

  @Override
  public SymbolicExpression visit(JIdExpression pJIdExpression) throws UnrecognizedCodeException {
    return super.visit(pJIdExpression);
  }

  @Override
  public SymbolicExpression visit(JMethodInvocationExpression pAFunctionCallExpression)
      throws UnrecognizedCodeException {
    return null;
  }

  @Override
  public SymbolicExpression visit(JClassInstanceCreation pJClassInstanceCreation)
      throws UnrecognizedCodeException {
    return null;
  }

  @Override
  public SymbolicExpression visit(JArrayCreationExpression pJBooleanLiteralExpression)
      throws UnrecognizedCodeException {
    throw new UnsupportedOperationException(
        "Array creations can't be transformed to ConstraintExpressions");
  }

  @Override
  public SymbolicExpression visit(JArrayInitializer pJArrayInitializer)
      throws UnrecognizedCodeException {
    throw new UnsupportedOperationException(
        "Array initializations can't be transformed to ConstraintExpressions");
  }

  @Override
  public SymbolicExpression visit(JArrayLengthExpression pJArrayLengthExpression)
      throws UnrecognizedCodeException {
    return null;
  }

  @Override
  public SymbolicExpression visit(JVariableRunTimeType pJThisRunTimeType)
      throws UnrecognizedCodeException {
    throw new UnsupportedOperationException(
        "A variable's runtime type can't be transformed to ConstraintExpressions");
  }

  @Override
  public SymbolicExpression visit(JRunTimeTypeEqualsType pJRunTimeTypeEqualsType)
      throws UnrecognizedCodeException {
    throw new UnsupportedOperationException(
        "Equal checks on runtime types can't be transformed to ConstraintExpressions");
  }

  @Override
  public SymbolicExpression visit(JCastExpression pJCastExpression)
      throws UnrecognizedCodeException {
    SymbolicExpression operand = pJCastExpression.getOperand().accept(this);

    return CastExpression.of(operand, pJCastExpression.getCastType());
  }

  @Override
  public SymbolicExpression visit(JThisExpression pThisExpression)
      throws UnrecognizedCodeException {
    return null;
  }

  @Override
  public SymbolicExpression visit(JClassLiteralExpression pJClassLiteralExpression)
      throws UnrecognizedCodeException {
    return null;
  }

  @Override
  public SymbolicExpression visit(JArraySubscriptExpression pAArraySubscriptExpression)
      throws UnrecognizedCodeException {
    return null; // TODO
  }
}
