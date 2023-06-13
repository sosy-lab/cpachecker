// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import java.util.List;

public final class FunctionContract implements ACSLAnnotation {

  private final RequiresClause requiresClause;
  private final EnsuresClause ensuresClause;
  private final ImmutableList<Behavior> behaviors;
  private final ImmutableList<CompletenessClause> completenessClauses;
  private final boolean usePreStateRepresentation;

  FunctionContract(
      RequiresClause req,
      EnsuresClause ens,
      List<Behavior> pBehaviors,
      List<CompletenessClause> pCompletenessClauses) {
    this(req, ens, pBehaviors, pCompletenessClauses, false);
  }

  private FunctionContract(
      RequiresClause req,
      EnsuresClause ens,
      List<Behavior> pBehaviors,
      List<CompletenessClause> pCompletenessClauses,
      boolean pUsePreStateRepresentation) {
    requiresClause = new RequiresClause(req.getPredicate().simplify());
    ensuresClause = new EnsuresClause(ens.getPredicate().simplify());
    behaviors = ImmutableList.copyOf(pBehaviors);
    completenessClauses = ImmutableList.copyOf(pCompletenessClauses);
    usePreStateRepresentation = pUsePreStateRepresentation;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder
        .append(requiresClause.toString())
        .append('\n')
        .append(ensuresClause.toString())
        .append('\n');
    Joiner.on('\n').appendTo(builder, behaviors.stream().map(x -> x.toString()).iterator());
    Joiner.on('\n')
        .appendTo(builder, completenessClauses.stream().map(x -> x.toString()).iterator());
    return builder.toString();
  }

  @Override
  public ACSLPredicate getPredicateRepresentation() {
    return usePreStateRepresentation ? getPreStateRepresentation() : getPostStateRepresentation();
  }

  /**
   * Returns a predicate that represents the semantics of the annotation as they could be used in an
   * invariant. Only properties that should be evaluated in the pre-state are considered.
   */
  private ACSLPredicate getPreStateRepresentation() {
    ACSLPredicate preStatePredicate = requiresClause.getPredicate();

    for (Behavior behavior : behaviors) {
      ACSLPredicate behaviorPredicate = behavior.getPreStatePredicate();
      preStatePredicate =
          new ACSLLogicalPredicate(preStatePredicate, behaviorPredicate, ACSLBinaryOperator.AND);
    }

    return preStatePredicate;
  }

  /**
   * Returns a predicate that represents the semantics of the annotation as they could be used in an
   * invariant. Only properties that should be evaluated in the post-state are considered.
   */
  private ACSLPredicate getPostStateRepresentation() {
    ACSLPredicate postStatePredicate = ensuresClause.getPredicate();

    for (Behavior behavior : behaviors) {
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

  public boolean isPreStateRepresentation() {
    return usePreStateRepresentation;
  }

  public RequiresClause getRequires() {
    return requiresClause;
  }

  public EnsuresClause getEnsures() {
    return ensuresClause;
  }

  @Override
  public List<Behavior> getDeclaredBehaviors() {
    return behaviors;
  }

  @Override
  public List<Behavior> getReferencedBehaviors() {
    return ImmutableList.of();
  }

  public List<CompletenessClause> getCompletenessClauses() {
    return completenessClauses;
  }

  public FunctionContract getCopyForPreState() {
    return new FunctionContract(
        requiresClause, ensuresClause, behaviors, completenessClauses, true);
  }

  public FunctionContract getCopyForPostState() {
    return new FunctionContract(
        requiresClause, ensuresClause, behaviors, completenessClauses, false);
  }

  @Override
  public int hashCode() {
    int hash = 17 * requiresClause.hashCode() * ensuresClause.hashCode();
    for (Behavior behavior : behaviors) {
      hash *= behavior.hashCode();
    }
    for (CompletenessClause completenessClause : completenessClauses) {
      hash *= completenessClause.hashCode();
    }
    return hash;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof FunctionContract other) {
      return requiresClause.equals(other.requiresClause)
          && ensuresClause.equals(other.ensuresClause)
          && behaviors.equals(other.behaviors)
          && completenessClauses.equals(other.completenessClauses);
    }
    return false;
  }
}
