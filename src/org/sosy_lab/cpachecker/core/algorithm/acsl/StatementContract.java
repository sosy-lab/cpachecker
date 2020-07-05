package org.sosy_lab.cpachecker.core.algorithm.acsl;

import java.util.List;

public class StatementContract implements ACSLAnnotation {

  private final RequiresClause requiresClause;
  private final EnsuresClause ensuresClause;
  private final List<Behavior> enclosingBehaviors;
  private final List<Behavior> ownBehaviors;
  private final List<CompletenessClause> completenessClauses;

  // this should be a valid C expression
  private ACSLPredicate predicateRepresentation;

  public StatementContract(
      RequiresClause req,
      EnsuresClause ens,
      List<Behavior> enclosing,
      List<Behavior> own,
      List<CompletenessClause> pCompletenessClauses) {
    requiresClause = req;
    ensuresClause = ens;
    enclosingBehaviors = enclosing;
    ownBehaviors = own;
    completenessClauses = pCompletenessClauses;
    makeRepresentation();
    predicateRepresentation = predicateRepresentation.simplify();
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
    return predicateRepresentation;
  }

  private void makeRepresentation() {
    ACSLPredicate left = ensuresClause.getPredicate();
    if (left != ACSLPredicate.getTrue()) {
      ACSLPredicate right = requiresClause.getPredicate();
      if (right != ACSLPredicate.getTrue()) {
        right = right.negate();
        predicateRepresentation = new ACSLLogicalPredicate(left, right, BinaryOperator.OR);
      } else {
        predicateRepresentation = left;
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
  }

  @Override
  public String toString() {
    return predicateRepresentation.toString();
  }
}
