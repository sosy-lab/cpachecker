// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.floatingpoint;

/* Implementation of the CFLoat interface that uses Java floats */
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
    long exponent = ((bits & 0xFF800000) >> 23) & 0x1FF;
    long mantissa = bits & 0x007FFFFF;
    return new CFloatWrapper(exponent, mantissa);
  }

  private float parseFloat(String repr) {
    if (repr == "nan") {
      return Float.NaN;
    }
    if (repr == "-inf") {
      return Float.NEGATIVE_INFINITY;
    }
    if (repr == "inf") {
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
    float above = (float) Math.ceil(value);
    float below = (float) Math.floor(value);
    return new JFloat(above - value > value - below ? below : above);
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
  public CFloat castTo(int toType) {
    // TODO: Implement casting
    throw new UnsupportedOperationException();
  }

  @Override
  public Number castToOther(int toType) {
    // TODO: Implement casting
    throw new UnsupportedOperationException();
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
    return value > toFloat(other.getWrapper());
  }
}