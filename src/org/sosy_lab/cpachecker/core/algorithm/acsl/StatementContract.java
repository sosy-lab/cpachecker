package org.sosy_lab.cpachecker.core.algorithm.acsl;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import java.util.List;

public class StatementContract implements ACSLAnnotation {

  private final RequiresClause requiresClause;
  private final EnsuresClause ensuresClause;
  private final ImmutableList<Behavior> enclosingBehaviors;
  private final ImmutableList<Behavior> ownBehaviors;
  private final ImmutableList<CompletenessClause> completenessClauses;
  private final boolean usePreStateRepresentation;

  public StatementContract(
      RequiresClause req,
      EnsuresClause ens,
      List<Behavior> enclosing,
      List<Behavior> own,
      List<CompletenessClause> pCompletenessClauses) {
    this(req, ens, enclosing, own, pCompletenessClauses, false);
  }

  public StatementContract(
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

  public static StatementContract fromFunctionContract(
      List<Behavior> enclosing, FunctionContract fcontract) {
    return new StatementContract(
        fcontract.getRequires(),
        fcontract.getEnsures(),
        enclosing,
        fcontract.getBehaviors(),
        fcontract.getCompletenessClauses());
  }

  @Override
  public ACSLPredicate getPredicateRepresentation() {
    return usePreStateRepresentation ? getPreStateRepresentation() : getPostStateRepresentation();
  }

  private ACSLPredicate getPreStateRepresentation() {
    ACSLPredicate preStatePredicate = requiresClause.getPredicate();

    for (Behavior behavior : ownBehaviors) {
      ACSLPredicate behaviorPredicate = behavior.getPreStatePredicate();
      preStatePredicate =
          new ACSLLogicalPredicate(preStatePredicate, behaviorPredicate, BinaryOperator.AND);
    }

    if (!enclosingBehaviors.isEmpty()) {
      ACSLPredicate enclosingDisjunction = ACSLPredicate.getFalse();
      ACSLPredicate enclosingConjunction = ACSLPredicate.getTrue();
      for (Behavior behavior : enclosingBehaviors) {
        AssumesClause assumesClause = behavior.getAssumesClause();
        enclosingConjunction =
            enclosingConjunction.and(assumesClause.getPredicate().negate()).simplify();
        enclosingDisjunction = enclosingDisjunction.or(assumesClause.getPredicate()).simplify();
      }
      preStatePredicate =
          new ACSLLogicalPredicate(enclosingDisjunction, preStatePredicate, BinaryOperator.AND);
      preStatePredicate =
          new ACSLLogicalPredicate(preStatePredicate, enclosingConjunction, BinaryOperator.OR);
    }

    return preStatePredicate;
  }

  private ACSLPredicate getPostStateRepresentation() {
    ACSLPredicate postStatePredicate = ensuresClause.getPredicate();

    for (Behavior behavior : ownBehaviors) {
      ACSLPredicate behaviorPredicate = behavior.getPostStatePredicate();
      postStatePredicate =
          new ACSLLogicalPredicate(postStatePredicate, behaviorPredicate, BinaryOperator.AND);
    }

    if (!enclosingBehaviors.isEmpty()) {
      ACSLPredicate enclosingDisjunction = ACSLPredicate.getFalse();
      ACSLPredicate enclosingConjunction = ACSLPredicate.getTrue();
      for (Behavior behavior : enclosingBehaviors) {
        AssumesClause assumesClause = behavior.getAssumesClause();
        enclosingConjunction =
            enclosingConjunction.and(assumesClause.getPredicate().negate()).simplify();
        enclosingDisjunction = enclosingDisjunction.or(assumesClause.getPredicate()).simplify();
      }
      postStatePredicate =
          new ACSLLogicalPredicate(enclosingDisjunction, postStatePredicate, BinaryOperator.AND);
      postStatePredicate =
          new ACSLLogicalPredicate(postStatePredicate, enclosingConjunction, BinaryOperator.OR);
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

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    if (!enclosingBehaviors.isEmpty()) {
      builder.append("for ");
      Joiner.on(", ")
          .appendTo(builder, enclosingBehaviors.stream().map(x -> x.getName()).iterator());
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
    if (obj instanceof StatementContract) {
      StatementContract other = (StatementContract) obj;
      return requiresClause.equals(other.requiresClause)
          && ensuresClause.equals(other.ensuresClause)
          && enclosingBehaviors.equals(other.enclosingBehaviors)
          && ownBehaviors.equals(other.ownBehaviors)
          && completenessClauses.equals(other.completenessClauses);
    }
    return false;
  }
}
