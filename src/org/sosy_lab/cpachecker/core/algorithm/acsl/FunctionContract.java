package org.sosy_lab.cpachecker.core.algorithm.acsl;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import java.util.List;

public class FunctionContract implements ACSLAnnotation {

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

  FunctionContract(
      RequiresClause req,
      EnsuresClause ens,
      List<Behavior> pBehaviors,
      List<CompletenessClause> pCompletenessClauses,
      boolean pUsePreStateRepresentation) {
    requiresClause = req;
    ensuresClause = ens;
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
          new ACSLLogicalPredicate(preStatePredicate, behaviorPredicate, BinaryOperator.AND);
    }

    return preStatePredicate.simplify();
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
          new ACSLLogicalPredicate(postStatePredicate, behaviorPredicate, BinaryOperator.AND);
    }

    return postStatePredicate.simplify();
  }

  @Override
  public ACSLPredicate getCompletenessPredicate() {
    ACSLPredicate completenessPredicate = ACSLPredicate.getTrue();
    for (CompletenessClause completenessClause : completenessClauses) {
      completenessPredicate =
          new ACSLLogicalPredicate(
              completenessPredicate,
              completenessClause.getPredicateRepresentation(),
              BinaryOperator.AND);
    }
    return completenessPredicate.simplify();
  }

  public RequiresClause getRequires() {
    return requiresClause;
  }

  public EnsuresClause getEnsures() {
    return ensuresClause;
  }

  public List<Behavior> getBehaviors() {
    return behaviors;
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
    if (obj instanceof FunctionContract) {
      FunctionContract other = (FunctionContract) obj;
      return requiresClause.equals(other.requiresClause)
          && ensuresClause.equals(other.ensuresClause)
          && behaviors.equals(other.behaviors)
          && completenessClauses.equals(other.completenessClauses);
    }
    return false;
  }
}
