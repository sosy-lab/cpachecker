// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.java;

import java.io.Serial;
import org.sosy_lab.cpachecker.cfa.ast.ABinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.java.JType;

/**
 * This class represents the infix expression AST node type.
 *
 * <pre>
 * InfixExpression:
 * Expression InfixOperator Expression { InfixOperator Expression }
 * </pre>
 *
 * Operand1 is the left operand. Operand2 the right operand. The possible Operators are represented
 * by the enum {@link JBinaryExpression.BinaryOperator}
 *
 * <p>Some expression in Java, like the postfix increment, will be transformed into an infix
 * expression in the CFA and also be represented by this class.
 */
public final class JBinaryExpression extends ABinaryExpression implements JExpression {

  @Serial private static final long serialVersionUID = 7830135105992595598L;

  public JBinaryExpression(
      FileLocation pFileLocation,
      JType pType,
      JExpression pOperand1,
      JExpression pOperand2,
      BinaryOperator pOperator) {
    super(pFileLocation, pType, pOperand1, pOperand2, pOperator);
  }

  @Override
  public <R, X extends Exception> R accept(JExpressionVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public JType getExpressionType() {
    return (JType) super.getExpressionType();
  }

  @Override
  public JExpression getOperand1() {
    return (JExpression) super.getOperand1();
  }

  @Override
  public JExpression getOperand2() {
    return (JExpression) super.getOperand2();
  }

  @Override
  public BinaryOperator getOperator() {
    return (BinaryOperator) super.getOperator();
  }

  public enum BinaryOperator implements ABinaryExpression.ABinaryOperator {
    MULTIPLY("*"),
    DIVIDE("/"),
    /** Java % (remainder) operator. */
    REMAINDER("%"),
    /**
     * Java + (addition) operator, but used exclusively for string concatenation.
     */
    STRING_CONCATENATION("+"),
    PLUS("+"),
    MINUS("-"),
    /**
     * Java << (signed left shift) operator, shifts a bit pattern to the left. The bit pattern is
     * given by the left-hand operand, and the number of positions to shift by the right-hand
     * operand.
     */
    SHIFT_LEFT_SIGNED("<<"),
    /**
     * Java >> (signed right shift) operator, shifts a bit pattern to the right. The bit pattern is
     * given by the left-hand operand, and the number of positions to shift by the right-hand
     * operand. The leftmost position after shifting depends on sign extension.
     */
    SHIFT_RIGHT_SIGNED(">>"),
    /**
     * Java >>> (unsigned right shift) operator, shifts a bit pattern to the right. The bit pattern
     * is given by the left-hand operand, and the number of positions to shift by the right-hand
     * operand. The left-hand operand is treated as unsigned. A zero is shifted into the leftmost
     * position.
     */
    SHIFT_RIGHT_UNSIGNED(">>>"),
    LESS_THAN("<"),
    GREATER_THAN(">"),
    LESS_EQUAL("<="),
    GREATER_EQUAL(">="),
    BINARY_AND("&"),
    BINARY_XOR("^"),
    BINARY_OR("|"),
    LOGICAL_AND("&"),
    LOGICAL_OR("|"),
    LOGICAL_XOR("^"),
    CONDITIONAL_AND("&&"),
    CONDITIONAL_OR("||"),
    EQUALS("=="),
    NOT_EQUALS("!="),
    ;

    private final String op;

    BinaryOperator(String pOp) {
      op = pOp;
    }

    /** Returns the string representation of this operator (e.g. "*", "+"). */
    @Override
    public String getOperator() {
      return op;
    }

    @Override
    public boolean isLogicalOperator() {
      return switch (this) {
        case STRING_CONCATENATION,
            MULTIPLY,
            DIVIDE,
            REMAINDER,
            PLUS,
            MINUS,
            SHIFT_LEFT_SIGNED,
            SHIFT_RIGHT_SIGNED,
            SHIFT_RIGHT_UNSIGNED,
            BINARY_AND,
            BINARY_OR,
            BINARY_XOR ->
            false;
        case LESS_EQUAL,
            LESS_THAN,
            GREATER_EQUAL,
            GREATER_THAN,
            EQUALS,
            NOT_EQUALS,
            CONDITIONAL_AND,
            CONDITIONAL_OR,
            LOGICAL_XOR,
            LOGICAL_OR,
            LOGICAL_AND ->
            true;
      };
    }
  }

  @Override
  public int hashCode() {
    int prime = 31;
    int result = 7;
    return prime * result + super.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof JBinaryExpression && super.equals(obj);
  }
}
