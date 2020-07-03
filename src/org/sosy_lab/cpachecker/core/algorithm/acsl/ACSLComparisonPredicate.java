package org.sosy_lab.cpachecker.core.algorithm.acsl;

import com.google.common.base.Preconditions;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.LeafExpression;

public class ACSLComparisonPredicate extends ACSLPredicate {

  private final ACSLTerm left;
  private final ACSLTerm right;
  private final BinaryOperator operator;

  public ACSLComparisonPredicate(ACSLTerm pLeft, ACSLTerm pRight, BinaryOperator op) {
    super();
    left = pLeft;
    right = pRight;
    Preconditions.checkArgument(
        BinaryOperator.isComparisonOperator(op), "Unknown comparison operator: %s", op);
    operator = op;
  }

  @Override
  public String toString() {
    String positiveTemplate = "%s";
    String negativeTemplate = "!(%s)";
    String template = isNegated() ? negativeTemplate : positiveTemplate;
    return String.format(template, left.toString() + operator.toString() + right.toString());
  }

  @Override
  public ACSLPredicate toPureC() {
    return new ACSLComparisonPredicate(left.toPureC(), right.toPureC(), operator);
  }

  @Override
  public ACSLPredicate getCopy() {
    return new ACSLComparisonPredicate(left, right, operator);
  }

  @Override
  public ACSLPredicate simplify() {
    if (isNegated()) {
      switch (operator) {
        case EQ:
          return new ACSLComparisonPredicate(left, right, BinaryOperator.NEQ);
        case NEQ:
          return new ACSLComparisonPredicate(left, right, BinaryOperator.EQ);
        case GT:
          return new ACSLComparisonPredicate(left, right, BinaryOperator.LEQ);
        case LT:
          return new ACSLComparisonPredicate(left, right, BinaryOperator.GEQ);
        case LEQ:
          return new ACSLComparisonPredicate(left, right, BinaryOperator.GT);
        case GEQ:
          return new ACSLComparisonPredicate(left, right, BinaryOperator.LT);
        default:
          throw new AssertionError("Unknown comparison operator: " + operator);
      }
    }
    return this;
  }

  @Override
  public boolean equals(Object o) {
    return equalsExceptNegation(o, true);
  }

  @Override
  public int hashCode() {
    int sign = isNegated() ? -1 : 1;
    return sign * (19 * left.hashCode() + 13 * right.hashCode() + operator.hashCode());
  }

  private boolean equalsExceptNegation(Object o, boolean shouldNegationMatch) {
    if (o instanceof ACSLComparisonPredicate) {
      ACSLComparisonPredicate other = (ACSLComparisonPredicate) o;
      if (shouldNegationMatch == (isNegated() == other.isNegated())) {
        return left.equals(other.left)
            && right.equals(other.right)
            && operator.equals(other.operator);
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
    try {
      CExpression exp = visitor.visit(this);
      return LeafExpression.of(exp, !isNegated());
    } catch (UnrecognizedCodeException pE) {
      throw new AssertionError("Failed to convert to CExpression: " + toString());
    }
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
}
