// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.acsl;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class ACSLBinaryTerm implements ACSLTerm {

  private final ACSLTerm left;
  private final ACSLTerm right;
  private final BinaryOperator operator;

  public ACSLBinaryTerm(ACSLTerm pLeft, ACSLTerm pRight, BinaryOperator op) {
    assert BinaryOperator.isArithmeticOperator(op)
            || BinaryOperator.isBitwiseOperator(op)
            || BinaryOperator.isComparisonOperator(op)
        : String.format(
            "ACSLTerm may only hold arithmetic, bitwise or comparison operator, %s is neither", op);
    switch (op) {
      case PLUS:
      case MINUS:
      case DIVIDE:
      case TIMES:
      case MOD:
      case LSHIFT:
      case RSHIFT:
      case BAND:
      case BOR:
      case BXOR:
      case EQ:
      case NEQ:
      case LEQ:
      case GEQ:
      case LT:
      case GT:
        left = pLeft;
        right = pRight;
        operator = op;
        break;
      case BIMP:
        left = new ACSLUnaryTerm(pLeft, UnaryOperator.BNEG);
        right = pRight;
        operator = BinaryOperator.BOR;
        break;
      case BEQV:
        left = new ACSLUnaryTerm(pLeft, UnaryOperator.BNEG);
        right = pRight;
        operator = BinaryOperator.BXOR;
        break;
      default:
        throw new AssertionError("Unknown operator: " + op);
    }
  }

  @Override
  public String toString() {
    return left.toString() + operator.toString() + right.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof ACSLBinaryTerm) {
      ACSLBinaryTerm other = (ACSLBinaryTerm) o;
      if (operator.equals(other.operator)) {
        return (left.equals(other.left) && right.equals(other.right))
            || (BinaryOperator.isCommutative(operator)
                && left.equals(other.right)
                && right.equals(other.left));
      }
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

  public BinaryOperator getOperator() {
    return operator;
  }

  public ACSLBinaryTerm flipOperator() {
    assert BinaryOperator.isComparisonOperator(operator);
    BinaryOperator op;
    switch (operator) {
      case EQ:
        op = BinaryOperator.NEQ;
        break;
      case NEQ:
        op = BinaryOperator.EQ;
        break;
      case LEQ:
        op = BinaryOperator.GT;
        break;
      case GEQ:
        op = BinaryOperator.LT;
        break;
      case LT:
        op = BinaryOperator.GEQ;
        break;
      case GT:
        op = BinaryOperator.LEQ;
        break;
      default:
        throw new AssertionError("Unknown BinaryOperator: " + operator);
    }
    return new ACSLBinaryTerm(left, right, op);
  }

  @Override
  public CExpression accept(ACSLTermToCExpressionVisitor visitor) throws UnrecognizedCodeException {
    return visitor.visit(this);
  }

  @Override
  public boolean isAllowedIn(Class<?> clauseType) {
    return left.isAllowedIn(clauseType) && right.isAllowedIn(clauseType);
  }

  @Override
  public Set<ACSLBuiltin> getUsedBuiltins() {
    ImmutableSet.Builder<ACSLBuiltin> builder = ImmutableSet.builder();
    return builder.addAll(left.getUsedBuiltins()).addAll(right.getUsedBuiltins()).build();
  }

  @Override
  public LogicExpression apply(Set<Binder> binders, Binder.Quantifier quantifier) {
    return new ACSLBinaryTerm(
        (ACSLTerm) left.apply(binders, quantifier),
        (ACSLTerm) right.apply(binders, quantifier),
        operator);
  }
}
