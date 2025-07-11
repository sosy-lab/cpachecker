// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.floatingpoint;

import java.util.Objects;
import java.util.Optional;
import org.kframework.mpfr.BigFloat;
import org.kframework.mpfr.BinaryMathContext;
import org.sosy_lab.cpachecker.util.floatingpoint.CFloatNativeAPI.CFloatType;
import org.sosy_lab.cpachecker.util.floatingpoint.CFloatNativeAPI.CIntegerType;

/**
 * Java based implementation of the {@link CFloat} interface that uses regular Java doubles.
 *
 * <p>For single precision use the sister class {@link JFloat}.
 */
class JDouble extends CFloat {
  private final CFloatWrapper wrapper;
  private final double value;

  public JDouble(double pValue) {
    value = pValue;
    wrapper = fromDouble(value);
  }

  public JDouble(String repr) {
    value = parseDouble(repr);
    wrapper = fromDouble(value);
  }

  private double toDouble(CFloatWrapper pWrapper) {
    long exponent = pWrapper.getExponent();
    long mantissa = pWrapper.getMantissa();
    return Double.longBitsToDouble((exponent << 52) + mantissa);
  }

  private CFloatWrapper fromDouble(double pValue) {
    long bits = Double.doubleToLongBits(pValue);
    long exponent = ((bits & 0xFFF0000000000000L) >> 52) & 0xFFF;
    long mantissa = bits & 0xFFFFFFFFFFFFFL;
    return new CFloatWrapper(exponent, mantissa);
  }

  private double parseDouble(String repr) {
    if ("-nan".equals(repr)) {
      return Double.longBitsToDouble(0xFFF8000000000000L);
    } else if ("nan".equals(repr)) {
      return Double.NaN;
    } else if ("-inf".equals(repr)) {
      return Double.NEGATIVE_INFINITY;
    } else if ("inf".equals(repr)) {
      return Double.POSITIVE_INFINITY;
    } else {
      return Double.parseDouble(repr);
    }
  }

  @Override
  public String toString() {
    if (isNan()) {
      return isNegative() ? "-nan" : "nan";
    } else if (isInfinity()) {
      return isNegative() ? "-inf" : "inf";
    }
    // FIXME: Find a solution that doesn't require BigFloat
    // Both of these return different results from MPFR:
    // String repr = String.format("%.17e", BigDecimal.valueOf(value));
    // String repr = Double.toString(value);
    BigFloat v = new BigFloat(value, BinaryMathContext.BINARY64);
    return v.toString().replaceAll(",", ".");
  }

  @Override
  public CFloat add(CFloat pSummand) {
    return new JDouble(value + toDouble(pSummand.getWrapper()));
  }

  @Override
  public CFloat multiply(CFloat pFactor) {
    return new JDouble(value * toDouble(pFactor.getWrapper()));
  }

  @Override
  public CFloat subtract(CFloat pSubtrahend) {
    return new JDouble(value - toDouble(pSubtrahend.getWrapper()));
  }

  @Override
  public CFloat divideBy(CFloat pDivisor) {
    return new JDouble(value / toDouble(pDivisor.getWrapper()));
  }

  @Override
  public CFloat modulo(CFloat pDivisor) {
    return new JDouble(value % toDouble(pDivisor.getWrapper()));
  }

  @Override
  public CFloat remainder(CFloat pDivisor) {
    return new JDouble(Math.IEEEremainder(value, toDouble(pDivisor.getWrapper())));
  }

  @Override
  public CFloat ln() {
    return new JDouble(Math.log(value));
  }

  @Override
  public CFloat exp() {
    return new JDouble(Math.exp(value));
  }

  @Override
  public CFloat powTo(CFloat exponent) {
    if ((isOne() && !isNegative()) || (isOne() && exponent.isInfinity())) {
      return new JDouble(1.0d);
    }
    return new JDouble(Math.pow(value, toDouble(exponent.getWrapper())));
  }

  @Override
  public CFloat powToIntegral(int exponent) {
    return new JDouble(Math.pow(value, exponent));
  }

  @Override
  public CFloat sqrt() {
    return new JDouble(Math.sqrt(value));
  }

  @Override
  public CFloat round() {
    double posValue = Math.abs(value);
    double above = Math.ceil(posValue);
    double below = Math.floor(posValue);
    double rounded = posValue >= (above / 2 + below / 2) ? above : below;
    return new JDouble(Double.compare(value, 0.0d) >= 0 ? rounded : -rounded);
  }

  @Override
  public CFloat trunc() {
    return value <= 0.0d ? ceil() : floor();
  }

  @Override
  public CFloat ceil() {
    return new JDouble(Math.ceil(value));
  }

  @Override
  public CFloat floor() {
    return new JDouble(Math.floor(value));
  }

  @Override
  public CFloat abs() {
    return new JDouble(Math.abs(value));
  }

  @Override
  public boolean isZero() {
    return value == 0.0d;
  }

  @Override
  public boolean isOne() {
    return value == 1.0d || value == -1.0d;
  }

  @Override
  public boolean isNan() {
    return Double.isNaN(value);
  }

  @Override
  public boolean isInfinity() {
    return Double.isInfinite(value);
  }

  @Override
  public boolean isNegative() {
    return (Double.doubleToRawLongBits(value) & 0x8000000000000000L) != 0;
  }

  @Override
  public CFloat copySignFrom(CFloat source) {
    return new JDouble(source.isNegative() ? -Math.abs(value) : Math.abs(value));
  }

  @Override
  public CFloat castTo(CFloatType toType) {
    return switch (toType) {
      case SINGLE -> new JFloat((float) value);
      case DOUBLE -> this;
      case LONG_DOUBLE -> throw new UnsupportedOperationException();
    };
  }

  @Override
  public Optional<Number> castToOther(CIntegerType toType) {
    Number r =
        switch (toType) {
          case CHAR -> (byte) value;
          case SHORT -> (short) value;
          case INT -> (int) value;
          case LONG -> (long) value;
          default -> throw new UnsupportedOperationException();
        };

    CFloat v = new JDouble(r.doubleValue());
    if (!v.equalTo(trunc())) {
      // Return Optional.empty() if the value was too large for the target type
      return Optional.empty();
    }
    return Optional.of(r);
  }

  @Override
  public CFloatWrapper copyWrapper() {
    return wrapper.copy();
  }

  @Override
  protected CFloatWrapper getWrapper() {
    return wrapper;
  }

  @Override
  public CFloatType getType() {
    return CFloatType.DOUBLE;
  }

  @Override
  public boolean equalTo(CFloat other) {
    return value == toDouble(other.getWrapper());
  }

  @Override
  public boolean lessOrGreater(CFloat other) {
    double that = toDouble(other.getWrapper());
    if (Double.isNaN(value) || Double.isNaN(that)) {
      return false;
    }
    return value != that;
  }

  @Override
  public boolean greaterThan(CFloat other) {
    return value > toDouble(other.getWrapper());
  }

  @Override
  public boolean greaterOrEqual(CFloat other) {
    return value >= toDouble(other.getWrapper());
  }

  @Override
  public boolean lessThan(CFloat other) {
    return value < toDouble(other.getWrapper());
  }

  @Override
  public boolean lessOrEqual(CFloat other) {
    return value <= toDouble(other.getWrapper());
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    return other instanceof JDouble otherDouble && Double.compare(value, otherDouble.value) == 0;
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }

  @Override
  public int compareTo(CFloat other) {
    return Double.compare(value, ((JDouble) other).value);
  }

  public double toDouble() {
    return value;
  }
}
