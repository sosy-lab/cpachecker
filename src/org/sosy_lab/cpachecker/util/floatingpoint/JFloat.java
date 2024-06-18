// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.floatingpoint;

import java.math.BigDecimal;
import java.math.MathContext;
import org.sosy_lab.cpachecker.util.floatingpoint.CFloatNativeAPI.CNativeType;

/**
 * Java based implementation of the {@link CFloat} interface that uses regular Java floats.
 *
 * <p>For double precision use the sister class {@link JDouble}.
 */
class JFloat extends CFloat {
  private final CFloatWrapper wrapper;
  private final float value;

  public JFloat(float pValue) {
    value = pValue;
    wrapper = fromFloat(value);
  }

  public JFloat(String repr) {
    value = parseFloat(repr);
    wrapper = fromFloat(value);
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
    if ("-nan".equals(repr)) {
      return Float.intBitsToFloat(0xFFC00000);
    } else if ("nan".equals(repr)) {
      return Float.NaN;
    } else if ("-inf".equals(repr)) {
      return Float.NEGATIVE_INFINITY;
    } else if ("inf".equals(repr)) {
      return Float.POSITIVE_INFINITY;
    } else {
      return Float.parseFloat(repr);
    }
  }

  @Override
  public String toString() {
    if (isNan()) {
      return isNegative() ? "-nan" : "nan";
    } else if (isInfinity()) {
      return isNegative() ? "-inf" : "inf";
    } else if (isZero()) {
      return isNegative() ? "-0.00000000e+00" : "0.00000000e+00";
    } else {
      BigDecimal decimal =
          BigDecimal.valueOf(value).plus(new MathContext(9, java.math.RoundingMode.HALF_EVEN));
      return String.format("%.8e", decimal);
    }
  }

  @Override
  public CFloat add(CFloat pSummand) {
    return new JFloat(value + toFloat(pSummand.getWrapper()));
  }

  @Override
  public CFloat add(CFloat... pSummands) {
    float result = value;
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
    float result = value;
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
    if ((isOne() && !isNegative()) || (isOne() && exponent.isInfinity())) {
      return new JFloat(1.0f);
    } else {
      return new JFloat((float) Math.pow(value, toFloat(exponent.getWrapper())));
    }
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
    double rounded = posValue >= (above / 2 + below / 2) ? above : below;
    return new JFloat((float) (Float.compare(value, 0.0f) >= 0 ? rounded : -rounded));
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
    return value == 1.0f || value == -1.0f;
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
    return (Float.floatToRawIntBits(value) & 0x80000000) != 0;
  }

  @Override
  public CFloat copySignFrom(CFloat source) {
    return new JFloat(source.isNegative() ? -Math.abs(value) : Math.abs(value));
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
