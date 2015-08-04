package org.sosy_lab.cpachecker.util.rationals;

import com.google.common.base.Preconditions;

/**
 * This class represents "extended rational": rationals which allow for infinities,
 * negative infinities and undefined numbers.
 *
 * Any operation on the numbers is guaranteed to never yield an exception.
 *
 * Represented as wrapper around {@link Rational} class.
 */
public class ExtendedRational implements Comparable<ExtendedRational>{
  @SuppressWarnings("hiding")
  public static enum NumberType {
    NEG_INFTY,
    RATIONAL, // Normal rational.
    INFTY,
    NaN, // Infinity + negative infinity etc.
    // Like java's Double, UNDEFINED is bigger than everything (when sorting).
  }

  private final NumberType numberType;
  private final Rational rational;

  public static final ExtendedRational INFTY = new ExtendedRational(NumberType.INFTY);
  public static final ExtendedRational NEG_INFTY = new ExtendedRational(NumberType.NEG_INFTY);
  public static final ExtendedRational NaN = new ExtendedRational(NumberType.NaN);

  public ExtendedRational(Rational pRational) {
    numberType = NumberType.RATIONAL;
    rational = pRational;
  }

  public boolean isRational() {
    return rational != null;
  }

  /**
   * If the represented number is rational, return the wrapped object.
   * Otherwise, throw exception.
   *
   * @throws java.lang.UnsupportedOperationException
   */
  public Rational getRational() {
    if (rational != null) {
      return rational;
    }
    throw new UnsupportedOperationException("Represented number is not rational");
  }

  private ExtendedRational(NumberType pType) {
    Preconditions.checkState(pType != NumberType.RATIONAL);
    numberType = pType;
    rational = null;
  }

  /**
   * @return rational converted to double.
   * The method works, because the Java Double class also supports
   * Infinity/-Infinity/NaN.
   */
  public double toDouble() {
    switch (numberType) {
      case NEG_INFTY:
        return Double.NEGATIVE_INFINITY;
      case RATIONAL:
        return rational.toDouble();
      case INFTY:
        return Double.POSITIVE_INFINITY;
      case NaN:
        return Double.NaN;
      default:
        throw new UnsupportedOperationException("Unexpected number type");
    }
  }

  /**
   * @return The method can return TWO things.
   * a) String of the form num/den if the number is rational.
   * b) String representation of infinity/etc, consistent with the
   * {@code Double} class.
   */
  @Override
  public String toString() {
    switch (numberType) {
      case RATIONAL:
        return rational.toString();
      default:
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
    switch (s) {
      case "Infinity":
        return ExtendedRational.INFTY;
      case "-Infinity":
        return ExtendedRational.NEG_INFTY;
      case "NaN":
        return ExtendedRational.NaN;
      default:
        return new ExtendedRational(Rational.ofString(s));
    }
  }

  @Override
  public int compareTo(ExtendedRational b) {
    NumberType us = numberType;
    NumberType them = b.numberType;
    if (us == them) {
      if (us == NumberType.RATIONAL) {
        return this.rational.compareTo(b.rational);
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
    if (this == y) return true;
    if (y == null) return false;
    if (y.getClass() != this.getClass()) return false;
    ExtendedRational b = (ExtendedRational) y;
    return compareTo(b) == 0;
  }

  @Override
  public int hashCode() {
    return toString().hashCode();
  }

  /**
   * No modifications are necessary to support extra types as no division is performed.
   */
  public ExtendedRational times(ExtendedRational b) {
    if (this == NaN || b == NaN) {
      return NaN;
    } else if (this == NEG_INFTY ^ b == NEG_INFTY) {
      return NEG_INFTY;
    } else if (this == INFTY || b == INFTY
        || (this == NEG_INFTY && b == NEG_INFTY)) {
      return INFTY;
    }
    return new ExtendedRational(rational.times(b.rational));
  }

  public ExtendedRational plus(ExtendedRational b) {
    if (this == NaN || b == NaN) {
      return NaN;
    } else if (this == NEG_INFTY && b == INFTY
        || this == INFTY && b == NEG_INFTY) {
      return NaN;
    } else if (this == INFTY || b == INFTY) {
      return INFTY;
    } else if (this == NEG_INFTY || b == NEG_INFTY) {
      return NEG_INFTY;
    }

    return new ExtendedRational(rational.plus(b.rational));
  }

  public ExtendedRational minus(ExtendedRational b) {
    ExtendedRational a = this;
    return a.plus(b.negate());
  }

  public ExtendedRational divides(ExtendedRational b) {
    ExtendedRational a = this;
    return a.times(b.reciprocal());
  }

  public ExtendedRational reciprocal() {
    if (this == NaN) {
      return NaN;
    } else if (this == INFTY || this == NEG_INFTY) {
      return new ExtendedRational(Rational.ZERO);
    }
    return new ExtendedRational(rational.reciprocal());
  }

  public ExtendedRational negate() {
    if (this == NaN) {
      return NaN;
    } else if (this == INFTY) {
      return NEG_INFTY;
    } else if (this == NEG_INFTY) {
      return INFTY;
    }
    return new ExtendedRational(rational.negate());
  }
}
