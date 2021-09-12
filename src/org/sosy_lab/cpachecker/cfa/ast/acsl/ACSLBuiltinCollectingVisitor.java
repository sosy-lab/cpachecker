// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

import com.google.common.collect.ImmutableSet;
import org.sosy_lab.cpachecker.exceptions.NoException;

/** Visitor to find all ACSL built-ins that are used in a logic expression. */
public class ACSLBuiltinCollectingVisitor
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
  public ImmutableSet<ACSLBuiltin> visit(ACSLTernaryCondition pred) {
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
  public ImmutableSet<ACSLBuiltin> visit(ACSLArrayAccess term) {
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
  public ImmutableSet<ACSLBuiltin> visit(ACSLCast term) {
    return term.getTerm().accept(this);
  }

  @Override
  public ImmutableSet<ACSLBuiltin> visit(BoundIdentifier term) {
    return ImmutableSet.of();
  }

  @Override
  public ImmutableSet<ACSLBuiltin> visit(ACSLIdentifier term) {
    return ImmutableSet.of();
  }

  @Override
  public ImmutableSet<ACSLBuiltin> visit(ACSLIntegerLiteral term) {
    return ImmutableSet.of();
  }

  @Override
  public ImmutableSet<ACSLBuiltin> visit(ACSLResult term) {
    return ImmutableSet.of(term);
  }

  @Override
  public ImmutableSet<ACSLBuiltin> visit(ACSLStringLiteral term) {
    return ImmutableSet.of();
  }
}
