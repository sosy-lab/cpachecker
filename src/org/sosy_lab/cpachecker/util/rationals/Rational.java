package org.sosy_lab.cpachecker.util.rationals;

import java.math.BigInteger;

/**
 * Rational class, throws exceptions on
 * unsupported operations (e.g. 1/0).
 */
public class Rational implements Comparable<Rational>{

  private final BigInteger num;

  // Invariant: denominator is always positive.
  private final BigInteger den;

  // -- Just some shortcuts for BigIntegers --
  static private final BigInteger b_zero = BigInteger.ZERO;
  static private final BigInteger b_one = BigInteger.ONE;
  static private final BigInteger b_m_one = b_one.negate();

  public static final Rational ZERO = new Rational(b_zero, b_one);
  public static final Rational ONE = new Rational(b_one, b_one);
  public static final Rational NEG_ONE = new Rational(b_m_one, b_one);

  /**
   * If the denominator and the numerator is zero, create NaN number.
   * If the denominator is zero and the numerator is positive, create INFTY
   * If the den is zero and the numerator is negative, create NEG_INFTY
   * Create a rational number otherwise.
   */
  public Rational(BigInteger numerator, BigInteger denominator) {
    if (denominator.equals(b_zero)) {
      throw new UnsupportedOperationException(
          "Infinity is not supported, use ExtendedRational instead");
    }

    // Make {@code denominator} positive.
    if (denominator.signum() == -1) {
      denominator = denominator.negate();
      numerator = numerator.negate();
    }

    // Reduce by GCD.
    BigInteger gcd = numerator.gcd(denominator);
    num = numerator.divide(gcd);
    den = denominator.divide(gcd);

  }

  public static Rational ofLongs(long numerator, long denominator) {
    return new Rational(
        BigInteger.valueOf(numerator), BigInteger.valueOf(denominator));
  }

  public static Rational ofLong(long numerator) {
    if (numerator == 1) {
      return Rational.ONE;
    } else if (numerator == 0) {
      return Rational.ZERO;
    }
    return new Rational(BigInteger.valueOf(numerator), b_one);
  }

  public static Rational ofInt(int numerator) {
    return ofLong(numerator);
  }

  public static Rational ofBigInteger(BigInteger numerator) {
    if (numerator.equals(BigInteger.ONE)) {
      return Rational.ONE;
    } else if (numerator.equals(BigInteger.ZERO)) {
      return Rational.ZERO;
    }
    return new Rational(numerator, b_one);
  }

  public static Rational ofBigIntegers(BigInteger numerator, BigInteger denominator) {
    return new Rational(numerator, denominator);
  }

  /**
   * @return rational converted to double.
   */
  public double toDouble() {
    return num.doubleValue() / den.doubleValue();
  }

  /**
   * @return  String of the form num/den.
   */
  @Override
  public String toString() {
      if (den.equals(BigInteger.ONE)) {
        return num.toString();
      }
      return num + "/" + den;
  }

  /**
   * Reverses the effect of {@link Rational#toString}.
   * Supports 2 different formats.
   *
   * d) a/b -> Rational(a, b)
   * e) a -> a/1
   *
   * @param s Input string,
   * @throws NumberFormatException {@code s} is not a valid representation
   * of Rational.
   * @return New {@link Rational}.
   */
  public static Rational ofString(String s) {
    int idx = s.indexOf('/');
    BigInteger num, den;
    if (idx == -1) { // No slash found.
      num = new BigInteger(s);
      return ofBigInteger(num);
    } else {
      num = new BigInteger(s.substring(0, idx));
      den = new BigInteger(s.substring(idx + 1, s.length()));
      return ofBigIntegers(num, den);
    }
  }

  @Override
  public int compareTo(Rational b) {
      Rational a = this;
      BigInteger lhs = a.num.multiply(b.den);
      BigInteger rhs = a.den.multiply(b.num);
      return lhs.subtract(rhs).signum();
  }

  @Override
  public boolean equals(Object y) {
    if (this == y) return true;
    if (y == null) return false;
    if (y.getClass() != this.getClass()) return false;
    Rational b = (Rational) y;
    return compareTo(b) == 0;
  }

  @Override
  public int hashCode() {
    return this.toString().hashCode();
  }

  /**
   * No modifications are necessary to support extra types as no division is performed.
   */
  public Rational times(Rational b) {
    Rational a = this;

    // reduce p1/q2 and p2/q1, then multiply, where a = p1/q1 and b = p2/q2
    Rational c = new Rational(a.num, b.den);
    Rational d = new Rational(b.num, a.den);
    return new Rational(c.num.multiply(d.num), c.den.multiply(d.den));
  }

  public Rational plus(Rational b) {
    Rational a = this;

    // special cases
    if (a.compareTo(ZERO) == 0) {
      return b;
    }
    if (b.compareTo(ZERO) == 0) {
      return a;
    }

    // Find gcd of numerators and denominators
    BigInteger f = a.num.gcd(b.num);
    BigInteger g = a.den.gcd(b.den);

    BigInteger num = ((
        a.num.divide(f)).multiply(b.den.divide(g)
    ).add(
        b.num.divide(f).multiply(a.den.divide(g))
    )).multiply(f);
    BigInteger den = lcm(a.den, b.den);
    return new Rational(num, den);
  }

  public Rational minus(Rational b) {
    Rational a = this;
    return a.plus(b.negate());
  }

  public Rational divides(Rational b) {
    Rational a = this;
    if (b.equals(Rational.ZERO)) {
      throw new UnsupportedOperationException(
          "Infinity is not supported, use ExtendedRational");
    }
    return a.times(b.reciprocal());
  }

  public Rational reciprocal() { return new Rational(den, num);  }

  public Rational negate() {
    return new Rational(num.negate(), den);
  }

  private static BigInteger lcm(BigInteger m, BigInteger n) {
    m = m.abs();
    n = n.abs();
    return m.multiply(n.divide(m.gcd(n)));
  }
}
