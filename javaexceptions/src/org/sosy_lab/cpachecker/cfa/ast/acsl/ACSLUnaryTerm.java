// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

public final class ACSLUnaryTerm implements ACSLTerm {

  private final ACSLTerm term;
  private final ACSLUnaryOperator operator;

  public ACSLUnaryTerm(ACSLTerm pTerm, ACSLUnaryOperator op) {
    term = pTerm;
    operator = op;
  }

  @Override
  public String toString() {
    if (operator.equals(ACSLUnaryOperator.SIZEOF)) {
      return operator.toString() + "(" + term + ")";
    }
    return operator.toString() + term;
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof ACSLUnaryTerm other) {
      return term.equals(other.term) && operator.equals(other.operator);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return 7 * term.hashCode() + operator.hashCode();
  }

  public ACSLTerm getInnerTerm() {
    return term;
  }

  public ACSLUnaryOperator getOperator() {
    return operator;
  }

  @Override
  public boolean isAllowedIn(Class<?> clauseType) {
    return term.isAllowedIn(clauseType);
  }

  @Override
  public <R, X extends Exception> R accept(ACSLTermVisitor<R, X> visitor) throws X {
    return visitor.visit(this);
  }
}
