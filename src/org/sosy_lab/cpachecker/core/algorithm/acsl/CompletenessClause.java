package org.sosy_lab.cpachecker.core.algorithm.acsl;

import java.util.List;

public class CompletenessClause {

  private final List<Behavior> behaviors;
  private final RelationKind kind;

  // this should be a valid C expression
  private ACSLPredicate predicateRepresentation;

  public CompletenessClause(List<Behavior> pBehaviors, RelationKind pKind) {
    behaviors = pBehaviors;
    kind = pKind;
    makePredicateRepresentation();
  }

  private void makePredicateRepresentation() {
    // TODO: Representation for complete clauses could get simplified to true, so currently only
    // usable for indication that something is wrong!?
    if (kind.equals(RelationKind.COMPLETE)) {
      predicateRepresentation = ACSLPredicate.getFalse();
      for (Behavior behavior : behaviors) {
        predicateRepresentation =
            new ACSLLogicalPredicate(
                predicateRepresentation,
                behavior.getAssumesClause().getPredicate(),
                BinaryOperator.OR);
      }
    } else if (kind.equals(RelationKind.DISJOINT)) {
      predicateRepresentation = ACSLPredicate.getTrue();
      for (Behavior behavior1 : behaviors) {
        for (Behavior behavior2 : behaviors) {
          if (behavior1 == behavior2) {
            continue;
          }
          ACSLPredicate notBoth =
              new ACSLLogicalPredicate(
                      behavior1.getAssumesClause().getPredicate(),
                      behavior2.getAssumesClause().getPredicate(),
                      BinaryOperator.AND)
                  .negate();
          predicateRepresentation =
              new ACSLLogicalPredicate(predicateRepresentation, notBoth, BinaryOperator.AND);
        }
      }
    } else {
      throw new AssertionError("Unknown kind: " + kind);
    }
    predicateRepresentation = predicateRepresentation.simplify();
  }

  public ACSLPredicate getPredicateRepresentation() {
    return predicateRepresentation;
  }

  public enum RelationKind {
    COMPLETE,
    DISJOINT
  }
}
