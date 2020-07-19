package org.sosy_lab.cpachecker.core.algorithm.acsl;

public class LoopInvariant {

  private final ACSLPredicate predicate;

  public LoopInvariant(ACSLPredicate p) {
    predicate = p;
  }

  public ACSLPredicate getPredicate() {
    return predicate;
  }

  public LoopInvariant and(ACSLPredicate p) {
    return new LoopInvariant(predicate.and(p).simplify());
  }

  @Override
  public String toString() {
    return "loop invariant " + predicate.toString() + ';';
  }
}
