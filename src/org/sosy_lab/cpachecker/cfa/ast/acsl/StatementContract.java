// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

import com.google.common.base.Joiner;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import java.util.List;

public final class StatementContract implements ACSLAnnotation {

  private final RequiresClause requiresClause;
  private final EnsuresClause ensuresClause;
  private final ImmutableList<Behavior> enclosingBehaviors;
  private final ImmutableList<Behavior> ownBehaviors;
  private final ImmutableList<CompletenessClause> completenessClauses;
  private final boolean usePreStateRepresentation;

  private StatementContract(
      RequiresClause req,
      EnsuresClause ens,
      List<Behavior> enclosing,
      List<Behavior> own,
      List<CompletenessClause> pCompletenessClauses) {
    this(req, ens, enclosing, own, pCompletenessClauses, false);
  }

  private StatementContract(
      RequiresClause req,
      EnsuresClause ens,
      List<Behavior> enclosing,
      List<Behavior> own,
      List<CompletenessClause> pCompletenessClauses,
      boolean pUsePreStateRepresentation) {
    requiresClause = req;
    ensuresClause = ens;
    enclosingBehaviors = ImmutableList.copyOf(enclosing);
    ownBehaviors = ImmutableList.copyOf(own);
    completenessClauses = ImmutableList.copyOf(pCompletenessClauses);
    usePreStateRepresentation = pUsePreStateRepresentation;
  }

  static StatementContract fromFunctionContract(
      List<Behavior> enclosing, FunctionContract fcontract) {
    ACSLBuiltinCollectingVisitor visitor = new ACSLBuiltinCollectingVisitor();
    assert FluentIterable.from(fcontract.getEnsures().getPredicate().accept(visitor))
            .filter(ACSLResult.class)
            .isEmpty()
        : "\\result is only allowed in function contracts";
    return new StatementContract(
        fcontract.getRequires(),
        fcontract.getEnsures(),
        enclosing,
        fcontract.getDeclaredBehaviors(),
        fcontract.getCompletenessClauses());
  }

  @Override
  public List<Behavior> getDeclaredBehaviors() {
    return ownBehaviors;
  }

  @Override
  public List<Behavior> getReferencedBehaviors() {
    return enclosingBehaviors;
  }

  public boolean isPreStateRepresentation() {
    return usePreStateRepresentation;
  }

  @Override
  public ACSLPredicate getPredicateRepresentation() {
    return usePreStateRepresentation ? getPreStateRepresentation() : getPostStateRepresentation();
  }

  private ACSLPredicate getPreStateRepresentation() {
    if (!enclosingBehaviors.isEmpty()) {
      // TODO: can currently not be expressed correctly
      return ACSLPredicate.getTrue();
    }

    ACSLPredicate preStatePredicate = requiresClause.getPredicate();

    for (Behavior behavior : ownBehaviors) {
      ACSLPredicate behaviorPredicate = behavior.getPreStatePredicate();
      preStatePredicate =
          new ACSLLogicalPredicate(preStatePredicate, behaviorPredicate, ACSLBinaryOperator.AND);
    }

    return preStatePredicate;
  }

  private ACSLPredicate getPostStateRepresentation() {
    if (!enclosingBehaviors.isEmpty()) {
      // TODO: can currently not be expressed correctly
      return ACSLPredicate.getTrue();
    }

    ACSLPredicate postStatePredicate = ensuresClause.getPredicate();

    for (Behavior behavior : ownBehaviors) {
      ACSLPredicate behaviorPredicate = behavior.getPostStatePredicate();
      postStatePredicate =
          new ACSLLogicalPredicate(postStatePredicate, behaviorPredicate, ACSLBinaryOperator.AND);
    }

    return postStatePredicate;
  }

  @Override
  public ACSLPredicate getCompletenessPredicate() {
    ACSLPredicate completenessPredicate = ACSLPredicate.getTrue();
    for (CompletenessClause completenessClause : completenessClauses) {
      completenessPredicate =
          new ACSLLogicalPredicate(
              completenessPredicate,
              completenessClause.getPredicateRepresentation(),
              ACSLBinaryOperator.AND);
    }
    return completenessPredicate;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    if (!enclosingBehaviors.isEmpty()) {
      builder.append("for ");
      Joiner.on(", ")
          .appendTo(builder, enclosingBehaviors.stream().map(Behavior::getName).iterator());
      builder.append(":\n");
    }
    builder.append(requiresClause.toString()).append('\n').append(ensuresClause.toString());
    for (Behavior b : ownBehaviors) {
      builder.append('\n').append(b.toString());
    }
    for (CompletenessClause c : completenessClauses) {
      builder.append('\n').append(c.toString());
    }
    return builder.toString();
  }

  public StatementContract getCopyForPreState() {
    return new StatementContract(
        requiresClause, ensuresClause, enclosingBehaviors, ownBehaviors, completenessClauses, true);
  }

  public StatementContract getCopyForPostState() {
    return new StatementContract(
        requiresClause,
        ensuresClause,
        enclosingBehaviors,
        ownBehaviors,
        completenessClauses,
        false);
  }

  @Override
  public int hashCode() {
    int hash = 23 * requiresClause.hashCode() * ensuresClause.hashCode();
    for (Behavior behavior : enclosingBehaviors) {
      hash *= behavior.hashCode();
    }
    for (Behavior behavior : ownBehaviors) {
      hash *= behavior.hashCode();
    }
    for (CompletenessClause completenessClause : completenessClauses) {
      hash *= completenessClause.hashCode();
    }
    return hash;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof StatementContract other) {
      return requiresClause.equals(other.requiresClause)
          && ensuresClause.equals(other.ensuresClause)
          && enclosingBehaviors.equals(other.enclosingBehaviors)
          && ownBehaviors.equals(other.ownBehaviors)
          && completenessClauses.equals(other.completenessClauses);
    }
    return false;
  }
}
