// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.floatingpoint;

import com.google.common.base.Preconditions;
import java.math.BigInteger;
import org.kframework.mpfr.BigFloat;
import org.kframework.mpfr.BinaryMathContext;
import org.sosy_lab.cpachecker.util.floatingpoint.CFloatNativeAPI.CNativeType;
import org.sosy_lab.cpachecker.util.floatingpoint.MyFloat.Format;
import org.sosy_lab.cpachecker.util.floatingpoint.MyFloat.RoundingMode;

public class CMyFloat extends CFloat {
  private final CFloatWrapper wrapper;
  private final MyFloat delegate;

  public CMyFloat(String repr, int pFloatType) {
    Preconditions.checkArgument(pFloatType < 2);
    delegate = pFloatType == CNativeType.SINGLE.getOrdinal() ? parseFloat(repr) : parseDouble(repr);
    wrapper = fromImpl(delegate);
  }

  public CMyFloat(String repr, BinaryMathContext pFormat) {
    Preconditions.checkArgument(
        pFormat.equals(BinaryMathContext.BINARY32) || pFormat.equals(BinaryMathContext.BINARY64));
    delegate = pFormat.equals(BinaryMathContext.BINARY32) ? parseFloat(repr) : parseDouble(repr);
    wrapper = fromImpl(delegate);
  }

  public CMyFloat(MyFloat pValue) {
    delegate = pValue;
    wrapper = fromImpl(pValue);
  }

  private CFloatWrapper fromImpl(MyFloat floatValue) {
    if (Format.Float32.equals(floatValue.getFormat())) {
      long bits = Float.floatToRawIntBits(floatValue.toFloat());
      long exponent = ((bits & 0xFF800000L) >> 23) & 0x1FF;
      long mantissa = bits & 0x007FFFFF;
      return new CFloatWrapper(exponent, mantissa);
    }
    if (Format.Float64.equals(floatValue.getFormat())) {
      long bits = Double.doubleToRawLongBits(floatValue.toDouble());
      long exponent = ((bits & 0xFFF0000000000000L) >> 52) & 0xFFFL;
      long mantissa = bits & 0xFFFFFFFFFFFFFL;
      return new CFloatWrapper(exponent, mantissa);
    }
    throw new IllegalArgumentException();
  }

  private MyFloat parseFloat(String repr) {
    if ("nan".equals(repr)) {
      return MyFloat.nan(Format.Float32);
    }
    if ("-inf".equals(repr)) {
      return MyFloat.negativeInfinity(Format.Float32);
    }
    if ("inf".equals(repr)) {
      return MyFloat.infinity(Format.Float32);
    }
    if ("-0.0".equals(repr)) {
      return MyFloat.negativeZero(Format.Float32);
    }
    if ("0.0".equals(repr)) {
      return MyFloat.zero(Format.Float32);
    }
    BigFloat floatValue = new BigFloat(repr, BinaryMathContext.BINARY32);
    long min = BinaryMathContext.BINARY32.minExponent;
    long max = BinaryMathContext.BINARY32.maxExponent;
    return new MyFloat(
        Format.Float32,
        floatValue.sign(),
        floatValue.exponent(min, max),
        floatValue.significand(min, max));
  }

  private MyFloat parseDouble(String repr) {
    if ("nan".equals(repr)) {
      return MyFloat.nan(Format.Float64);
    }
    if ("-inf".equals(repr)) {
      return MyFloat.negativeInfinity(Format.Float64);
    }
    if ("inf".equals(repr)) {
      return MyFloat.infinity(Format.Float64);
    }
    if ("-0.0".equals(repr)) {
      return MyFloat.negativeZero(Format.Float64);
    }
    if ("0.0".equals(repr)) {
      return MyFloat.zero(Format.Float64);
    }
    BigFloat doubleValue = new BigFloat(repr, BinaryMathContext.BINARY64);
    long min = BinaryMathContext.BINARY64.minExponent;
    long max = BinaryMathContext.BINARY64.maxExponent;
    return new MyFloat(
        Format.Float64,
        doubleValue.sign(),
        doubleValue.exponent(min, max),
        doubleValue.significand(min, max));
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
    MyFloat r = delegate;
    for (CFloat f : pSummands) {
      CMyFloat mf = (CMyFloat) f;
      r = r.add(mf.delegate);
    }
    return new CMyFloat(r);
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
    MyFloat r = delegate;
    for (CFloat f : pFactors) {
      CMyFloat mf = (CMyFloat) f;
      r = r.multiply(mf.delegate);
    }
    return new CMyFloat(r);
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
  public CFloat ln() {
    return new CMyFloat(delegate.ln());
  }

  @Override
  public CFloat exp() {
    return new CMyFloat(delegate.exp());
  }

  @Override
  public CFloat powTo(CFloat pExponent) {
    if (pExponent instanceof CMyFloat myExponent) {
      return new CMyFloat(delegate.pow(myExponent.delegate));
    }
    throw new UnsupportedOperationException();
  }

  @Override
  public CFloat powToIntegral(int exponent) {
    return new CMyFloat(delegate.powInt(BigInteger.valueOf(exponent)));
  }

  @Override
  public CFloat sqrt() {
    return new CMyFloat(delegate.sqrt());
  }

  @Override
  public CFloat round() {
    return new CMyFloat(delegate.roundToInteger(RoundingMode.NEAREST));
  }

  @Override
  public CFloat trunc() {
    return new CMyFloat(delegate.roundToInteger(RoundingMode.TRUNCATE));
  }

  @Override
  public CFloat ceil() {
    return new CMyFloat(delegate.roundToInteger(RoundingMode.CEILING));
  }

  @Override
  public CFloat floor() {
    return new CMyFloat(delegate.roundToInteger(RoundingMode.FLOOR));
  }

  @Override
  public CFloat abs() {
    return new CMyFloat(delegate.abs());
  }

  @Override
  public boolean isZero() {
    return delegate.isZero();
  }

  @Override
  public boolean isOne() {
    return delegate.isOne() || delegate.isNegativeOne();
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
    if (source instanceof CMyFloat mySource) {
      return new CMyFloat(mySource.isNegative() ? delegate.abs().negate() : delegate.abs());
    }
    throw new UnsupportedOperationException();
  }

  @Override
  public CFloat castTo(CNativeType toType) {
    return switch (toType) {
      case SINGLE -> new CMyFloat(delegate.withPrecision(Format.Float32));
      case DOUBLE -> new CMyFloat(delegate.withPrecision(Format.Float64));
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

  public MyFloat getValue() {
    return delegate;
  }

  @Override
  public int getType() {
    if (Format.Float32.equals(delegate.getFormat())) {
      return CNativeType.SINGLE.getOrdinal();
    }
    if (Format.Float64.equals(delegate.getFormat())) {
      return CNativeType.DOUBLE.getOrdinal();
    }
    throw new IllegalStateException();
  }

  @Override
  public boolean greaterThan(CFloat other) {
    if (other instanceof CMyFloat myOther) {
      return delegate.greaterThan(myOther.delegate);
    }
    throw new UnsupportedOperationException();
  }

  @Override
  public BigFloat toBigFloat() {
    return delegate.toBigFloat();
  }

  @Override
  public String toString() {
    return delegate.toString();
  }
}
