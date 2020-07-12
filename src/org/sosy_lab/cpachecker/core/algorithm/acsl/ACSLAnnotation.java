package org.sosy_lab.cpachecker.core.algorithm.acsl;

public interface ACSLAnnotation {

  /**
   * Returns a predicate that represents the semantics of the annotation as they could be used in an
   * invariant.
   *
   * <p>An implementation of this interface should either implement <code>
   * getPredicateRepresentation()</code> or both <code>getPreStateRepresentation()</code> and <code>
   * getPostStateRepresentation()</code> depending on what makes sense for the specific kind of
   * annotation.
   */
  default ACSLPredicate getPredicateRepresentation() {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns a predicate that represents the semantics of the annotation as they could be used in an
   * invariant. Only properties that should be evaluated in the pre-state are considered.
   *
   * <p>An implementation of this interface should either implement <code>
   * getPredicateRepresentation()</code> or both <code>getPreStateRepresentation()</code> and <code>
   * getPostStateRepresentation()</code> depending on what makes sense for the specific kind of
   * annotation.
   */
  default ACSLPredicate getPreStateRepresentation() {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns a predicate that represents the semantics of the annotation as they could be used in an
   * invariant. Only properties that should be evaluated in the post-state are considered.
   *
   * <p>An implementation of this interface should either implement <code>
   * getPredicateRepresentation()</code> or both <code>getPreStateRepresentation()</code> and <code>
   * getPostStateRepresentation()</code> depending on what makes sense for the specific kind of
   * annotation.
   */
  default ACSLPredicate getPostStateRepresentation() {
    throw new UnsupportedOperationException();
  }
}
