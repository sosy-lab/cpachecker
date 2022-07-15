// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

public class ACSLTernaryCondition extends ACSLPredicate {

  private final ACSLPredicate condition;
  private final ACSLPredicate then;
  private final ACSLPredicate otherwise;

  public ACSLTernaryCondition(ACSLPredicate p1, ACSLPredicate p2, ACSLPredicate p3) {
    this(p1, p2, p3, false);
  }

  public ACSLTernaryCondition(
      ACSLPredicate p1, ACSLPredicate p2, ACSLPredicate p3, boolean negated) {
    super(negated);
    condition = p1;
    then = p2;
    otherwise = p3;
  }

  public ACSLPredicate getCondition() {
    return condition;
  }

  public ACSLPredicate getThen() {
    return then;
  }

  public ACSLPredicate getOtherwise() {
    return otherwise;
  }

  @Override
  public String toString() {
    return condition.toString() + " ? " + then + " : " + otherwise;
  }

  @Override
  public ACSLPredicate negate() {
    return new ACSLTernaryCondition(condition, then, otherwise, !isNegated());
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
        ? new ACSLLogicalPredicate(simpleConditionNegated, simpleThenNegated, ACSLBinaryOperator.OR)
            .and(
                new ACSLLogicalPredicate(
                    simpleCondition, simpleOtherwiseNegated, ACSLBinaryOperator.OR))
        : new ACSLTernaryCondition(simpleCondition, simpleThen, simpleOtherwise);
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof ACSLTernaryCondition) {
      ACSLTernaryCondition other = (ACSLTernaryCondition) o;
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
  public boolean isAllowedIn(Class<?> clauseType) {
    return condition.isAllowedIn(clauseType)
        && then.isAllowedIn(clauseType)
        && otherwise.isAllowedIn(clauseType);
  }

  @Override
  public <R, X extends Exception> R accept(ACSLPredicateVisitor<R, X> visitor) throws X {
    return visitor.visit(this);
  }
}
