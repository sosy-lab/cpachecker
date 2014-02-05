/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.invariants.operators;

import java.math.BigInteger;

import javax.annotation.Nullable;

import org.sosy_lab.cpachecker.cpa.invariants.SimpleInterval;

/**
 * This utility class provides helper functions for working with simple
 * intervals.
 */
class IntervalHelper {

  /**
   * Private constructor for utility class.
   */
  private IntervalHelper() {

  }

  /**
   * Gets a simple interval with the given bounds, even if one or both of
   * the bounds are <code>null</code>. A <code>null</code> lower bound
   * means that the resulting interval will not have a lower bound while a
   * <code>null</code> upper bound means that the resulting interval will
   * not have an upper bound.
   *
   * If both given bounds are non-null, then the first bound must be
   * less than or equal to the second bound.
   *
   * @param pLowerBound the lower bound of the resulting interval or
   * <code>null</code> if the resulting interval is not supposed to have
   * a lower bound.
   * @param pUpperBound the upper bound of the resulting interval or
   * <code>null</code> if the resulting interval is not supposed to have
   * an upper bound.
   * @return an interval with the given lower bound or no lower bound if
   * the given lower bound was <code>null</code> and the given upper bound
   * or no upper bound if the given upper bound was <code>null</code>.
   */
  public static SimpleInterval ofNullableBounds(@Nullable BigInteger pLowerBound, @Nullable BigInteger pUpperBound) {
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
   * Gets the lower bound of the given interval or <code>null</code> if it
   * does not have a lower bound.
   *
   * @param pInterval the interval to obtain the lower bound from.
   * @return the lower bound of the given interval or <code>null</code> if
   * the given interval does not have a lower bound.
   */
  public static BigInteger getLowerBoundOrNull(SimpleInterval pInterval) {
    if (pInterval.hasLowerBound()) {
      return pInterval.getLowerBound();
    }
    return null;
  }

  /**
   * Gets the upper bound of the given interval or <code>null</code> if it
   * does not have an upper bound.
   *
   * @param pInterval the interval to obtain the upper bound from.
   * @return the upper bound of the given interval or <code>null</code> if
   * the given interval does not have an upper bound.
   */
  public static @Nullable BigInteger getUpperBoundOrNull(SimpleInterval pInterval) {
    if (pInterval.hasUpperBound()) {
      return pInterval.getUpperBound();
    }
    return null;
  }

}
