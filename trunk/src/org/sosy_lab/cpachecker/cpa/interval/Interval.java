// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.interval;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.math.LongMath.saturatedAdd;
import static com.google.common.math.LongMath.saturatedMultiply;

import com.google.common.primitives.Longs;
import java.io.Serializable;
import java.util.Objects;

public final class Interval implements Serializable {
  private static final long serialVersionUID = 4223098080993616295L;

  /** the lower bound of the interval */
  private final Long low;

  /** the upper bound of the interval */
  private final Long high;

  private static final Interval EMPTY = new Interval(null, null);
  public static final Interval UNBOUND = new Interval(Long.MIN_VALUE, Long.MAX_VALUE);
  public static final Interval BOOLEAN_INTERVAL = new Interval(0L, 1L);
  public static final Interval ZERO = new Interval(0L, 0L);
  public static final Interval ONE = new Interval(1L, 1L);

  /**
   * This method acts as constructor for a single-value interval.
   *
   * @param value for the lower and upper bound
   */
  public Interval(Long value) {
    low = value;

    high = value;

    isSane();
  }

  /**
   * This method acts as constructor for a long-based interval.
   *
   * @param low the lower bound
   * @param high the upper bound
   */
  public Interval(Long low, Long high) {
    this.low = low;

    this.high = high;

    isSane();
  }

  private boolean isSane() {
    checkState((low == null) == (high == null), "invalid empty interval");
    checkState(low == null || low <= high, "low cannot be larger than high");

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

  @Override
  public boolean equals(Object other) {
    if (other != null && getClass().equals(other.getClass())) {
      Interval another = (Interval) other;
      return Objects.equals(low, another.low) && Objects.equals(high, another.high);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(low, high);
  }

  /**
   * This method creates a new interval instance representing the union of this interval with
   * another interval.
   *
   * <p>The lower bound and upper bound of the new interval is the minimum of both lower bounds and
   * the maximum of both upper bounds, respectively.
   *
   * @param other the other interval
   * @return the new interval with the respective bounds
   */
  public Interval union(Interval other) {
    if (isEmpty() || other.isEmpty()) {
      return EMPTY;
    } else if (low <= other.low && high >= other.high) {
      return this;
    } else if (low >= other.low && high <= other.high) {
      return other;
    } else {
      return new Interval(Math.min(low, other.low), Math.max(high, other.high));
    }
  }

  /**
   * This method creates a new interval instance representing the intersection of this interval with
   * another interval.
   *
   * <p>The lower bound and upper bound of the new interval is the maximum of both lower bounds and
   * the minimum of both upper bounds, respectively.
   *
   * @param other the other interval
   * @return the new interval with the respective bounds
   */
  public Interval intersect(Interval other) {
    if (intersects(other)) {
      return new Interval(Math.max(low, other.low), Math.min(high, other.high));
    } else {
      return EMPTY;
    }
  }

  /**
   * This method determines if this interval is definitely greater than the other interval.
   *
   * @param other interval to compare with
   * @return true if the lower bound of this interval is always strictly greater than the upper
   *     bound of the other interval, else false
   */
  public boolean isGreaterThan(Interval other) {
    return !isEmpty() && !other.isEmpty() && low > other.high;
  }

  /**
   * This method determines if this interval is definitely greater or equal than the other interval.
   * The equality is only satisfied for one single value!
   *
   * @param other interval to compare with
   * @return true if the lower bound of this interval is always strictly greater or equal than the
   *     upper bound of the other interval, else false
   */
  public boolean isGreaterOrEqualThan(Interval other) {
    return !isEmpty() && !other.isEmpty() && low >= other.high;
  }

  /**
   * This method determines if this interval maybe greater than the other interval.
   *
   * @param other interval to compare with
   * @return true if the upper bound of this interval is strictly greater than the lower bound of
   *     the other interval, else false
   */
  public boolean mayBeGreaterThan(Interval other) {
    return other.isEmpty() || (!isEmpty() && !other.isEmpty() && high > other.low);
  }

  /**
   * This method determines if this interval maybe greater or equal than the other interval.
   *
   * @param other interval to compare with
   * @return true if the upper bound of this interval is strictly greater than the lower bound of
   *     the other interval, else false
   */
  public boolean mayBeGreaterOrEqualThan(Interval other) {
    return other.isEmpty() || (!isEmpty() && !other.isEmpty() && high >= other.low);
  }

  /**
   * New interval instance after the modulo computation.
   *
   * @param other the other interval
   * @return the new interval with the respective bounds.
   */
  public Interval modulo(Interval other) {
    if (other.contains(ZERO)) {
      return Interval.UNBOUND;
    }

    // The interval doesn't contain zero, hence low and high has to be of the same sign.
    // In that case we can call an absolute value on both, as "% (-x)" is the same as "% x".
    other = new Interval(Math.abs(other.low), Math.abs(other.high));

    long newHigh;
    long newLow;

    // New high of the interval can't be higher than the highest value in the divisor.
    // If the divisible element is positive, it is also bounded by it's highest number,
    // or by the absolute value of the lowest number.
    // (-1 % 6 CAN be either -1 or 5 according to the C standard).
    long top;
    if (low >= 0) {
      top = high;
    } else {
      if (low == Long.MIN_VALUE) {
        top = Long.MAX_VALUE;
      } else {
        top = Math.max(Math.abs(low), high);
      }
    }
    newHigh = Math.min(top, other.high - 1);

    // Separate consideration for the case where the divisible number can be negative.
    if (low >= 0) { // If the divisible interval is all positive, the lowest we can ever get is 0.

      // We can only get zero if we include 0 or the number higher than the smallest value of the
      // other interval.
      if (low == 0 || high >= other.low) {
        newLow = 0;
      } else {
        newLow = low;
      }
    } else {
      // The remainder can go negative, but it can not be more negative than the negation of the
      // highest value
      // of the other interval plus 1.
      // (e.g. X mod 14 can not be lower than -13)

      // Remember, <low> is negative in this branch.
      newLow = Math.max(low, 1 - other.high);
    }

    Interval out = new Interval(newLow, newHigh);
    return out;
  }

  /**
   * This method returns a new interval with a limited, i.e. higher, lower bound.
   *
   * @param other the interval to limit this interval
   * @return the new interval with the upper bound of this interval and the lower bound set to the
   *     maximum of this interval's and the other interval's lower bound or an empty interval if
   *     this interval is less than the other interval.
   */
  public Interval limitLowerBoundBy(Interval other) {
    Interval interval = null;

    if (isEmpty() || other.isEmpty() || high < other.low) {
      interval = EMPTY;
    } else {
      interval = new Interval(Math.max(low, other.low), high);
    }

    return interval;
  }

  /**
   * This method returns a new interval with a limited, i.e. lower, upper bound.
   *
   * @param other the interval to limit this interval
   * @return the new interval with the lower bound of this interval and the upper bound set to the
   *     minimum of this interval's and the other interval's upper bound or an empty interval if
   *     this interval is greater than the other interval.
   */
  public Interval limitUpperBoundBy(Interval other) {
    Interval interval = null;

    if (isEmpty() || other.isEmpty() || low > other.high) {
      interval = EMPTY;
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
   * <p>The method still returns true, if the borders match. An empty interval does not contain any
   * interval and is not contained in any interval either. So if the callee or parameter is an empty
   * interval, this method will return false.
   *
   * @param other the other interval
   * @return true if this interval contains the other interval, else false
   */
  public boolean contains(Interval other) {
    return (!isEmpty() && !other.isEmpty() && low <= other.low && other.high <= high);
  }

  /**
   * This method adds an interval from this interval, overflow is handled by setting the bound to
   * Long.MIN_VALUE or Long.MAX_VALUE respectively.
   *
   * @param interval the interval to add
   * @return a new interval with the respective bounds
   */
  public Interval plus(Interval interval) {
    if (isEmpty() || interval.isEmpty()) {
      return EMPTY;
    }

    return new Interval(saturatedAdd(low, interval.low), saturatedAdd(high, interval.high));
  }

  /**
   * This method adds a constant offset to this interval, overflow is handled by setting the bound
   * to Long.MIN_VALUE or Long.MAX_VALUE respectively.
   *
   * @param offset the constant offset to add
   * @return a new interval with the respective bounds
   */
  public Interval plus(Long offset) {
    return plus(new Interval(offset, offset));
  }

  /**
   * This method subtracts an interval from this interval, overflow is handled by setting the bound
   * to Long.MIN_VALUE or Long.MAX_VALUE respectively.
   *
   * @param other interval to subtract
   * @return a new interval with the respective bounds
   */
  public Interval minus(Interval other) {
    return plus(other.negate());
  }

  /**
   * This method subtracts a constant offset to this interval, overflow is handled by setting the
   * bound to Long.MIN_VALUE or Long.MAX_VALUE respectively.
   *
   * @param offset the constant offset to subtract
   * @return a new interval with the respective bounds
   */
  public Interval minus(Long offset) {
    return plus(-offset);
  }

  /**
   * This method multiplies this interval with another interval. In case of an overflow
   * Long.MAX_VALUE and Long.MIN_VALUE are used instead.
   *
   * @param other interval to multiply this interval with
   * @return new interval that represents the result of the multiplication of the two intervals
   */
  public Interval times(Interval other) {
    long[] values = {
      saturatedMultiply(low, other.low),
      saturatedMultiply(low, other.high),
      saturatedMultiply(high, other.low),
      saturatedMultiply(high, other.high)
    };

    return new Interval(Longs.min(values), Longs.max(values));
  }

  /**
   * This method divides this interval by another interval. If the other interval contains "0" an
   * unbound interval is returned.
   *
   * @param other interval to divide this interval by
   * @return new interval that represents the result of the division of the two intervals
   */
  public Interval divide(Interval other) {
    // other interval contains "0", return unbound interval
    if (other.contains(ZERO)) {
      return UNBOUND;
    } else {
      long[] values = {low / other.low, low / other.high, high / other.low, high / other.high};

      return new Interval(Longs.min(values), Longs.max(values));
    }
  }

  /**
   * This method performs an arithmetical left shift of the interval bounds.
   *
   * @param offset Interval offset to perform an arithmetical left shift on the interval bounds. If
   *     the offset maybe less than zero an unbound interval is returned.
   * @return new interval that represents the result of the arithmetical left shift
   */
  public Interval shiftLeft(Interval offset) {
    // create an unbound interval upon trying to shift by a possibly negative offset
    if (ZERO.mayBeGreaterThan(offset)) {
      return UNBOUND;
    } else {
      // if lower bound is negative, shift it by upper bound of offset, else by lower bound of
      // offset
      Long newLow = low << ((low < 0L) ? offset.high : offset.low);

      // if upper bound is negative, shift it by lower bound of offset, else by upper bound of
      // offset
      Long newHigh = high << ((high < 0L) ? offset.low : offset.high);

      if ((low < 0 && newLow > low) || (high > 0 && newHigh < high)) {
        return UNBOUND;
      } else {
        return new Interval(newLow, newHigh);
      }
    }
  }

  /**
   * This method performs an arithmetical right shift of the interval bounds. If the offset maybe
   * less than zero an unbound interval is returned.
   *
   * @param offset Interval offset to perform an arithmetical right shift on the interval bounds
   * @return new interval that represents the result of the arithmetical right shift
   */
  public Interval shiftRight(Interval offset) {
    // create an unbound interval upon trying to shift by a possibly negative offset
    if (ZERO.mayBeGreaterThan(offset)) {
      return UNBOUND;
    } else {
      // if lower bound is negative, shift it by lower bound of offset, else by upper bound of
      // offset
      Long newLow = low >> ((low < 0L) ? offset.low : offset.high);

      // if upper bound is negative, shift it by upper bound of offset, else by lower bound of
      // offset
      Long newHigh = high >> ((high < 0L) ? offset.high : offset.low);

      return new Interval(newLow, newHigh);
    }
  }

  /**
   * This method negates this interval.
   *
   * @return new negated interval
   */
  public Interval negate() {
    return new Interval(saturatedMultiply(high, -1L), saturatedMultiply(low, -1L));
  }

  /**
   * This method determines whether the interval is empty or not.
   *
   * @return true, if the interval is empty, i.e. the lower and upper bounds are null
   */
  public boolean isEmpty() {
    return low == null && high == null;
  }

  public boolean isUnbound() {
    return !isEmpty() && low == Long.MIN_VALUE && high == Long.MAX_VALUE;
  }

  @Override
  public String toString() {
    return "[" + (low == null ? "" : low) + "; " + (high == null ? "" : high) + "]";
  }

  /**
   * This method is a factory method for a lower bounded interval.
   *
   * @param lowerBound the lower bound to set
   * @return a lower bounded interval, i.e. the lower bound is set to the given lower bound, the
   *     upper bound is set to Long.MAX_VALUE
   */
  public static Interval createLowerBoundedInterval(Long lowerBound) {
    return new Interval(lowerBound, Long.MAX_VALUE);
  }

  /**
   * This method is a factory method for an upper bounded interval.
   *
   * @param upperBound the upper bound to set
   * @return an upper bounded interval, i.e. the lower bound is set to Long.MIN_VALUE, the upper
   *     bound is set to the given upper bound
   */
  public static Interval createUpperBoundedInterval(Long upperBound) {
    return new Interval(Long.MIN_VALUE, upperBound);
  }
}
