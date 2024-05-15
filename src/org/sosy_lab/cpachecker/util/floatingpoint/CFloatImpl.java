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
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.kframework.mpfr.BigFloat;
import org.kframework.mpfr.BinaryMathContext;
import org.sosy_lab.cpachecker.util.floatingpoint.CFloatNativeAPI.CNativeType;
import org.sosy_lab.cpachecker.util.floatingpoint.FloatP.Format;
import org.sosy_lab.cpachecker.util.floatingpoint.FloatP.RoundingMode;

/**
 * Adapter class for {@link FloatP} that implements the CFloat interface.
 *
 * <p>This class provides an entirely Java based implementation of the CFloat interface and should
 * be used over the now deprecated {@link CFloatNative}.
 *
 * <p>Unlike {@link FloatP} this class does not expect arguments to have the same precision.
 */
public class CFloatImpl extends CFloat {
  private final CFloatWrapper wrapper;
  private final FloatP delegate;

  public CFloatImpl(CFloatWrapper pWrapper, int pType) {
    wrapper = pWrapper;
    delegate = toMyFloat(pWrapper, CFloatNativeAPI.toNativeType(pType));
  }

  public CFloatImpl(String repr, int pType) {
    delegate = parseFloat(repr, toMathContext(CFloatNativeAPI.toNativeType(pType)), null);
    wrapper = fromImpl(delegate);
  }

  public CFloatImpl(String repr, BinaryMathContext pFormat) {
    delegate = parseFloat(repr, pFormat, null);
    wrapper = fromImpl(delegate);
  }

  public CFloatImpl(String repr, BinaryMathContext pFormat, Map<Integer, Integer> fromStringStats) {
    delegate = parseFloat(repr, pFormat, fromStringStats);
    wrapper = fromImpl(delegate);
  }

  public CFloatImpl(BigFloat value, BinaryMathContext pFormat) {
    Format format = new Format(calculateExpWidth(pFormat), pFormat.precision - 1);
    delegate = FloatP.fromBigFloat(format, value);
    wrapper = null; // fromImpl(delegate);
  }

  public CFloatImpl(FloatP pValue) {
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

  private FloatP toMyFloat(CFloatWrapper floatWrapper, CNativeType pType) {
    Format format = toFormat(pType);
    long signMask = 1L << format.expBits();
    long exponentMask = signMask - 1;

    // Extract bits for sign, exponent and mantissa from the wrapper
    long signBit = floatWrapper.getExponent() & signMask;
    long exponentBits = floatWrapper.getExponent() & exponentMask;
    long mantissaBits = floatWrapper.getMantissa();

    // Shift the exponent and convert the values
    boolean sign = signBit != 0;
    long exponent = exponentBits - format.bias();
    BigInteger mantissa = new BigInteger(Long.toUnsignedString(mantissaBits));

    // Check if the value is "normal" (= not 0, Inf or NaN) and add the missing 1 to the mantissa.
    if (pType != CNativeType.LONG_DOUBLE) { // Extended precision has no hidden bit
      if (exponentBits != 0 && exponentBits != exponentMask) {
        BigInteger leadingOne = BigInteger.ONE.shiftLeft(format.sigBits());
        mantissa = mantissa.add(leadingOne);
      }
    }
    return new FloatP(format, sign, exponent, mantissa);
  }

  private CFloatWrapper fromImpl(FloatP floatValue) {
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
    if (new Format(15, 63).equals(floatValue.getFormat())) {
      long signBit = floatValue.isNegative() ? 1 << 15 : 0;
      long exponent = signBit + floatValue.extractExpBits();
      long mantissa = floatValue.extractSigBits().longValue();
      return new CFloatWrapper(exponent, mantissa);
    }
    throw new IllegalArgumentException();
  }

  private int calculateExpWidth(BinaryMathContext pFormat) {
    BigInteger val = BigInteger.valueOf(2 * pFormat.maxExponent + 1);
    int r = val.bitLength() - 1;
    return val.bitCount() > 1 ? r + 1 : r;
  }

  private FloatP parseFloat(
      String repr, BinaryMathContext pFormat, @Nullable Map<Integer, Integer> fromStringMap) {
    Format format = new Format(calculateExpWidth(pFormat), pFormat.precision - 1);
    if ("nan".equals(repr)) {
      return FloatP.nan(format);
    }
    if ("-inf".equals(repr)) {
      return FloatP.negativeInfinity(format);
    }
    if ("inf".equals(repr)) {
      return FloatP.infinity(format);
    }
    if ("-0.0".equals(repr)) {
      return FloatP.negativeZero(format);
    }
    if ("0.0".equals(repr)) {
      return FloatP.zero(format);
    }
    return FloatP.fromStringWithStats(format, repr, fromStringMap);
  }

  @Override
  public CFloat add(CFloat pSummand) {
    if (pSummand instanceof CFloatImpl mySummand) {
      Format p = delegate.getFormat().sup(mySummand.delegate.getFormat());
      FloatP arg1 = delegate.withPrecision(p);
      FloatP arg2 = mySummand.delegate.withPrecision(p);
      return new CFloatImpl(arg1.add(arg2));
    }
    throw new UnsupportedOperationException();
  }

  @Override
  public CFloat add(CFloat... pSummands) {
    Format p = new Format(0, 0);
    for (CFloat f : pSummands) {
      CFloatImpl mf = (CFloatImpl) f;
      p = p.sup(mf.delegate.getFormat());
    }
    FloatP r = delegate.withPrecision(p);
    for (CFloat f : pSummands) {
      CFloatImpl mf = (CFloatImpl) f;
      r = r.add(mf.delegate.withPrecision(p));
    }
    return new CFloatImpl(r);
  }

  @Override
  public CFloat multiply(CFloat pFactor) {
    if (pFactor instanceof CFloatImpl myFactor) {
      Format p = delegate.getFormat().sup(myFactor.delegate.getFormat());
      FloatP arg1 = delegate.withPrecision(p);
      FloatP arg2 = myFactor.delegate.withPrecision(p);
      return new CFloatImpl(arg1.multiply(arg2));
    }
    throw new UnsupportedOperationException();
  }

  @Override
  public CFloat multiply(CFloat... pFactors) {
    Format p = new Format(0, 0);
    for (CFloat f : pFactors) {
      CFloatImpl mf = (CFloatImpl) f;
      p = p.sup(mf.delegate.getFormat());
    }
    FloatP r = delegate.withPrecision(p);
    for (CFloat f : pFactors) {
      CFloatImpl mf = (CFloatImpl) f;
      r = r.multiply(mf.delegate.withPrecision(p));
    }
    return new CFloatImpl(r);
  }

  @Override
  public CFloat subtract(CFloat pSubtrahend) {
    if (pSubtrahend instanceof CFloatImpl mySubtrahend) {
      Format p = delegate.getFormat().sup(mySubtrahend.delegate.getFormat());
      FloatP arg1 = delegate.withPrecision(p);
      FloatP arg2 = mySubtrahend.delegate.withPrecision(p);
      return new CFloatImpl(arg1.subtract(arg2));
    }
    throw new UnsupportedOperationException();
  }

  @Override
  public CFloat divideBy(CFloat pDivisor) {
    if (pDivisor instanceof CFloatImpl myDivisor) {
      Format p = delegate.getFormat().sup(myDivisor.delegate.getFormat());
      FloatP arg1 = delegate.withPrecision(p);
      FloatP arg2 = myDivisor.delegate.withPrecision(p);
      return new CFloatImpl(arg1.divide(arg2));
    }
    throw new UnsupportedOperationException();
  }

  @Override
  public CFloat ln() {
    return new CFloatImpl(delegate.ln());
  }

  /** See {@link FloatP#lnWithStats} */
  CFloat lnWithStats(Map<Integer, Integer> lnStats) {
    return new CFloatImpl(delegate.lnWithStats(lnStats));
  }

  @Override
  public CFloat exp() {
    return new CFloatImpl(delegate.exp());
  }

  /** See {@link FloatP#expWithStats} */
  CFloat expWithStats(Map<Integer, Integer> expStats) {
    return new CFloatImpl(delegate.expWithStats(expStats));
  }

  @Override
  public CFloat powTo(CFloat pExponent) {
    if (pExponent instanceof CFloatImpl myExponent) {
      Format p = delegate.getFormat().sup(myExponent.delegate.getFormat());
      FloatP arg1 = delegate.withPrecision(p);
      FloatP arg2 = myExponent.delegate.withPrecision(p);
      return new CFloatImpl(arg1.pow(arg2));
    }
    throw new UnsupportedOperationException();
  }

  /** See {@link FloatP#powWithStats} */
  CFloat powToWithStats(CFloat pExponent, Map<Integer, Integer> powStats) {
    if (pExponent instanceof CFloatImpl myExponent) {
      Format p = delegate.getFormat().sup(myExponent.delegate.getFormat());
      FloatP arg1 = delegate.withPrecision(p);
      FloatP arg2 = myExponent.delegate.withPrecision(p);
      return new CFloatImpl(arg1.powWithStats(arg2, powStats));
    }
    throw new UnsupportedOperationException();
  }

  @Override
  public CFloat powToIntegral(int exponent) {
    return new CFloatImpl(delegate.powInt(BigInteger.valueOf(exponent)));
  }

  @Override
  public CFloat sqrt() {
    return new CFloatImpl(delegate.sqrt());
  }

  @Override
  public CFloat round() {
    return new CFloatImpl(delegate.roundToInteger(RoundingMode.NEAREST_AWAY));
  }

  @Override
  public CFloat trunc() {
    return new CFloatImpl(delegate.roundToInteger(RoundingMode.TRUNCATE));
  }

  @Override
  public CFloat ceil() {
    return new CFloatImpl(delegate.roundToInteger(RoundingMode.CEILING));
  }

  @Override
  public CFloat floor() {
    return new CFloatImpl(delegate.roundToInteger(RoundingMode.FLOOR));
  }

  @Override
  public CFloat abs() {
    return new CFloatImpl(delegate.abs());
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
    if (source instanceof CFloatImpl mySource) {
      return new CFloatImpl(mySource.isNegative() ? delegate.abs().negate() : delegate.abs());
    }
    throw new UnsupportedOperationException();
  }

  @Override
  public CFloat castTo(CNativeType toType) {
    return switch (toType) {
      case HALF -> new CFloatImpl(delegate.withPrecision(Format.Float16));
      case SINGLE -> new CFloatImpl(delegate.withPrecision(Format.Float32));
      case DOUBLE -> new CFloatImpl(delegate.withPrecision(Format.Float64));
      case LONG_DOUBLE -> new CFloatImpl(delegate.withPrecision(new Format(15, 63)));
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

  public FloatP getValue() {
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
    Format ext = new Format(15, 63);
    if (ext.equals(delegate.getFormat())) {
      return CNativeType.LONG_DOUBLE.getOrdinal();
    }
    throw new IllegalStateException();
  }

  @Override
  public boolean greaterThan(CFloat other) {
    if (other instanceof CFloatImpl myOther) {
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
