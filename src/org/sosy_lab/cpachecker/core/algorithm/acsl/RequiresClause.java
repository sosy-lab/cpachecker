package org.sosy_lab.cpachecker.core.algorithm.acsl;

public class RequiresClause {

  private final ACSLPredicate predicate;

  public RequiresClause(ACSLPredicate acslPredicate) {
    predicate = acslPredicate;
  }

  public ACSLPredicate getPredicate() {
    return predicate;
  }

  public RequiresClause and(RequiresClause other) {
    return new RequiresClause(predicate.and(other.getPredicate()));
  }

  @Override
  public String toString() {
    return "requires " + predicate.toString() + ';';
  }
}
