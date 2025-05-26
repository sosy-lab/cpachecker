// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serial;
import org.sosy_lab.cpachecker.cfa.ast.ABinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public final class AcslBinaryTerm extends ABinaryExpression implements AcslTerm {

  @Serial private static final long serialVersionUID = 7019912361956900L;

  public AcslBinaryTerm(
      FileLocation pFileLocation,
      AcslType pType,
      AcslTerm pOperand1,
      AcslTerm pOperand2,
      AcslBinaryTermOperator pOperator) {
    super(pFileLocation, pType, pOperand1, pOperand2, pOperator);
    checkNotNull(pFileLocation);
    checkNotNull(pType);
    checkNotNull(pOperand1);
    checkNotNull(pOperand2);
    checkNotNull(pOperator);
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

  public enum AcslBinaryTermOperator implements ABinaryExpression.ABinaryOperator, AcslAstNode {
    LESS_EQUAL("<="),
    GREATER_EQUAL(">="),
    LESS_THAN("<"),
    GREATER_THAN(">"),
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
    private final FileLocation fileLocation;

    AcslBinaryTermOperator(String pOperator) {
      operator = pOperator;
      fileLocation = FileLocation.DUMMY;
    }

    public static AcslBinaryTermOperator of(String pOperator) {
      for (AcslBinaryTermOperator op : values()) {
        if (op.operator.equals(pOperator)) {
          return op;
        }
      }
      throw new IllegalArgumentException("Unknown operator: " + pOperator);
    }

    public static boolean isComparisonOperator(AcslBinaryTermOperator op) {
      return switch (op) {
        case LESS_EQUAL, GREATER_EQUAL, LESS_THAN, GREATER_THAN -> true;
        default -> false;
      };
    }

    @SuppressWarnings("unused")
    public static boolean isLogicalOperator(AcslBinaryTermOperator op) {
      return false;
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
    public <R, X extends Exception> R accept(AcslAstNodeVisitor<R, X> v) throws X {
      return v.visit(this);
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
    public FileLocation getFileLocation() {
      return fileLocation;
    }

    @Override
    public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
      return toString();
    }

    @Override
    public String toParenthesizedASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
      return toString();
    }
  }
}
