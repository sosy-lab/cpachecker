// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.floatingpoint;

import org.kframework.mpfr.BigFloat;
import org.kframework.mpfr.BinaryMathContext;
import org.sosy_lab.cpachecker.util.floatingpoint.CFloatNativeAPI.CNativeType;

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
  public CFloat add(CFloat... pSummands) {
    double result = value;
    for (CFloat f : pSummands) {
      result += toDouble(f.getWrapper());
    }
    return new JDouble(result);
  }

  @Override
  public CFloat multiply(CFloat pFactor) {
    return new JDouble(value * toDouble(pFactor.getWrapper()));
  }

  @Override
  public CFloat multiply(CFloat... pFactors) {
    double result = value;
    for (CFloat f : pFactors) {
      result *= toDouble(f.getWrapper());
    }
    return new JDouble(result);
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
  public CFloat ln() {
    return new JDouble(Math.log(toDouble()));
  }

  @Override
  public CFloat exp() {
    return new JDouble(Math.exp(toDouble()));
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
  public CFloat castTo(CNativeType toType) {
    return switch (toType) {
      case SINGLE -> new JFloat((float) value);
      case DOUBLE -> this;
      case LONG_DOUBLE -> throw new UnsupportedOperationException();
      default -> throw new IllegalArgumentException();
    };
  }

  @Override
  public Number castToOther(CNativeType toType) {
    Double doubleValue = Double.valueOf(value);
    return switch (toType) {
      case CHAR -> doubleValue.byteValue();
      case SHORT -> doubleValue.shortValue();
      case INT -> doubleValue.intValue();
      case LONG -> doubleValue.longValue();
      case SINGLE -> throw new IllegalArgumentException();
      case DOUBLE -> throw new IllegalArgumentException();
      case LONG_DOUBLE -> throw new IllegalArgumentException();
      default -> throw new UnsupportedOperationException();
    };
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
  public int getType() {
    return CNativeType.DOUBLE.getOrdinal();
  }

  @Override
  public boolean equalTo(CFloat other) {
    return value == toDouble(other.getWrapper());
  }

  @Override
  public boolean notEqualTo(CFloat other) {
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
}
