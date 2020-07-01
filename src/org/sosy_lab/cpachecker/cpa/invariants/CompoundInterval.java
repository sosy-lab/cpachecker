/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.invariants;

import java.math.BigInteger;
import java.util.List;

/**
 * Instances of this class represent compound states of intervals.
 */
public interface CompoundInterval {

  boolean isSingleton();

  boolean hasLowerBound();

  boolean hasUpperBound();

  Number getLowerBound();

  Number getUpperBound();

  Number getValue();

  boolean isDefinitelyFalse();

  boolean isDefinitelyTrue();

  boolean isBottom();

  boolean contains(BigInteger pBigInteger);

  CompoundInterval extendToMinValue();

  CompoundInterval extendToMaxValue();

  CompoundInterval invert();

  CompoundInterval span();

  /**
   * Checks if this compound state contains every possible value.
   *
   * @return {@code true} if this state contains every possible value,
   * {@code false} otherwise.
   */
  boolean containsAllPossibleValues();

  boolean containsNegative();

  boolean containsPositive();

  CompoundInterval signum();

  List<? extends CompoundInterval> splitIntoIntervals();

  List<SimpleInterval> getIntervals();

}
