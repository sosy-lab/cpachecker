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
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;

public class At extends ACSLPredicate implements ACSLBuiltin {

  private final LogicExpression inner;
  private final ACSLLabel label;

  public At(LogicExpression pInner, ACSLLabel pLabel) {
    this(pInner, pLabel, false);
  }

  public At(LogicExpression pInner, ACSLLabel pLabel, boolean negated) {
    super(negated);
    inner = pInner;
    label = pLabel;
  }

  public LogicExpression getInner() {
    return inner;
  }

  public ACSLLabel getLabel() {
    return label;
  }

  @Override
  public String toString() {
    return "\\at(" + inner.toString() + ", " + label.toString() + ")";
  }

  @Override
  public int hashCode() {
    return 7 * inner.hashCode();
  }

  @Override
  public ExpressionTree<Object> toExpressionTree(ACSLTermToCExpressionVisitor visitor) {
    throw new UnsupportedOperationException(
        "There is currently no concrete translation of \\at available");
  }

  @Override
  public ACSLPredicate negate() {
    return new At(inner, label, !isNegated());
  }

  @Override
  public ACSLPredicate simplify() {
    return this;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof At) {
      At other = (At) obj;
      return inner.equals(other.inner) && label.equals(other.label);
    }
    return false;
  }

  @Override
  public CExpression accept(ACSLTermToCExpressionVisitor visitor) throws UnrecognizedCodeException {
    return visitor.visit(this);
  }

  @Override
  public boolean isAllowedIn(Class<?> clauseType) {
    return inner.isAllowedIn(clauseType);
  }

  @Override
  public Set<ACSLBuiltin> getUsedBuiltins() {
    ImmutableSet.Builder<ACSLBuiltin> builder = ImmutableSet.builder();
    builder.addAll(inner.getUsedBuiltins());
    builder.add(this);
    return builder.build();
  }

  @Override
  public LogicExpression apply(Set<Binder> binders, Binder.Quantifier quantifier) {
    return new At(inner.apply(binders, quantifier), label, isNegated());
  }
}
