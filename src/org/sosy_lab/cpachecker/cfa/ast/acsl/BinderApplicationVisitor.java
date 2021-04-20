// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.acsl.Binder.Quantifier;
import org.sosy_lab.cpachecker.exceptions.NoException;

/**
 * Visitor that replaces all identifiers captured by the given binders with bound identifiers.
 */
public class BinderApplicationVisitor
    implements ACSLTermVisitor<ACSLTerm, NoException>,
        ACSLPredicateVisitor<ACSLPredicate, NoException> {

  private Set<Binder> binders;
  private Binder.Quantifier quantifier;

  public BinderApplicationVisitor(Set<Binder> pBinders, Quantifier pQuantifier) {
    binders = pBinders;
    quantifier = pQuantifier;
  }

  @Override
  public ACSLPredicate visitTrue() {
    return ACSLPredicate.getTrue();
  }

  @Override
  public ACSLPredicate visitFalse() {
    return ACSLPredicate.getFalse();
  }

  @Override
  public ACSLPredicate visit(ACSLSimplePredicate pred) {
    return new ACSLSimplePredicate(pred.getTerm().accept(this), pred.isNegated());
  }

  @Override
  public ACSLPredicate visit(ACSLLogicalPredicate pred) {
    return new ACSLLogicalPredicate(
        pred.getLeft().accept(this),
        pred.getRight().accept(this),
        pred.getOperator(),
        pred.isNegated());
  }

  @Override
  public ACSLPredicate visit(TernaryCondition pred) {
    return new TernaryCondition(
        pred.getCondition().accept(this),
        pred.getThen().accept(this),
        pred.getOtherwise().accept(this),
        pred.isNegated());
  }

  @Override
  public ACSLPredicate visit(PredicateAt pred) {
    return new PredicateAt(pred.getInner().accept(this), pred.getLabel(), pred.isNegated());
  }

  @Override
  public ACSLTerm visit(TermAt term) {
    return new TermAt(term.getInner().accept(this), term.getLabel());
  }

  @Override
  public ACSLTerm visit(ACSLBinaryTerm term) {
    return new ACSLBinaryTerm(
        term.getLeft().accept(this), term.getRight().accept(this), term.getOperator());
  }

  @Override
  public ACSLTerm visit(ACSLUnaryTerm term) {
    return new ACSLUnaryTerm(term.getInnerTerm().accept(this), term.getOperator());
  }

  @Override
  public ACSLTerm visit(ArrayAccess term) {
    return new ArrayAccess(term.getArray().accept(this), term.getIndex().accept(this));
  }

  @Override
  public ACSLTerm visit(BoundIdentifier term) {
    return term;
  }

  @Override
  public ACSLTerm visit(Cast term) {
    return new Cast(term.getType(), term.getTerm().accept(this));
  }

  @Override
  public ACSLTerm visit(Identifier term) {
    for (Binder binder : binders) {
      if (binder.getVariables().contains(term.getName())) {
        return new BoundIdentifier(
            term.getName(), term.getFunctionName(), binder.getType(), quantifier);
      }
    }
    return term;
  }

  @Override
  public ACSLTerm visit(IntegerLiteral term) {
    return term;
  }

  @Override
  public ACSLTerm visit(Result term) {
    return term;
  }

  @Override
  public ACSLTerm visit(StringLiteral term) {
    return term;
  }
}
