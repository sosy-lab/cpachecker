// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acslDeprecated;

public final class ACSLBinaryTerm implements ACSLTerm {

  private final ACSLTerm left;
  private final ACSLTerm right;
  private final ACSLBinaryOperator operator;

  public ACSLBinaryTerm(ACSLTerm pLeft, ACSLTerm pRight, ACSLBinaryOperator op) {
    assert ACSLBinaryOperator.isArithmeticOperator(op)
            || ACSLBinaryOperator.isBitwiseOperator(op)
            || ACSLBinaryOperator.isComparisonOperator(op)
        : String.format(
            "ACSLTerm may only hold arithmetic, bitwise or comparison operator, %s is neither", op);
    switch (op) {
      case PLUS,
          MINUS,
          DIVIDE,
          TIMES,
          MOD,
          LSHIFT,
          RSHIFT,
          BAND,
          BOR,
          BXOR,
          EQ,
          NEQ,
          LEQ,
          GEQ,
          LT,
          GT -> {
        left = pLeft;
        right = pRight;
        operator = op;
      }
      case BIMP -> {
        left = new ACSLUnaryTerm(pLeft, ACSLUnaryOperator.BNEG);
        right = pRight;
        operator = ACSLBinaryOperator.BOR;
      }
      case BEQV -> {
        left = new ACSLUnaryTerm(pLeft, ACSLUnaryOperator.BNEG);
        right = pRight;
        operator = ACSLBinaryOperator.BXOR;
      }
      default -> throw new AssertionError("Unknown operator: " + op);
    }
  }

  @Override
  public String toString() {
    return left.toString() + operator + right;
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof ACSLBinaryTerm other && operator.equals(other.operator)) {
      // Some operators are commutative.
      return (left.equals(other.left) && right.equals(other.right))
          || (ACSLBinaryOperator.isCommutative(operator)
              && left.equals(other.right)
              && right.equals(other.left));
    }
    return false;
  }

  @Override
  public int hashCode() {
    return 31 * left.hashCode() + 31 * right.hashCode() + operator.hashCode();
  }

  public ACSLTerm getLeft() {
    return left;
  }

  public ACSLTerm getRight() {
    return right;
  }

  public ACSLBinaryOperator getOperator() {
    return operator;
  }

  public ACSLBinaryTerm flipOperator() {
    assert ACSLBinaryOperator.isComparisonOperator(operator);
    ACSLBinaryOperator op =
        switch (operator) {
          case EQ -> ACSLBinaryOperator.NEQ;
          case NEQ -> ACSLBinaryOperator.EQ;
          case LEQ -> ACSLBinaryOperator.GT;
          case GEQ -> ACSLBinaryOperator.LT;
          case LT -> ACSLBinaryOperator.GEQ;
          case GT -> ACSLBinaryOperator.LEQ;
          default -> throw new AssertionError("Unknown BinaryOperator: " + operator);
        };
    return new ACSLBinaryTerm(left, right, op);
  }

  @Override
  public boolean isAllowedIn(Class<?> clauseType) {
    return left.isAllowedIn(clauseType) && right.isAllowedIn(clauseType);
  }

  @Override
  public <R, X extends Exception> R accept(ACSLTermVisitor<R, X> visitor) throws X {
    return visitor.visit(this);
  }
}
