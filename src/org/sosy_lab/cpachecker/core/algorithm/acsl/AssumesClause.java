package org.sosy_lab.cpachecker.core.algorithm.acsl;

public class AssumesClause {
  private final ACSLPredicate predicate;

  public AssumesClause(ACSLPredicate acslPredicate) {
    predicate = acslPredicate;
  }

  public ACSLPredicate getPredicate() {
    return predicate;
  }

  public AssumesClause and(AssumesClause other) {
    return new AssumesClause(predicate.and(other.getPredicate()).simplify());
  }

  @Override
  public String toString() {
    return "assumes " + predicate.toString() + ';';
  }
}
