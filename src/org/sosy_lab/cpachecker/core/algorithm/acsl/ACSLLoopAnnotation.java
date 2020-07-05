package org.sosy_lab.cpachecker.core.algorithm.acsl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ACSLLoopAnnotation implements ACSLAnnotation {

  private LoopInvariant lInvariant;
  private Map<List<Behavior>, LoopInvariant> additionalInvariants;

  private ACSLPredicate predicateRepresentation;

  public ACSLLoopAnnotation(LoopInvariant invariant) {
    this(invariant, new HashMap<>());
  }

  public ACSLLoopAnnotation(Map<List<Behavior>, LoopInvariant> pAdditionalInvariants) {
    this(new LoopInvariant(ACSLPredicate.getTrue()), pAdditionalInvariants);
  }

  public ACSLLoopAnnotation(
      LoopInvariant invariant, Map<List<Behavior>, LoopInvariant> pAdditionalInvariants) {
    lInvariant = invariant;
    additionalInvariants = pAdditionalInvariants;
    makePredicateRepresentation();
  }

  private void makePredicateRepresentation() {
    predicateRepresentation = lInvariant.getPredicate();
    for (List<Behavior> behaviors : additionalInvariants.keySet()) {
      ACSLPredicate enclosingConjunction = ACSLPredicate.getTrue();
      ACSLPredicate enclosingDisjunction = ACSLPredicate.getFalse();
      for (Behavior behavior : behaviors) {
        AssumesClause assumesClause = behavior.getAssumesClause();
        enclosingConjunction =
            enclosingConjunction.and(assumesClause.getPredicate().negate());
        enclosingDisjunction = enclosingDisjunction.or(assumesClause.getPredicate());
      }
      enclosingConjunction = enclosingConjunction.simplify();
      enclosingDisjunction = enclosingDisjunction.simplify();
      ACSLPredicate behaviorRepresentation =
          new ACSLLogicalPredicate(
              enclosingDisjunction,
              additionalInvariants.get(behaviors).getPredicate(),
              BinaryOperator.AND);
      behaviorRepresentation =
          new ACSLLogicalPredicate(behaviorRepresentation, enclosingConjunction, BinaryOperator.OR);
      predicateRepresentation =
          new ACSLLogicalPredicate(
              predicateRepresentation, behaviorRepresentation, BinaryOperator.AND);
    }
  }

  @Override
  public ACSLPredicate getPredicateRepresentation() {
    return predicateRepresentation;
  }

  @Override
  public String toString() {
    return predicateRepresentation.toString();
  }
}
