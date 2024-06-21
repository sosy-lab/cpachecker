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
import org.sosy_lab.cpachecker.util.floatingpoint.CFloatNativeAPI.CNativeType;
import org.sosy_lab.cpachecker.util.floatingpoint.FloatValue.Format;
import org.sosy_lab.cpachecker.util.floatingpoint.FloatValue.RoundingMode;

/**
 * Adapter class for {@link FloatValue} that implements the CFloat interface.
 *
 * <p>This class provides an entirely Java based implementation of the CFloat interface and should
 * be used over the now deprecated {@link CFloatNative}.
 */
class CFloatImpl extends CFloat {
  private final CFloatWrapper wrapper;
  private final FloatValue delegate;

  public CFloatImpl(CFloatWrapper pWrapper, int pType) {
    wrapper = pWrapper;
    delegate = toMyFloat(pWrapper, CFloatNativeAPI.toNativeType(pType));
  }

  public CFloatImpl(String repr, int pType) {
    delegate = parseFloat(repr, toFormat(CFloatNativeAPI.toNativeType(pType)));
    wrapper = fromImpl(delegate);
  }

  public CFloatImpl(String repr, Format pFormat) {
    delegate = parseFloat(repr, pFormat);
    wrapper = fromImpl(delegate);
  }

  public CFloatImpl(FloatValue pValue) {
    delegate = pValue;
    wrapper = fromImpl(pValue);
  }

  private Format toFormat(CNativeType pType) {
    return switch (pType) {
      case SINGLE -> Format.Float32;
      case DOUBLE -> Format.Float64;
      case LONG_DOUBLE -> new Format(15, 63);
      default -> throw new UnsupportedOperationException();
    };
  }

  private FloatValue toMyFloat(CFloatWrapper floatWrapper, CNativeType pType) {
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
    return new FloatValue(format, sign, exponent, mantissa);
  }

  private CFloatWrapper fromImpl(FloatValue floatValue) {
    ImmutableList<Format> tiny = ImmutableList.of(Format.Float8, Format.Float16, Format.Float32);
    if (tiny.contains(floatValue.getFormat())) {
      // FIXME: In Float8 and Float16 this may be broken for subnormal numbers
      long bits = Float.floatToRawIntBits(floatValue.floatValue());
      long exponent = ((bits & 0xFF800000L) >> 23) & 0x1FF;
      long mantissa = bits & 0x007FFFFF;
      return new CFloatWrapper(exponent, mantissa);
    } else if (Format.Float64.equals(floatValue.getFormat())) {
      long bits = Double.doubleToRawLongBits(floatValue.doubleValue());
      long exponent = ((bits & 0xFFF0000000000000L) >> 52) & 0xFFFL;
      long mantissa = bits & 0xFFFFFFFFFFFFFL;
      return new CFloatWrapper(exponent, mantissa);
    } else if (new Format(15, 63).equals(floatValue.getFormat())) {
      long signBit = floatValue.isNegative() ? 1 << 15 : 0;
      long exponent = signBit + floatValue.extractExpBits() + floatValue.getFormat().bias();
      long mantissa = floatValue.extractSigBits().longValue();
      return new CFloatWrapper(exponent, mantissa);
    } else {
      throw new IllegalArgumentException();
    }
  }

  private FloatValue parseFloat(String repr, Format format) {
    if ("nan".equals(repr)) {
      return FloatValue.nan(format);
    } else if ("-inf".equals(repr)) {
      return FloatValue.negativeInfinity(format);
    } else if ("inf".equals(repr)) {
      return FloatValue.infinity(format);
    } else if ("-0.0".equals(repr)) {
      return FloatValue.negativeZero(format);
    } else if ("0.0".equals(repr)) {
      return FloatValue.zero(format);
    } else {
      return FloatValue.fromString(format, repr);
    }
  }

  @Override
  public CFloat add(CFloat pSummand) {
    if (pSummand instanceof CFloatImpl mySummand) {
      return new CFloatImpl(delegate.add(mySummand.delegate));
    }
    throw new UnsupportedOperationException();
  }

  @Override
  public CFloat add(CFloat... pSummands) {
    Format p = new Format(2, 1); // the smallest supported format
    for (CFloat f : pSummands) {
      CFloatImpl mf = (CFloatImpl) f;
      p = p.sup(mf.delegate.getFormat());
    }
    FloatValue r = delegate.withPrecision(p);
    for (CFloat f : pSummands) {
      CFloatImpl mf = (CFloatImpl) f;
      r = r.add(mf.delegate.withPrecision(p));
    }
    return new CFloatImpl(r);
  }

  @Override
  public CFloat multiply(CFloat pFactor) {
    if (pFactor instanceof CFloatImpl myFactor) {
      return new CFloatImpl(delegate.multiply(myFactor.delegate));
    }
    throw new UnsupportedOperationException();
  }

  @Override
  public CFloat multiply(CFloat... pFactors) {
    Format p = new Format(2, 1); // the smallest supported format
    for (CFloat f : pFactors) {
      CFloatImpl mf = (CFloatImpl) f;
      p = p.sup(mf.delegate.getFormat());
    }
    FloatValue r = delegate.withPrecision(p);
    for (CFloat f : pFactors) {
      CFloatImpl mf = (CFloatImpl) f;
      r = r.multiply(mf.delegate.withPrecision(p));
    }
    return new CFloatImpl(r);
  }

  @Override
  public CFloat subtract(CFloat pSubtrahend) {
    if (pSubtrahend instanceof CFloatImpl mySubtrahend) {
      return new CFloatImpl(delegate.subtract(mySubtrahend.delegate));
    }
    throw new UnsupportedOperationException();
  }

  @Override
  public CFloat divideBy(CFloat pDivisor) {
    if (pDivisor instanceof CFloatImpl myDivisor) {
      return new CFloatImpl(delegate.divide(myDivisor.delegate));
    }
    throw new UnsupportedOperationException();
  }

  @Override
  public CFloat ln() {
    return new CFloatImpl(delegate.ln());
  }

  /** See {@link FloatValue#lnWithStats} */
  CFloat lnWithStats(Map<Integer, Integer> lnStats) {
    return new CFloatImpl(delegate.lnWithStats(lnStats));
  }

  @Override
  public CFloat exp() {
    return new CFloatImpl(delegate.exp());
  }

  /** See {@link FloatValue#expWithStats} */
  CFloat expWithStats(Map<Integer, Integer> expStats) {
    return new CFloatImpl(delegate.expWithStats(expStats));
  }

  @Override
  public CFloat powTo(CFloat pExponent) {
    if (pExponent instanceof CFloatImpl myExponent) {
      return new CFloatImpl(delegate.pow(myExponent.delegate));
    }
    throw new UnsupportedOperationException();
  }

  /** See {@link FloatValue#powWithStats} */
  CFloat powToWithStats(CFloat pExponent, Map<Integer, Integer> powStats) {
    if (pExponent instanceof CFloatImpl myExponent) {
      return new CFloatImpl(delegate.powWithStats(myExponent.delegate, powStats));
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
      case CHAR -> delegate.byteValue();
      case SHORT -> delegate.shortValue();
      case INT -> delegate.intValue();
      case LONG -> delegate.longValue();
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

  public FloatValue getValue() {
    return delegate;
  }

  @Override
  public int getType() {
    if (Format.Float16.equals(delegate.getFormat())) {
      return CNativeType.HALF.getOrdinal();
    } else if (Format.Float32.equals(delegate.getFormat())) {
      return CNativeType.SINGLE.getOrdinal();
    } else if (Format.Float64.equals(delegate.getFormat())) {
      return CNativeType.DOUBLE.getOrdinal();
    } else if (Format.Extended.equals(delegate.getFormat())) {
      return CNativeType.LONG_DOUBLE.getOrdinal();
    } else {
      throw new IllegalStateException();
    }
  }

  @Override
  public boolean equalTo(CFloat other) {
    if (other instanceof CFloatImpl that) {
      return delegate.equalTo(that.delegate);
    }
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean notEqualTo(CFloat other) {
    if (other instanceof CFloatImpl that) {
      return delegate.notEqualTo(that.delegate);
    }
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean greaterThan(CFloat other) {
    if (other instanceof CFloatImpl myOther) {
      return delegate.greaterThan(myOther.delegate);
    }
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean greaterOrEqual(CFloat other) {
    if (other instanceof CFloatImpl that) {
      return delegate.greaterOrEqual(that.delegate);
    }
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean lessThan(CFloat other) {
    if (other instanceof CFloatImpl that) {
      return delegate.lessThan(that.delegate);
    }
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean lessOrEqual(CFloat other) {
    if (other instanceof CFloatImpl that) {
      return delegate.lessOrEqual(that.delegate);
    }
    throw new UnsupportedOperationException();
  }

  @Override
  public String toString() {
    return delegate.toString();
  }
}
