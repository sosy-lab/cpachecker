// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

import java.math.BigInteger;

public class ACSLIntegerLiteral implements ACSLTerm {

  private final BigInteger literal;

  public ACSLIntegerLiteral(BigInteger i) {
    literal = i;
  }

  @Override
  public String toString() {
    return literal.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof ACSLIntegerLiteral other) {
      return literal.equals(other.literal);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return 31 * literal.hashCode() * literal.hashCode() + 31;
  }

  public BigInteger getLiteral() {
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
