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

  // TODO: Java instanceof (type comparison) operator, defined in §15.20.2 of the Java specification
  public enum BinaryOperator implements ABinaryExpression.ABinaryOperator {
    /** Java * (multiplication) operator. Defined in the Java specification §15.17.1. */
    MULTIPLY("*"),
    /** Java / (division) operator. Defined in the Java specification §15.17.2. */
    DIVIDE("/"),
    /** Java % (remainder) operator. Defined in the Java specification §15.17.3. */
    REMAINDER("%"),
    /** Java + (string concatenation) operator. Defined in the Java specification §15.18.1. */
    STRING_CONCATENATION("+"),
    /**
     * Java + (additive) operator, but exclusively for arithmetic addition. Defined in the Java
     * specification §15.18.2.
     */
    PLUS("+"),
    /** Java - (subtraction) operator. Defined in the Java specification §15.18.2. */
    MINUS("-"),
    /**
     * Java << (signed left shift) operator. Defined in the Java specification §15.19, shifts a bit
     * pattern to the left. The bit pattern is given by the left-hand operand, and the number of
     * positions to shift by the right-hand operand.
     */
    SHIFT_LEFT_SIGNED("<<"),
    /**
     * Java >> (signed right shift) operator. Defined in the Java specification §15.19, shifts a bit
     * pattern to the right. The bit pattern is given by the left-hand operand, and the number of
     * positions to shift by the right-hand operand. The leftmost position after shifting depends on
     * sign extension.
     */
    SHIFT_RIGHT_SIGNED(">>"),
    /**
     * Java >>> (unsigned right shift) operator. Defined in the Java specification §15.19, shifts a
     * bit pattern to the right. The bit pattern is given by the left-hand operand, and the number
     * of positions to shift by the right-hand operand. The left-hand operand is treated as
     * unsigned. A zero is shifted into the leftmost position.
     */
    SHIFT_RIGHT_UNSIGNED(">>>"),
    /** Java < (less than) operator, defined in §15.20.1 of the Java specification. */
    LESS_THAN("<"),
    /** Java > (greater than) operator, defined in §15.20.1 of the Java specification. */
    GREATER_THAN(">"),
    /** Java <= (less than or equal to) operator, defined in §15.20.1 of the Java specification. */
    LESS_EQUAL("<="),
    /**
     * Java >= (greater than or equal to) operator, defined in §15.20.1 of the Java specification.
     */
    GREATER_EQUAL(">="),
    /** Java & (integer bitwise AND) operator, defined in §15.22.1 of the Java specification. */
    INTEGER_BITWISE_AND("&"),
    /**
     * Java ^ (integer bitwise exclusive OR) operator, defined in §15.22.1 of the Java
     * specification.
     */
    INTEGER_BITWISE_XOR("^"),
    /**
     * Java | (integer bitwise inclusive OR) operator, defined in §15.22.1 of the Java
     * specification.
     */
    INTEGER_BITWISE_OR("|"),
    /**
     * Java & (boolean logical bitwise AND) operator, defined in §15.22.2 of the Java specification.
     */
    BOOLEAN_LOGICAL_BITWISE_AND("&"),
    /**
     * Java | (boolean logical bitwise inclusive OR) operator, defined in §15.22.2 of the Java
     * specification.
     */
    BOOLEAN_LOGICAL_BITWISE_OR("|"),
    /**
     * Java ^ (boolean logical bitwise exclusive OR) operator, defined in §15.22.2 of the Java
     * specification.
     */
    BOOLEAN_LOGICAL_BITWISE_XOR("^"),
    /**
     * Java && (conditional AND) operator, defined in <a
     * href="https://docs.oracle.com/javase/specs/jls/se25/jls25.pdf#%5B%7B%22num%22%3A7590%2C%22gen%22%3A0%7D%2C%7B%22name%22%3A%22XYZ%22%7D%2C72%2C361%2Cnull%5D">§15.23
     * of the Java specification</a>.
     */
    CONDITIONAL_AND("&&"),
    /**
     * Java || (conditional OR) operator, defined in <a
     * href="https://docs.oracle.com/javase/specs/jls/se25/jls25.pdf#%5B%7B%22num%22%3A7628%2C%22gen%22%3A0%7D%2C%7B%22name%22%3A%22XYZ%22%7D%2C72%2C455%2Cnull%5D">§15.24
     * of the Java specification</a>.
     */
    CONDITIONAL_OR("||"),
    /**
     * Java == (equal to) operator, defined in <a
     * href="https://docs.oracle.com/javase/specs/jls/se25/jls25.pdf#%5B%7B%22num%22%3A7515%2C%22gen%22%3A0%7D%2C%7B%22name%22%3A%22XYZ%22%7D%2C72%2C179%2Cnull%5D">§15.21
     * of the Java specification</a>.
     */
    EQUALS("=="),
    /**
     * Java != (not equal to) operator, defined in <a
     * href="https://docs.oracle.com/javase/specs/jls/se25/jls25.pdf#%5B%7B%22num%22%3A7515%2C%22gen%22%3A0%7D%2C%7B%22name%22%3A%22XYZ%22%7D%2C72%2C179%2Cnull%5D">§15.21
     * of the Java specification</a>.
     */
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
            INTEGER_BITWISE_AND,
            INTEGER_BITWISE_OR,
            INTEGER_BITWISE_XOR ->
            false;
        case LESS_EQUAL,
            LESS_THAN,
            GREATER_EQUAL,
            GREATER_THAN,
            EQUALS,
            NOT_EQUALS,
            CONDITIONAL_AND,
            CONDITIONAL_OR,
            BOOLEAN_LOGICAL_BITWISE_XOR,
            BOOLEAN_LOGICAL_BITWISE_OR,
            BOOLEAN_LOGICAL_BITWISE_AND ->
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
