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

public class ACSLUnaryTerm implements ACSLTerm {

  private final ACSLTerm term;
  private final UnaryOperator operator;

  public ACSLUnaryTerm(ACSLTerm pTerm, UnaryOperator op) {
    term = pTerm;
    operator = op;
  }

  @Override
  public String toString() {
    if (operator.equals(UnaryOperator.SIZEOF)) {
      return operator.toString() + "(" + term.toString() + ")";
    }
    return operator.toString() + term.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof ACSLUnaryTerm) {
      ACSLUnaryTerm other = (ACSLUnaryTerm) o;
      return term.equals(other.term) && operator.equals(other.operator);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return 7 * term.hashCode() + operator.hashCode();
  }

  public ACSLTerm getInnerTerm() {
    return term;
  }

  public UnaryOperator getOperator() {
    return operator;
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
    return new ACSLUnaryTerm((ACSLTerm) term.apply(binders, quantifier), operator);
  }
}
