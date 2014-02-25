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
package org.sosy_lab.cpachecker.cpa.interval;

import java.util.Arrays;
import java.util.Collections;

public class Interval {
  /**
   * the lower bound of the interval
   */
  private final Long low;

  /**
   * the upper bound of the interval
   */
  private final Long high;

  /**
   * an interval representing a false value
   */
  public static final Interval FALSE = new Interval(0L, 0L);

  /**
   * an interval representing an impossible interval
   */
  public static final Interval EMPTY = createEmptyInterval();

  /**
   * This method acts as constructor for an empty interval.
   */
  private Interval() {
    this.low = null;
    this.high = null;
  }

  /**
   * This method acts as constructor for a single-value interval.
   *
   * @param value for the lower and upper bound
   */
  public Interval(Long value) {
    this.low  = value;

    this.high = value;

    isSane();
  }

  /**
   * This method acts as constructor for a long-based interval.
   *
   * @param low the lower bound
   * @param high the upper bound
   */
  public Interval(Long low, Long high) {
    this.low  = low;

    this.high = high;

    isSane();
  }

  private boolean isSane() {
    if (low > high) {
      throw new IllegalStateException("low cannot be larger than high");
    }

    return true;
  }

  /**
   * This method returns the lower bound of the interval.
   *
   * @return the lower bound
   */
  public Long getLow() {
    return low;
  }

  /**
   * This method returns the upper bound of the interval.
   *
   * @return the upper bound
   */
  public Long getHigh() {
    return high;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object other) {
    if (other != null && getClass().equals(other.getClass())) {
      Interval another = (Interval)other;

      if (isEmpty() && another.isEmpty()) {
        return true;
      } else if (isEmpty() || another.isEmpty()) {
        return false;
      }

      return low.equals(another.low) && high.equals(another.high);
    } else {
      return false;
    }
  }

  public boolean isSingular() {
    return low.equals(high);
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    if (isEmpty()) {
      return 0;
    }

    int result = 17;

    result = 31 * result + low.hashCode();
    result = 31 * result + high.hashCode();

    return result;
  }

  /**
   * This method creates a new interval instance representing the union of this interval with another interval.
   *
   * The lower bound and upper bound of the new interval is the minimum of both lower bounds and the maximum of both upper bounds, respectively.
   *
   * @param other the other interval
   * @return the new interval with the respective bounds
   */
  public Interval union(Interval other) {
    if (isEmpty() || other.isEmpty()) {
      return createEmptyInterval();
    } else if (low >= other.low && high <= other.high) {
      return other;
    } else {
      return new Interval(Math.min(low, other.low), Math.max(high, other.high));
    }
  }

  /**
   * This method creates a new interval instance representing the intersection of this interval with another interval.
   *
   * The lower bound and upper bound of the new interval is the maximum of both lower bounds and the minimum of both upper bounds, respectively.
   *
   * @param other the other interval
   * @return the new interval with the respective bounds
   */
  public Interval intersect(Interval other) {
    Interval interval = null;

    if (this.intersects(other)) {
      interval = new Interval(Math.max(low, other.low), Math.min(high, other.high));
    } else {
      interval = createEmptyInterval();
    }

    return interval;
  }

  /**
   * This method determines if this interval is definitely less than the other interval.
   *
   * @param other interval to compare with
   * @return true if the upper bound of this interval is always strictly lower than the lower bound of the other interval, else false
   */
  public boolean isLessThan(Interval other) {
    return !isEmpty() && !other.isEmpty() && high < other.low;
  }

  /**
   * This method determines if this interval is definitely greater than the other interval.
   *
   * @param other interval to compare with
   * @return true if the lower bound of this interval is always strictly greater than the upper bound of the other interval, else false
   */
  public boolean isGreaterThan(Interval other) {
    return !isEmpty() && !other.isEmpty() && low > other.high;
  }

  /**
   * This method determines if this interval maybe less than the other interval.
   *
   * @param other interval to compare with
   * @return true if the lower bound of this interval is strictly lower than the upper bound of the other interval, else false
   */
  public boolean mayBeLessThan(Interval other) {
    return isEmpty() || (!isEmpty() && !other.isEmpty() && low < other.high);
  }

  /**
   * This method determines if this interval maybe less or equal than the other interval.
   *
   * @param other interval to compare with
   * @return true if the lower bound of this interval is strictly lower than the upper bound of the other interval, else false
   */
  public boolean mayBeLessOrEqualThan(Interval other) {
    return isEmpty() || (!isEmpty() && !other.isEmpty() && low <= other.high);
  }

  /**
   * This method determines if this interval maybe greater than the other interval.
   *
   * @param other interval to compare with
   * @return true if the upper bound of this interval is strictly greater than the lower bound of the other interval, else false
   */
  public boolean mayBeGreaterThan(Interval other) {
    return other.isEmpty() || (!isEmpty() && !other.isEmpty() && high > other.low);
  }

  /**
   * This method determines if this interval maybe greater or equal than the other interval.
   *
   * @param other interval to compare with
   * @return true if the upper bound of this interval is strictly greater than the lower bound of the other interval, else false
   */
  public boolean mayBeGreaterOrEqualThan(Interval other) {
    return other.isEmpty() || (!isEmpty() && !other.isEmpty() && high >= other.low);
  }

  /**
   * This method determines if this interval represents a false value.
   *
   * @return true if this interval represents only values in the interval [0, 0].
   */
  public boolean isFalse() {
    return equals(FALSE);
  }

  /**
   * This method determines if this interval represents a true value.
   *
   * @return true if this interval represents values that are strictly less than 0 or greater than 0.
   */
  public boolean isTrue() {
    return !isEmpty() && (high < 0 || low > 0);
  }

  /**
   * This method creates a new interval instance with the lower and upper bounds being the minimum of both the lower and upper bounds, respectively.
   *
   * @param other the other interval
   * @return the new interval with the respective bounds
   */
  public Interval minimum(Interval other) {
    Interval interval = new Interval(Math.min(low, other.low), Math.min(high, other.high));

    return interval;
  }

  /**
   * This method creates a new interval instance with the lower and upper bounds being the maximum of both the lower and upper bounds, respectively.
   *
   * @param other the other interval
   * @return the new interval with the respective bounds
   */
  public Interval maximum(Interval other) {
    Interval interval = new Interval(Math.max(low, other.low), Math.max(high, other.high));

    return interval;
  }

  /**
   * This method returns a new interval with a limited, i.e. higher, lower bound.
   *
   * @param other the interval to limit this interval
   * @return the new interval with the upper bound of this interval and the lower bound set to the maximum of this interval's and the other interval's lower bound or an empty interval if this interval is less than the other interval.
   */
  public Interval limitLowerBoundBy(Interval other) {
    Interval interval = null;

    if (isEmpty() || other.isEmpty() || high < other.low) {
      interval = createEmptyInterval();
    } else {
      interval = new Interval(Math.max(low, other.low), high);
    }

    return interval;
  }

  /**
   * This method returns a new interval with a limited, i.e. lower, upper bound.
   *
   * @param other the interval to limit this interval
   * @return the new interval with the lower bound of this interval and the upper bound set to the minimum of this interval's and the other interval's upper bound or an empty interval if this interval is greater than the other interval.
   */
  public Interval limitUpperBoundBy(Interval other) {
    Interval interval = null;

    if (isEmpty() || other.isEmpty() || low > other.high) {
      interval = createEmptyInterval();
    } else {
      interval = new Interval(low, Math.min(high, other.high));
    }

    return interval;
  }

  /**
   * This method determines if this interval intersects with another interval.
   *
   * @param other the other interval
   * @return true if the intervals intersect, else false
   */
  public boolean intersects(Interval other) {
      if (isEmpty() || other.isEmpty()) {
        return false;
      }

      return (low >= other.low && low <= other.high)
        || (high >= other.low && high <= other.high)
        || (low <= other.low && high >= other.high);
  }

  /**
   * This method determines if this interval contains another interval.
   *
   * The method still returns true, if the borders match. An empty interval does not contain any interval and is not contained in any interval either. So if the callee or parameter is an empty interval, this method will return false.
   *
   * @param other the other interval
   * @return true if this interval contains the other interval, else false
   */
  public boolean contains(Interval other) {
     return (!isEmpty() && !other.isEmpty()
               && low <= other.low && other.high <= high);
  }

  /**
   * This method adds an interval from this interval, overflow is handled by setting the bound to Long.MIN_VALUE or Long.MAX_VALUE respectively.
   *
   * @param interval the interval to add
   * @return a new interval with the respective bounds
   */
  public Interval plus(Interval interval) {
    if (isEmpty() || interval.isEmpty()) {
      return createEmptyInterval();
    }

    return new Interval(scalarPlus(low, interval.low), scalarPlus(high, interval.high));
  }

  /**
   * This method adds a constant offset to this interval, overflow is handled by setting the bound to Long.MIN_VALUE or Long.MAX_VALUE respectively.
   *
   * @param offset the constant offset to add
   * @return a new interval with the respective bounds
   */
  public Interval plus(Long offset) {
    return plus(new Interval(offset, offset));
  }

  /**
   * This method subtracts an interval from this interval, overflow is handled by setting the bound to Long.MIN_VALUE or Long.MAX_VALUE respectively.
   *
   * @param other interval to subtract
   * @return a new interval with the respective bounds
   */
  public Interval minus(Interval other) {
    return plus(other.negate());
  }

  /**
   * This method subtracts a constant offset to this interval, overflow is handled by setting the bound to Long.MIN_VALUE or Long.MAX_VALUE respectively.
   *
   * @param offset the constant offset to subtract
   * @return a new interval with the respective bounds
   */
  public Interval minus(Long offset) {
    return plus(-offset);
  }

  /**
   * This method multiplies this interval with another interval. In case of an overflow Long.MAX_VALUE and Long.MIN_VALUE are used instead.
   *
   * @param other interval to multiply this interval with
   * @return new interval that represents the result of the multiplication of the two intervals
   */
  public Interval times(Interval other) {
    Long[] values = {
                      scalarTimes(low, other.low),
                      scalarTimes(low, other.high),
                      scalarTimes(high, other.low),
                      scalarTimes(high, other.high)
                    };

    return new Interval(Collections.min(Arrays.asList(values)), Collections.max(Arrays.asList(values)));
  }

  /**
   * This method divides this interval by another interval. If the other interval contains "0" an unbound interval is returned.
   *
   * @param other interval to divide this interval by
   * @return new interval that represents the result of the division of the two intervals
   */
  public Interval divide(Interval other) {
    // other interval contains "0", return unbound interval
    if (other.contains(FALSE)) {
      return createUnboundInterval();
    } else {
      Long[] values = {
                        low / other.low,
                        low / other.high,
                        high / other.low,
                        high / other.high
                      };

      return new Interval(Collections.min(Arrays.asList(values)), Collections.max(Arrays.asList(values)));
    }
  }

  /**
  * This method performs an arithmetical left shift of the interval bounds.
  *
  * @param Interval offset to perform an arithmetical left shift on the interval bounds. If the offset maybe less than zero an unbound interval is returned.
  * @return new interval that represents the result of the arithmetical left shift
  */
  public Interval shiftLeft(Interval offset) {
    // create an unbound interval upon trying to shift by a possibly negative offset
    if (offset.mayBeLessThan(FALSE)) {
      return createUnboundInterval();
    } else {
      // if lower bound is negative, shift it by upper bound of offset, else by lower bound of offset
      Long newLow   = low << ((low < 0L) ? offset.high : offset.low);

      // if upper bound is negative, shift it by lower bound of offset, else by upper bound of offset
      Long newHigh  = high << ((high < 0L) ? offset.low : offset.high);

      if ((low < 0 && newLow > low) || (high > 0 && newHigh < high)) {
        return createUnboundInterval();
      } else {
        return new Interval(newLow, newHigh);
      }
    }
  }

  /**
  * This method performs an arithmetical right shift of the interval bounds. If the offset maybe less than zero an unbound interval is returned.
  *
  * @param Interval offset to perform an arithmetical right shift on the interval bounds
  * @return new interval that represents the result of the arithmetical right shift
  */
  public Interval shiftRight(Interval offset) {
    // create an unbound interval upon trying to shift by a possibly negative offset
    if (offset.mayBeLessThan(FALSE)) {
      return createUnboundInterval();
    } else {
      // if lower bound is negative, shift it by lower bound of offset, else by upper bound of offset
      Long newLow   = low >> ((low < 0L) ? offset.low : offset.high);

      // if upper bound is negative, shift it by upper bound of offset, else by lower bound of offset
      Long newHigh  = high >> ((high < 0L) ? offset.high : offset.low);

      return new Interval(newLow, newHigh);
    }
  }

  /**
   * This method negates this interval.
   *
   * @return new negated interval
   */
  public Interval negate() {
    return new Interval(scalarTimes(high, -1L), scalarTimes(low, -1L));
  }

  /**
   * This method determines whether the interval is empty or not.
   *
   * @return true, if the interval is empty, i.e. the lower and upper bounds are null
   */
  public boolean isEmpty() {
    return low == null && high == null;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "[" + low + "; " + high + "]";
  }

  /**
   * This method is a factory method for an empty interval
   *
   * @return an empty interval
   */
  private static Interval createEmptyInterval() {
    return new Interval();
  }

  /**
   * This method is a factory method for an unbounded interval
   *
   * @return an unbounded interval, i.e. the lower and upper bound are set to Long.MIN_VALUE and Long.MAX_VALUE respectively
   */
  public static Interval createUnboundInterval() {
    return new Interval(Long.MIN_VALUE, Long.MAX_VALUE);
  }

  /**
   * This method is a factory method for a lower bounded interval.
   *
   * @param lowerBound the lower bound to set
   * @return a lower bounded interval, i.e. the lower bound is set to the given lower bound, the upper bound is set to Long.MAX_VALUE
   */
  public static Interval createLowerBoundedInterval(Long lowerBound) {
    return new Interval(lowerBound, Long.MAX_VALUE);
  }

  /**
   * This method is a factory method for an upper bounded interval.
   *
   * @param upperBound the upper bound to set
   * @return an upper bounded interval, i.e. the lower bound is set to Long.MIN_VALUE, the upper bound is set to the given upper bound
   */
  public static Interval createUpperBoundedInterval(Long upperBound) {
    return new Interval(Long.MIN_VALUE, upperBound);
  }

  /**
   * This method is a factory method for an interval representing the FALSE value.
   *
   * @return an interval representing the FALSE value, i.e. the lower and upper bound are set to 0
   */
  public static Interval createFalseInterval() {
    return new Interval(0L);
  }

  /**
   * This method is a factory method for an interval representing the TRUE value.
   *
   * @return an interval representing the TRUE value, i.e. the lower and upper bound are set to 1
   */
  public static Interval createTrueInterval() {
    return new Interval(1L);
  }

  /**
   * This method adds two scalar values and returns their sum, or on overflow Long.MAX_VALUE or Long.MIN_VALUE, respectively.
   *
   * @param x the first scalar operand
   * @param y the second scalar operand
   * @return the sum of the first and second scalar operand or on overflow Long.MAX_VALUE and Long.MIN_VALUE, respectively.
   */
  private static Long scalarPlus(Long x, Long y) {
    Long result = x + y;

    // both operands are positive but the result is negative
    if ((Long.signum(x) + Long.signum(y) == 2) && Long.signum(result) == -1) {
      result = Long.MAX_VALUE;
    } else  if ((Long.signum(x) + Long.signum(y) == -2) && Long.signum(result) == +1) {
      result = Long.MIN_VALUE;
    }

    return result;
  }

  /**
   * This method multiplies two scalar values and returns their product, or on overflow Long.MAX_VALUE or Long.MIN_VALUE, respectively.
   *
   * @param x the first scalar operand
   * @param y the second scalar operand
   * @return the product of the first and second scalar operand or on overflow Long.MAX_VALUE and Long.MIN_VALUE, respectively.
   */
  private static Long scalarTimes(Long x, Long y) {
    Long bound = (Long.signum(x) == Long.signum(y)) ? Long.MAX_VALUE : Long.MIN_VALUE;

    // if overflow occurs, return the respective bound
    if (x != 0 && (y > 0 && y > (bound / x) || y < 0 && y < (bound / x))) {
      return bound;
    } else {
      return x * y;
    }
  }
}
