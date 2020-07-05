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

  public StatementContract(
      RequiresClause req,
      EnsuresClause ens,
      List<Behavior> enclosing,
      List<Behavior> own,
      List<CompletenessClause> pCompletenessClauses) {
    requiresClause = req;
    ensuresClause = ens;
    enclosingBehaviors = ImmutableList.copyOf(enclosing);
    ownBehaviors = ImmutableList.copyOf(own);
    completenessClauses = ImmutableList.copyOf(pCompletenessClauses);
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

  private ACSLPredicate makeRepresentation() {
    ACSLPredicate predicateRepresentation;

    ACSLPredicate right = ensuresClause.getPredicate();
    if (right != ACSLPredicate.getTrue()) {
      ACSLPredicate left = requiresClause.getPredicate();
      if (left != ACSLPredicate.getTrue()) {
        left = left.negate();
        predicateRepresentation = new ACSLLogicalPredicate(right, left, BinaryOperator.OR);
      } else {
        predicateRepresentation = right;
      }
    } else {
      predicateRepresentation = ACSLPredicate.getTrue();
    }

    for (Behavior behavior : ownBehaviors) {
      ACSLPredicate behaviorRepresentation = behavior.getPredicateRepresentation();
      predicateRepresentation =
          new ACSLLogicalPredicate(
              predicateRepresentation, behaviorRepresentation, BinaryOperator.AND);
    }

    ACSLPredicate enclosingDisjunction = ACSLPredicate.getFalse();
    ACSLPredicate enclosingConjunction = ACSLPredicate.getTrue();
    for (Behavior behavior : enclosingBehaviors) {
      AssumesClause assumesClause = behavior.getAssumesClause();
      enclosingConjunction =
          enclosingConjunction.and(assumesClause.getPredicate().negate()).simplify();
      enclosingDisjunction = enclosingDisjunction.or(assumesClause.getPredicate()).simplify();
    }
    predicateRepresentation =
        new ACSLLogicalPredicate(enclosingDisjunction, predicateRepresentation, BinaryOperator.AND);
    predicateRepresentation =
        new ACSLLogicalPredicate(predicateRepresentation, enclosingConjunction, BinaryOperator.OR);

    ACSLPredicate completenessRepresentation = ACSLPredicate.getTrue();
    for (CompletenessClause completenessClause : completenessClauses) {
      completenessRepresentation =
          new ACSLLogicalPredicate(
              completenessRepresentation,
              completenessClause.getPredicateRepresentation(),
              BinaryOperator.AND);
    }
    predicateRepresentation =
        new ACSLLogicalPredicate(
            predicateRepresentation, completenessRepresentation, BinaryOperator.AND);

    return predicateRepresentation.simplify();
  }

  @Override
  public ACSLPredicate getPredicateRepresentation() {
    return makeRepresentation();
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    if (!enclosingBehaviors.isEmpty()) {
      builder.append("for ");
      Joiner.on(", ").appendTo(builder, enclosingBehaviors.stream().map(x -> x.getName()).iterator());
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
}
