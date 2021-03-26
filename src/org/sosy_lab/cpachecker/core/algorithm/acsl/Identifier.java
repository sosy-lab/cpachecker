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

public class Identifier implements ACSLTerm {

  private final String name;
  private final String functionName;
  // TODO: Needs a type! Perhaps use MemoryLocation instead altogether?

  public Identifier(String pName, String pFunctionName) {
    name = pName;
    functionName = pFunctionName;
  }

  @Override
  public String toString() {
    return name;
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof Identifier) {
      Identifier other = (Identifier) o;
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
