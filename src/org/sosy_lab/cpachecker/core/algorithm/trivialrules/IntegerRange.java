// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.trivialrules;

import static com.google.common.base.Preconditions.checkArgument;

import java.math.BigInteger;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * An interval that over-approximates the values of an expression.
 *
 * <p>All operations compute with arbitrary precision and never wrap around, i.e., they return the
 * mathematical result. This is exactly what a rule about overflows needs: whether an operation
 * leaves the range of its type is decided by comparing the mathematical result with that range.
 */
record IntegerRange(BigInteger low, BigInteger high) {

  IntegerRange {
    checkArgument(low.compareTo(high) <= 0, "empty range [%s, %s]", low, high);
  }

  static IntegerRange of(BigInteger pValue) {
    return new IntegerRange(pValue, pValue);
  }

  static IntegerRange of(long pLow, long pHigh) {
    return new IntegerRange(BigInteger.valueOf(pLow), BigInteger.valueOf(pHigh));
  }

  /** The value of this range if it has only one, and {@code null} otherwise. */
  @Nullable BigInteger exactValue() {
    return low.equals(high) ? low : null;
  }

  boolean contains(BigInteger pValue) {
    return low.compareTo(pValue) <= 0 && high.compareTo(pValue) >= 0;
  }

  boolean isWithin(IntegerRange pOther) {
    return pOther.low.compareTo(low) <= 0 && pOther.high.compareTo(high) >= 0;
  }

  boolean isNonNegative() {
    return low.signum() >= 0;
  }

  /** The smallest range that contains this range and the given one. */
  IntegerRange span(IntegerRange pOther) {
    return new IntegerRange(low.min(pOther.low), high.max(pOther.high));
  }

  IntegerRange negate() {
    return new IntegerRange(high.negate(), low.negate());
  }

  IntegerRange plus(IntegerRange pOther) {
    return new IntegerRange(low.add(pOther.low), high.add(pOther.high));
  }

  IntegerRange minus(IntegerRange pOther) {
    return new IntegerRange(low.subtract(pOther.high), high.subtract(pOther.low));
  }

  IntegerRange times(IntegerRange pOther) {
    // The extreme values of a product are among the products of the extreme values.
    BigInteger a = low.multiply(pOther.low);
    BigInteger b = low.multiply(pOther.high);
    BigInteger c = high.multiply(pOther.low);
    BigInteger d = high.multiply(pOther.high);
    return new IntegerRange(a.min(b).min(c).min(d), a.max(b).max(c).max(d));
  }

  @Override
  public String toString() {
    return "[" + low + ", " + high + "]";
  }
}
