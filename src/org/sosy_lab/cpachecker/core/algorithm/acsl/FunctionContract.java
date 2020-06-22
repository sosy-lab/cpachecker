package org.sosy_lab.cpachecker.core.algorithm.acsl;

import java.util.List;

public class FunctionContract implements ACSLAnnotation {

  private RequiresClause requiresClause;
  private EnsuresClause ensuresClause;
  private List<Behavior> behaviors;
  private List<CompletenessClause> completenessClauses;

  // this should be a valid C expression
  private ACSLPredicate predicateRepresentation;

  FunctionContract(
      RequiresClause req,
      EnsuresClause ens,
      List<Behavior> pBehaviors,
      List<CompletenessClause> pCompletenessClauses) {
    requiresClause = req;
    ensuresClause = ens;
    behaviors = pBehaviors;
    completenessClauses = pCompletenessClauses;
    toPureC();
    makeRepresentation();
    predicateRepresentation = predicateRepresentation.simplify();
  }

  @Override
  public String toString() {
    return predicateRepresentation.toString();
  }

  private void toPureC() {
    ensuresClause.toPureC();
    requiresClause.toPureC();
    for (Behavior behavior : behaviors) {
      behavior.toPureC();
    }
  }

  private void makeRepresentation() {
    ACSLPredicate left = ensuresClause.getPredicate();
    if (left != ACSLPredicate.getTrue()) {
      ACSLPredicate right = requiresClause.getPredicate();
      if (right != ACSLPredicate.getTrue()) {
        right.negate();
        predicateRepresentation = new ACSLLogicalPredicate(left, right, BinaryOperator.OR);
      } else {
        predicateRepresentation = left;
      }
    } else {
      predicateRepresentation = ACSLPredicate.getTrue();
    }
    for (Behavior behavior : behaviors) {
      ACSLPredicate behaviorRepresentation = behavior.getPredicateRepresentation();
      predicateRepresentation =
          new ACSLLogicalPredicate(
              predicateRepresentation, behaviorRepresentation, BinaryOperator.AND);
    }
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
  }

  @Override
  public ACSLPredicate getPredicateRepresentation() {
    return predicateRepresentation;
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
