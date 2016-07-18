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
package org.sosy_lab.cpachecker.cpa.octagon.values;

import java.util.Arrays;
import java.util.Collections;

@SuppressWarnings("rawtypes")
public class OctagonInterval {

  /**
   * the lower bound of the OctInterval
   */
  private final OctagonNumericValue low;

  /**
   * the upper bound of the OctInterval
   */
  private final OctagonNumericValue high;

  /**
   * an OctInterval representing an impossible OctInterval
   */
  public static final OctagonInterval EMPTY = createEmptyOctInterval();
  public static final OctagonInterval FALSE = new OctagonInterval(OctagonIntValue.ZERO, OctagonIntValue.ZERO);
  public static final OctagonInterval DELTA = new OctagonInterval(-0.00001, 0.00001);

  /**
   * This method acts as constructor for an empty OctInterval.
   */
  private OctagonInterval() {
    this.low = null;
    this.high = null;
  }

  /**
   * This method acts as constructor for a single-value OctInterval.
   *
   * @param value for the lower and upper bound
   */
  public OctagonInterval(Long value) {
    this.low  = OctagonIntValue.of(value);
    this.high = OctagonIntValue.of(value);
  }

  /**
   * This method acts as constructor for a long-based OctInterval.
   *
   * @param low the lower bound
   * @param high the upper bound
   */
  public OctagonInterval(Long low, Long high) {
    this.low  = OctagonIntValue.of(low);
    this.high = OctagonIntValue.of(high);

    isSane();
  }

  public OctagonInterval(double low, double high) {
    this.low  = new OctagonDoubleValue(low);
    this.high = new OctagonDoubleValue(high);

    isSane();
  }

  public OctagonInterval(OctagonNumericValue low, OctagonNumericValue high) {
    this.low  = low;
    this.high = high;

    isSane();
  }

  public OctagonInterval(OctagonNumericValue pValue) {
    this.low = pValue;
    this.high = pValue;
  }

  private boolean isSane() {
    if (low.greaterThan(high)) {
      throw new IllegalStateException("low cannot be larger than high");
    }

    return true;
  }

  public boolean isInfinite() {
    boolean isInfinite = false;
    if (low instanceof OctagonDoubleValue) {
      isInfinite = ((OctagonDoubleValue)low).getValue().isInfinite();
    }
    if (!isInfinite && high instanceof OctagonDoubleValue) {
      isInfinite = ((OctagonDoubleValue)high).getValue().isInfinite();
    }
    return isInfinite;
  }

  /**
   * This method returns the lower bound of the OctInterval.
   *
   * @return the lower bound
   */
  public OctagonNumericValue getLow() {
    return low;
  }

  /**
   * This method returns the upper bound of the OctInterval.
   *
   * @return the upper bound
   */
  public OctagonNumericValue getHigh() {
    return high;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof OctagonInterval)) {
      return false;
    }

    OctagonInterval other = (OctagonInterval)obj;

    if (isEmpty() && other.isEmpty()) {
      return true;
    } else if (isEmpty() || other.isEmpty()) {
      return false;
    }

    return low.isEqual(other.low) && high.isEqual(other.high);
  }

  public boolean isSingular() {
    return low.isEqual(high);
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
   * This method creates a new OctInterval instance representing the union of this OctInterval with another OctInterval.
   *
   * The lower bound and upper bound of the new OctInterval is the minimum of both lower bounds and the maximum of both upper bounds, respectively.
   *
   * @param other the other OctInterval
   * @return the new OctInterval with the respective bounds
   */
  public OctagonInterval union(OctagonInterval other) {
    if (isEmpty() || other.isEmpty()) {
      return createEmptyOctInterval();
    } else {
      return new OctagonInterval(low.min(other.low), high.max(other.high));
    }
  }

  /**
   * This method creates a new OctInterval instance representing the intersection of this OctInterval with another OctInterval.
   *
   * The lower bound and upper bound of the new OctInterval is the maximum of both lower bounds and the minimum of both upper bounds, respectively.
   *
   * @param other the other OctInterval
   * @return the new OctInterval with the respective bounds
   */
  public OctagonInterval intersect(OctagonInterval other) {
    OctagonInterval OctInterval = null;

    if (this.intersects(other)) {
      OctInterval = new OctagonInterval(low.max(other.low), high.max(other.high));
    } else {
      OctInterval = createEmptyOctInterval();
    }

    return OctInterval;
  }

  /**
   * This method determines if this OctInterval is definitely less than the other OctInterval.
   *
   * @param other OctInterval to compare with
   * @return true if the upper bound of this OctInterval is always strictly lower than the lower bound of the other OctInterval, else false
   */
  public boolean isLessThan(OctagonInterval other) {
    return !isEmpty() && !other.isEmpty() && high.lessThan(other.low);
  }

  /**
   * This method determines if this OctInterval is definitely greater than the other OctInterval.
   *
   * @param other OctInterval to compare with
   * @return true if the lower bound of this OctInterval is always strictly greater than the upper bound of the other OctInterval, else false
   */
  public boolean isGreaterThan(OctagonInterval other) {
    return !isEmpty() && !other.isEmpty() && low.greaterThan(other.high);
  }

  /**
   * This method determines if this OctInterval maybe less than the other OctInterval.
   *
   * @param other OctInterval to compare with
   * @return true if the lower bound of this OctInterval is strictly lower than the upper bound of the other OctInterval, else false
   */
  public boolean mayBeLessThan(OctagonInterval other) {
    return isEmpty() || (!isEmpty() && !other.isEmpty() && low.lessThan(other.high));
  }

  /**
   * This method determines if this OctInterval maybe less or equal than the other OctInterval.
   *
   * @param other OctInterval to compare with
   * @return true if the lower bound of this OctInterval is strictly lower than the upper bound of the other OctInterval, else false
   */
  public boolean mayBeLessOrEqualThan(OctagonInterval other) {
    return isEmpty() || (!isEmpty() && !other.isEmpty() && low.lessEqual(other.high));
  }

  /**
   * This method determines if this OctInterval maybe greater than the other OctInterval.
   *
   * @param other OctInterval to compare with
   * @return true if the upper bound of this OctInterval is strictly greater than the lower bound of the other OctInterval, else false
   */
  public boolean mayBeGreaterThan(OctagonInterval other) {
    return other.isEmpty() || (!isEmpty() && !other.isEmpty() && high.greaterEqual(other.low));
  }

  /**
   * This method determines if this OctInterval maybe greater or equal than the other OctInterval.
   *
   * @param other OctInterval to compare with
   * @return true if the upper bound of this OctInterval is strictly greater than the lower bound of the other OctInterval, else false
   */
  public boolean mayBeGreaterOrEqualThan(OctagonInterval other) {
    return other.isEmpty() || (!isEmpty() && !other.isEmpty() && high.greaterEqual(other.low));
  }

  /**
   * This method determines if this OctInterval represents a true value.
   *
   * @return true if this OctInterval represents values that are strictly less than 0 or greater than 0.
   */
  public boolean isTrue() {
    return !isEmpty() && (high.lessThan(0) || low.greaterThan(0));
  }

  /**
   * This method creates a new OctInterval instance with the lower and upper bounds being the minimum of both the lower and upper bounds, respectively.
   *
   * @param other the other OctInterval
   * @return the new OctInterval with the respective bounds
   */
  public OctagonInterval minimum(OctagonInterval other) {
    OctagonInterval OctInterval = new OctagonInterval(low.min(other.low), high.min(other.high));

    return OctInterval;
  }

  /**
   * This method creates a new OctInterval instance with the lower and upper bounds being the maximum of both the lower and upper bounds, respectively.
   *
   * @param other the other OctInterval
   * @return the new OctInterval with the respective bounds
   */
  public OctagonInterval maximum(OctagonInterval other) {
    OctagonInterval OctInterval = new OctagonInterval(low.max(other.low), high.max(other.high));

    return OctInterval;
  }

  /**
   * This method returns a new OctInterval with a limited, i.e. higher, lower bound.
   *
   * @param other the OctInterval to limit this OctInterval
   * @return the new OctInterval with the upper bound of this OctInterval and the lower bound set to the maximum of this OctInterval's and the other OctInterval's lower bound or an empty OctInterval if this OctInterval is less than the other OctInterval.
   */
  public OctagonInterval limitLowerBoundBy(OctagonInterval other) {
    OctagonInterval OctInterval = null;

    if (isEmpty() || other.isEmpty() || high.lessEqual(other.low)) {
      OctInterval = createEmptyOctInterval();
    } else {
      OctInterval = new OctagonInterval(low.max(other.low), high);
    }

    return OctInterval;
  }

  /**
   * This method returns a new OctInterval with a limited, i.e. lower, upper bound.
   *
   * @param other the OctInterval to limit this OctInterval
   * @return the new OctInterval with the lower bound of this OctInterval and the upper bound set to the minimum of this OctInterval's and the other OctInterval's upper bound or an empty OctInterval if this OctInterval is greater than the other OctInterval.
   */
  public OctagonInterval limitUpperBoundBy(OctagonInterval other) {
    OctagonInterval OctInterval = null;

    if (isEmpty() || other.isEmpty() || low.greaterThan(other.high)) {
      OctInterval = createEmptyOctInterval();
    } else {
      OctInterval = new OctagonInterval(low, high.min(other.high));
    }

    return OctInterval;
  }

  /**
   * This method determines if this OctInterval intersects with another OctInterval.
   *
   * @param other the other OctInterval
   * @return true if the OctIntervals intersect, else false
   */
  public boolean intersects(OctagonInterval other) {
      if (isEmpty() || other.isEmpty()) {
        return false;
      }

      return (low.greaterEqual(other.low) && low.lessEqual(other.high))
        || (high.greaterEqual(other.low) && high.lessEqual(other.high))
        || (low.lessEqual(other.low) && high.greaterEqual(other.high));
  }

  /**
   * This method determines if this OctInterval contains another OctInterval.
   *
   * The method still returns true, if the borders match. An empty OctInterval does not contain any OctInterval and is not contained in any OctInterval either. So if the callee or parameter is an empty OctInterval, this method will return false.
   *
   * @param other the other OctInterval
   * @return true if this OctInterval contains the other OctInterval, else false
   */
  public boolean contains(OctagonInterval other) {
     return (!isEmpty() && !other.isEmpty()
               && low.lessEqual(other.low) && other.high.lessEqual(high));
  }

  /**
   * This method adds an OctInterval from this OctInterval, overflow is handled by setting the bound to Long.MIN_VALUE or Long.MAX_VALUE respectively.
   *
   * @param OctInterval the OctInterval to add
   * @return a new OctInterval with the respective bounds
   */
  public OctagonInterval plus(OctagonInterval OctInterval) {
    if (isEmpty() || OctInterval.isEmpty()) {
      return createEmptyOctInterval();
    }

    return new OctagonInterval(scalarPlus(low, OctInterval.low), scalarPlus(high, OctInterval.high));
  }

  /**
   * This method adds a constant offset to this OctInterval, overflow is handled by setting the bound to Long.MIN_VALUE or Long.MAX_VALUE respectively.
   *
   * @param offset the constant offset to add
   * @return a new OctInterval with the respective bounds
   */
  public OctagonInterval plus(Long offset) {
    return plus(new OctagonInterval(offset, offset));
  }

  /**
   * This method subtracts an OctInterval from this OctInterval, overflow is handled by setting the bound to Long.MIN_VALUE or Long.MAX_VALUE respectively.
   *
   * @param other OctInterval to subtract
   * @return a new OctInterval with the respective bounds
   */
  public OctagonInterval minus(OctagonInterval other) {
    return plus(other.negate());
  }

  /**
   * This method subtracts a constant offset to this OctInterval, overflow is handled by setting the bound to Long.MIN_VALUE or Long.MAX_VALUE respectively.
   *
   * @param offset the constant offset to subtract
   * @return a new OctInterval with the respective bounds
   */
  public OctagonInterval minus(Long offset) {
    return plus(-offset);
  }

  /**
   * This method multiplies this OctInterval with another OctInterval. In case of an overflow Long.MAX_VALUE and Long.MIN_VALUE are used instead.
   *
   * @param other OctInterval to multiply this OctInterval with
   * @return new OctInterval that represents the result of the multiplication of the two OctIntervals
   */
  @SuppressWarnings("unchecked")
  public OctagonInterval times(OctagonInterval other) {
    OctagonNumericValue[] values = {
                      scalarTimes(low, other.low),
                      scalarTimes(low, other.high),
                      scalarTimes(high, other.low),
                      scalarTimes(high, other.high)
                    };

    return new OctagonInterval(Collections.min(Arrays.asList(values)), Collections.max(Arrays.asList(values)));
  }

  /**
   * This method divides this OctInterval by another OctInterval. If the other OctInterval contains "0" an unbound OctInterval is returned.
   *
   * @param other OctInterval to divide this OctInterval by
   * @return new OctInterval that represents the result of the division of the two OctIntervals
   */
  @SuppressWarnings("unchecked")
  public OctagonInterval divide(OctagonInterval other) {
    // other OctInterval contains "0", return unbound OctInterval
    if (other.contains(FALSE)) {
      return createUnboundOctInterval();
    } else {
      OctagonNumericValue[] values = {
                        low.div(other.low),
                        low.div(other.high),
                        high.div(other.low),
                        high.div(other.high)
                      };

      return new OctagonInterval(Collections.min(Arrays.asList(values)), Collections.max(Arrays.asList(values)));
    }
  }

  /**
   * This method negates this OctInterval.
   *
   * @return new negated OctInterval
   */
  public OctagonInterval negate() {
    return new OctagonInterval(high.mul(OctagonIntValue.NEG_ONE), low.mul(OctagonIntValue.NEG_ONE));
  }

  /**
   * This method determines whether the OctInterval is empty or not.
   *
   * @return true, if the OctInterval is empty, i.e. the lower and upper bounds are null
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
   * This method is a factory method for an empty OctInterval
   *
   * @return an empty OctInterval
   */
  private static OctagonInterval createEmptyOctInterval() {
    return new OctagonInterval();
  }

  /**
   * This method is a factory method for an unbounded OctInterval
   *
   * @return an unbounded OctInterval, i.e. the lower and upper bound are set to Long.MIN_VALUE and Long.MAX_VALUE respectively
   */
  public static OctagonInterval createUnboundOctInterval() {
    return new OctagonInterval(Long.MIN_VALUE, Long.MAX_VALUE);
  }

  /**
   * This method is a factory method for an OctInterval representing the FALSE value.
   *
   * @return an OctInterval representing the FALSE value, i.e. the lower and upper bound are set to 0
   */
  public static OctagonInterval createFalseOctInterval() {
    return new OctagonInterval(0L);
  }

  /**
   * This method is a factory method for an OctInterval representing the TRUE value.
   *
   * @return an OctInterval representing the TRUE value, i.e. the lower and upper bound are set to 1
   */
  public static OctagonInterval createTrueOctInterval() {
    return new OctagonInterval(1L);
  }

  /**
   * This method adds two scalar values and returns their sum, or on overflow Long.MAX_VALUE or Long.MIN_VALUE, respectively.
   *
   * @param x the first scalar operand
   * @param y the second scalar operand
   * @return the sum of the first and second scalar operand or on overflow Long.MAX_VALUE and Long.MIN_VALUE, respectively.
   */
  private static OctagonNumericValue scalarPlus(OctagonNumericValue x, OctagonNumericValue y) {
    OctagonNumericValue result = x.add(y);

    // TODO overflows

    return result;
  }

  /**
   * This method multiplies two scalar values and returns their product, or on overflow Long.MAX_VALUE or Long.MIN_VALUE, respectively.
   *
   * @param x the first scalar operand
   * @param y the second scalar operand
   * @return the product of the first and second scalar operand or on overflow Long.MAX_VALUE and Long.MIN_VALUE, respectively.
   */
  private static OctagonNumericValue scalarTimes(OctagonNumericValue x, OctagonNumericValue y) {
    if (x.equals(OctagonIntValue.ONE)) {
      return y;
    } else if (y.equals(OctagonIntValue.ONE)) {
      return x;
    }

    Long bound = (x.signum() == y.signum()) ? Long.MAX_VALUE : Long.MIN_VALUE;

    // if overflow occurs, return the respective bound
    if (!x.isEqual(0)
        && ((y.greaterThan(0) && y.greaterThan(OctagonIntValue.ONE.div(x).mul(bound)))
            || (y.lessThan(0) && y.lessThan(OctagonIntValue.ONE.div(x).mul(bound))))) {
      return OctagonIntValue.of(bound);
    } else {
      return x.mul(y);
    }
  }
}

