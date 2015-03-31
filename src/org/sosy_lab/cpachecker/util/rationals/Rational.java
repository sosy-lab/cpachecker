package org.sosy_lab.cpachecker.util.rationals;

import java.math.BigInteger;

import com.google.common.base.Objects;

/**
 * Rational class, throws exceptions on
 * unsupported operations (e.g. 1/0).
 */
@SuppressWarnings("NumberEquality")
public class Rational extends Number implements Comparable<Rational> {

  // -- Just some shortcuts for BigIntegers --
  static private final BigInteger b_zero = BigInteger.ZERO;
  static private final BigInteger b_one = BigInteger.ONE;
  static private final BigInteger b_m_one = b_one.negate();

  public static final Rational ZERO = new Rational(b_zero, b_one);
  public static final Rational ONE = new Rational(b_one, b_one);
  public static final Rational NEG_ONE = new Rational(b_m_one, b_one);

  /**
   * Rationals are always stored in the normal form.
   * That is:
   *
   * a) denominator is strictly positive.
   * b) numerator and denominator do not have common factors.
   * c) 0, 1 and -1 have unique representation corresponding to the
   * class static constants ZERO, ONE and NEG_ONE. That is, they can be
   * compared using the '==' operator.
   */
  private final BigInteger num;
  private final BigInteger den;

  private Rational(BigInteger numerator, BigInteger denominator) {
    num = numerator;
    den = denominator;
  }

  /** Factory functions **/

  /**
   * Create a new rational.
   *
   * Function responsible for maintaining the internal invariants.
   */
  public static Rational of(BigInteger numerator, BigInteger denominator) {
    int denSignum = denominator.signum();
    if (denSignum == 0) {
      throw new IllegalArgumentException(
          "Infinity is not supported, use ExtendedRational instead");
    }

    if (denSignum == -1) {
      // Make {@code denominator} positive.
      denominator = denominator.negate();
      numerator = numerator.negate();
    }

    // Reduce by GCD. GCD will never be zero as the denominator is never
    // zero at this stage.
    BigInteger gcd = numerator.gcd(denominator);
    numerator = numerator.divide(gcd);
    denominator = denominator.divide(gcd);

    return ofNormalForm(numerator, denominator);
  }

  public static Rational ofLongs(long numerator, long denominator) {
    return of(
        BigInteger.valueOf(numerator), BigInteger.valueOf(denominator));
  }

  public static Rational ofLong(long numerator) {
    return of(BigInteger.valueOf(numerator), b_one);
  }

  public static Rational ofBigInteger(BigInteger numerator) {
    return of(numerator, b_one);
  }

  /**
   * Reverses the effect of {@link Rational#toString}.
   * Supports 2 different formats: with slash (e.g. "25/17")
   * or without slash (e.g. "5")
   *
   * @param s Input string,
   * @throws NumberFormatException {@code s} is not a valid representation
   * of Rational.
   * @return New {@link Rational}.
   */
  public static Rational ofString(String s) throws NumberFormatException {
    int idx = s.indexOf('/');
    BigInteger num, den;
    if (idx == -1) { // No slash found.
      num = new BigInteger(s);
      return ofBigInteger(num);
    } else {
      num = new BigInteger(s.substring(0, idx));
      den = new BigInteger(s.substring(idx + 1, s.length()));
      return of(num, den);
    }
  }

  /**
   * Wrapper around the constructor, returns cached constants if possible.
   * Assumes that <code>num</code> and <code>den</code> are in the normal form.
   */
  private static Rational ofNormalForm(BigInteger num, BigInteger den) {
    if (num.equals(b_zero)) {
      return ZERO;
    } else if (den.equals(b_one)) {
      if (num.equals(b_one)) {
        return ONE;
      } else if (num.equals(b_m_one)) {
        return NEG_ONE;
      }
    }
    return new Rational(num, den);
  }

  /** Arithmetic operations. **/

  public Rational times(Rational b) {
    Rational a = this;
    if (a == ZERO || b == ZERO) {
      return ZERO;
    }
    if (a == ONE) {
      return b;
    }
    if (b == ONE) {
      return a;
    }

    // reduce p1/q2 and p2/q1, then multiply, where a = p1/q1 and b = p2/q2
    Rational c = of(a.num, b.den);
    Rational d = of(b.num, a.den);
    return ofNormalForm(c.num.multiply(d.num), c.den.multiply(d.den));
  }

  public Rational plus(Rational b) {
    Rational a = this;
    if (a == ZERO) {
      return b;
    }
    if (b == ZERO) {
      return a;
    }

    return of((a.num.multiply(b.den).add(b.num.multiply(a.den))),
        a.den.multiply(b.den));
  }

  public Rational minus(Rational b) {
    return plus(b.negate());
  }

  public Rational divides(Rational b) {
    // Reciprocal method will throw the exception for the division-by-zero
    // error if required.
    return times(b.reciprocal());
  }

  public Rational reciprocal() throws IllegalArgumentException {
    if (num.equals(b_zero)) {
      throw new IllegalArgumentException(
          "Division by zero not supported, use ExtendedRational if you need it");
    }
    return ofNormalForm(den, num);
  }

  public Rational negate() {
    return ofNormalForm(num.negate(), den);
  }

  /**
   * @return rational converted to double.
   */
  @Override
  public double doubleValue() {
    return num.doubleValue() / den.doubleValue();
  }

  public boolean isIntegral() {
    return den.equals(b_one);
  }

  public BigInteger getNum() {
    return num;
  }

  /**
   * @return -1, 0 or 1, representing the sign of the rational number.
   */
  public int signum() {
    return num.signum();
  }

  /**
   * @return  String of the form num/den.
   */
  @Override
  public String toString() {
    if (den.equals(b_one)) {
      return num.toString();
    }
    return num + "/" + den;
  }

  @Override
  public int compareTo(Rational b) {
    BigInteger lhs = num.multiply(b.den);
    BigInteger rhs = den.multiply(b.num);
    return lhs.subtract(rhs).signum();
  }

  @Override
  public boolean equals(Object y) {
    if (this == y) {
      return true;
    }
    if (y == null) {
      return false;
    }
    if (y.getClass() != this.getClass()) {
      return false;
    }
    Rational b = (Rational) y;
    return (num.equals(b.num) && den.equals(b.den));
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(num, den);
  }

  public static Rational max(Rational a, Rational b) {
    if (a.compareTo(b) >= 0) {
      return a;
    }
    return b;
  }

  @Override
  public int intValue() {
    return (int)doubleValue();
  }

  @Override
  public long longValue() {
    return (long)doubleValue();
  }

  @Override
  public float floatValue() {
    return (float)doubleValue();
  }
}
