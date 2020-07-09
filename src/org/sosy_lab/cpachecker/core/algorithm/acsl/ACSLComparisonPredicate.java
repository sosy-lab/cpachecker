package org.sosy_lab.cpachecker.core.algorithm.acsl;

import com.google.common.base.Preconditions;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.LeafExpression;

public class ACSLComparisonPredicate extends ACSLPredicate {

  private final ACSLTerm term;

  public ACSLComparisonPredicate(ACSLTerm pTerm) {
    this(pTerm, false);
  }

  public ACSLComparisonPredicate(ACSLTerm pTerm, boolean negated) {
    super(negated);
    Preconditions.checkArgument(
        pTerm instanceof ACSLBinaryTerm
            && BinaryOperator.isComparisonOperator(((ACSLBinaryTerm) pTerm).getOperator()),
        "Simple predicate should hold comparison term.");
    term = pTerm;
  }

  @Override
  public String toString() {
    String positiveTemplate = "%s";
    String negativeTemplate = "!(%s)";
    String template = isNegated() ? negativeTemplate : positiveTemplate;
    return String.format(template, term.toString());
  }

  @Override
  public ACSLPredicate negate() {
    return new ACSLComparisonPredicate(term, !isNegated());
  }

  @Override
  public ACSLPredicate simplify() {
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof ACSLComparisonPredicate) {
      ACSLComparisonPredicate other = (ACSLComparisonPredicate) o;
      return super.equals(o) && term.equals(other.term);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return super.hashCode() * 3 * term.hashCode();
  }

  @Override
  public boolean isNegationOf(ACSLPredicate other) {
    return simplify().equals(other.negate().simplify());
  }

  @Override
  public ExpressionTree<Object> toExpressionTree(ACSLToCExpressionVisitor visitor) {
    try {
      return LeafExpression.of(term.accept(visitor), !isNegated());
    } catch (UnrecognizedCodeException pE) {
      throw new AssertionError("Failed to convert term to CExpression: " + term.toString());
    }
  }

  public ACSLTerm getTerm() {
    return term;
  }

  @Override
  public ACSLPredicate useOldValues() {
    return new ACSLComparisonPredicate(term.useOldValues(), isNegated());
  }
}
