package org.sosy_lab.cpachecker.core.algorithm.acsl;

public class LoopInvariant {

  private ACSLPredicate predicate;

  public LoopInvariant(ACSLPredicate p) {
    predicate = p.simplify();
  }

  public ACSLPredicate getPredicate() {
    return predicate;
  }

  public LoopInvariant and(ACSLPredicate p) {
    predicate = predicate.and(p);
    return this;
  }

  @Override
  public String toString() {
    return "loop invariant " + predicate.toString() + ';';
  }
}
