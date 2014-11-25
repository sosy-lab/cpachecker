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
import org.sosy_lab.cpachecker.cpa.invariants.formula.InvariantsFormula;
import org.sosy_lab.cpachecker.cpa.invariants.formula.InvariantsFormulaManager;
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
public class JExpressionToFormulaVisitor extends ExpressionToFormulaVisitor
    implements JRightHandSideVisitor<InvariantsFormula<Value>, UnrecognizedCodeException> {

  public JExpressionToFormulaVisitor(String pFunctionName) {
    super(pFunctionName);
  }

  public InvariantsFormula<Value> transform(JExpression pExpression) throws UnrecognizedCodeException {
    return pExpression.accept(this);
  }

  @Override
  public InvariantsFormula<Value> visit(JBinaryExpression paBinaryExpression) throws UnrecognizedCodeException {
    InvariantsFormula<Value> leftOperand = paBinaryExpression.getOperand1().accept(this);
    InvariantsFormula<Value> rightOperand = paBinaryExpression.getOperand2().accept(this);
    final JBinaryExpression.BinaryOperator operator = paBinaryExpression.getOperator();
    final InvariantsFormulaManager factory = InvariantsFormulaManager.INSTANCE;

    if (leftOperand == null) {
      return rightOperand;
    } else if (rightOperand == null) {
      return leftOperand;
    }

    switch (operator) {
      case MINUS:
        rightOperand = negate(rightOperand);
        // $FALL-THROUGH$
      case PLUS:
        return factory.add(leftOperand, rightOperand);
      case MULTIPLY:
        return factory.multiply(leftOperand, rightOperand);
      case DIVIDE:
        return factory.divide(leftOperand, rightOperand);
      case MODULO:
        return factory.modulo(leftOperand, rightOperand);
      case SHIFT_LEFT:
        return factory.shiftLeft(leftOperand, rightOperand);
      case SHIFT_RIGHT_SIGNED:
        return factory.shiftRight(leftOperand, rightOperand);
      case SHIFT_RIGHT_UNSIGNED:
        throw new AssertionError("Shift right unsigned not implemented"); // TODO implement
      case BINARY_AND:
        return factory.binaryAnd(leftOperand, rightOperand);
      case BINARY_OR:
        return factory.binaryOr(leftOperand, rightOperand);
      case BINARY_XOR:
        return factory.binaryXor(leftOperand, rightOperand);
      case EQUALS:
        return factory.equal(leftOperand, rightOperand);
      case NOT_EQUALS:
        return factory.logicalNot(factory.equal(leftOperand, rightOperand));
      case LESS_THAN:
        return factory.lessThan(leftOperand, rightOperand);
      case LESS_EQUAL:
        return factory.lessThanOrEqual(leftOperand, rightOperand);
      case GREATER_THAN:
        return factory.greaterThan(leftOperand, rightOperand);
      case GREATER_EQUAL:
        return factory.greaterThanOrEqual(leftOperand, rightOperand);
      case LOGICAL_AND:
      case CONDITIONAL_AND:
        return factory.logicalAnd(leftOperand, rightOperand);
      case LOGICAL_OR:
      case CONDITIONAL_OR:
        return factory.logicalOr(leftOperand, rightOperand);
      case LOGICAL_XOR:
        return factory.binaryXor(leftOperand, rightOperand);
      default:
        throw new AssertionError("Unhandled operator " + operator);
    }
  }

  @Override
  public InvariantsFormula<Value> visit(JUnaryExpression pAUnaryExpression) throws UnrecognizedCodeException {
    InvariantsFormula<Value> operand = pAUnaryExpression.getOperand().accept(this);

    if (operand == null) {
      return null;
    } else {
      final InvariantsFormulaManager factory = InvariantsFormulaManager.INSTANCE;
      final JUnaryExpression.UnaryOperator operator = pAUnaryExpression.getOperator();

      switch (operator) {
        case PLUS:
          return operand;
        case MINUS:
          return negate(operand);
        case NOT:
          return factory.logicalNot(operand);
        case COMPLEMENT:
          return factory.binaryNot(operand);
        default:
          throw new AssertionError("Unhandled operation " + operator);
      }
    }
  }

  @Override
  public InvariantsFormula<Value> visit(JCharLiteralExpression paCharLiteralExpression)
      throws UnrecognizedCodeException {
    return super.visit(paCharLiteralExpression);
  }

  @Override
  public InvariantsFormula<Value> visit(JStringLiteralExpression paStringLiteralExpression)
      throws UnrecognizedCodeException {
    return null;
  }

  @Override
  public InvariantsFormula<Value> visit(JIntegerLiteralExpression pJIntegerLiteralExpression)
      throws UnrecognizedCodeException {
    return super.visit(pJIntegerLiteralExpression);
  }

  @Override
  public InvariantsFormula<Value> visit(JBooleanLiteralExpression pJBooleanLiteralExpression)
      throws UnrecognizedCodeException {
    final boolean value = pJBooleanLiteralExpression.getValue();

    return InvariantsFormulaManager.INSTANCE.asConstant(createBooleanValue(value));
  }

  private Value createBooleanValue(boolean pValue) {
    return BooleanValue.valueOf(pValue);
  }

  @Override
  public InvariantsFormula<Value> visit(JFloatLiteralExpression pJFloatLiteralExpression)
      throws UnrecognizedCodeException {
    return super.visit(pJFloatLiteralExpression);
  }

  @Override
  public InvariantsFormula<Value> visit(JNullLiteralExpression pJNullLiteralExpression)
      throws UnrecognizedCodeException {
    return InvariantsFormulaManager.INSTANCE.asConstant(getNullValue());
  }

  private Value getNullValue() {
    return NullValue.getInstance();
  }

  @Override
  public InvariantsFormula<Value> visit(JEnumConstantExpression pJEnumConstantExpression)
      throws UnrecognizedCodeException {
    String enumConstant = pJEnumConstantExpression.getConstantName();
    Type enumType = pJEnumConstantExpression.getExpressionType();
    return InvariantsFormulaManager.INSTANCE.asConstant(createEnumValue(enumType, enumConstant));
  }

  private Value createEnumValue(Type pType, String pConstant) {
    return new EnumConstantValue(pType, pConstant);
  }

  @Override
  public InvariantsFormula<Value> visit(JIdExpression pJIdExpression) throws UnrecognizedCodeException {
    return super.visit(pJIdExpression);
  }

  @Override
  public InvariantsFormula<Value> visit(JMethodInvocationExpression pAFunctionCallExpression)
      throws UnrecognizedCodeException {
    return null;
  }

  @Override
  public InvariantsFormula<Value> visit(JClassInstanceCreation pJClassInstanceCreation)
      throws UnrecognizedCodeException {
    return null;
  }

  @Override
  public InvariantsFormula<Value> visit(JArrayCreationExpression pJBooleanLiteralExpression)
      throws UnrecognizedCodeException {
    return null;
  }

  @Override
  public InvariantsFormula<Value> visit(JArrayInitializer pJArrayInitializer) throws UnrecognizedCodeException {
    return null;
  }

  @Override
  public InvariantsFormula<Value> visit(JVariableRunTimeType pJThisRunTimeType) throws UnrecognizedCodeException {
    return null;
  }

  @Override
  public InvariantsFormula<Value> visit(JRunTimeTypeEqualsType pJRunTimeTypeEqualsType)
      throws UnrecognizedCodeException {
    return null;
  }

  @Override
  public InvariantsFormula<Value> visit(JCastExpression pJCastExpression) throws UnrecognizedCodeException {
    return null;
  }

  @Override
  public InvariantsFormula<Value> visit(JThisExpression pThisExpression) throws UnrecognizedCodeException {
    return null;
  }

  @Override
  public InvariantsFormula<Value> visit(JArraySubscriptExpression pAArraySubscriptExpression)
      throws UnrecognizedCodeException {
    return null;
  }
}
