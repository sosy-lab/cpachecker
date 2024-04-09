// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.floatingpoint;

import com.google.common.collect.ImmutableList;
import java.math.BigInteger;
import org.kframework.mpfr.BigFloat;
import org.kframework.mpfr.BinaryMathContext;
import org.sosy_lab.common.NativeLibraries;
import org.sosy_lab.cpachecker.util.floatingpoint.CFloatNativeAPI.CNativeType;
import org.sosy_lab.cpachecker.util.floatingpoint.MyFloat.Format;
import org.sosy_lab.cpachecker.util.floatingpoint.MyFloat.RoundingMode;

public class CMyFloat extends CFloat {
  static {
    NativeLibraries.loadLibrary("mpfr_java");
  }

  private final CFloatWrapper wrapper;
  private final MyFloat delegate;

  public CMyFloat(CFloatWrapper pWrapper, int pType) {
    wrapper = pWrapper;
    delegate = toMyFloat(pWrapper, CFloatNativeAPI.toNativeType(pType));
  }

  public CMyFloat(String repr, int pType) {
    delegate = parseFloat(repr, toMathContext(CFloatNativeAPI.toNativeType(pType)));
    wrapper = fromImpl(delegate);
  }

  public CMyFloat(String repr, BinaryMathContext pFormat) {
    Format format = new Format(calculateExpWidth(pFormat), pFormat.precision - 1);
    delegate = MyFloat.parseFloat(format, repr);
    wrapper = null; // fromImpl(delegate);
  }

  public CMyFloat(MyFloat pValue) {
    delegate = pValue;
    wrapper = fromImpl(pValue);
  }

  private BinaryMathContext toMathContext(CNativeType pType) {
    return switch (pType) {
      case SINGLE -> BinaryMathContext.BINARY32;
      case DOUBLE -> BinaryMathContext.BINARY64;
      case LONG_DOUBLE -> new BinaryMathContext(64, 15);
      default -> throw new UnsupportedOperationException();
    };
  }

  private Format toFormat(CNativeType pType) {
    return switch (pType) {
      case SINGLE -> Format.Float32;
      case DOUBLE -> Format.Float64;
      case LONG_DOUBLE -> new Format(15, 63);
      default -> throw new UnsupportedOperationException();
    };
  }

  private MyFloat toMyFloat(CFloatWrapper floatWrapper, CNativeType pType) {
    Format format = toFormat(pType);
    long signMask = 1L << format.getExpBits();
    long exponentMask = signMask - 1;

    // Extract bits for sign, exponent and mantissa from the wrapper
    long signBit = floatWrapper.getExponent() & signMask;
    long exponentBits = floatWrapper.getExponent() & exponentMask;
    long mantissaBits = floatWrapper.getMantissa();

    // Shift the exponent and convert the values
    boolean sign = signBit != 0;
    long exponent = exponentBits - format.bias();
    BigInteger mantissa = BigInteger.valueOf(mantissaBits);

    // Check that the value is "normal" (= not 0, Inf or NaN) and add the missing 1 to the mantissa
    if (exponentBits != 0 && exponentBits != exponentMask) {
      BigInteger leadingOne = BigInteger.ONE.shiftLeft(format.getSigBits());
      mantissa = mantissa.add(leadingOne);
    }
    return new MyFloat(format, sign, exponent, mantissa);
  }

  private CFloatWrapper fromImpl(MyFloat floatValue) {
    ImmutableList<Format> tiny = ImmutableList.of(Format.Float8, Format.Float16, Format.Float32);
    if (tiny.contains(floatValue.getFormat())) {
      // FIXME: In Float8 and Float16 this may be broken for subnormal numbers
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

  private double lb(double number) {
    return Math.log(number) / Math.log(2);
  }

  private int calculateExpWidth(BinaryMathContext pFormat) {
    return (int) Math.ceil(lb(2 * pFormat.maxExponent + 1));
  }

  private MyFloat parseFloat(String repr, BinaryMathContext pFormat) {
    Format format = new Format(calculateExpWidth(pFormat), pFormat.precision - 1);
    if ("nan".equals(repr)) {
      return MyFloat.nan(format);
    }
    if ("-inf".equals(repr)) {
      return MyFloat.negativeInfinity(format);
    }
    if ("inf".equals(repr)) {
      return MyFloat.infinity(format);
    }
    if ("-0.0".equals(repr)) {
      return MyFloat.negativeZero(format);
    }
    if ("0.0".equals(repr)) {
      return MyFloat.zero(format);
    }
    BigFloat floatValue = new BigFloat(repr, pFormat);
    long min = pFormat.minExponent;
    long max = pFormat.maxExponent;
    return new MyFloat(
        format, floatValue.sign(), floatValue.exponent(min, max), floatValue.significand(min, max));
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
    return new CMyFloat(delegate.roundToInteger(RoundingMode.NEAREST_AWAY));
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
      case HALF -> new CMyFloat(delegate.withPrecision(Format.Float16));
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
    if (Format.Float16.equals(delegate.getFormat())) {
      return CNativeType.HALF.getOrdinal();
    }
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
