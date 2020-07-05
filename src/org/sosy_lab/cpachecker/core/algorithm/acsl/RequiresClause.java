package org.sosy_lab.cpachecker.core.algorithm.acsl;

public class RequiresClause {

  private ACSLPredicate predicate;

  public RequiresClause(ACSLPredicate acslPredicate) {
    predicate = acslPredicate;
  }

  @Override
  public String toString() {
    return "requires" + predicate.toString() + ';';
  }

  public ACSLPredicate getPredicate() {
    return predicate;
  }

  public void and(RequiresClause other) {
    predicate = predicate.and(other.getPredicate()).simplify();
  }
}
