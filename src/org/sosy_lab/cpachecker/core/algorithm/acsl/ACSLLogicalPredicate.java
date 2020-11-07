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
import org.sosy_lab.cpachecker.util.expressions.And;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.Or;

public class ACSLLogicalPredicate extends ACSLPredicate {

  private final ACSLPredicate left;
  private final ACSLPredicate right;
  private final BinaryOperator operator;

  public ACSLLogicalPredicate(ACSLPredicate pLeft, ACSLPredicate pRight, BinaryOperator op) {
    this(pLeft, pRight, op, false);
  }

  public ACSLLogicalPredicate(
      ACSLPredicate pLeft, ACSLPredicate pRight, BinaryOperator op, boolean negated) {
    super(negated);
    assert BinaryOperator.isLogicalOperator(op)
        : "ACSLLogicalPredicate may only hold logical operator";
    switch (op) {
      case AND:
      case OR:
        left = pLeft;
        right = pRight;
        operator = op;
        break;
      case XOR:
        left = new ACSLLogicalPredicate(pLeft, pRight.negate(), BinaryOperator.AND);
        right = new ACSLLogicalPredicate(pLeft.negate(), pRight, BinaryOperator.AND);
        operator = BinaryOperator.OR;
        break;
      case IMP:
        left = pLeft.negate();
        right = pRight;
        operator = BinaryOperator.OR;
        break;
      case EQV:
        left = new ACSLLogicalPredicate(pLeft, pRight, BinaryOperator.AND);
        right = new ACSLLogicalPredicate(pLeft.negate(), pRight.negate(), BinaryOperator.AND);
        operator = BinaryOperator.OR;
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
    return String.format(template, left.toString() + operator.toString() + right.toString());
  }

  @Override
  public ACSLPredicate negate() {
    return new ACSLLogicalPredicate(left, right, operator, !isNegated());
  }

  @Override
  public ACSLPredicate simplify() {
    ACSLPredicate simpleLeft = left.simplify();
    ACSLPredicate simpleNegatedLeft = left.negate().simplify();
    ACSLPredicate simpleRight = right.simplify();
    ACSLPredicate simpleNegatedRight = right.negate().simplify();
    switch (operator) {
      case AND:
        if (simpleLeft.equals(getFalse())
            || simpleRight.equals(getFalse())
            || simpleLeft.isNegationOf(simpleRight)) {
          return isNegated() ? getTrue() : getFalse();
        } else if (simpleLeft.equals(simpleRight)) {
          return isNegated() ? simpleNegatedLeft : simpleLeft;
        } else if (simpleLeft.equals(getTrue())) {
          return isNegated() ? simpleNegatedRight : simpleRight;
        } else if (simpleRight.equals(getTrue())) {
          return isNegated() ? simpleNegatedLeft : simpleLeft;
        } else if (isNegated()) {
          return new ACSLLogicalPredicate(simpleNegatedLeft, simpleNegatedRight, BinaryOperator.OR);
        }
        break;
      case OR:
        if (simpleLeft.equals(getTrue())
            || simpleRight.equals(getTrue())
            || simpleLeft.isNegationOf(simpleRight)) {
          return isNegated() ? getFalse() : getTrue();
        } else if (simpleLeft.equals(simpleRight)) {
          return isNegated() ? simpleNegatedLeft : simpleLeft;
        } else if (simpleLeft.equals(getFalse())) {
          return isNegated() ? simpleNegatedRight : simpleRight;
        } else if (simpleRight.equals(getFalse())) {
          return isNegated() ? simpleNegatedLeft : simpleLeft;
        } else if (isNegated()) {
          return new ACSLLogicalPredicate(
              simpleNegatedLeft, simpleNegatedRight, BinaryOperator.AND);
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
            || (BinaryOperator.isCommutative(operator)
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

  @Override
  public boolean isNegationOf(ACSLPredicate o) {
    return simplify().equals(o.negate().simplify());
  }

  @Override
  public ExpressionTree<Object> toExpressionTree(ACSLTermToCExpressionVisitor visitor) {
    ExpressionTree<Object> leftTree;
    ExpressionTree<Object> rightTree;
    switch (operator) {
      case AND:
        if (isNegated()) {
          leftTree = left.negate().toExpressionTree(visitor);
          rightTree = right.negate().toExpressionTree(visitor);
          return Or.of(leftTree, rightTree);
        }
        leftTree = left.toExpressionTree(visitor);
        rightTree = right.toExpressionTree(visitor);
        return And.of(leftTree, rightTree);
      case OR:
        if (isNegated()) {
          leftTree = left.negate().toExpressionTree(visitor);
          rightTree = right.negate().toExpressionTree(visitor);
          return And.of(leftTree, rightTree);
        }
        leftTree = left.toExpressionTree(visitor);
        rightTree = right.toExpressionTree(visitor);
        return Or.of(leftTree, rightTree);
      default:
        throw new AssertionError("Pure predicate should contain AND or OR");
    }
  }

  public ACSLPredicate getRight() {
    return right;
  }

  @Override
  public boolean isAllowedIn(Class<?> clauseType) {
    return left.isAllowedIn(clauseType) && right.isAllowedIn(clauseType);
  }

  @Override
  public Set<ACSLBuiltin> getUsedBuiltins() {
    ImmutableSet.Builder<ACSLBuiltin> builder = ImmutableSet.builder();
    builder.addAll(left.getUsedBuiltins()).addAll(right.getUsedBuiltins());
    return builder.build();
  }
}
