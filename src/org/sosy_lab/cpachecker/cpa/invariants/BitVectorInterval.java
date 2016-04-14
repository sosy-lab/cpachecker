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

import com.google.common.base.Preconditions;

import java.math.BigInteger;
import java.util.Objects;

/**
 * This class represents simple convex ranges of BigIntegers.
 * It has an lower bound and an upper bound, both of which may either be a
 * concrete value or infinity. In case of a concrete value, the bound is assumed
 * to be included in the range.
 *
 * All instances of this class are immutable.
 */
public class BitVectorInterval implements BitVectorType {

  /**
   * The lower bound of the interval.
   */
  private final BigInteger lowerBound;

  /**
   * The upper bound of the interval.
   */
  private final BigInteger upperBound;

  /**
   * Size and signedness.
   */
  private final BitVectorInfo info;

  /**
   * Creates a new interval from the given lower bound to the given upper bound.
   *
   * The lower bound must be a value less than or equal to the upper bound.
   *
   * @param pLowerBound the lower bound of the interval.
   * @param pUpperBound the upper bound of the interval.
   */
  private BitVectorInterval(BitVectorInfo pInfo, BigInteger pLowerBound, BigInteger pUpperBound) {
    checkNotNull(pInfo);
    checkNotNull(pLowerBound);
    checkNotNull(pUpperBound);
    checkArgument(pLowerBound.compareTo(pUpperBound) <= 0
        , "lower endpoint greater than upper end point");
    checkArgument(pLowerBound.compareTo(pInfo.getMinValue()) >= 0, "lower bound must fit the bit vector");
    checkArgument(pUpperBound.compareTo(pInfo.getMaxValue()) <= 0, "upper bound must fit the bit vector");

    info = pInfo;
    lowerBound = pLowerBound;
    upperBound = pUpperBound;
  }

  /**
   * Gets information about size and signedness of the bit vector.
   *
   * @return information about size and signedness of the bit vector.
   */
  @Override
  public BitVectorInfo getTypeInfo() {
    return info;
  }

  /**
   * Return lower bound (may only be called if {@link #hasLowerBound()} returns true.
   */
  public BigInteger getLowerBound() {
    return lowerBound;
  }

  /**
   * Return upper bound (may only be called if {@link #hasUpperBound()} returns true.
   */
  public BigInteger getUpperBound() {
    return upperBound;
  }

  /**
   * Checks if the interval includes every value.
   * @return <code>true</code> if the interval has neither a lower nor an upper bound, <code>false</code> otherwise.
   */
  public boolean isTop() {
    return false;
  }

  public void checkBitVectorCompatibilityWith(BitVectorInterval pOther) {
    Preconditions.checkArgument(info.equals(pOther.info),
        "bit vectors are incompatible in size or signedness");
  }

  /**
   * Intersects this interval with the given interval. May only be called
   * if {@link #intersectsWith(BitVectorInterval)} returns true.
   *
   * @param pOther the interval to intersect this interval with.
   * @return the intersection of this interval with the given interval.
   */
  public BitVectorInterval intersectWith(final BitVectorInterval pOther) {
    checkBitVectorCompatibilityWith(pOther);

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

    // The lower bound of the other interval is a candidate as well
    BigInteger otherLowerBound = pOther.getLowerBound();
    /*
     *  The new lower bound is the maximum of both lower bounds.
     */
    lowerBound = lowerBound.max(otherLowerBound);

    // The upper bound of this interval is a candidate for the new lower bound
    BigInteger upperBound = this.upperBound;
    // The upper bound of the other interval is a candidate as well
    BigInteger otherUpperBound = pOther.getUpperBound();
    /*
     *  The new upper bound is the minimum of both upper bounds.
     */
    upperBound = upperBound.min(otherUpperBound);

    return new BitVectorInterval(info, lowerBound, upperBound);
  }

  public BitVectorInterval getNegativePart() {
    Preconditions.checkArgument(containsNegative(), "This interval has no negative part.");
    return BitVectorInterval.of(info, lowerBound, BigInteger.valueOf(-1).min(upperBound));
  }

  public BitVectorInterval getPositivePart() {
    Preconditions.checkArgument(containsPositive(), "This interval has no positive part.");
    return BitVectorInterval.of(info, BigInteger.ONE.max(lowerBound), upperBound);
  }

  /**
   * Return whether this interval has a concrete lower bound
   * (otherwise it's positive infinity).
   */
  public boolean hasLowerBound() {
    return true;
  }

  /**
   * Return whether this interval has a concrete upper bound
   * (otherwise it's positive infinity).
   */
  public boolean hasUpperBound() {
    return true;
  }

  /**
   * Checks if this interval contains at least one positive value.
   * @return <code>true</code> if this interval contains at least one
   * positive value, <code>false</code> otherwise.
   */
  public boolean containsPositive() {
    return upperBound.signum() == 1;
  }

  /**
   * Checks if this interval contains the value zero.
   * @return <code>true</code> if this interval contains the value zero,
   * <code>false</code> otherwise.
   */
  public boolean containsZero() {
    return upperBound.signum() >= 0
        && lowerBound.signum() <= 0;
  }

  /**
   * Checks if this interval contains the value zero.
   * @return <code>true</code> if this interval contains the value zero,
   * <code>false</code> otherwise.
   */
  public boolean contains(BigInteger pValue) {
    return upperBound.compareTo(pValue) >= 0
        && lowerBound.compareTo(pValue) <= 0;
  }

  /**
   * Checks if this interval contains at least one negative value.
   * @return <code>true</code> if this interval contains at least one
   * negative value, <code>false</code> otherwise.
   */
  public boolean containsNegative() {
    return lowerBound.signum() == -1;
  }

  /**
   * Computes the size of this interval:
   * The upper bound minus the lower bound plus one.
   *
   * @return The upper bound minus the lower bound plus one.
   */
  public BigInteger size() {
    return upperBound.subtract(lowerBound).add(BigInteger.ONE);
  }

  /**
   * Checks if this interval contains exactly one single value. If this
   * function returns <code>true</code>, {@link #getLowerBound()} may
   * be called to retrieve the value.
   *
   * @return <code>true</code> if this interval contains exactly one
   * single value, <code>false</code> otherwise.
   */
  public boolean isSingleton() {
    return lowerBound.equals(upperBound);
  }

  /**
   * Returns the mathematical negation of this interval. The lower bound
   * of the resulting interval is the negated upper bound of this interval
   * and vice versa.
   *
   * @param pAllowSignedWrapAround whether or not to allow wrap-around for
   * signed bit vectors.
   *
   * @return the mathematical negation of this interval.
   */
  public BitVectorInterval negate(boolean pAllowSignedWrapAround, OverflowEventHandler pOverflowEventHandler) {
    BigInteger newLowerBound = upperBound.negate();
    BigInteger newUpperBound = lowerBound.negate();

    boolean lbExceedsBelow = newLowerBound.compareTo(info.getMinValue()) < 0;
    boolean lbExceedsAbove = !lbExceedsBelow && newLowerBound.compareTo(info.getMaxValue()) > 0;
    boolean ubExceedsBelow = newUpperBound.compareTo(info.getMinValue()) < 0;
    boolean ubExceedsAbove = !ubExceedsBelow && newUpperBound.compareTo(info.getMaxValue()) > 0;
    if (lbExceedsBelow || lbExceedsAbove || ubExceedsBelow || ubExceedsAbove) {
      // If the type is signed, wrap-around is implementation defined
      if (!pAllowSignedWrapAround && info.isSigned()) {
        pOverflowEventHandler.signedOverflow();
        return info.getRange();
      }
      final BigInteger fromLB;
      final BigInteger fromUB;
      BigInteger rangeLength = info.getRange().size();
      if (lbExceedsBelow) {
        fromLB = rangeLength.add(newLowerBound);
      } else if (lbExceedsAbove) {
        fromLB = newLowerBound.subtract(rangeLength);
      } else {
        fromLB = newLowerBound;
      }
      if (ubExceedsBelow) {
        fromUB = rangeLength.add(newUpperBound);
      } else if (ubExceedsAbove) {
        fromUB = newUpperBound.subtract(rangeLength);
      } else {
        fromUB = newUpperBound;
      }
      if (fromLB.compareTo(fromUB) > 0) {
        return info.getRange();
      }
      newLowerBound = fromLB;
      newUpperBound = fromUB;
    }

    return new BitVectorInterval(info, newLowerBound, newUpperBound);
  }

  public static BitVectorInterval cast(BitVectorInfo pInfo,
      BigInteger pI,
      boolean pAllowSignedWrapAround,
      OverflowEventHandler pOverflowEventHandler) {
    if (pInfo.getRange().contains(pI)) {
      return BitVectorInterval.singleton(pInfo, pI);
    }
    // If the type is signed, wrap-around is implementation defined
    if (!pAllowSignedWrapAround && pInfo.isSigned()) {
      pOverflowEventHandler.signedOverflow();
      return pInfo.getRange();
    }
    BigInteger rangeLength = pInfo.getRange().size();
    BigInteger value = pI.remainder(rangeLength);
    if (value.compareTo(pInfo.getMinValue()) < 0) {
      value = value.add(rangeLength);
    } else if (value.compareTo(pInfo.getMaxValue()) > 0) {
      value = value.subtract(rangeLength);
    }
    return BitVectorInterval.singleton(pInfo, value);
  }

  public static BitVectorInterval cast(BitVectorInfo pInfo,
      BigInteger pLowerBound,
      BigInteger pUpperBound,
      boolean pAllowSignedWrapAround,
      OverflowEventHandler pOverflowEventHandler) {
    if (pLowerBound.equals(pUpperBound)) {
      return cast(pInfo, pLowerBound, pAllowSignedWrapAround, pOverflowEventHandler);

    }
    BigInteger lowerBound = pLowerBound;
    BigInteger upperBound = pUpperBound;

    boolean lbExceedsBelow = lowerBound.compareTo(pInfo.getMinValue()) < 0;
    boolean lbExceedsAbove = !lbExceedsBelow && lowerBound.compareTo(pInfo.getMaxValue()) > 0;
    boolean ubExceedsBelow = upperBound.compareTo(pInfo.getMinValue()) < 0;
    boolean ubExceedsAbove = !ubExceedsBelow && upperBound.compareTo(pInfo.getMaxValue()) > 0;

    // If the value fits in the range, there is no problem
    if (!(lbExceedsBelow || lbExceedsAbove || ubExceedsBelow || ubExceedsAbove)) {
      return BitVectorInterval.of(pInfo, pLowerBound, pUpperBound);
    }

    // From here on out, we know the interval does not fit

    // If the type is signed, wrap-around is implementation defined
    if (!pAllowSignedWrapAround && pInfo.isSigned()) {
      pOverflowEventHandler.signedOverflow();
      return pInfo.getRange();
    }

    BigInteger rangeLength = pInfo.getRange().size();
    assert rangeLength.compareTo(BigInteger.ZERO) >= 0;

    // If the value is larger than the full range, just return the full range
    if (upperBound.subtract(lowerBound).add(BigInteger.ONE).compareTo(rangeLength) >= 0) {
      return pInfo.getRange();
    }

    if (ubExceedsBelow) { // Full interval is below the minimum value
      lowerBound = pLowerBound.remainder(rangeLength);
      if (lowerBound.compareTo(pInfo.getMinValue()) < 0) {
        lowerBound = lowerBound.add(rangeLength);
      }
      upperBound = lowerBound.add(pUpperBound.subtract(pLowerBound));
      assert lowerBound.compareTo(pInfo.getMinValue()) >= 0;

      // If the interval still exceeds the range, there is nothing we can do here
      if (upperBound.compareTo(pInfo.getMaxValue()) > 0) {
        return pInfo.getRange();
      }
    } else if (lbExceedsAbove) { // Full interval is above the maximum value
      upperBound = pUpperBound.remainder(rangeLength);
      if (upperBound.compareTo(pInfo.getMaxValue()) > 0) {
        upperBound = upperBound.subtract(rangeLength);
      }
      lowerBound = upperBound.subtract(pUpperBound.subtract(pLowerBound));
      assert upperBound.compareTo(pInfo.getMaxValue()) <= 0;

      // If the interval still exceeds the range, there is nothing we can do here
      if (lowerBound.compareTo(pInfo.getMinValue()) < 0) {
        return pInfo.getRange();
      }
    } else if (lbExceedsBelow) { // Part of the interval is below the minimum value
      return pInfo.getRange();
    } else if (ubExceedsAbove) { // Part of the interval is above the minimum value
      return pInfo.getRange();
    }

    return BitVectorInterval.of(pInfo, lowerBound, upperBound);
  }

  /**
   * Returns an interval from this interval's lower bound to the maximum value
   * allowed by the bit vector size.
   *
   * @return an interval from this interval's lower bound to the maximum value
   * allowed by the bit vector size.
   */
  public BitVectorInterval extendToMaxValue() {
    if (upperBound.equals(info.getMaxValue())) {
      return this;
    }
    return new BitVectorInterval(info, lowerBound, info.getMaxValue());
  }

  /**
   * Returns an interval from this interval's lower bound to the minimum value
   * allowed by the bit vector size.
   *
   * @return an interval from this interval's lower bound to the minimum value
   * allowed by the bit vector size.
   */
  public BitVectorInterval extendToMinValue() {
    if (lowerBound.equals(info.getMinValue())) {
      return this;
    }
    return new BitVectorInterval(info, info.getMinValue(), upperBound);
  }

  @Override
  public boolean equals(Object pObj) {
    if (pObj == this) {
      return true;
    } else if (!(pObj instanceof BitVectorInterval)) {
      return false;
    }

    BitVectorInterval other = (BitVectorInterval) pObj;
    return Objects.equals(this.lowerBound, other.lowerBound)
        && Objects.equals(this.upperBound, other.upperBound);
  }

  @Override
  public int hashCode() {
    return Objects.hash(lowerBound, upperBound);
  }

  @Override
  public String toString() {
    return "[" + lowerBound + ", " + upperBound + "]";
  }

  /**
   * Checks if this interval contains the given interval.
   * @param pOther the interval that this interval is checked for containing.
   * @return <code>true</code> if this interval contains the given
   * interval, <code>false</code> otherwise.
   */
  public boolean contains(BitVectorInterval pOther) {
    if (this == pOther) {
      return true;
    }
    if (pOther == null) {
      return false;
    }

    return this.lowerBound.compareTo(pOther.lowerBound) <= 0
        && this.upperBound.compareTo(pOther.upperBound) >= 0;
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
  public boolean touches(BitVectorInterval pOther) {
    if (pOther == null) { return false; }
    if (intersectsWith(pOther)) { return true; }
    return pOther.upperBound.add(BigInteger.ONE).equals(this.lowerBound)
        || this.upperBound.add(BigInteger.ONE).equals(pOther.lowerBound);
  }

  /**
   * Checks if the given interval intersects with this interval.
   * @param other the interval to check for intersecting this interval.
   * @return <code>true</code> if this interval intersects with the
   * given interval, <code>false</code> otherwise.
   */
  public boolean intersectsWith(BitVectorInterval other) {
    if (this == other) { return true; }

    // this is [a, b]; other is [c, d]
    // result is true if a <= d and b >= c
    boolean aLessThanOrEqB = this.lowerBound.compareTo(other.upperBound) <= 0;
    boolean bGreaterThanOrEqC = this.upperBound.compareTo(other.lowerBound) >= 0;
    return aLessThanOrEqB && bGreaterThanOrEqC;
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
    if (getUpperBound().signum() < 0) { return getUpperBound(); }
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
    if (getLowerBound().signum() > 0) { return getLowerBound(); }
    return BigInteger.ONE;
  }

  public static BitVectorInterval singleton(BitVectorInfo pInfo, BigInteger pI) {
    return new BitVectorInterval(pInfo, pI, pI);
  }

  public static BitVectorInterval greaterOrEqual(BitVectorInfo pInfo, BigInteger pI) {
    return singleton(pInfo, pI).extendToMaxValue();
  }

  public static BitVectorInterval lessOrEqual(BitVectorInfo pInfo, BigInteger pI) {
    return singleton(pInfo, pI).extendToMinValue();
  }

  public static BitVectorInterval of(BitVectorInfo pInfo, BigInteger pLowerBound, BigInteger pUpperBound) {
    return new BitVectorInterval(pInfo, pLowerBound, pUpperBound);
  }

  /**
   * Create the smallest interval that contains two given intervals;
   */
  public static BitVectorInterval span(BitVectorInterval a, BitVectorInterval b) {
    a.checkBitVectorCompatibilityWith(b);
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
      return new BitVectorInterval(a.info, lower, upper);
    }
  }
}
