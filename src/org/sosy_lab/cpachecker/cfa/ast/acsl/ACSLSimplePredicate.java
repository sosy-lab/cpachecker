// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

import com.google.common.base.Preconditions;

public final class ACSLSimplePredicate extends ACSLPredicate {

  private final ACSLTerm term;

  public ACSLSimplePredicate(ACSLTerm pTerm) {
    this(pTerm, false);
  }

  public ACSLSimplePredicate(ACSLTerm pTerm, boolean negated) {
    super(negated);
    Preconditions.checkArgument(
        pTerm instanceof ACSLBinaryTerm
            && ACSLBinaryOperator.isComparisonOperator(((ACSLBinaryTerm) pTerm).getOperator()),
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
    return new ACSLSimplePredicate(term, !isNegated());
  }

  @Override
  public ACSLPredicate simplify() {
    if (isNegated()) {
      return new ACSLSimplePredicate(((ACSLBinaryTerm) term).flipOperator());
    }
    return this;
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof ACSLSimplePredicate other && super.equals(o) && term.equals(other.term);
  }

  @Override
  public int hashCode() {
    return super.hashCode() * 3 * term.hashCode();
  }

  public ACSLTerm getTerm() {
    return term;
  }

  @Override
  public boolean isAllowedIn(Class<?> clauseType) {
    return term.isAllowedIn(clauseType);
  }

  @Override
  public <R, X extends Exception> R accept(ACSLPredicateVisitor<R, X> visitor) throws X {
    return visitor.visit(this);
  }
}
