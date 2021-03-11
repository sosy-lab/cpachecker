// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.acsl;

import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class Cast implements ACSLTerm {

  private final Type type;
  private final ACSLTerm term;

  public Cast(Type pType, ACSLTerm pTerm) {
    type = pType;
    term = pTerm;
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof Cast) {
      Cast other = (Cast) o;
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
    return "(" + type.toString() + ") " + term.toString();
  }

  public Type getType() {
    return type;
  }

  public ACSLTerm getTerm() {
    return term;
  }

  @Override
  public CExpression accept(ACSLTermToCExpressionVisitor visitor) throws UnrecognizedCodeException {
    return visitor.visit(this);
  }

  @Override
  public boolean isAllowedIn(Class<?> clauseType) {
    return term.isAllowedIn(clauseType);
  }

  @Override
  public Set<ACSLBuiltin> getUsedBuiltins() {
    return term.getUsedBuiltins();
  }

  @Override
  public LogicExpression apply(Set<Binder> binders, Binder.Quantifier quantifier) {
    return new Cast(type, (ACSLTerm) term.apply(binders, quantifier));
  }
}
