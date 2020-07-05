package org.sosy_lab.cpachecker.core.algorithm.acsl;

public class EnsuresClause {

  private final ACSLPredicate predicate;

  public EnsuresClause(ACSLPredicate acslPredicate) {
    predicate = acslPredicate;
  }

  public ACSLPredicate getPredicate() {
    return predicate;
  }

  public EnsuresClause and(EnsuresClause other) {
    return new EnsuresClause(predicate.and(other.getPredicate()));
  }

  @Override
  public String toString() {
    return "ensures " + predicate.toString() + ';';
  }
}
