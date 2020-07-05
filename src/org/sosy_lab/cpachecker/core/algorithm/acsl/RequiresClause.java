package org.sosy_lab.cpachecker.core.algorithm.acsl;

// TODO: Just a wrapper for predicate, could be removed
public class RequiresClause {

  private ACSLPredicate predicate;

  public RequiresClause(ACSLPredicate acslPredicate) {
    predicate = acslPredicate;
  }

  @Override
  public String toString() {
    return predicate.toString();
  }

  public ACSLPredicate getPredicate() {
    return predicate;
  }

  public void and(RequiresClause other) {
    predicate = predicate.and(other.getPredicate()).simplify();
  }
}
