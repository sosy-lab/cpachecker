// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.invariants.operators.mathematical;

import java.math.BigInteger;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cpa.invariants.SimpleInterval;

/** This utility class provides helper functions for working with simple intervals. */
class IntervalHelper {

  /** Private constructor for utility class. */
  private IntervalHelper() {}

  /**
   * Gets a simple interval with the given bounds, even if one or both of the bounds are <code>null
   * </code>. A <code>null</code> lower bound means that the resulting interval will not have a
   * lower bound while a <code>null</code> upper bound means that the resulting interval will not
   * have an upper bound.
   *
   * <p>If both given bounds are non-null, then the first bound must be less than or equal to the
   * second bound.
   *
   * @param pLowerBound the lower bound of the resulting interval or <code>null</code> if the
   *     resulting interval is not supposed to have a lower bound.
   * @param pUpperBound the upper bound of the resulting interval or <code>null</code> if the
   *     resulting interval is not supposed to have an upper bound.
   * @return an interval with the given lower bound or no lower bound if the given lower bound was
   *     <code>null</code> and the given upper bound or no upper bound if the given upper bound was
   *     <code>null</code>.
   */
  public static SimpleInterval ofNullableBounds(
      @Nullable BigInteger pLowerBound, @Nullable BigInteger pUpperBound) {
    if (pLowerBound == null) {
      if (pUpperBound == null) {
        return SimpleInterval.infinite();
      } else {
        return SimpleInterval.singleton(pUpperBound).extendToNegativeInfinity();
      }
    }
    if (pUpperBound == null) {
      return SimpleInterval.singleton(pLowerBound).extendToPositiveInfinity();
    }
    return SimpleInterval.of(pLowerBound, pUpperBound);
  }

  /**
   * Gets the lower bound of the given interval or <code>null</code> if it does not have a lower
   * bound.
   *
   * @param pInterval the interval to obtain the lower bound from.
   * @return the lower bound of the given interval or <code>null</code> if the given interval does
   *     not have a lower bound.
   */
  public static BigInteger getLowerBoundOrNull(SimpleInterval pInterval) {
    if (pInterval.hasLowerBound()) {
      return pInterval.getLowerBound();
    }
    return null;
  }

  /**
   * Gets the upper bound of the given interval or <code>null</code> if it does not have an upper
   * bound.
   *
   * @param pInterval the interval to obtain the upper bound from.
   * @return the upper bound of the given interval or <code>null</code> if the given interval does
   *     not have an upper bound.
   */
  public static @Nullable BigInteger getUpperBoundOrNull(SimpleInterval pInterval) {
    if (pInterval.hasUpperBound()) {
      return pInterval.getUpperBound();
    }
    return null;
  }
}
