// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serial;
import org.sosy_lab.cpachecker.cfa.ast.ABinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.acsl.AcslPolymorphicType;
import org.sosy_lab.cpachecker.cfa.types.acsl.AcslType;

public final class AcslBinaryTerm extends ABinaryExpression implements AcslTerm {

  @Serial private static final long serialVersionUID = 8110585630483048533L;

  public AcslBinaryTerm(
      FileLocation pFileLocation,
      AcslType pType,
      AcslTerm pOperand1,
      AcslTerm pOperand2,
      AcslBinaryTermOperator pOperator) {
    super(
        checkNotNull(pFileLocation),
        checkNotNull(pType),
        checkNotNull(pOperand1),
        checkNotNull(pOperand2),
        checkNotNull(pOperator));
    checkArgument(
        operandsFit(pOperator, pOperand1.getExpressionType(), pOperand2.getExpressionType()),
        "The operator %s cannot be applied to %s and %s",
        pOperator,
        pOperand1,
        pOperand2);
  }

  private static boolean operandsFit(
      AcslBinaryTermOperator pOperator, AcslType pType1, AcslType pType2) {
    if (pType1 instanceof AcslPolymorphicType || pType2 instanceof AcslPolymorphicType) {
      return true;
    }
    return switch (pOperator) {
      case PLUS ->
          (AcslType.isNumeric(pType1) && AcslType.isNumeric(pType2))
              || (AcslType.isPointer(pType1) && AcslType.isIntegral(pType2))
              || (AcslType.isIntegral(pType1) && AcslType.isPointer(pType2));
      case MINUS ->
          (AcslType.isNumeric(pType1) && AcslType.isNumeric(pType2))
              || (AcslType.isPointer(pType1) && AcslType.isIntegral(pType2))
              || (AcslType.isPointer(pType1) && AcslType.isPointer(pType2));
      case MULTIPLY, DIVIDE -> AcslType.isNumeric(pType1) && AcslType.isNumeric(pType2);
      case MODULO, SHIFT_LEFT, SHIFT_RIGHT ->
          AcslType.isIntegral(pType1) && AcslType.isIntegral(pType2);
      case BINARY_AND, BINARY_OR, BINARY_XOR, BINARY_IMPLICATION, BINARY_EQUIVALENT ->
          (AcslType.isIntegral(pType1) && AcslType.isIntegral(pType2))
              || (AcslType.isBoolean(pType1) && AcslType.isBoolean(pType2));
    };
  }

  @Override
  public AcslTerm getOperand1() {
    return (AcslTerm) super.getOperand1();
  }

  @Override
  public AcslTerm getOperand2() {
    return (AcslTerm) super.getOperand2();
  }

  @Override
  public AcslType getExpressionType() {
    return (AcslType) super.getExpressionType();
  }

  @Override
  public AcslBinaryTermOperator getOperator() {
    return (AcslBinaryTermOperator) super.getOperator();
  }

  @Override
  public <R, X extends Exception> R accept(AcslTermVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(AcslAstNodeVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  public enum AcslBinaryTermOperator implements ABinaryExpression.ABinaryOperator {
    BINARY_AND("&"),
    BINARY_OR("|"),
    BINARY_IMPLICATION("-->"),
    BINARY_EQUIVALENT("<-->"),
    BINARY_XOR("^"),
    PLUS("+"),
    MINUS("-"),
    MULTIPLY("*"),
    DIVIDE("/"),
    MODULO("%"),
    SHIFT_LEFT("<<"),
    SHIFT_RIGHT(">>");

    private final String operator;

    AcslBinaryTermOperator(String pOperator) {
      operator = pOperator;
    }

    public static AcslBinaryTermOperator of(String pOperator) {
      for (AcslBinaryTermOperator op : values()) {
        if (op.operator.equals(pOperator)) {
          return op;
        }
      }
      throw new IllegalArgumentException("Unknown operator: " + pOperator);
    }

    public static boolean isBitwiseOperator(AcslBinaryTermOperator op) {
      return switch (op) {
        case BINARY_AND, BINARY_OR, BINARY_IMPLICATION, BINARY_EQUIVALENT, BINARY_XOR -> true;
        default -> false;
      };
    }

    public static boolean isArithmeticOperator(AcslBinaryTermOperator op) {
      return switch (op) {
        case PLUS, MINUS, MULTIPLY, DIVIDE, MODULO, SHIFT_LEFT, SHIFT_RIGHT -> true;
        default -> false;
      };
    }

    public static boolean isCommutative(AcslBinaryTermOperator op) {
      return switch (op) {
        case BINARY_AND, BINARY_OR, BINARY_EQUIVALENT, BINARY_XOR, PLUS, MULTIPLY -> true;
        default -> false;
      };
    }

    @Override
    public String toString() {
      return operator;
    }

    /** Returns the string representation of this operator (e.g. "*", "+"). */
    @Override
    public String getOperator() {
      return operator;
    }

    @Override
    public boolean isLogicalOperator() {
      return false;
    }
  }
}
