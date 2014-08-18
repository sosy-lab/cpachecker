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
package org.sosy_lab.cpachecker.util.rationals;

import java.math.BigInteger;

/**
 * This class represents "extended rational": rationals which allow for infinities,
 * negative infinities and undefined numbers.
 * Represented using big integers.
 */

public class ExtendedRational implements Comparable<ExtendedRational>{
  /**
   * For ExtendedRational, NumberType is encoded in a (num, den) pair.
   * (0, 0) is UNDEFINED, (+n, 0) is INFTY, (-n, 0) is NEG_INFTY, everything
   * else is RATIONAL.
   */
  @SuppressWarnings("hiding")
  public static enum NumberType {
    NEG_INFTY,
    RATIONAL, // Normal rational.
    INFTY,
    NaN, // Infinity + negative infinity etc.
    // Like java's Double, UNDEFINED is bigger than everything (when sorting).
  }

  private final BigInteger num;
  private final BigInteger den;

  // -- Just some shortcuts for BigIntegers --
  static private final BigInteger b_zero = BigInteger.ZERO;
  static private final BigInteger b_one = BigInteger.ONE;
  static private final BigInteger b_m_one = BigInteger.ONE.negate();

  public static final ExtendedRational ZERO = new ExtendedRational(b_zero, b_one);
  public static final ExtendedRational ONE = new ExtendedRational(b_one, b_one);
  public static final ExtendedRational INFTY = new ExtendedRational(b_one, b_zero);
  public static final ExtendedRational NEG_INFTY = new ExtendedRational(b_m_one, b_zero);
  public static final ExtendedRational NaN = new ExtendedRational(b_zero, b_zero);

  /**
   * If the denominator and the numerator is zero, create NaN number.
   * If the denominator is zero and the numerator is positive, create INFTY
   * If the den is zero and the numerator is negative, create NEG_INFTY
   * Create a rational number otherwise.
   */
  public ExtendedRational(BigInteger numerator, BigInteger denominator) {
    if (!isZero(denominator)) {
      // Make {@code denominator} positive.
      if (isNeg(denominator)) {
        denominator = denominator.negate();
        numerator = numerator.negate();
      }

      // Reduce by GCD.
      BigInteger gcd = numerator.gcd(denominator);
      num = numerator.divide(gcd);
      den = denominator.divide(gcd);

    } else {
      num = BigInteger.valueOf(numerator.signum());
      den = denominator;
    }
  }

  public static ExtendedRational ofLongs(long numerator, long denominator) {
    return new ExtendedRational(
        BigInteger.valueOf(numerator), BigInteger.valueOf(denominator));
  }

  public static ExtendedRational ofLong(long numerator) {
    return new ExtendedRational(BigInteger.valueOf(numerator), b_one);
  }

  public static ExtendedRational ofInt(int numerator) {
    return new ExtendedRational(BigInteger.valueOf(numerator), b_one);
  }

  public NumberType getType() {
    if (isZero(num) && isZero(den)) {
      return NumberType.NaN;
    } else if (isZero(den) && isPos(num)) {
      return NumberType.INFTY;
    } else if (isZero(den) && isNeg(num)) {
      return NumberType.NEG_INFTY;
    } else {
      return NumberType.RATIONAL;
    }
  }

  /**
   * @return rational converted to double.
   * The method works, because the Java Double class also supports
   * Infinity/-Infinity/NaN.
   */
  public double toDouble() {
    return num.doubleValue() / den.doubleValue();
  }

  /**
   * @return The method can return TWO things.
   * a) String of the form num/den if the number is rational.
   * b) String containing INF | NEG_INF | NaN otherwise.
   */
  @Override
  public String toString() {
    switch (getType()) {
      case RATIONAL:
        return num + "/" + den;
      default:
        // Double will do the conversion for us, works just fine for infinity/etc.
        return Double.toString(toDouble());
    }
  }

  /**
   * Reverses the effect of {@link ExtendedRational#toString}.
   * Supports 4 different formats, to be consistent with the {@link Double} class.
   *
   * a) Infinity -> 1/0
   * b) -Infinity -> -1/0
   * c) NaN -> 0/0
   * d) a/b -> ExtendedRational(a, b)
   * e) a -> a/1
   *
   * @param s Input string,
   * @throws NumberFormatException {@code s} is not a valid representation
   * of ExtendedRational.
   * @return New {@link ExtendedRational}.
   */
  public static ExtendedRational ofString(String s) {
    ExtendedRational ret;
    switch (s) {
      case "Infinity":
        ret = ExtendedRational.ofLongs(1, 0);
        break;
      case "-Infinity":
        ret = ExtendedRational.ofLongs(-1, 0);
        break;
      case "NaN":
        ret = ExtendedRational.ofLongs(0, 0);
        break;
      default:
        int idx = s.indexOf('/');
        BigInteger num, den;
        if (idx == -1) { // No slash found.
          num = new BigInteger(s);
          den = b_one;
        } else {
          num = new BigInteger(s.substring(0, idx));
          den = new BigInteger(s.substring(idx + 1, s.length()));
        }
        ret = new ExtendedRational(num, den);
        break;
    }
    return ret;
  }

  @Override
  public int compareTo(ExtendedRational b) {
    NumberType us = getType();
    NumberType them = b.getType();
    if (us == them) {
      if (us == NumberType.RATIONAL) {
        ExtendedRational a = this;
        BigInteger lhs = a.num.multiply(b.den);
        BigInteger rhs = a.den.multiply(b.num);
        return lhs.subtract(rhs).signum();
      } else {
        return 0;
      }
    } else {

      // Take the ordering provided by the enum.
      return us.ordinal() - them.ordinal();
    }
  }

  @Override
  public boolean equals(Object y) {
    if (y == null) return false;
    if (y.getClass() != this.getClass()) return false;
    ExtendedRational b = (ExtendedRational) y;
    return compareTo(b) == 0;
  }

  @Override
  public int hashCode() {
    return this.toString().hashCode();
  }

  /**
   * No modifications are necessary to support extra types as no division is performed.
   */
  public ExtendedRational times(ExtendedRational b) {
    ExtendedRational a = this;

    // reduce p1/q2 and p2/q1, then multiply, where a = p1/q1 and b = p2/q2
    ExtendedRational c = new ExtendedRational(a.num, b.den);
    ExtendedRational d = new ExtendedRational(b.num, a.den);
    return new ExtendedRational(c.num.multiply(d.num), c.den.multiply(d.den));
  }

  public ExtendedRational plus(ExtendedRational b) {
    ExtendedRational a = this;
    NumberType typeA = a.getType();
    NumberType typeB = b.getType();

    if (typeA == NumberType.NaN || typeB == NumberType.NaN) {
      return ExtendedRational.NaN;
    }

    if (typeA == typeB) {
      if (typeA == NumberType.RATIONAL) {
        // special cases
        if (a.compareTo(ZERO) == 0) return b;
        if (b.compareTo(ZERO) == 0) return a;

        // Find gcd of numerators and denominators
        BigInteger f = a.num.gcd(b.num);
        BigInteger g = a.den.gcd(b.den);

        BigInteger num = ((
              a.num.divide(f)).multiply(b.den.divide(g)
          ).add(
              b.num.divide(f).multiply(a.den.divide(g))
          )).multiply(f);
        BigInteger den = lcm(a.den, b.den);
        return new ExtendedRational(num, den);
      } else {
        return a;
      }
    } else {
      if (typeA == NumberType.INFTY && typeB == NumberType.NEG_INFTY
          || typeB == NumberType.INFTY && typeA == NumberType.NEG_INFTY) {
        return ExtendedRational.NaN;
      } else if (typeA == NumberType.RATIONAL) {
        return b;
      } else if (typeB == NumberType.RATIONAL) {
        return a;
      } else {
        throw new RuntimeException("Unaccounted branch;" +
            "error in the ExtendedRationals library.");
      }
    }
  }

  public ExtendedRational minus(ExtendedRational b) {
    ExtendedRational a = this;
    return a.plus(b.negate());
  }

  public ExtendedRational divides(ExtendedRational b) {
    ExtendedRational a = this;
    return a.times(b.reciprocal());
  }

  public ExtendedRational reciprocal() { return new ExtendedRational(den, num);  }

  public ExtendedRational negate() {
    return new ExtendedRational(num.negate(), den);
  }

  private static BigInteger lcm(BigInteger m, BigInteger n) {
    m = m.abs();
    n = n.abs();
    return m.multiply(n.divide(m.gcd(n)));
  }

  // Helper functions for dealing with big integers.
  private static boolean isZero(BigInteger x) {
    return x.equals(b_zero);
  }

  private static boolean isNeg(BigInteger x) {
    return x.compareTo(b_zero) < 0;
  }

  private static boolean isPos(BigInteger x) {
    return x.compareTo(b_zero) > 0;
  }
}
