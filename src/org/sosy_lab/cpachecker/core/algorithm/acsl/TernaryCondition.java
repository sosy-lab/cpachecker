package org.sosy_lab.cpachecker.core.algorithm.acsl;

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
  public ACSLPredicate toPureC() {
    return new TernaryCondition(condition.toPureC(), then.toPureC(), otherwise.toPureC());
  }

  @Override
  public ACSLPredicate simplify() {
    return new TernaryCondition(
        condition.simplify(), then.simplify(), otherwise.simplify(), isNegated());
  }

  @Override
  public boolean equals(Object obj) {
    return equalsExceptNegation(obj, true);
  }

  @Override
  public int hashCode() {
    int sign = isNegated() ? -1 : 1;
    return sign * (19 * condition.hashCode() + 11 * then.hashCode() + otherwise.hashCode());
  }

  private boolean equalsExceptNegation(Object o, boolean shouldNegationMatch) {
    if (o instanceof TernaryCondition) {
      TernaryCondition other = (TernaryCondition) o;
      if (shouldNegationMatch == (isNegated() == other.isNegated())) {
        return condition.equals(other.condition)
            && then.equals(other.then)
            && otherwise.equals(other.otherwise);
      }
    }
    return false;
  }

  @Override
  public boolean isNegationOf(ACSLPredicate other) {
    return equalsExceptNegation(other, false);
  }

  @Override
  public ExpressionTree<Object> toExpressionTree(ACSLToCExpressionVisitor visitor) {
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
  public ACSLPredicate useOldValues() {
    return new TernaryCondition(
        condition.useOldValues(), then.useOldValues(), otherwise.useOldValues(), isNegated());
  }
}
