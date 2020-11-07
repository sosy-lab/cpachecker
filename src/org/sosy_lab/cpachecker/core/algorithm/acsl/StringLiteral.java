// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.acsl;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class StringLiteral implements ACSLTerm {

  private final String literal;

  public StringLiteral(String s) {
    literal = s;
  }

  @Override
  public String toString() {
    return literal;
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof StringLiteral) {
      StringLiteral other = (StringLiteral) o;
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
  public CExpression accept(ACSLTermToCExpressionVisitor visitor) throws UnrecognizedCodeException {
    return visitor.visit(this);
  }

  @Override
  public boolean isAllowedIn(Class<?> clauseType) {
    return true;
  }

  @Override
  public Set<ACSLBuiltin> getUsedBuiltins() {
    return ImmutableSet.of();
  }
}
