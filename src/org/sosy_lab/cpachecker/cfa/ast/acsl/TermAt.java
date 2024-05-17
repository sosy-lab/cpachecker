// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

public final class TermAt implements ACSLBuiltin, ACSLTerm {

  private final ACSLTerm inner;
  private final ACSLLabel label;

  public TermAt(ACSLTerm pInner, ACSLLabel pLabel) {
    inner = pInner;
    label = pLabel;
  }

  public ACSLTerm getInner() {
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
  public boolean equals(Object obj) {
    return obj instanceof TermAt other && inner.equals(other.inner) && label.equals(other.label);
  }

  @Override
  public boolean isAllowedIn(Class<?> clauseType) {
    if (label.equals(ACSLDefaultLabel.OLD)) {
      return clauseType.equals(EnsuresClause.class) && inner.isAllowedIn(clauseType);
    }
    return inner.isAllowedIn(clauseType);
  }

  @Override
  public <R, X extends Exception> R accept(ACSLTermVisitor<R, X> visitor) throws X {
    return visitor.visit(this);
  }
}
