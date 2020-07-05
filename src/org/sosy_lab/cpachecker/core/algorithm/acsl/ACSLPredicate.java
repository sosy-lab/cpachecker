package org.sosy_lab.cpachecker.core.algorithm.acsl;

import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;

public abstract class ACSLPredicate {

  private boolean negated;

  public ACSLPredicate(boolean pNegated) {
    negated = pNegated;
  }

  public static ACSLPredicate getTrue() {
    return TRUE.get();
  }

  public static ACSLPredicate getFalse() {
    return FALSE.get();
  }

  /** Returns the negation of the predicate. */
  public abstract ACSLPredicate negate();

  public boolean isNegated() {
    return negated;
  }

  /**
   * Returns a copy of the predicate that has the same logical value as the original but is a valid
   * C expression.
   */
  public abstract ACSLPredicate toPureC();

  /** Returns a simplified version of the predicate. */
  public abstract ACSLPredicate simplify();

  /** Returns the conjunction of the predicate with the given other predicate. */
  public ACSLPredicate and(ACSLPredicate other) {
    return new ACSLLogicalPredicate(this, other, BinaryOperator.AND);
  }

  /** Returns the disjunction of the predicate with the given other predicate. */
  public ACSLPredicate or(ACSLPredicate other) {
    return new ACSLLogicalPredicate(this, other, BinaryOperator.OR);
  }

  /**
   * Returns true iff the given ACSLPredicate is a negation of the one this method is called on. It
   * is advised to call <code>simplify()</code> on both predicates before calling this method.
   *
   * @param other The predicate that shall be compared with <code>this</code>.
   * @return true if <code>this</code> is a negation of <code>other</code>, false otherwise.
   */
  // TODO: All non-trivial implementations of isNegationOf are currently too weak
  public abstract boolean isNegationOf(ACSLPredicate other);

  /**
   * Returns an expression tree representing the predicate.
   *
   * @param visitor Visitor for converting terms to CExpressions.
   * @return An expression tree representation of the predicate.
   */
  public abstract ExpressionTree<Object> toExpressionTree(ACSLToCExpressionVisitor visitor);

  /**
   * Returns a version of the predicate where each identifier is augmented with the ACSL builtin
   * predicate "\old" to signal to use the value from the pre-state when evaluating.
   */
  public abstract ACSLPredicate useOldValues();

  private static class TRUE extends ACSLPredicate {

    private static final TRUE singleton = new TRUE();

    private static TRUE get() {
      return singleton;
    }

    private TRUE() {
      super(false);
    }

    @Override
    public String toString() {
      return "true";
    }

    @Override
    public ACSLPredicate toPureC() {
      return this;
    }

    @Override
    public ACSLPredicate simplify() {
      return isNegated() ? ACSLPredicate.getFalse() : this;
    }

    @Override
    public ACSLPredicate negate() {
      return ACSLPredicate.getFalse();
    }

    @Override
    public boolean isNegationOf(ACSLPredicate other) {
      return other instanceof FALSE;
    }

    @Override
    public ExpressionTree<Object> toExpressionTree(ACSLToCExpressionVisitor visitor) {
      return ExpressionTrees.getTrue();
    }

    @Override
    public ACSLPredicate useOldValues() {
      return this;
    }
  }

  private static class FALSE extends ACSLPredicate {

    private static final FALSE singleton = new FALSE();

    private static FALSE get() {
      return singleton;
    }

    private FALSE() {
      super(false);
    }

    @Override
    public String toString() {
      return "false";
    }

    @Override
    public ACSLPredicate toPureC() {
      return this;
    }

    @Override
    public ACSLPredicate simplify() {
      return isNegated() ? ACSLPredicate.getTrue() : this;
    }

    @Override
    public ACSLPredicate negate() {
      return ACSLPredicate.getTrue();
    }

    @Override
    public boolean isNegationOf(ACSLPredicate other) {
      return other instanceof TRUE;
    }

    @Override
    public ExpressionTree<Object> toExpressionTree(ACSLToCExpressionVisitor visitor) {
      return ExpressionTrees.getFalse();
    }

    @Override
    public ACSLPredicate useOldValues() {
      return this;
    }
  }
}
