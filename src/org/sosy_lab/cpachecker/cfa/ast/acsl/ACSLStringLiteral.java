// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

public final class ACSLStringLiteral implements ACSLTerm {

  private final String literal;

  public ACSLStringLiteral(String s) {
    literal = s;
  }

  @Override
  public String toString() {
    return literal;
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof ACSLStringLiteral other) {
      return literal.equals(other.literal);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return 23 * literal.hashCode() * literal.hashCode() + 23;
  }

  public String getLiteral() {
    return literal;
  }

  @Override
  public boolean isAllowedIn(Class<?> clauseType) {
    return true;
  }

  @Override
  public <R, X extends Exception> R accept(ACSLTermVisitor<R, X> visitor) throws X {
    return visitor.visit(this);
  }
}
