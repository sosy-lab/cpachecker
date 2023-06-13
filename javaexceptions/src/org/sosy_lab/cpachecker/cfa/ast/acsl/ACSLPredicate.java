// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

public abstract sealed class ACSLPredicate implements ACSLLogicExpression
    permits ACSLLogicalPredicate,
        ACSLSimplePredicate,
        ACSLTernaryCondition,
        ACSLPredicate.FALSE,
        PredicateAt,
        ACSLPredicate.TRUE {

  private final boolean negated;

  protected ACSLPredicate(boolean pNegated) {
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

  /** Returns a simplified version of the predicate. */
  public abstract ACSLPredicate simplify();

  /** Returns the conjunction of the predicate with the given other predicate. */
  public ACSLPredicate and(ACSLPredicate other) {
    return new ACSLLogicalPredicate(this, other, ACSLBinaryOperator.AND);
  }

  /** Returns the disjunction of the predicate with the given other predicate. */
  public ACSLPredicate or(ACSLPredicate other) {
    return new ACSLLogicalPredicate(this, other, ACSLBinaryOperator.OR);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof ACSLPredicate other) {
      return negated == other.negated;
    }
    return false;
  }

  @Override
  public int hashCode() {
    return isNegated() ? -1 : 1;
  }

  /**
   * Returns true iff the given ACSLPredicate is a negation of the one this method is called on. It
   * is advised to call <code>simplify()</code> on both predicates before calling this method.
   *
   * @param other The predicate that shall be compared with <code>this</code>.
   * @return true if <code>this</code> is a negation of <code>other</code>, false otherwise.
   */
  public boolean isNegationOf(ACSLPredicate other) {
    return equals(other.negate());
  }

  public abstract <R, X extends Exception> R accept(ACSLPredicateVisitor<R, X> visitor) throws X;

  static final class TRUE extends ACSLPredicate {

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
    public ACSLPredicate simplify() {
      return this;
    }

    @Override
    public ACSLPredicate negate() {
      return ACSLPredicate.getFalse();
    }

    @Override
    public boolean isNegated() {
      assert !super.isNegated() : "True should not be explicitly negated, should be False instead!";
      return false;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof TRUE) {
        assert obj == singleton && this == singleton;
        return true;
      }
      return false;
    }

    @Override
    public int hashCode() {
      return 23;
    }

    @Override
    public boolean isAllowedIn(Class<?> clauseType) {
      return true;
    }

    @Override
    public <R, X extends Exception> R accept(ACSLPredicateVisitor<R, X> visitor) throws X {
      return visitor.visitTrue();
    }
  }

  static final class FALSE extends ACSLPredicate {

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
    public ACSLPredicate simplify() {
      return this;
    }

    @Override
    public ACSLPredicate negate() {
      return ACSLPredicate.getTrue();
    }

    @Override
    public boolean isNegated() {
      assert !super.isNegated() : "False should not be explicitly negated, should be True instead!";
      return false;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof FALSE) {
        assert obj == singleton && this == singleton;
        return true;
      }
      return false;
    }

    @Override
    public int hashCode() {
      return 19;
    }

    @Override
    public boolean isAllowedIn(Class<?> clauseType) {
      return true;
    }

    @Override
    public <R, X extends Exception> R accept(ACSLPredicateVisitor<R, X> visitor) throws X {
      return visitor.visitFalse();
    }
  }
}
