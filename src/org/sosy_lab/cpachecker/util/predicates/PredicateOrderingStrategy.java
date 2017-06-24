/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.util.predicates;

/**
 * This enum represents the different strategies available for sorting the bdd variables that store
 * predicates during the predicate analysis.
 */
public enum PredicateOrderingStrategy {
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

  private PredicateOrderingStrategy(boolean pIsFrameworkStrategy) {
    isFrameworkStrategy = pIsFrameworkStrategy;
  }

  public boolean getIsFrameworkStrategy() {
    return this.isFrameworkStrategy;
  }
}
