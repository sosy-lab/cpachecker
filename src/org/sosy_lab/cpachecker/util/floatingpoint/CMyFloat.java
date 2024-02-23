// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.floatingpoint;

import com.google.common.base.Preconditions;
import org.kframework.mpfr.BigFloat;
import org.kframework.mpfr.BinaryMathContext;
import org.sosy_lab.cpachecker.util.floatingpoint.CFloatNativeAPI.CNativeType;
import org.sosy_lab.cpachecker.util.floatingpoint.MyFloat.Format;

public class CMyFloat extends CFloat {
  private final CFloatWrapper wrapper;
  private final MyFloat delegate;

  public CMyFloat(String repr, int pFloatType) {
    Preconditions.checkArgument(pFloatType == 0);
    delegate = parseString(repr);
    wrapper = fromImpl(delegate);
  }

  public CMyFloat(MyFloat pValue) {
    delegate = pValue;
    wrapper = fromImpl(pValue);
  }

  private CFloatWrapper fromImpl(MyFloat floatValue) {
    // copied from JFloat class
    long bits = Double.doubleToRawLongBits(floatValue.toDouble());
    long exponent = ((bits & 0xFFF0000000000000L) >> 52) & 0xFFF;
    long mantissa = bits & 0xFFFFFFFFFFFFFL;
    return new CFloatWrapper(exponent, mantissa);
  }

  private MyFloat parseString(String repr) {
    if ("nan".equals(repr)) {
      return MyFloat.nan(Format.FLOAT);
    }
    if ("-inf".equals(repr)) {
      return MyFloat.negativeInfinity(Format.FLOAT);
    }
    if ("inf".equals(repr)) {
      return MyFloat.infinity(Format.FLOAT);
    }
    if ("-0.0".equals(repr)) {
      return MyFloat.negativeZero(Format.FLOAT);
    }
    if ("0.0".equals(repr)) {
      return MyFloat.zero(Format.FLOAT);
    }
    BigFloat floatValue = new BigFloat(repr, BinaryMathContext.BINARY32);
    long min = BinaryMathContext.BINARY32.minExponent;
    long max = BinaryMathContext.BINARY32.maxExponent;
    return new MyFloat(
        Format.FLOAT,
        floatValue.sign(),
        floatValue.exponent(min, max),
        floatValue.significand(min, max));
  }

  @Override
  public CFloat add(CFloat pSummand) {
    if (pSummand instanceof CMyFloat mySummand) {
      return new CMyFloat(delegate.add(mySummand.delegate));
    }
    throw new UnsupportedOperationException();
  }

  @Override
  public CFloat add(CFloat... pSummands) {
    throw new UnsupportedOperationException();
  }

  @Override
  public CFloat multiply(CFloat pFactor) {
    if (pFactor instanceof CMyFloat myFactor) {
      return new CMyFloat(delegate.multiply(myFactor.delegate));
    }
    throw new UnsupportedOperationException();
  }

  @Override
  public CFloat multiply(CFloat... pFactors) {
    throw new UnsupportedOperationException();
  }

  @Override
  public CFloat subtract(CFloat pSubtrahend) {
    if (pSubtrahend instanceof CMyFloat mySubtrahend) {
      return new CMyFloat(delegate.subtract(mySubtrahend.delegate));
    }
    throw new UnsupportedOperationException();
  }

  @Override
  public CFloat divideBy(CFloat pDivisor) {
    if (pDivisor instanceof CMyFloat myDivisor) {
      return new CMyFloat(delegate.divide(myDivisor.delegate));
    }
    throw new UnsupportedOperationException();
  }

  @Override
  public CFloat powTo(CFloat exponent) {
    throw new UnsupportedOperationException();
  }

  @Override
  public CFloat powToIntegral(int exponent) {
    throw new UnsupportedOperationException();
  }

  @Override
  public CFloat sqrt() {
    throw new UnsupportedOperationException();
  }

  @Override
  public CFloat round() {
    throw new UnsupportedOperationException();
  }

  @Override
  public CFloat trunc() {
    throw new UnsupportedOperationException();
  }

  @Override
  public CFloat ceil() {
    throw new UnsupportedOperationException();
  }

  @Override
  public CFloat floor() {
    throw new UnsupportedOperationException();
  }

  @Override
  public CFloat abs() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isZero() {
    return delegate.isZero();
  }

  @Override
  public boolean isOne() {
    MyFloat one = parseString("1.0");
    return one.equals(delegate);
  }

  @Override
  public boolean isNan() {
    return delegate.isNan();
  }

  @Override
  public boolean isInfinity() {
    return delegate.isInfinite();
  }

  @Override
  public boolean isNegative() {
    return delegate.isNegative();
  }

  @Override
  public CFloat copySignFrom(CFloat source) {
    throw new UnsupportedOperationException();
  }

  @Override
  public CFloat castTo(CNativeType toType) {
    return switch (toType) {
      case SINGLE -> new CMyFloat(delegate.withPrecision(Format.FLOAT));
      case DOUBLE -> new CMyFloat(delegate.withPrecision(Format.DOUBLE));
      case LONG_DOUBLE -> throw new UnsupportedOperationException();
      default -> throw new IllegalArgumentException();
    };
  }

  @Override
  public Number castToOther(CNativeType toType) {
    return switch (toType) {
      case CHAR -> delegate.toByte();
      case SHORT -> delegate.toShort();
      case INT -> delegate.toInt();
      case LONG -> delegate.toLong();
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
    throw new UnsupportedOperationException();
  }

  @Override
  public String toString() {
    return delegate.toString();
  }
}
