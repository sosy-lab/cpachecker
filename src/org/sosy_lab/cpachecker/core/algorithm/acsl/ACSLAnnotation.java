// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.acsl;

public interface ACSLAnnotation {

  /**
   * Returns a predicate that represents the semantics of the annotation as they could be used in an
   * invariant.
   */
  ACSLPredicate getPredicateRepresentation();

  ACSLPredicate getCompletenessPredicate();
}
