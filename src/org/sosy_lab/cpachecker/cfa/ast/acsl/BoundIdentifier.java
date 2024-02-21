// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

public final class BoundIdentifier implements ACSLTerm {

  private final String name;
  private final String functionName;
  private final ACSLType type;
  private final Binder.Quantifier quantifier;

  public BoundIdentifier(
      String pName, String pFunctionName, ACSLType pType, Binder.Quantifier pQuantifier) {
    name = pName;
    functionName = pFunctionName;
    type = pType;
    quantifier = pQuantifier;
  }

  @Override
  public String toString() {
    return name;
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof BoundIdentifier other && name.equals(other.name);
  }

  @Override
  public int hashCode() {
    return 31 * name.hashCode() * name.hashCode() + 17;
  }

  public String getName() {
    return name;
  }

  public String getFunctionName() {
    return functionName;
  }

  public ACSLType getType() {
    return type;
  }

  public Binder.Quantifier getQuantifier() {
    return quantifier;
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
