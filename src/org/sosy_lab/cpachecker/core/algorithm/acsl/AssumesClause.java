package org.sosy_lab.cpachecker.core.algorithm.acsl;

public class AssumesClause {
  private ACSLPredicate predicate;

  public AssumesClause(ACSLPredicate acslPredicate) {
    predicate = acslPredicate;
  }

  @Override
  public String toString() {
    return "assumes" + predicate.toString() + ';';
  }

  public ACSLPredicate getPredicate() {
    return predicate;
  }

  public void and(AssumesClause other) {
    predicate = predicate.and(other.getPredicate()).simplify();
  }
}
