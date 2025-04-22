// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

import java.io.Serial;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.ast.ABinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public final class AcslBinaryTerm extends AcslTerm {

  @Serial
  private static final long serialVersionUID = 7019912361956900L;

  private AcslTerm operand1;
  private AcslTerm operand2;
  private AcslBinaryTermOperator operator;

  public AcslBinaryTerm(
      FileLocation pFileLocation,
      AcslType pType,
      AcslTerm pOperand1,
      AcslTerm pOperand2,
      AcslBinaryTermOperator pOperator) {
    super(pFileLocation, pType);
    operand1 = pOperand1;
    operand2 = pOperand2;
    operator = pOperator;
  }

  public AcslTerm getOperand1() {
    return operand1;
  }

  public AcslTerm getOperand2() {
    return operand2;
  }

  public AcslBinaryTermOperator getOperator() {
    return operator;
  }

  @Override
  public <R, X extends Exception> R accept(AcslTermVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(AcslAstNodeVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return toParenthesizedASTString(pAAstNodeRepresentation);
  }

  @Override
  public String toParenthesizedASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return "(("
        + operand1.toASTString(pAAstNodeRepresentation)
        + ")"
        + " "
        + operator.toString()
        + " "
        + "("
        + operand2.toASTString(pAAstNodeRepresentation)
        + "))";
  }

  @Override
  public int hashCode() {
    final int prime = 37;
    int result = 5;
    result = prime * result + super.hashCode();
    result = prime * result + Objects.hashCode(operand1);
    result = prime * result + Objects.hashCode(operand2);
    result = prime * result + Objects.hashCode(operator);
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof AcslBinaryTerm other
        && super.equals(obj)
        && Objects.equals(other.operator, operator)
        && Objects.equals(other.operand1, operand1)
        && Objects.equals(other.operand2, operand2);
  }

  public enum AcslBinaryTermOperator implements ABinaryExpression.ABinaryOperator, AcslAstNode {
    AND("&&"),
    OR("||"),
    XOR("^^"),
    EQUALS("=="),
    NOT_EQUALS("!="),
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
        case EQUALS, NOT_EQUALS, LESS_EQUAL, GREATER_EQUAL, LESS_THAN, GREATER_THAN -> true;
        default -> false;
      };
    }

    public static boolean isLogicalOperator(AcslBinaryTermOperator op) {
      return switch (op) {
        case AND, OR, XOR -> true;
        default -> false;
      };
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
        case EQUALS,
            NOT_EQUALS,
            AND,
            OR,
            XOR,
            BINARY_AND,
            BINARY_OR,
            BINARY_EQUIVALENT,
            BINARY_XOR,
            PLUS,
            MULTIPLY ->
            true;
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
