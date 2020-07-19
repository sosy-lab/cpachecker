package org.sosy_lab.cpachecker.core.algorithm.acsl;

public interface ACSLAnnotation {

  /**
   * Returns a predicate that represents the semantics of the annotation as they could be used in an
   * invariant.
   */
  ACSLPredicate getPredicateRepresentation();

  ACSLPredicate getCompletenessPredicate();
}
