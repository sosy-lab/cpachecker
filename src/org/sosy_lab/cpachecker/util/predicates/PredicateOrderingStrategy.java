// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.util.predicates.regions.RegionManager.VariableOrderingStrategy;

/**
 * This enum represents the different strategies available for sorting the bdd variables that store
 * predicates during the predicate analysis.
 */
public enum PredicateOrderingStrategy {
  CHRONOLOGICAL(null), // do not execute any reordering, variables are in creation order
  SIMILARITY(null),
  FREQUENCY(null),
  IMPLICATION(null),
  REV_IMPLICATION(null),
  RANDOMLY(null),
  FRAMEWORK_RANDOM(VariableOrderingStrategy.RANDOM),
  FRAMEWORK_SIFT(VariableOrderingStrategy.SIFT),
  FRAMEWORK_SIFTITE(VariableOrderingStrategy.SIFTITE),
  FRAMEWORK_WIN2(VariableOrderingStrategy.WIN2),
  FRAMEWORK_WIN2ITE(VariableOrderingStrategy.WIN2ITE),
  FRAMEWORK_WIN3(VariableOrderingStrategy.WIN3),
  FRAMEWORK_WIN3ITE(VariableOrderingStrategy.WIN3ITE);

  private final @Nullable VariableOrderingStrategy frameworkStrategy;

  PredicateOrderingStrategy(VariableOrderingStrategy pFrameworkStrategy) {
    frameworkStrategy = pFrameworkStrategy;
  }

  public @Nullable VariableOrderingStrategy getFrameworkStrategy() {
    return frameworkStrategy;
  }
}
