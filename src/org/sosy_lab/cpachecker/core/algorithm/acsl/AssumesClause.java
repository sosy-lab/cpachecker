package org.sosy_lab.cpachecker.core.algorithm.acsl;

// TODO: Just a wrapper for predicate, could be removed
public class AssumesClause {
  private ACSLPredicate predicate;

  public AssumesClause(ACSLPredicate acslPredicate) {
    predicate = acslPredicate;
  }

  @Override
  public String toString() {
    return predicate.toString();
  }

  public void toPureC() {
    predicate = predicate.toPureC();
  }

  public ACSLPredicate getPredicate() {
    return predicate;
  }

  public void and(AssumesClause other) {
    predicate = predicate.and(other.getPredicate()).simplify();
  }
}
