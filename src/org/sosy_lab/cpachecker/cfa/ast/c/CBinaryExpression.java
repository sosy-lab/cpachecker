// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.c;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.Serial;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.ast.ABinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

public final class CBinaryExpression extends ABinaryExpression implements CExpression {

  @Serial private static final long serialVersionUID = 1902123965106390020L;
  private final CType calculationType;

  public CBinaryExpression(
      final FileLocation pFileLocation,
      final CType pExpressionType,
      final CType pCalculationType,
      final CExpression pOperand1,
      final CExpression pOperand2,
      final BinaryOperator pOperator) {
    super(pFileLocation, pExpressionType, pOperand1, pOperand2, pOperator);
    calculationType = pCalculationType;
    checkArgument(
        pOperator.isLogicalOperator()
            || pOperator == BinaryOperator.PLUS
            || pOperator == BinaryOperator.MINUS
            || !(calculationType.getCanonicalType() instanceof CPointerType));
  }

  @Override
  public <R, X extends Exception> R accept(CExpressionVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(CRightHandSideVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(CAstNodeVisitor<R, X> pV) throws X {
    return pV.visit(this);
  }

  @Override
  public CType getExpressionType() {
    return (CType) super.getExpressionType();
  }

  /**
   * This method returns the type for the 'calculation' of this binary expression.
   *
   * <p>This is not the type of the 'result' of this binary expression. The result-type is returned
   * from getExpressionType().
   *
   * <p>Before the calculation, if necessary, both operand should be casted to the calculation-type.
   * In most cases this is a widening.
   *
   * <p>Then the operation is performed in this type. This may cause an overflow, if the
   * calculation-type is not big enough.
   *
   * <p>After the calculation, if necessary, the result of the binary operation should be casted to
   * the result-type.
   */
  public CType getCalculationType() {
    return calculationType;
  }

  @Override
  public CExpression getOperand1() {
    return (CExpression) super.getOperand1();
  }

  @Override
  public CExpression getOperand2() {
    return (CExpression) super.getOperand2();
  }

  @Override
  public BinaryOperator getOperator() {
    return (BinaryOperator) super.getOperator();
  }

  /*
   * More information about the operands, e.g. integer promotion, pointer arithmetics etc.,
   * can be found in the standard as well, and should be looked up before implementing them!
   */
  public enum BinaryOperator implements ABinaryExpression.ABinaryOperator {
    /**
     * Binary * (multiplication) operator, defined in the C11 standard §6.5.5 as the product of the
     * operands for arithmetic types.
     */
    MULTIPLY("*"),
    /**
     * C / (division) operator, defined in the C11 standard §6.5.5 as the quotient from the division
     * of the first operand by the second for arithmetic types. Since C99, integer division is
     * always truncated to zero. If the value of the second operand is zero, the behavior is
     * undefined.
     */
    DIVIDE("/"),
    /**
     * C % (remainder) operator, defined in the C11 standard §6.5.5 as (a/b)*b + a%b = a, i.e. a%b =
     * a - (a/b)*b, for arithmetic types. Since C99, integer division is always truncated to zero.
     * If the value of the second operand is zero, the behavior is undefined. Note: modulo is
     * defined distinctly, and there is no modulo operator in C!
     */
    REMAINDER("%"),
    /**
     * Binary + (additive) operator, defined in the C11 standard §6.5.6 as the sum of the operands
     * for arithmetic types.
     */
    PLUS("+"),
    /**
     * Binary - (subtraction) operator, defined in the C11 standard §6.5.6 as the difference
     * resulting from the subtraction of the second operand from the first for arithmetic types.
     */
    MINUS("-"),
    /**
     * Bitwise left shift operator <<, defined in the C11 standard §6.5.7 as: the result of E1 << E2
     * is E1 left-shifted E2 bit positions; vacated bits are filled with zeros. If E1 has an
     * unsigned type, the value of the result is E1 × 2E2, reduced modulo one more than the maximum
     * value representable in the result type. If E1 has a signed type and nonnegative value, and E1
     * × 2E2 is representable in the result type, then that is the resulting value; otherwise, the
     * behavior is undefined. If the value of the right operand is negative or is greater than or
     * equal to the width of the promoted left operand, the behavior is undefined.
     */
    SHIFT_LEFT("<<"),
    /**
     * Bitwise left shift operator <<, defined in the C11 standard §6.5.7 as: the result of E1 >> E2
     * is E1 right-shifted E2 bit positions. If E1 has an unsigned type or if E1 has a signed type
     * and a nonnegative value, the value of the result is the integral part of the quotient of E1 /
     * 2E2. If E1 has a signed type and a negative value, the resulting value is
     * implementation-defined. If the value of the right operand is negative or is greater than or
     * equal to the width of the promoted left operand, the behavior is undefined.
     */
    SHIFT_RIGHT(">>"),
    /**
     * Relational operator < (less than), defined in the C11 standard §6.5.8 as yielding integer
     * literal 1 if the specified relation is true and integer literal 0 if it is false.
     */
    LESS_THAN("<"),
    /**
     * Relational operator > (greater than), defined in the C11 standard §6.5.8 as yielding integer
     * literal 1 if the specified relation is true and integer literal 0 if it is false.
     */
    GREATER_THAN(">"),
    /**
     * Relational operator <= (less than or equal to), defined in the C11 standard §6.5.8 as
     * yielding integer literal 1 if the specified relation is true and integer literal 0 if it is
     * false.
     */
    LESS_EQUAL("<="),
    /**
     * Relational operator >= (greater than or equal to), defined in the C11 standard §6.5.8 as
     * yielding integer literal 1 if the specified relation is true and integer literal 0 if it is
     * false.
     */
    GREATER_EQUAL(">="),
    /**
     * Bitwise & (AND) operator, defined in the C11 standard §6.5.10 such that each bit in the
     * result is set if and only if each of the corresponding bits in the converted operands is set
     */
    BITWISE_AND("&"),
    /**
     * Bitwise ^ (exclusive OR) operator, defined in the C11 standard §6.5.11 such that each bit in
     * the result is set if and only if exactly one of the corresponding bits in the operands is
     * set.
     */
    BITWISE_XOR("^"),
    /**
     * Bitwise | (inclusive OR) operator, defined in the C11 standard §6.5.12 such that each bit in
     * the result is set if and only if at least one of the corresponding bits in the converted
     * operands is set.
     */
    BITWISE_OR("|"),
    /**
     * == (equal to) operator, defined in the C11 standard §6.5.9 as returning integer literal 1 if
     * the specified relation is true, i.e. the two operands are equal, and integer literal 0 if the
     * specified relation is false.
     */
    EQUALS("=="),
    /**
     * != (not equal to) operator, defined in the C11 standard §6.5.9 as the inverse operator to
     * {@link BinaryOperator#EQUALS}.
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
        case MULTIPLY,
            DIVIDE,
            REMAINDER,
            PLUS,
            MINUS,
            SHIFT_LEFT,
            SHIFT_RIGHT,
            BITWISE_AND,
            BITWISE_OR,
            BITWISE_XOR ->
            false;
        case LESS_EQUAL, LESS_THAN, GREATER_EQUAL, GREATER_THAN, EQUALS, NOT_EQUALS -> true;
      };
    }

    public BinaryOperator getSwitchOperandsSidesLogicalOperator() {
      assert isLogicalOperator();
      return switch (this) {
        case LESS_EQUAL -> GREATER_EQUAL;
        case LESS_THAN -> GREATER_THAN;
        case GREATER_EQUAL -> LESS_EQUAL;
        case GREATER_THAN -> LESS_THAN;
        case EQUALS -> EQUALS;
        case NOT_EQUALS -> NOT_EQUALS;
        default -> this;
      };
    }

    public BinaryOperator getOppositLogicalOperator() {
      assert isLogicalOperator();
      return switch (this) {
        case LESS_EQUAL -> GREATER_THAN;
        case LESS_THAN -> GREATER_EQUAL;
        case GREATER_EQUAL -> LESS_THAN;
        case GREATER_THAN -> LESS_EQUAL;
        case EQUALS -> NOT_EQUALS;
        case NOT_EQUALS -> EQUALS;
        default -> this;
      };
    }
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(calculationType) * 31 + super.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof CBinaryExpression other
        && Objects.equals(other.calculationType, calculationType)
        && super.equals(obj);
  }
}
