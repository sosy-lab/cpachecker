// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates;

/**
 * This enum represents the different strategies available for sorting the bdd variables that store
 * predicates during the predicate analysis.
 */
public enum PredicateOrderingStrategy {
  DISABLE(false), // do not execute any reordering.
  SIMILARITY(false),
  FREQUENCY(false),
  IMPLICATION(false),
  REV_IMPLICATION(false),
  RANDOMLY(false),
  FRAMEWORK_RANDOM(true),
  FRAMEWORK_SIFT(true),
  FRAMEWORK_SIFTITE(true),
  FRAMEWORK_WIN2(true),
  FRAMEWORK_WIN2ITE(true),
  FRAMEWORK_WIN3(true),
  FRAMEWORK_WIN3ITE(true),
  CHRONOLOGICAL(true);

  private final boolean isFrameworkStrategy;

  PredicateOrderingStrategy(boolean pIsFrameworkStrategy) {
    isFrameworkStrategy = pIsFrameworkStrategy;
  }

  public boolean getIsFrameworkStrategy() {
    return this.isFrameworkStrategy;
  }
}
