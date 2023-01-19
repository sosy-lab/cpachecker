// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

import java.util.List;

public sealed interface ACSLAnnotation
    permits ACSLAssertion,
        ACSLLoopAnnotation,
        FunctionContract,
        InvalidAnnotation,
        StatementContract {

  /**
   * Returns a predicate that represents the semantics of the annotation as they could be used in an
   * invariant.
   */
  ACSLPredicate getPredicateRepresentation();

  /**
   * Returns a predicate representation of the completeness clauses of the annotation.
   *
   * <p>The returned predicate should be logically equivalent to true if all of the completeness
   * clauses are fulfilled.
   */
  ACSLPredicate getCompletenessPredicate();

  /** Returns all behaviors declared in the annotation. */
  List<Behavior> getDeclaredBehaviors();

  /**
   * Returns all behaviors that are referenced by the annotation. The returned list does not contain
   * newly declared behaviors, those can be acquired via {@link #getDeclaredBehaviors()}.
   */
  List<Behavior> getReferencedBehaviors();
}
