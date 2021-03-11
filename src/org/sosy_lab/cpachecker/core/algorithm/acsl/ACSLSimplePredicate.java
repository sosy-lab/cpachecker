// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.acsl;

import com.google.common.base.Preconditions;
import java.util.Set;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.LeafExpression;

public class ACSLSimplePredicate extends ACSLPredicate {

  private final ACSLTerm term;

  public ACSLSimplePredicate(ACSLTerm pTerm) {
    this(pTerm, false);
  }

  public ACSLSimplePredicate(ACSLTerm pTerm, boolean negated) {
    super(negated);
    Preconditions.checkArgument(
        pTerm instanceof ACSLBinaryTerm
            && BinaryOperator.isComparisonOperator(((ACSLBinaryTerm) pTerm).getOperator()),
        "Simple predicate should hold comparison term.");
    term = pTerm;
  }

  @Override
  public String toString() {
    String positiveTemplate = "%s";
    String negativeTemplate = "!(%s)";
    String template = isNegated() ? negativeTemplate : positiveTemplate;
    return String.format(template, term.toString());
  }

  @Override
  public ACSLPredicate negate() {
    return new ACSLSimplePredicate(term, !isNegated());
  }

  @Override
  public ACSLPredicate simplify() {
    if (isNegated()) {
      return new ACSLSimplePredicate(((ACSLBinaryTerm) term).flipOperator());
    }
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof ACSLSimplePredicate) {
      ACSLSimplePredicate other = (ACSLSimplePredicate) o;
      return super.equals(o) && term.equals(other.term);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return super.hashCode() * 3 * term.hashCode();
  }

  @Override
  public ExpressionTree<Object> toExpressionTree(ACSLTermToCExpressionVisitor visitor) {
    try {
      return LeafExpression.of(term.accept(visitor), !isNegated());
    } catch (UnrecognizedCodeException pE) {
      throw new AssertionError("Failed to convert term to CExpression: " + term.toString());
    }
  }

  public ACSLTerm getTerm() {
    return term;
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
    return new ACSLSimplePredicate((ACSLTerm) term.apply(binders, quantifier), isNegated());
  }
}
