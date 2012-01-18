/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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

import static com.google.common.base.Preconditions.*;

import java.math.BigInteger;

import com.google.common.base.Objects;
import com.google.common.base.Predicate;

/**
 * This class represents simple convex ranges of BigIntegers.
 * It has an lower bound and an upper bound, both of which may either be a
 * concrete value or infinity. In case of a concrete value, the bound is assumed
 * to be included in the range.
 *
 * All instances of this class are immutable.
 */
class SimpleInterval {

  private final BigInteger lowerBound; // null means negative infinity
  private final BigInteger upperBound; // null means positive infinity

  private SimpleInterval(BigInteger pLowerBound, BigInteger pUpperBound) {
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

  public boolean containsPositive() {
    return (upperBound == null || upperBound.signum() == 1);
  }

  public boolean containsZero() {
    return (upperBound == null || upperBound.signum() >= 0)
        && (lowerBound == null || lowerBound.signum() <= 0);
  }

  public boolean containsNegative() {
    return (lowerBound == null || upperBound.signum() == -1);
  }

  public BigInteger size() {
    if (hasLowerBound() && hasUpperBound()) {
      return upperBound.subtract(lowerBound).add(BigInteger.ONE);
    } else {
      return null;
    }
  }

  public boolean isSingleton() {
    return hasLowerBound() && lowerBound.equals(upperBound);
  }

  public SimpleInterval negate() {
    BigInteger newUpperBound = (lowerBound == null ? null : lowerBound.negate());
    BigInteger newLowerBound = (upperBound == null ? null : upperBound.negate());

    if (newLowerBound == null && newUpperBound == null) {
      return infinite();
    } else {
      return new SimpleInterval(newLowerBound, newUpperBound);
    }
  }

  public SimpleInterval extendToPositiveInfinity() {
    if (lowerBound == null) {
      return infinite();
    } else if (upperBound == null) {
      return this;
    } else {
      return new SimpleInterval(lowerBound, null);
    }
  }

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

    SimpleInterval other = (SimpleInterval)pObj;
    return Objects.equal(this.lowerBound, other.lowerBound)
        && Objects.equal(this.upperBound, other.upperBound);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(lowerBound, upperBound);
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

  public boolean intersectsWith(SimpleInterval other) {
    if (this == other) {
      return true;
    }

    if (this.lowerBound == null) {
      if (this.upperBound == null || other.lowerBound == null)  {
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
        // result is true if a <= d or b >= c
        return this.lowerBound.compareTo(other.upperBound) <= 0
            || this.upperBound.compareTo(other.lowerBound) >= 0;
      }
    }
  }

  static Predicate<SimpleInterval> HAS_BOUNDS = new Predicate<SimpleInterval>() {
    @Override
    public boolean apply(SimpleInterval pArg0) {
      return pArg0.hasLowerBound() || pArg0.hasUpperBound();
    }
  };

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
