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

public class TernaryCondition extends ACSLPredicate {

  private final ACSLPredicate condition;
  private final ACSLPredicate then;
  private final ACSLPredicate otherwise;

  public TernaryCondition(ACSLPredicate p1, ACSLPredicate p2, ACSLPredicate p3) {
    this(p1, p2, p3, false);
  }

  public TernaryCondition(ACSLPredicate p1, ACSLPredicate p2, ACSLPredicate p3, boolean negated) {
    super(negated);
    condition = p1;
    then = p2;
    otherwise = p3;
  }

  @Override
  public String toString() {
    return condition.toString() + " ? " + then.toString() + " : " + otherwise.toString();
  }

  @Override
  public ACSLPredicate negate() {
    return new TernaryCondition(condition, then, otherwise, !isNegated());
  }

  @Override
  public ACSLPredicate simplify() {
    ACSLPredicate simpleCondition = condition.simplify();
    ACSLPredicate simpleConditionNegated = simpleCondition.negate();
    ACSLPredicate simpleThen = then.simplify();
    ACSLPredicate simpleThenNegated = simpleThen.negate();
    ACSLPredicate simpleOtherwise = otherwise.simplify();
    ACSLPredicate simpleOtherwiseNegated = simpleOtherwise.negate();
    if (simpleCondition.equals(getTrue())) {
      return isNegated() ? simpleThenNegated : simpleThen;
    } else if (simpleCondition.equals(getFalse())) {
      return isNegated() ? simpleOtherwiseNegated : simpleOtherwise;
    } else if (then.equals(otherwise) || simpleThen.equals(simpleOtherwise)) {
      return isNegated() ? simpleThenNegated : simpleThen;
    }
    return isNegated()
        ? new ACSLLogicalPredicate(simpleConditionNegated, simpleThenNegated, BinaryOperator.OR)
            .and(
                new ACSLLogicalPredicate(
                    simpleCondition, simpleOtherwiseNegated, BinaryOperator.OR))
        : new TernaryCondition(simpleCondition, simpleThen, simpleOtherwise);
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof TernaryCondition) {
      TernaryCondition other = (TernaryCondition) o;
      return super.equals(o)
          && condition.equals(other.condition)
          && then.equals(other.then)
          && otherwise.equals(other.otherwise);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return super.hashCode()
        * (19 * condition.hashCode() + 11 * then.hashCode() + otherwise.hashCode());
  }

  @Override
  public ExpressionTree<Object> toExpressionTree(ACSLTermToCExpressionVisitor visitor) {
    if (isNegated()) {
      ExpressionTree<Object> left =
          Or.of(
              condition.negate().toExpressionTree(visitor),
              then.negate().toExpressionTree(visitor));
      ExpressionTree<Object> right =
          Or.of(condition.toExpressionTree(visitor), otherwise.negate().toExpressionTree(visitor));
      return And.of(left, right);
    }
    ExpressionTree<Object> left =
        And.of(condition.toExpressionTree(visitor), then.toExpressionTree(visitor));
    ExpressionTree<Object> right =
        And.of(condition.negate().toExpressionTree(visitor), otherwise.toExpressionTree(visitor));
    return Or.of(left, right);
  }

  @Override
  public boolean isAllowedIn(Class<?> clauseType) {
    return condition.isAllowedIn(clauseType)
        && then.isAllowedIn(clauseType)
        && otherwise.isAllowedIn(clauseType);
  }

  @Override
  public Set<ACSLBuiltin> getUsedBuiltins() {
    ImmutableSet.Builder<ACSLBuiltin> builder = ImmutableSet.builder();
    return builder
        .addAll(condition.getUsedBuiltins())
        .addAll(then.getUsedBuiltins())
        .addAll(otherwise.getUsedBuiltins())
        .build();
  }

  @Override
  public LogicExpression apply(Set<Binder> binders, Binder.Quantifier quantifier) {
    return new TernaryCondition(
        (ACSLPredicate) condition.apply(binders, quantifier),
        (ACSLPredicate) then.apply(binders, quantifier),
        (ACSLPredicate) otherwise.apply(binders, quantifier),
        isNegated());
  }
}
