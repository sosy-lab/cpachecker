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
package org.sosy_lab.cpachecker.cpa.invariants.operators.interval.interval.tointerval;

import java.math.BigInteger;

import javax.annotation.Nullable;

import org.sosy_lab.cpachecker.cpa.invariants.SimpleInterval;
import org.sosy_lab.cpachecker.cpa.invariants.operators.interval.IntervalHelper;
import org.sosy_lab.cpachecker.cpa.invariants.operators.interval.scalar.tointerval.ISIOperator;

/**
 * The instance of this class is an operator for multiplying two simple
 * intervals together to produce another simple interval.
 */
enum MultiplyOperator implements IIIOperator {

  INSTANCE;

  /**
   * Calculates a superset of the possible results obtained by multiplying
   * any value of the first operand interval with any value of the second operand interval.
   *
   * @param pFirstOperand the values to multiply the values of the second operand interval with.
   * @param pSecondOperand the values to multiply the values of the first operand interval with.
   * @return a superset of the possible results obtained by multiplying
   * any value of the first operand interval with any value of the second operand interval.
   */
  @Override
  public SimpleInterval apply(SimpleInterval pFirstOperand, SimpleInterval pSecondOperand) {
    /*
     * If one of the two intervals is a single value, use the easier
     * scalar multiplication. This also takes care of special cases
     * like multiplication with zero ore one.
     */
    if (pSecondOperand.isSingleton()) {
      return ISIOperator.MULTIPLY_OPERATOR.apply(pFirstOperand, pSecondOperand.getLowerBound());
    }
    if (pFirstOperand.isSingleton()) {
      return ISIOperator.MULTIPLY_OPERATOR.apply(pSecondOperand, pFirstOperand.getLowerBound());
    }
    // If one  of the intervals is top, the resulting interval is top
    if (pFirstOperand.isTop() || pSecondOperand.isTop()) {
      return SimpleInterval.infinite();
    }
    /*
     * Multiply the bounds with each other to find the new extremes.
     * At most one bound of each interval can be infinite and neither
     * of the intervals is a singleton at this point.
     */
    BigInteger pLowerBound = IntervalHelper.getLowerBoundOrNull(pSecondOperand);
    BigInteger pUpperBound = IntervalHelper.getUpperBoundOrNull(pSecondOperand);
    BigInteger lbLb = null;
    BigInteger lbUb = null;
    BigInteger ubLb = null;
    BigInteger ubUb = null;
    boolean negInf = false;
    boolean posInf = false;
    if (!pFirstOperand.hasLowerBound()) {
      // One of the lower bounds is negative infinity, so if one of
      // its co-factors is non-zero, an extreme can easily be found.
      // Otherwise, zero is a candidate for an extreme.
      if (pLowerBound == null || pLowerBound.signum() < 0) {
        posInf = true;
      } else if (pLowerBound.signum() > 0) {
        negInf = true;
      } else {
        lbLb = ubLb = BigInteger.ZERO;
      }
      if (pUpperBound == null || pUpperBound.signum() > 0) {
        negInf = true;
      } else if (pUpperBound.signum() < 0) {
        posInf = true;
      }
    } else {
      int thisLowerBoundSignum = pFirstOperand.getLowerBound().signum();
      if (pLowerBound == null) {
        // One of the lower bounds is negative infinity, so if one of
        // its co-factors is non-zero, an extreme can easily be found
        // Otherwise, zero is a candidate for an extreme.
        if (thisLowerBoundSignum < 0) {
          posInf = true;
        } else if (thisLowerBoundSignum > 0) {
          negInf = true;
        } else {
          lbLb = lbUb = BigInteger.ZERO;
        }
      } else {
        // Both lower bounds are finite,
        // so a candidate for the new extremes can be calculated
        lbLb = pFirstOperand.getLowerBound().multiply(pLowerBound);
      }
      if (pUpperBound == null) {
        // One of the upper bounds is negative infinity, so if one of
        // its co-factors is non-zero, an extreme can easily be found
        // Otherwise, zero is a candidate for an extreme.
        if (thisLowerBoundSignum < 0) {
          negInf = true;
        } else if (thisLowerBoundSignum > 0) {
          posInf = true;
        } else {
          lbLb = lbUb = BigInteger.ZERO;
        }
      } else {
        // The first lower and second upper bound are both finite,
        // so a candidate for the new extremes can be calculated
        lbUb = pFirstOperand.getLowerBound().multiply(pUpperBound);
      }
    }
    if (!pFirstOperand.hasUpperBound()) {
      // One of the upper bounds is negative infinity, so if one of
      // its co-factors is non-zero, an extreme can easily be found
      // Otherwise, zero is a candidate for an extreme.
      if (pLowerBound == null || pLowerBound.signum() < 0) {
        negInf = true;
      } else if (pLowerBound.signum() > 0) {
        posInf = true;
      } else {
        lbLb = ubLb = BigInteger.ZERO;
      }
      if (pUpperBound == null || pUpperBound.signum() > 0) {
        posInf = true;
      } else if (pUpperBound.signum() < 0) {
        negInf = true;
      } else {
        lbUb = ubUb = BigInteger.ZERO;
      }
    } else {
      int thisUpperBoundSignum = pFirstOperand.getUpperBound().signum();
      if (pLowerBound == null) {
        // One of the lower bounds is negative infinity, so if one of
        // its co-factors is non-zero, an extreme can easily be found
        if (thisUpperBoundSignum < 0) {
          posInf = true;
        } else if (thisUpperBoundSignum > 0) {
          negInf = true;
        } else {
          ubLb = ubUb = BigInteger.ZERO;
        }
      } else {
        // The first upper and second lower bound are both finite,
        // so a candidate for the new extremes can be calculated
        ubLb = pFirstOperand.getUpperBound().multiply(pLowerBound);
      }
      if (!pFirstOperand.hasUpperBound()) {
        // One of the upper bounds is negative infinity, so if one of
        // its co-factors is non-zero, an extreme can easily be found
        if (thisUpperBoundSignum < 0) {
          negInf = true;
        } else if (thisUpperBoundSignum > 0) {
          posInf = true;
        } else {
          ubLb = ubUb = BigInteger.ZERO;
        }
      } else {
        // Both upper bounds are finite,
        // so a candidate for the new extremes can be calculated
        ubUb = pFirstOperand.getUpperBound().multiply(pUpperBound);
      }
    }
    if (negInf && posInf) { return SimpleInterval.infinite(); }
    // Find the lowest and highest extremes
    BigInteger lowerBound = min(min(lbLb, lbUb), min(ubLb, ubUb));
    BigInteger upperBound = max(max(lbLb, lbUb), max(ubLb, ubUb));
    SimpleInterval result = IntervalHelper.ofNullableBounds(lowerBound, upperBound);
    if (negInf) {
      result = result.extendToNegativeInfinity();
    }
    if (posInf) {
      result = result.extendToPositiveInfinity();
    }
    return result;
  }

  /**
   * If any of the given values is <code>null</code>,
   * the other value is returned, otherwise the maximum is determined.
   *
   * @param first the first of the values to determine the maximum for. May be <code>null</code>.
   * @param second the second of the values to determine the maximum for. May be <code>null</code>.
   * @return <code>null</code> if both given values are <code>null</code>,
   * the first value if the second value is <code>null</code>,
   * the second value if the first value is <code>null</code>,
   * otherwise the maximum of the given values.
   */
  private static @Nullable BigInteger max(@Nullable BigInteger first, @Nullable BigInteger second) {
    if (first == null) {
      return second;
    } else if (second == null) {
      return first;
    }
    return first.max(second);
  }

  /**
   * If any of the given values is <code>null</code>,
   * the other value is returned, otherwise the minimum is determined.
   *
   * @param first the first of the values to determine the minimum for. May be <code>null</code>.
   * @param second the second of the values to determine the minimum for. May be <code>null</code>.
   * @return <code>null</code> if both given values are <code>null</code>,
   * the first value if the second value is <code>null</code>,
   * the second value if the first value is <code>null</code>,
   * otherwise the minimum of the given values.
   */
  private static @Nullable BigInteger min(@Nullable BigInteger first, @Nullable BigInteger second) {
    if (first == null) {
      return second;
    } else if (second == null) {
      return first;
    }
    return first.min(second);
  }
}
