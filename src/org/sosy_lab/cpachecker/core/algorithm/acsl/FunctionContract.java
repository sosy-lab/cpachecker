package org.sosy_lab.cpachecker.core.algorithm.acsl;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import java.util.List;

public class FunctionContract implements ACSLAnnotation {

  private final RequiresClause requiresClause;
  private final EnsuresClause ensuresClause;
  private final ImmutableList<Behavior> behaviors;
  private final ImmutableList<CompletenessClause> completenessClauses;

  FunctionContract(
      RequiresClause req,
      EnsuresClause ens,
      List<Behavior> pBehaviors,
      List<CompletenessClause> pCompletenessClauses) {
    requiresClause = req;
    ensuresClause = ens;
    behaviors = ImmutableList.copyOf(pBehaviors);
    completenessClauses = ImmutableList.copyOf(pCompletenessClauses);
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
  public ACSLPredicate getPreStateRepresentation() {
    ACSLPredicate preStatePredicate = requiresClause.getPredicate();

    for (Behavior behavior : behaviors) {
      ACSLPredicate behaviorPredicate = behavior.getPreStatePredicate();
      preStatePredicate =
          new ACSLLogicalPredicate(preStatePredicate, behaviorPredicate, BinaryOperator.AND);
    }

    return preStatePredicate;
  }

  @Override
  public ACSLPredicate getPostStateRepresentation() {
    ACSLPredicate postStatePredicate = ensuresClause.getPredicate();

    for (Behavior behavior : behaviors) {
      ACSLPredicate behaviorPredicate = behavior.getPostStatePredicate();
      postStatePredicate =
          new ACSLLogicalPredicate(postStatePredicate, behaviorPredicate, BinaryOperator.AND);
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
}
