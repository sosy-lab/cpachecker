package org.sosy_lab.cpachecker.core.algorithm.acsl;

import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;

public abstract class ACSLPredicate {

  private boolean negated;

  public ACSLPredicate() {
    negated = false;
  }

  public static ACSLPredicate getTrue() {
    return TRUE.get();
  }

  public static ACSLPredicate getFalse() {
    return FALSE.get();
  }

  /** Negates and returns the predicate. */
  public ACSLPredicate negate() {
    negated = !negated;
    return this;
  }

  public boolean isNegated() {
    return negated;
  }

  /**
   * Returns a copy of the predicate that has the same logical value as the original but is a valid
   * C expression.
   */
  public abstract ACSLPredicate toPureC();

  /** Returns a deep copy of the predicate. */
  public abstract ACSLPredicate getCopy();

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
   * @return An expression tree represntation of the predicate.
   */
  public abstract ExpressionTree<Object> toExpressionTree(ACSLToCExpressionVisitor visitor);

  private static class TRUE extends ACSLPredicate {

    private static final TRUE singleton = new TRUE();

    private static TRUE get() {
      return singleton;
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
    public ACSLPredicate getCopy() {
      return singleton;
    }

    @Override
    public ACSLPredicate simplify() {
      return this;
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
  }

  private static class FALSE extends ACSLPredicate {

    private static final FALSE singleton = new FALSE();

    private static FALSE get() {
      return singleton;
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
    public ACSLPredicate getCopy() {
      return singleton;
    }

    @Override
    public ACSLPredicate simplify() {
      return this;
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
  }
}
