// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.floatingpoint;

import org.sosy_lab.cpachecker.util.floatingpoint.CFloatNativeAPI.CNativeType;

/* Implementation of the CFloat interface that uses Java floats */
public class JFloat extends CFloat {
  private final CFloatWrapper wrapper;
  private final float value;

  public JFloat(float pValue) {
    value = pValue;
    wrapper = fromFloat(value);
  }

  public JFloat(String repr, int floatType) {
    assert floatType == 0;
    value = parseFloat(repr);
    wrapper = fromFloat(value);
  }

  public JFloat(CFloatWrapper pWrapper, int floatType) {
    assert floatType == 0;
    value = toFloat(pWrapper);
    wrapper = pWrapper;
  }

  private float toFloat(CFloatWrapper wfloat) {
    long exponent = wfloat.getExponent();
    long mantissa = wfloat.getMantissa();
    return Float.intBitsToFloat((int) ((exponent << 23) + mantissa));
  }

  private CFloatWrapper fromFloat(float jfloat) {
    long bits = Float.floatToRawIntBits(jfloat);
    long exponent = ((bits & 0xFF800000L) >> 23) & 0x1FF;
    long mantissa = bits & 0x007FFFFF;
    return new CFloatWrapper(exponent, mantissa);
  }

  private float parseFloat(String repr) {
    if ("nan".equals(repr)) {
      return Float.NaN;
    }
    if ("-inf".equals(repr)) {
      return Float.NEGATIVE_INFINITY;
    }
    if ("inf".equals(repr)) {
      return Float.POSITIVE_INFINITY;
    }
    return Float.parseFloat(repr);
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
    return String.format("%.6e", value);
  }

  @Override
  public CFloat add(CFloat pSummand) {
    return new JFloat(value + toFloat(pSummand.getWrapper()));
  }

  @Override
  public CFloat add(CFloat... pSummands) {
    float result = 0.0f;
    for (CFloat f : pSummands) {
      result += toFloat(f.getWrapper());
    }
    return new JFloat(result);
  }

  @Override
  public CFloat multiply(CFloat pFactor) {
    return new JFloat(value * toFloat(pFactor.getWrapper()));
  }

  @Override
  public CFloat multiply(CFloat... pFactors) {
    float result = 0.0f;
    for (CFloat f : pFactors) {
      result *= toFloat(f.getWrapper());
    }
    return new JFloat(result);
  }

  @Override
  public CFloat subtract(CFloat pSubtrahend) {
    return new JFloat(value - toFloat(pSubtrahend.getWrapper()));
  }

  @Override
  public CFloat divideBy(CFloat pDivisor) {
    return new JFloat(value / toFloat(pDivisor.getWrapper()));
  }

  @Override
  public CFloat ln() {
    return new JFloat((float) Math.log(toFloat()));
  }

  @Override
  public CFloat exp() {
    return new JFloat((float) Math.exp(toFloat()));
  }

  @Override
  public CFloat powTo(CFloat exponent) {
    return new JFloat((float) Math.pow(value, toFloat(exponent.getWrapper())));
  }

  @Override
  public CFloat powToIntegral(int exponent) {
    return new JFloat((float) Math.pow(value, exponent));
  }

  @Override
  public CFloat sqrt() {
    return new JFloat((float) Math.sqrt(value));
  }

  @Override
  public CFloat round() {
    double posValue = Math.abs(value);
    double above = Math.ceil(posValue);
    double below = Math.floor(posValue);
    double rounded = posValue >= (above + below) / 2 ? above : below;
    return new JFloat((float) (value > 0 ? rounded : -rounded));
  }

  @Override
  public CFloat trunc() {
    return value <= 0.0f ? ceil() : floor();
  }

  @Override
  public CFloat ceil() {
    return new JFloat((float) Math.ceil(value));
  }

  @Override
  public CFloat floor() {
    return new JFloat((float) Math.floor(value));
  }

  @Override
  public CFloat abs() {
    return new JFloat(Math.abs(value));
  }

  @Override
  public boolean isZero() {
    return value == 0.0f;
  }

  @Override
  public boolean isOne() {
    return value == 1.0f;
  }

  @Override
  public boolean isNan() {
    return Float.isNaN(value);
  }

  @Override
  public boolean isInfinity() {
    return Float.isInfinite(value);
  }

  @Override
  public boolean isNegative() {
    return Float.compare(value, 0.0f) < 0;
  }

  @Override
  public CFloat copySignFrom(CFloat source) {
    return new JFloat(
        Float.compare(toFloat(source.getWrapper()), 0.0f) < 0 ? -Math.abs(value) : Math.abs(value));
  }

  @Override
  public CFloat castTo(CNativeType toType) {
    return switch (toType) {
      case SINGLE -> this;
      case DOUBLE -> new JDouble(value);
      case LONG_DOUBLE -> throw new UnsupportedOperationException();
      default -> throw new IllegalArgumentException();
    };
  }

  @Override
  public Number castToOther(CNativeType toType) {
    Float floatValue = Float.valueOf(value);
    return switch (toType) {
      case CHAR -> floatValue.byteValue();
      case SHORT -> floatValue.shortValue();
      case INT -> floatValue.intValue();
      case LONG -> floatValue.longValue();
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
    return CNativeType.SINGLE.getOrdinal();
  }

  @Override
  public boolean greaterThan(CFloat other) {
    return value > toFloat(other.getWrapper());
  }
}
