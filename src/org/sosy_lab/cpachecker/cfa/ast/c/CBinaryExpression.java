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

  public enum BinaryOperator implements ABinaryExpression.ABinaryOperator {
    MULTIPLY("*"),
    DIVIDE("/"),
    MODULO("%"),
    PLUS("+"),
    MINUS("-"),
    SHIFT_LEFT("<<"),
    SHIFT_RIGHT(">>"),
    LESS_THAN("<"),
    GREATER_THAN(">"),
    LESS_EQUAL("<="),
    GREATER_EQUAL(">="),
    BINARY_AND("&"),
    BINARY_XOR("^"),
    BINARY_OR("|"),
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

    public boolean isLogicalOperator() {
      return switch (this) {
        case MULTIPLY,
            DIVIDE,
            MODULO,
            PLUS,
            MINUS,
            SHIFT_LEFT,
            SHIFT_RIGHT,
            BINARY_AND,
            BINARY_OR,
            BINARY_XOR ->
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
