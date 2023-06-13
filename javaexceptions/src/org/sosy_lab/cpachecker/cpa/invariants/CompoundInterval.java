// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.invariants;

import java.math.BigInteger;
import java.util.List;

/** Instances of this class represent compound states of intervals. */
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
   * @return {@code true} if this state contains every possible value, {@code false} otherwise.
   */
  boolean containsAllPossibleValues();

  boolean containsNegative();

  boolean containsPositive();

  CompoundInterval signum();

  List<? extends CompoundInterval> splitIntoIntervals();

  List<SimpleInterval> getIntervals();
}
