// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

public class ACSLResult implements ACSLBuiltin, ACSLTerm {

  private final String functionName;

  public ACSLResult(String pFunctionName) {
    functionName = pFunctionName;
  }

  public String getFunctionName() {
    return functionName;
  }

  @Override
  public String toString() {
    return "\\result";
  }

  @Override
  public int hashCode() {
    return 3 * functionName.hashCode() * functionName.hashCode() + 3;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof ACSLResult) {
      ACSLResult other = (ACSLResult) obj;
      return functionName.equals(other.functionName);
    }
    return false;
  }

  @Override
  public boolean isAllowedIn(Class<?> clauseType) {
    return clauseType.equals(EnsuresClause.class);
  }

  @Override
  public <R, X extends Exception> R accept(ACSLTermVisitor<R, X> visitor) throws X {
    return visitor.visit(this);
  }
}
