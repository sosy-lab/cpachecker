// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.acsl;

import com.google.common.collect.ImmutableSet;
import org.sosy_lab.cpachecker.exceptions.NoException;

/** Visitor to find all ACSL built-ins that are used in a logic expression. */
public class BuiltinCollectingVisitor
    implements ACSLTermVisitor<ImmutableSet<ACSLBuiltin>, NoException>,
        ACSLPredicateVisitor<ImmutableSet<ACSLBuiltin>, NoException> {

  @Override
  public ImmutableSet<ACSLBuiltin> visitTrue() {
    return ImmutableSet.of();
  }

  @Override
  public ImmutableSet<ACSLBuiltin> visitFalse() {
    return ImmutableSet.of();
  }

  @Override
  public ImmutableSet<ACSLBuiltin> visit(ACSLSimplePredicate pred) {
    return pred.getTerm().accept(this);
  }

  @Override
  public ImmutableSet<ACSLBuiltin> visit(ACSLLogicalPredicate pred) {
    ImmutableSet.Builder<ACSLBuiltin> builder = ImmutableSet.builder();
    builder.addAll(pred.getLeft().accept(this));
    builder.addAll(pred.getRight().accept(this));
    return builder.build();
  }

  @Override
  public ImmutableSet<ACSLBuiltin> visit(TernaryCondition pred) {
    ImmutableSet.Builder<ACSLBuiltin> builder = ImmutableSet.builder();
    builder.addAll(pred.getCondition().accept(this));
    builder.addAll(pred.getThen().accept(this));
    builder.addAll(pred.getOtherwise().accept(this));
    return builder.build();
  }

  @Override
  public ImmutableSet<ACSLBuiltin> visit(PredicateAt pred) {
    ImmutableSet.Builder<ACSLBuiltin> builder = ImmutableSet.builder();
    builder.addAll(pred.getInner().accept(this));
    builder.add(pred);
    return builder.build();
  }

  @Override
  public ImmutableSet<ACSLBuiltin> visit(ACSLBinaryTerm term) {
    ImmutableSet.Builder<ACSLBuiltin> builder = ImmutableSet.builder();
    builder.addAll(term.getLeft().accept(this));
    builder.addAll(term.getRight().accept(this));
    return builder.build();
  }

  @Override
  public ImmutableSet<ACSLBuiltin> visit(ACSLUnaryTerm term) {
    return term.getInnerTerm().accept(this);
  }

  @Override
  public ImmutableSet<ACSLBuiltin> visit(ArrayAccess term) {
    ImmutableSet.Builder<ACSLBuiltin> builder = ImmutableSet.builder();
    builder.addAll(term.getArray().accept(this));
    builder.addAll(term.getIndex().accept(this));
    return builder.build();
  }

  @Override
  public ImmutableSet<ACSLBuiltin> visit(TermAt term) {
    ImmutableSet.Builder<ACSLBuiltin> builder = ImmutableSet.builder();
    builder.addAll(term.getInner().accept(this));
    builder.add(term);
    return builder.build();
  }

  @Override
  public ImmutableSet<ACSLBuiltin> visit(Cast term) {
    return term.getTerm().accept(this);
  }

  @Override
  public ImmutableSet<ACSLBuiltin> visit(BoundIdentifier term) {
    return ImmutableSet.of();
  }

  @Override
  public ImmutableSet<ACSLBuiltin> visit(Identifier term) {
    return ImmutableSet.of();
  }

  @Override
  public ImmutableSet<ACSLBuiltin> visit(IntegerLiteral term) {
    return ImmutableSet.of();
  }

  @Override
  public ImmutableSet<ACSLBuiltin> visit(Result term) {
    return ImmutableSet.of(term);
  }

  @Override
  public ImmutableSet<ACSLBuiltin> visit(StringLiteral term) {
    return ImmutableSet.of();
  }
}
