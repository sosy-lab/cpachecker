// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

public class ACSLIdentifier implements ACSLTerm {

  private final String name;
  private final String functionName;

  public ACSLIdentifier(String pName, String pFunctionName) {
    name = pName;
    functionName = pFunctionName;
  }

  @Override
  public String toString() {
    return name;
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof ACSLIdentifier) {
      ACSLIdentifier other = (ACSLIdentifier) o;
      return name.equals(other.name) && functionName.equals(other.functionName);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return 29 * name.hashCode() * name.hashCode()
        + 13 * functionName.hashCode() * functionName.hashCode()
        + 29;
  }

  public String getName() {
    return name;
  }

  public String getFunctionName() {
    return functionName;
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
