// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

public final class ACSLCast implements ACSLTerm {

  private final ACSLType type;
  private final ACSLTerm term;

  public ACSLCast(ACSLType pType, ACSLTerm pTerm) {
    type = pType;
    term = pTerm;
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof ACSLCast other) {
      return type.equals(other.type) && term.equals(other.term);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return 17 * term.hashCode() + type.hashCode();
  }

  @Override
  public String toString() {
    return "(" + type + ") " + term;
  }

  public ACSLType getType() {
    return type;
  }

  public ACSLTerm getTerm() {
    return term;
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
