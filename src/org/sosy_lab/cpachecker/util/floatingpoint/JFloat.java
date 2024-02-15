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
  private float value;

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
    long e = wfloat.getExponent();
    long m = wfloat.getMantissa();
    float ret = Float.intBitsToFloat((int) ((e << 23) + m));
    return ret;
  }

  private CFloatWrapper fromFloat(float jfloat) {
    long bits = Float.floatToRawIntBits(jfloat);
    long eBits = ((bits & 0xFF800000) >> 23) & 0x1FF;
    long mBits = bits & 0x007FFFFF;
    return new CFloatWrapper(eBits, mBits);
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
    if (Float.isNaN(value)) {
      return "nan";
    }
    if (Float.isInfinite(value)) {
      return Float.compare(value, 0.0f) < 0 ? "-inf" : "inf";
    }
    return String.valueOf(value);
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
  public CFloat multiply(CFloat pSummand) {
    return new JFloat(value * toFloat(pSummand.getWrapper()));
  }

  @Override
  public CFloat multiply(CFloat... pSummands) {
    float result = 0.0f;
    for (CFloat f : pSummands) {
      result *= toFloat(f.getWrapper());
    }
    return new JFloat(result);
  }

  @Override
  public CFloat subtract(CFloat pSummand) {
    return new JFloat(value - toFloat(pSummand.getWrapper()));
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
  public boolean isNegative() {
    return value < 0.0f;
  }

  @Override
  public CFloat copySignFrom(CFloat source) {
    return new JFloat(
        Math.signum(toFloat(source.getWrapper())) < 0.0f ? -Math.abs(value) : Math.abs(value));
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
