package org.sosy_lab.cpachecker.core.algorithm.acsl;

public interface ACSLAnnotation {
  default ACSLPredicate getPredicateRepresentation() {
    return getPreStateRepresentation();
  }

  default ACSLPredicate getPreStateRepresentation() {
    return getPredicateRepresentation();
  }

  default ACSLPredicate getPostStateRepresentation() {
    return getPredicateRepresentation();
  }
}
