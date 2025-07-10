// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.floatingpoint;

import com.google.common.collect.Multiset;
import java.math.BigInteger;
import java.util.Optional;
import org.sosy_lab.cpachecker.util.floatingpoint.CFloatNativeAPI.CFloatType;
import org.sosy_lab.cpachecker.util.floatingpoint.CFloatNativeAPI.CIntegerType;
import org.sosy_lab.cpachecker.util.floatingpoint.FloatValue.Format;
import org.sosy_lab.cpachecker.util.floatingpoint.FloatValue.RoundingMode;

/**
 * Adapter class for {@link FloatValue} that implements the CFloat interface.
 *
 * <p>This class is used to test {@link FloatValue} against other implementations.
 */
class CFloatImpl extends CFloat {
  private final CFloatWrapper wrapper;
  private final FloatValue delegate;

  public CFloatImpl(CFloatWrapper pWrapper, CFloatType pType) {
    wrapper = pWrapper;
    delegate = fromWrapper(pWrapper, pType);
  }

  public CFloatImpl(String repr, CFloatType pType) {
    delegate = FloatValue.fromString(toFormat(pType), repr);
    wrapper = toWrapper(delegate);
  }

  public CFloatImpl(String repr, Format pFormat) {
    delegate = FloatValue.fromString(pFormat, repr);
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
      case LONG_DOUBLE -> Format.Float80;
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
        || Format.Float80.equals(format)) {
      long signBit = floatValue.isNegative() ? getSignBitMask() : 0;
      long exponent = floatValue.getExponentWithoutBias() + floatValue.getFormat().bias();
      BigInteger mantissa = floatValue.getSignificandWithoutHiddenBit();
      if (!format.equals(Format.Float80)) {
        mantissa = mantissa.clearBit(format.sigBits());
      }
      return new CFloatWrapper(signBit | exponent, mantissa.longValue());
    } else {
      throw new IllegalArgumentException();
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
  public CFloat multiply(CFloat pFactor) {
    if (pFactor instanceof CFloatImpl myFactor) {
      return new CFloatImpl(delegate.multiply(myFactor.delegate));
    }
    throw new UnsupportedOperationException();
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
  public CFloat modulo(CFloat pDivisor) {
    if (pDivisor instanceof CFloatImpl myDivisor) {
      return new CFloatImpl(delegate.modulo(myDivisor.delegate));
    }
    throw new UnsupportedOperationException();
  }

  @Override
  public CFloat remainder(CFloat pDivisor) {
    if (pDivisor instanceof CFloatImpl myDivisor) {
      return new CFloatImpl(delegate.remainder(myDivisor.delegate));
    }
    throw new UnsupportedOperationException();
  }

  @Override
  public CFloat ln() {
    return new CFloatImpl(delegate.ln());
  }

  /** See {@link FloatValue#lnWithStats} */
  CFloat lnWithStats(Multiset<Integer> lnStats) {
    return new CFloatImpl(delegate.lnWithStats(lnStats));
  }

  @Override
  public CFloat exp() {
    return new CFloatImpl(delegate.exp());
  }

  /** See {@link FloatValue#expWithStats} */
  CFloat expWithStats(Multiset<Integer> expStats) {
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
  CFloat powToWithStats(CFloat pExponent, Multiset<Integer> powStats) {
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
      return new CFloatImpl(delegate.copySign(mySource.delegate));
    }
    throw new UnsupportedOperationException();
  }

  @Override
  public CFloat castTo(CFloatType toType) {
    return switch (toType) {
      case SINGLE -> new CFloatImpl(delegate.withPrecision(Format.Float32));
      case DOUBLE -> new CFloatImpl(delegate.withPrecision(Format.Float64));
      case LONG_DOUBLE -> new CFloatImpl(delegate.withPrecision(Format.Float80));
    };
  }

  @Override
  public Optional<Number> castToOther(CIntegerType toType) {
    return switch (toType) {
      case INT ->
          delegate.toInt().isPresent()
              ? Optional.of(delegate.toInt().orElseThrow())
              : Optional.empty();
      case LONG ->
          delegate.toLong().isPresent()
              ? Optional.of(delegate.toLong().orElseThrow())
              : Optional.empty();
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
    } else if (Format.Float80.equals(format)) {
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
  public boolean lessOrGreater(CFloat other) {
    if (other instanceof CFloatImpl that) {
      return delegate.lessOrGreater(that.delegate);
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
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    return other instanceof CFloatImpl that && delegate.equals(that.delegate);
  }

  @Override
  public int hashCode() {
    return delegate.hashCode();
  }

  @Override
  public int compareTo(CFloat other) {
    // FIXME: We only allow comparisons with other CFloatImpl objects
    if (other instanceof CFloatImpl that) {
      return delegate.compareTo(that.delegate);
    }
    throw new UnsupportedOperationException();
  }

  @Override
  public String toString() {
    return delegate.toString();
  }
}
