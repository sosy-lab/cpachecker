// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.floatingpoint;

import org.sosy_lab.cpachecker.util.floatingpoint.CFloatNativeAPI.CNativeType;

/* Implementation of the CFloat interface that uses Java doubles */
public class JDouble extends CFloat {
  private final CFloatWrapper wrapper;
  private final double value;

  public JDouble(double pValue) {
    value = pValue;
    wrapper = fromFloat(value);
  }

  public JDouble(String repr, int floatType) {
    assert floatType == 1;
    value = parseDouble(repr);
    wrapper = fromFloat(value);
  }

  public JDouble(CFloatWrapper pWrapper, int floatType) {
    assert floatType == 1;
    value = toDouble(pWrapper);
    wrapper = pWrapper;
  }

  private double toDouble(CFloatWrapper pWrapper) {
    long exponent = pWrapper.getExponent();
    long mantissa = pWrapper.getMantissa();
    return Double.longBitsToDouble((exponent << 52) + mantissa);
  }

  private CFloatWrapper fromFloat(double pValue) {
    long bits = Double.doubleToLongBits(pValue);
    long exponent = ((bits & 0xFFF0000000000000L) >> 52) & 0xFFF;
    long mantissa = bits & 0xFFFFFFFFFFFFFL;
    return new CFloatWrapper(exponent, mantissa);
  }

  private double parseDouble(String repr) {
    if ("nan".equals(repr)) {
      return Double.NaN;
    }
    if ("-inf".equals(repr)) {
      return Double.NEGATIVE_INFINITY;
    }
    if ("inf".equals(repr)) {
      return Double.POSITIVE_INFINITY;
    }
    return Double.parseDouble(repr);
  }

  @Override
  public String toString() {
    if (isNan()) {
      return "nan";
    }
    if (isInfinity()) {
      return isNegative() ? "-inf" : "inf";
    }
    if (isZero()) {
      return isNegative() ? "-0.0" : "0.0";
    }
    return String.format("%.6e", value); // TODO: Use more precision here?
  }

  @Override
  public CFloat add(CFloat pSummand) {
    return new JDouble(value + toDouble(pSummand.getWrapper()));
  }

  @Override
  public CFloat add(CFloat... pSummands) {
    double result = 0.0d;
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
    double result = 0.0d;
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
  public CFloat powTo(CFloat exponent) {
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
    double above = Math.ceil(value);
    double below = Math.floor(value);
    return new JDouble(above - value > value - below ? below : above);
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
    return value == 1.0d;
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
    return Double.compare(value, 0.0d) < 0;
  }

  @Override
  public CFloat copySignFrom(CFloat source) {
    return new JDouble(
        Double.compare(toDouble(source.getWrapper()), 0.0d) < 0
            ? -Math.abs(value)
            : Math.abs(value));
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
    return 0;
  }

  @Override
  public boolean greaterThan(CFloat other) {
    return value > toDouble(other.getWrapper());
  }
}
