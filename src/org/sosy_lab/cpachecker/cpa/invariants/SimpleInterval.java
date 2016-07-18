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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.math.BigInteger;
import java.util.Objects;

import javax.annotation.Nullable;

/**
 * This class represents simple convex ranges of BigIntegers.
 * It has an lower bound and an upper bound, both of which may either be a
 * concrete value or infinity. In case of a concrete value, the bound is assumed
 * to be included in the range.
 *
 * All instances of this class are immutable.
 */
public class SimpleInterval {

  /**
   * The lower bound of the interval. <code>null</code> represents
   * negative infinity.
   */
  private final BigInteger lowerBound; // null means negative infinity

  /**
   * The upper bound of the interval. <code>null</code> represents
   * positive infinity.
   */
  private final BigInteger upperBound; // null means positive infinity

  /**
   * Creates a new interval from the given lower bound to the given
   * upper bound. <code>null</code> values are allowed and represent
   * negative infinity for the lower bound or positive infinity for
   * the upper bound.
   *
   * If both bounds are not <code>null</code>, the lower bound must be
   * a value less than or equal to the upper bound.
   *
   * @param pLowerBound the lower bound of the interval. <code>null</code> represents
   * negative infinity.
   * @param pUpperBound the upper bound of the interval. <code>null</code> represents
   * positive infinity.
   */
  private SimpleInterval(@Nullable BigInteger pLowerBound, @Nullable BigInteger pUpperBound) {
    checkArgument((pLowerBound == null)
        || (pUpperBound == null)
        || (pLowerBound.compareTo(pUpperBound) <= 0)
        , "lower endpoint greater than upper end point");

    lowerBound = pLowerBound;
    upperBound = pUpperBound;
  }

  /**
   * Return lower bound (may only be called if {@link #hasLowerBound()} returns true.
   */
  public BigInteger getLowerBound() {
    checkState(lowerBound != null);
    return lowerBound;
  }

  /**
   * Return upper bound (may only be called if {@link #hasUpperBound()} returns true.
   */
  public BigInteger getUpperBound() {
    checkState(upperBound != null);
    return upperBound;
  }

  /**
   * Checks if the interval includes every value.
   * @return <code>true</code> if the interval has neither a lower nor an upper bound, <code>false</code> otherwise.
   */
  public boolean isTop() {
    return upperBound == null && lowerBound == null;
  }

  /**
   * Intersects this interval with the given interval. May only be called
   * if {@link #intersectsWith(SimpleInterval)} returns true.
   *
   * @param pOther the interval to intersect this interval with.
   * @return the intersection of this interval with the given interval.
   */
  public SimpleInterval intersectWith(final SimpleInterval pOther) {
    // Ensure that there is a non-empty intersection between the two intervals
    checkArgument(intersectsWith(pOther));
    if (isSingleton() || pOther.contains(this)) {
      return this;
    }
    if (pOther.isSingleton() || contains(pOther)) {
      return pOther;
    }
    // The lower bound of this interval is a candidate for the new lower bound
    BigInteger lowerBound = this.lowerBound;
    // If the other interval has a finite lower bound, it is a candidate as well
    if (pOther.hasLowerBound()) {
      BigInteger otherLowerBound = pOther.getLowerBound();
      /*
       *  If this interval has negative infinity as its lower bound, the
       *  lower bound of the other interval automatically becomes the new
       *  lower bound, otherwise the new lower bound is the maximum of
       *  both lower bounds.
       */
      if (lowerBound == null) {
        lowerBound = otherLowerBound;
      } else {
        lowerBound = lowerBound.max(otherLowerBound);
      }
    }
    // The upper bound of this interval is a candidate for the new lower bound
    BigInteger upperBound = this.upperBound;
    // If the other interval has a finite upper bound, it is a candidate as well
    if (pOther.hasUpperBound()) {
      BigInteger otherUpperBound = pOther.getUpperBound();
      /*
       *  If this interval has positive infinity as its upper bound, the
       *  upper bound of the other interval automatically becomes the new
       *  upper bound, otherwise the new upper bound is the minimum of
       *  both upper bounds.
       */
      if (upperBound == null) {
        upperBound = otherUpperBound;
      } else {
        upperBound = upperBound.min(otherUpperBound);
      }
    }
    return new SimpleInterval(lowerBound, upperBound);
  }

  /**
   * Return whether this interval has a concrete lower bound
   * (otherwise it's positive infinity).
   */
  public boolean hasLowerBound() {
    return lowerBound != null;
  }

  /**
   * Return whether this interval has a concrete upper bound
   * (otherwise it's positive infinity).
   */
  public boolean hasUpperBound() {
    return upperBound != null;
  }

  /**
   * Checks if this interval contains at least one positive value.
   * @return <code>true</code> if this interval contains at least one
   * positive value, <code>false</code> otherwise.
   */
  public boolean containsPositive() {
    return (upperBound == null || upperBound.signum() == 1);
  }

  /**
   * Checks if this interval contains the value zero.
   * @return <code>true</code> if this interval contains the value zero,
   * <code>false</code> otherwise.
   */
  public boolean containsZero() {
    return (upperBound == null || upperBound.signum() >= 0)
        && (lowerBound == null || lowerBound.signum() <= 0);
  }

  /**
   * Checks if this interval contains the value zero.
   * @return <code>true</code> if this interval contains the value zero,
   * <code>false</code> otherwise.
   */
  public boolean contains(BigInteger pValue) {
    return (upperBound == null || upperBound.compareTo(pValue) >= 0)
        && (lowerBound == null || lowerBound.compareTo(pValue) <= 0);
  }

  /**
   * Checks if this interval contains at least one negative value.
   * @return <code>true</code> if this interval contains at least one
   * negative value, <code>false</code> otherwise.
   */
  public boolean containsNegative() {
    return (lowerBound == null || lowerBound.signum() == -1);
  }

  /**
   * Computes the size of this interval. If any of the two bounds are
   * infinity, <code>null</code> is returned, otherwise the result
   * is the upper bound minus the lower bound plus one.
   *
   * @return <code>null</code> if any of the two bounds are
   * infinity, otherwise the upper bound minus the lower bound plus one.
   */
  public @Nullable BigInteger size() {
    if (hasLowerBound() && hasUpperBound()) {
      return upperBound.subtract(lowerBound).add(BigInteger.ONE);
    } else {
      return null;
    }
  }

  /**
   * Checks if this interval contains exactly one single value. If this
   * function returns <code>true</code>, {@link #getLowerBound()} may
   * safely be called to retrieve the value.
   *
   * @return <code>true</code> if this interval contains exactly one
   * single value, <code>false</code> otherwise.
   */
  public boolean isSingleton() {
    return hasLowerBound() && lowerBound.equals(upperBound);
  }

  /**
   * Returns the mathematical negation of this interval. The lower bound
   * of the resulting interval is the negated upper bound of this interval
   * and vice versa. This, of course, includes infinity becoming negative
   * infinity and negative infinity becoming infinity.
   *
   * @return the mathematical negation of this interval.
   */
  public SimpleInterval negate() {
    BigInteger newUpperBound = (lowerBound == null ? null : lowerBound.negate());
    BigInteger newLowerBound = (upperBound == null ? null : upperBound.negate());

    if (newLowerBound == null && newUpperBound == null) {
      return infinite();
    } else {
      return new SimpleInterval(newLowerBound, newUpperBound);
    }
  }

  /**
   * Returns an interval from this interval's lower bound to positive
   * infinity.
   *
   * @return an interval from this interval's lower bound to positive
   * infinity.
   */
  public SimpleInterval extendToPositiveInfinity() {
    if (lowerBound == null) {
      return infinite();
    } else if (upperBound == null) {
      return this;
    } else {
      return new SimpleInterval(lowerBound, null);
    }
  }

  /**
   * Returns an interval from this interval's lower bound to negative
   * infinity.
   *
   * @return an interval from this interval's lower bound to negative
   * infinity.
   */
  public SimpleInterval extendToNegativeInfinity() {
    if (upperBound == null) {
      return infinite();
    } else if (lowerBound == null) {
      return this;
    } else {
      return new SimpleInterval(null, upperBound);
    }
  }

  @Override
  public boolean equals(Object pObj) {
    if (pObj == this) {
      return true;
    } else if (!(pObj instanceof SimpleInterval)) {
      return false;
    }

    SimpleInterval other = (SimpleInterval) pObj;
    return Objects.equals(this.lowerBound, other.lowerBound)
        && Objects.equals(this.upperBound, other.upperBound);
  }

  @Override
  public int hashCode() {
    return Objects.hash(lowerBound, upperBound);
  }

  @Override
  public String toString() {
    String result;
    if (lowerBound == null) {
      result = "(-INF, ";
    } else {
      result = "[" + lowerBound + ", ";
    }

    if (upperBound == null) {
      result += "INF)";
    } else {
      result += upperBound + "]";
    }

    return result;
  }

  /**
   * Checks if this interval contains the given interval.
   * @param other the interval that this interval is checked for containing.
   * @return <code>true</code> if this interval contains the given
   * interval, <code>false</code> otherwise.
   */
  public boolean contains(SimpleInterval other) {
    if (this == other) {
      return true;
    }

    if (this.lowerBound != null && other.lowerBound == null) {
      return false;
    }

    if (this.upperBound != null && other.upperBound == null) {
      return false;
    }

    return (this.lowerBound == null || this.lowerBound.compareTo(other.lowerBound) <= 0)
        && (this.upperBound == null || this.upperBound.compareTo(other.upperBound) >= 0);
  }

  /**
   * Checks if this interval touches the given interval, which means the
   * intervals either intersect each other or there is one upper bound
   * in one of the two intervals that is exactly the lower bound of the
   * other interval minus one.
   *
   * @param pOther the interval to check for touching this interval.
   * @return <code>true</code> if this interval touches the given
   * interval, <code>false</code> otherwise.
   */
  public boolean touches(SimpleInterval pOther) {
    if (pOther == null) { return false; }
    if (intersectsWith(pOther)) { return true; }
    return (this.lowerBound != null
            && pOther.upperBound != null
            && pOther.upperBound.add(BigInteger.ONE).equals(this.lowerBound))
        || (pOther.lowerBound != null
            && this.upperBound != null
            && this.upperBound.add(BigInteger.ONE).equals(pOther.lowerBound));
  }

  /**
   * Checks if the given interval intersects with this interval.
   * @param other the interval to check for intersecting this interval.
   * @return <code>true</code> if this interval intersects with the
   * given interval, <code>false</code> otherwise.
   */
  public boolean intersectsWith(SimpleInterval other) {
    if (this == other) { return true; }

    if (this.lowerBound == null) {
      if (this.upperBound == null || other.lowerBound == null) {
        return true;
      } else {
        // this is (-INF, a]; other is [b, ?)
        // result is true if a >= b
        return this.upperBound.compareTo(other.lowerBound) >= 0;
      }

    } else if (this.upperBound == null) {
      if (other.upperBound == null) {
        return true;
      } else {
        // this is [a, INF); other is (?, b]
        // result is true if a <= b
        return this.lowerBound.compareTo(other.upperBound) <= 0;
      }

    } else {
      if (other.lowerBound == null && other.upperBound == null) {
        // this is [a, b]; other is (-INF, INF)
        return true;
      } else if (other.lowerBound == null) {
        // this is [a, b]; other is (-INF, c]
        // result is true if a <= c
        return this.lowerBound.compareTo(other.upperBound) <= 0;
      } else if (other.upperBound == null) {
        // this is [a, b]; other is [c, INF)
        // result is true if b >= c
        return this.upperBound.compareTo(other.lowerBound) >= 0;
      } else {
        // this is [a, b]; other is [c, d]
        // result is true if a <= d and b >= c
        boolean aLessThanOrEqB = this.lowerBound.compareTo(other.upperBound) <= 0;
        boolean bGreaterThanOrEqC = this.upperBound.compareTo(other.lowerBound) >= 0;
        return aLessThanOrEqB && bGreaterThanOrEqC;
      }
    }
  }

  /**
   * Gets the closest negative value to zero of this interval.
   * May only be called if {@link #containsNegative()} returns true.
   *
   * @return the closest negative value to zero of this interval.
   */
  public BigInteger closestNegativeToZero() {
    checkState(containsNegative());
    if (isSingleton()) { return getLowerBound(); }
    if (hasUpperBound() && getUpperBound().signum() < 0) { return getUpperBound(); }
    return BigInteger.ONE.negate();
  }

  /**
   * Gets the closest positive value to zero of this interval.
   * May only be called if {@link #containsPositive()} returns true.
   *
   * @return the closest positive value to zero of this interval.
   */
  public BigInteger closestPositiveToZero() {
    checkState(containsPositive());
    if (isSingleton()) { return getLowerBound(); }
    if (hasLowerBound() && getLowerBound().signum() > 0) { return getLowerBound(); }
    return BigInteger.ONE;
  }

  private static SimpleInterval INFINITE = new SimpleInterval(null, null);

  public static SimpleInterval infinite() {
    return INFINITE;
  }

  public static SimpleInterval singleton(BigInteger i) {
    return new SimpleInterval(checkNotNull(i), i);
  }

  public static SimpleInterval greaterOrEqual(BigInteger i) {
    return new SimpleInterval(checkNotNull(i), null);
  }

  public static SimpleInterval lessOrEqual(BigInteger i) {
    return new SimpleInterval(null, checkNotNull(i));
  }

  public static SimpleInterval of(BigInteger lowerBound, BigInteger upperBound) {
    return new SimpleInterval(checkNotNull(lowerBound), checkNotNull(upperBound));
  }

  /**
   * Create the smallest interval that contains two given intervals;
   */
  public static SimpleInterval span(SimpleInterval a, SimpleInterval b) {
    BigInteger lower;
    if (a.lowerBound == null || b.lowerBound == null) {
      lower = null;
    } else {
      lower = a.lowerBound.min(b.lowerBound);
    }

    BigInteger upper;
    if (a.upperBound == null || b.upperBound == null) {
      upper = null;
    } else {
      upper = a.upperBound.max(b.upperBound);
    }

    if (lower == a.lowerBound && upper == a.upperBound) {
      return a;
    } else if (lower == b.lowerBound && upper == b.upperBound) {
      return b;
    } else {
      return new SimpleInterval(lower, upper);
    }
  }
}
