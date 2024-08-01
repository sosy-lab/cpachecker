// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.floatingpoint;

import java.math.BigInteger;
import java.util.Map;
import org.sosy_lab.cpachecker.util.floatingpoint.CFloatNativeAPI.CFloatType;
import org.sosy_lab.cpachecker.util.floatingpoint.CFloatNativeAPI.CIntegerType;
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

  public CFloatImpl(CFloatWrapper pWrapper, CFloatType pType) {
    wrapper = pWrapper;
    delegate = fromWrapper(pWrapper, pType);
  }

  public CFloatImpl(String repr, CFloatType pType) {
    delegate = parseFloat(repr, toFormat(pType));
    wrapper = toWrapper(delegate);
  }

  public CFloatImpl(String repr, Format pFormat) {
    delegate = parseFloat(repr, pFormat);
    wrapper = toWrapper(delegate);
  }

  public CFloatImpl(FloatValue pValue) {
    delegate = pValue;
    wrapper = toWrapper(pValue);
  }

  private static Format toFormat(CFloatType pType) {
    return switch (pType) {
      case SINGLE -> Format.Float32;
      case DOUBLE -> Format.Float64;
      case LONG_DOUBLE -> Format.Extended;
      default -> throw new UnsupportedOperationException();
    };
  }

  private static FloatValue fromWrapper(CFloatWrapper floatWrapper, CFloatType pType) {
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
    if (pType != CFloatType.LONG_DOUBLE) { // Extended precision has no hidden bit
      if (exponentBits != 0 && exponentBits != exponentMask) {
        BigInteger leadingOne = BigInteger.ONE.shiftLeft(format.sigBits());
        mantissa = mantissa.add(leadingOne);
      }
    }
    return new FloatValue(format, sign, exponent, mantissa);
  }

  private CFloatWrapper toWrapper(FloatValue floatValue) {
    Format format = floatValue.getFormat();
    if (Format.Float8.equals(format)
        || Format.Float16.equals(format)
        || Format.Float32.equals(format)
        || Format.Float64.equals(format)
        || Format.Extended.equals(format)) {
      long signBit = floatValue.isNegative() ? getSignBitMask() : 0;
      long exponent = floatValue.getExponent() + floatValue.getFormat().bias();
      BigInteger mantissa = floatValue.getSignificand();
      if (!format.equals(Format.Extended)) {
        mantissa = mantissa.clearBit(format.sigBits());
      }
      return new CFloatWrapper(signBit | exponent, mantissa.longValue());
    } else {
      throw new IllegalArgumentException();
    }
  }

  private static FloatValue parseFloat(String repr, Format format) {
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
      p = p.matchWith(mf.delegate.getFormat());
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
      p = p.matchWith(mf.delegate.getFormat());
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
    return new CFloatImpl(delegate.round(RoundingMode.NEAREST_AWAY));
  }

  @Override
  public CFloat trunc() {
    return new CFloatImpl(delegate.round(RoundingMode.TRUNCATE));
  }

  @Override
  public CFloat ceil() {
    return new CFloatImpl(delegate.round(RoundingMode.CEILING));
  }

  @Override
  public CFloat floor() {
    return new CFloatImpl(delegate.round(RoundingMode.FLOOR));
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
  public CFloat castTo(CFloatType toType) {
    return switch (toType) {
      case SINGLE -> new CFloatImpl(delegate.withPrecision(Format.Float32));
      case DOUBLE -> new CFloatImpl(delegate.withPrecision(Format.Float64));
      case LONG_DOUBLE -> new CFloatImpl(delegate.withPrecision(Format.Extended));
      default -> throw new IllegalArgumentException();
    };
  }

  @Override
  public Number castToOther(CIntegerType toType) {
    return switch (toType) {
      case CHAR -> delegate.byteValue();
      case SHORT -> delegate.shortValue();
      case INT -> delegate.intValue();
      case LONG -> delegate.longValue();
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
  public CFloatType getType() {
    Format format = delegate.getFormat();
    if (Format.Float8.equals(format)
        || Format.Float16.equals(format)
        || Format.Float32.equals(format)) {
      // We return "single-precision" format for Float8 and Float16
      // FIXME: This may cause issues with subnormal numbers
      return CFloatType.SINGLE;
    } else if (Format.Float64.equals(format)) {
      return CFloatType.DOUBLE;
    } else if (Format.Extended.equals(format)) {
      return CFloatType.LONG_DOUBLE;
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
