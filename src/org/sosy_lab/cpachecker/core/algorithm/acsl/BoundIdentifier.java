// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.acsl;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class BoundIdentifier implements ACSLTerm {

  private final String name;
  private final String functionName;
  private final Type type;
  private final Binder.Quantifier quantifier;

  public BoundIdentifier(
      String pName, String pFunctionName, Type pType, Binder.Quantifier pQuantifier) {
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
    if (o instanceof BoundIdentifier) {
      BoundIdentifier other = (BoundIdentifier) o;
      return name.equals(other.name);
    }
    return false;
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

  public Type getType() {
    return type;
  }

  public Binder.Quantifier getQuantifier() {
    return quantifier;
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

  @Override
  public LogicExpression apply(Set<Binder> binders, Binder.Quantifier pQuantifier) {
    return this;
  }
}
