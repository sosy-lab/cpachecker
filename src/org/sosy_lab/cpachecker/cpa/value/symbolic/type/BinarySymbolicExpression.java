// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.value.symbolic.type;

import java.io.Serial;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.java.JBinaryExpression;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/**
 * A binary {@link SymbolicExpression}. Represents all <code>SymbolicExpression</code>s that consist
 * of two operands.
 */
public abstract sealed class BinarySymbolicExpression extends SymbolicExpression
    permits AdditionExpression,
        BinaryAndExpression,
        BinaryOrExpression,
        BinaryXorExpression,
        DivisionExpression,
        EqualsExpression,
        LessThanExpression,
        LessThanOrEqualExpression,
        LogicalAndExpression,
        LogicalOrExpression,
        ModuloExpression,
        MultiplicationExpression,
        ShiftLeftExpression,
        ShiftRightExpression,
        SubtractionExpression {

  @Serial private static final long serialVersionUID = -5708374107141557273L;

  private final SymbolicExpression operand1;
  private final SymbolicExpression operand2;

  /** {@link Type} the operands are cast to during calculation. */
  private final Type calculationType;

  /** {@link Type} of the binary expression */
  private final Type expressionType;

  BinarySymbolicExpression(
      SymbolicExpression pOperand1,
      SymbolicExpression pOperand2,
      Type pExpressionType,
      Type pCalculationType) {
    operand1 = pOperand1;
    operand2 = pOperand2;
    expressionType = pExpressionType;
    calculationType = pCalculationType;
  }

  BinarySymbolicExpression(
      SymbolicExpression pOperand1,
      SymbolicExpression pOperand2,
      Type pExpressionType,
      Type pCalculationType,
      MemoryLocation pRepresentedLocation) {

    super(pRepresentedLocation);
    operand1 = pOperand1;
    operand2 = pOperand2;
    expressionType = pExpressionType;
    calculationType = pCalculationType;
  }

  BinarySymbolicExpression(
      SymbolicExpression pOperand1,
      SymbolicExpression pOperand2,
      Type pExpressionType,
      Type pCalculationType,
      AbstractState pAbstractState) {

    super(pAbstractState);
    operand1 = pOperand1;
    operand2 = pOperand2;
    expressionType = pExpressionType;
    calculationType = pCalculationType;
  }

  /**
   * Creates an appropriate {@link BinarySymbolicExpression} for Java, based on the given {@link
   * org.sosy_lab.cpachecker.cfa.ast.java.JBinaryExpression.BinaryOperator}. The operands are not
   * modified, i.e. they are not transformed into constant expressions. This has to be done before
   * calling this if needed! All {@link
   * org.sosy_lab.cpachecker.cfa.ast.java.JBinaryExpression.BinaryOperator}s, except for {@link
   * org.sosy_lab.cpachecker.cfa.ast.java.JBinaryExpression.BinaryOperator#STRING_CONCATENATION} are
   * handled by this method!
   */
  public static SymbolicExpression of(
      SymbolicExpression pOperand1,
      SymbolicExpression pOperand2,
      Type pExpressionType,
      Type pCalculationType,
      JBinaryExpression.BinaryOperator javaOperator) {
    return switch (javaOperator) {
      // +
      case JBinaryExpression.BinaryOperator.PLUS ->
          AdditionExpression.of(pOperand1, pOperand2, pExpressionType, pCalculationType);
      // -
      case JBinaryExpression.BinaryOperator.MINUS ->
          SubtractionExpression.of(pOperand1, pOperand2, pExpressionType, pCalculationType);
      // *
      case JBinaryExpression.BinaryOperator.MULTIPLY ->
          MultiplicationExpression.of(pOperand1, pOperand2, pExpressionType, pCalculationType);
      // /
      case JBinaryExpression.BinaryOperator.DIVIDE ->
          DivisionExpression.of(pOperand1, pOperand2, pExpressionType, pCalculationType);
      // % (actually remainder, not modulo)
      case JBinaryExpression.BinaryOperator.MODULO ->
          ModuloExpression.of(pOperand1, pOperand2, pExpressionType, pCalculationType);
      // <<
      case JBinaryExpression.BinaryOperator.SHIFT_LEFT ->
          ShiftLeftExpression.of(pOperand1, pOperand2, pExpressionType, pCalculationType);
      // >> (Signed)
      case JBinaryExpression.BinaryOperator.SHIFT_RIGHT_SIGNED ->
          ShiftRightExpression.ofSigned(pOperand1, pOperand2, pExpressionType, pCalculationType);
      // Binary &
      case JBinaryExpression.BinaryOperator.BINARY_AND ->
          BinaryAndExpression.of(pOperand1, pOperand2, pExpressionType, pCalculationType);
      // Binary |
      case JBinaryExpression.BinaryOperator.BINARY_OR ->
          BinaryOrExpression.of(pOperand1, pOperand2, pExpressionType, pCalculationType);
      // ^
      // TODO: why is the (Java) logical XOR handled like the binary operator?
      case JBinaryExpression.BinaryOperator.BINARY_XOR,
          JBinaryExpression.BinaryOperator.LOGICAL_XOR ->
          BinaryXorExpression.of(pOperand1, pOperand2, pExpressionType, pCalculationType);
      // ==
      case JBinaryExpression.BinaryOperator.EQUALS ->
          EqualsExpression.of(pOperand1, pOperand2, pExpressionType, pCalculationType);
      // !=
      case JBinaryExpression.BinaryOperator.NOT_EQUALS ->
          NotEqualsExpression.of(pOperand1, pOperand2, pExpressionType, pCalculationType);
      // <
      case JBinaryExpression.BinaryOperator.LESS_THAN ->
          LessThanExpression.of(pOperand1, pOperand2, pExpressionType, pCalculationType);
      // <=
      case JBinaryExpression.BinaryOperator.LESS_EQUAL ->
          LessThanOrEqualExpression.of(pOperand1, pOperand2, pExpressionType, pCalculationType);
      // >
      case JBinaryExpression.BinaryOperator.GREATER_THAN ->
          GreaterThanExpression.of(pOperand1, pOperand2, pExpressionType, pCalculationType);
      // >=
      case JBinaryExpression.BinaryOperator.GREATER_EQUAL ->
          GreaterThanOrEqualsExpression.of(pOperand1, pOperand2, pExpressionType, pCalculationType);
      // >>>
      case JBinaryExpression.BinaryOperator.SHIFT_RIGHT_UNSIGNED ->
          ShiftRightExpression.ofUnsigned(pOperand1, pOperand2, pExpressionType, pCalculationType);
      // Logical &
      // TODO: why is the (Java) conditional AND (i.e. &&) handled like the logical operator?
      case JBinaryExpression.BinaryOperator.LOGICAL_AND,
          JBinaryExpression.BinaryOperator.CONDITIONAL_AND ->
          LogicalAndExpression.of(pOperand1, pOperand2, pExpressionType, pCalculationType);
      // Logical |
      // TODO: why is the (Java) conditional OR (i.e. ||) handled like the logical operator?
      case JBinaryExpression.BinaryOperator.LOGICAL_OR,
          JBinaryExpression.BinaryOperator.CONDITIONAL_OR ->
          LogicalOrExpression.of(pOperand1, pOperand2, pExpressionType, pCalculationType);
      case JBinaryExpression.BinaryOperator.STRING_CONCATENATION ->
          throw new IllegalStateException("Unhandled binary operator: " + javaOperator);
    };
  }

  /**
   * Creates the appropriate {@link BinarySymbolicExpression} for C, based on the given {@link
   * org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator}. The operands are not
   * modified, i.e. they are not transformed into constant expressions. This has to be done before
   * calling this if needed! This method handles all available {@link
   * org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator}s.
   */
  public static SymbolicExpression of(
      SymbolicExpression pOperand1,
      SymbolicExpression pOperand2,
      Type pExpressionType,
      Type pCalculationType,
      CBinaryExpression.BinaryOperator cOperator) {
    return switch (cOperator) {
      // +
      case BinaryOperator.PLUS ->
          AdditionExpression.of(pOperand1, pOperand2, pExpressionType, pCalculationType);
      // -
      case BinaryOperator.MINUS ->
          SubtractionExpression.of(pOperand1, pOperand2, pExpressionType, pCalculationType);
      // *
      case BinaryOperator.MULTIPLY ->
          MultiplicationExpression.of(pOperand1, pOperand2, pExpressionType, pCalculationType);
      // /
      case BinaryOperator.DIVIDE ->
          DivisionExpression.of(pOperand1, pOperand2, pExpressionType, pCalculationType);
      // % (actually remainder, not modulo)
      case BinaryOperator.MODULO ->
          ModuloExpression.of(pOperand1, pOperand2, pExpressionType, pCalculationType);
      // <<
      case BinaryOperator.SHIFT_LEFT ->
          ShiftLeftExpression.of(pOperand1, pOperand2, pExpressionType, pCalculationType);
      // >> (Signed)
      case BinaryOperator.SHIFT_RIGHT ->
          ShiftRightExpression.ofSigned(pOperand1, pOperand2, pExpressionType, pCalculationType);
      // Binary &
      case BinaryOperator.BINARY_AND ->
          BinaryAndExpression.of(pOperand1, pOperand2, pExpressionType, pCalculationType);
      // Binary |
      case BinaryOperator.BINARY_OR ->
          BinaryOrExpression.of(pOperand1, pOperand2, pExpressionType, pCalculationType);
      // ^
      case BinaryOperator.BINARY_XOR ->
          BinaryXorExpression.of(pOperand1, pOperand2, pExpressionType, pCalculationType);
      // ==
      case BinaryOperator.EQUALS ->
          EqualsExpression.of(pOperand1, pOperand2, pExpressionType, pCalculationType);
      // !=
      case BinaryOperator.NOT_EQUALS ->
          NotEqualsExpression.of(pOperand1, pOperand2, pExpressionType, pCalculationType);
      // <
      case BinaryOperator.LESS_THAN ->
          LessThanExpression.of(pOperand1, pOperand2, pExpressionType, pCalculationType);
      // <=
      case BinaryOperator.LESS_EQUAL ->
          LessThanOrEqualExpression.of(pOperand1, pOperand2, pExpressionType, pCalculationType);
      // >
      case BinaryOperator.GREATER_THAN ->
          GreaterThanExpression.of(pOperand1, pOperand2, pExpressionType, pCalculationType);
      // >=
      case BinaryOperator.GREATER_EQUAL ->
          GreaterThanOrEqualsExpression.of(pOperand1, pOperand2, pExpressionType, pCalculationType);
    };
  }

  @Override
  public Type getType() {
    return expressionType;
  }

  public Type getCalculationType() {
    return calculationType;
  }

  public SymbolicExpression getOperand1() {
    return operand1;
  }

  public SymbolicExpression getOperand2() {
    return operand2;
  }

  @Override
  public boolean isTrivial() {
    return operand1.isTrivial() && operand2.isTrivial();
  }

  @Override
  @SuppressWarnings("EqualsGetClass") // on purpose, case-class structure with single equals()
  public final boolean equals(Object pObj) {
    if (this == pObj) {
      return true;
    }
    if (pObj == null || getClass() != pObj.getClass()) {
      return false;
    }

    BinarySymbolicExpression that = (BinarySymbolicExpression) pObj;

    return super.equals(that)
        && operand1.equals(that.operand1)
        && operand2.equals(that.operand2)
        && expressionType.equals(that.expressionType);
  }

  @Override
  public final int hashCode() {
    return super.hashCode()
        + Objects.hash(getClass().getCanonicalName(), operand1, operand2, expressionType);
  }

  @Override
  public String getRepresentation() {
    if (getRepresentedLocation().isPresent()) {
      return getRepresentedLocation().orElseThrow().toString();

    } else {
      return "("
          + operand1.getRepresentation()
          + " "
          + getOperationString()
          + " "
          + operand2.getRepresentation()
          + ")";
    }
  }

  @Override
  public String toString() {
    return operand1 + " " + getOperationString() + " " + operand2;
  }

  public abstract String getOperationString();
}
