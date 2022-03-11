// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

public class ACSLLogicalPredicate extends ACSLPredicate {

  private final ACSLPredicate left;
  private final ACSLPredicate right;
  private final ACSLBinaryOperator operator;

  public ACSLLogicalPredicate(ACSLPredicate pLeft, ACSLPredicate pRight, ACSLBinaryOperator op) {
    this(pLeft, pRight, op, false);
  }

  public ACSLLogicalPredicate(
      ACSLPredicate pLeft, ACSLPredicate pRight, ACSLBinaryOperator op, boolean negated) {
    super(negated);
    assert ACSLBinaryOperator.isLogicalOperator(op)
        : "ACSLLogicalPredicate may only hold logical operator";
    switch (op) {
      case AND:
      case OR:
        left = pLeft;
        right = pRight;
        operator = op;
        break;
      case XOR:
        left = new ACSLLogicalPredicate(pLeft, pRight.negate(), ACSLBinaryOperator.AND);
        right = new ACSLLogicalPredicate(pLeft.negate(), pRight, ACSLBinaryOperator.AND);
        operator = ACSLBinaryOperator.OR;
        break;
      case IMP:
        left = pLeft.negate();
        right = pRight;
        operator = ACSLBinaryOperator.OR;
        break;
      case EQV:
        left = new ACSLLogicalPredicate(pLeft, pRight, ACSLBinaryOperator.AND);
        right = new ACSLLogicalPredicate(pLeft.negate(), pRight.negate(), ACSLBinaryOperator.AND);
        operator = ACSLBinaryOperator.OR;
        break;
      default:
        throw new AssertionError("Unknown logical operator: " + op);
    }
  }

  @Override
  public String toString() {
    String positiveTemplate = "(%s)";
    String negativeTemplate = "!(%s)";
    String template = isNegated() ? negativeTemplate : positiveTemplate;
    return String.format(template, left.toString() + operator + right);
  }

  @Override
  public ACSLPredicate negate() {
    return new ACSLLogicalPredicate(left, right, operator, !isNegated());
  }

  @Override
  public ACSLPredicate simplify() {
    ACSLPredicate simpleLeft = left.simplify();
    ACSLPredicate simpleLeftNegated = simpleLeft.negate();
    ACSLPredicate simpleRight = right.simplify();
    ACSLPredicate simpleRightNegated = simpleRight.negate();
    switch (operator) {
      case AND:
        if (simpleLeft.equals(getFalse())
            || simpleRight.equals(getFalse())
            || simpleLeft.isNegationOf(simpleRight)) {
          return isNegated() ? getTrue() : getFalse();
        } else if (simpleLeft.equals(simpleRight)) {
          return isNegated() ? simpleLeftNegated : simpleLeft;
        } else if (simpleLeft.equals(getTrue())) {
          return isNegated() ? simpleRightNegated : simpleRight;
        } else if (simpleRight.equals(getTrue())) {
          return isNegated() ? simpleLeftNegated : simpleLeft;
        } else if (isNegated()) {
          return new ACSLLogicalPredicate(
              simpleLeftNegated, simpleRightNegated, ACSLBinaryOperator.OR);
        }
        break;
      case OR:
        if (simpleLeft.equals(getTrue())
            || simpleRight.equals(getTrue())
            || simpleLeft.isNegationOf(simpleRight)) {
          return isNegated() ? getFalse() : getTrue();
        } else if (simpleLeft.equals(simpleRight)) {
          return isNegated() ? simpleLeftNegated : simpleLeft;
        } else if (simpleLeft.equals(getFalse())) {
          return isNegated() ? simpleRightNegated : simpleRight;
        } else if (simpleRight.equals(getFalse())) {
          return isNegated() ? simpleLeftNegated : simpleLeft;
        } else if (isNegated()) {
          return new ACSLLogicalPredicate(
              simpleLeftNegated, simpleRightNegated, ACSLBinaryOperator.AND);
        }
        break;
      default:
        throw new AssertionError("Unknown C logical operator: " + operator);
    }
    assert !isNegated();
    return new ACSLLogicalPredicate(simpleLeft, simpleRight, operator);
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof ACSLLogicalPredicate) {
      ACSLLogicalPredicate other = (ACSLLogicalPredicate) o;
      if (super.equals(o) && operator.equals(other.operator)) {
        return (left.equals(other.left) && right.equals(other.right))
            || (ACSLBinaryOperator.isCommutative(operator)
                && left.equals(other.right)
                && right.equals(other.left));
      }
    }
    return false;
  }

  @Override
  public int hashCode() {
    return super.hashCode() * (13 * left.hashCode() + 13 * right.hashCode() + operator.hashCode());
  }

  public ACSLPredicate getLeft() {
    return left;
  }

  public ACSLPredicate getRight() {
    return right;
  }

  public ACSLBinaryOperator getOperator() {
    return operator;
  }

  @Override
  public boolean isAllowedIn(Class<?> clauseType) {
    return left.isAllowedIn(clauseType) && right.isAllowedIn(clauseType);
  }

  @Override
  public <R, X extends Exception> R accept(ACSLPredicateVisitor<R, X> visitor) throws X {
    return visitor.visit(this);
  }
}
