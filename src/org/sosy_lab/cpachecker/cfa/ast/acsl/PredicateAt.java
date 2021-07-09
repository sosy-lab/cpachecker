// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

public class PredicateAt extends ACSLPredicate implements ACSLBuiltin {

  private final ACSLPredicate inner;
  private final ACSLLabel label;

  public PredicateAt(ACSLPredicate pInner, ACSLLabel pLabel) {
    this(pInner, pLabel, false);
  }

  public PredicateAt(ACSLPredicate pInner, ACSLLabel pLabel, boolean negated) {
    super(negated);
    inner = pInner;
    label = pLabel;
  }

  public ACSLPredicate getInner() {
    return inner;
  }

  public ACSLLabel getLabel() {
    return label;
  }

  @Override
  public String toString() {
    return "\\at(" + inner + ", " + label + ")";
  }

  @Override
  public int hashCode() {
    return 7 * inner.hashCode();
  }

  @Override
  public ACSLPredicate negate() {
    return new PredicateAt(inner, label, !isNegated());
  }

  @Override
  public ACSLPredicate simplify() {
    return this;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof PredicateAt) {
      PredicateAt other = (PredicateAt) obj;
      return inner.equals(other.inner) && label.equals(other.label);
    }
    return false;
  }

  @Override
  public boolean isAllowedIn(Class<?> clauseType) {
    if (label.equals(ACSLDefaultLabel.OLD)) {
      return clauseType.equals(EnsuresClause.class) && inner.isAllowedIn(clauseType);
    }
    return inner.isAllowedIn(clauseType);
  }

  @Override
  public <R, X extends Exception> R accept(ACSLPredicateVisitor<R, X> visitor) throws X {
    return visitor.visit(this);
  }
}
