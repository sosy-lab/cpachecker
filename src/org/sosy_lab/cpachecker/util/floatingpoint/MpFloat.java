// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.floatingpoint;

import java.math.BigInteger;
import java.math.RoundingMode;
import org.kframework.mpfr.BigFloat;
import org.kframework.mpfr.BinaryMathContext;
import org.sosy_lab.common.NativeLibraries;
import org.sosy_lab.cpachecker.util.floatingpoint.CFloatNativeAPI.CNativeType;

public class MpFloat extends CFloat {
  static {
    NativeLibraries.loadLibrary("mpfr_java");
  }

  private final CFloatWrapper wrapper;

  private final BinaryMathContext format;
  private final BigFloat value;

  public MpFloat(BigFloat pValue, BinaryMathContext pMathContext) {
    format = pMathContext;
    value = pValue;
    wrapper = fromBigFloat(value);
  }

  public MpFloat(String repr, int floatType) {
    format = toBinaryMathContext(floatType);
    value = parseBigFloat(repr);
    wrapper = fromBigFloat(value);
  }

  public MpFloat(CFloatWrapper pWrapper, int floatType) {
    format = toBinaryMathContext(floatType);
    value = toBigFloat(pWrapper);
    wrapper = pWrapper;
  }

  private BinaryMathContext toBinaryMathContext(int floatType) {
    if (floatType == CNativeType.SINGLE.getOrdinal()) {
      return BinaryMathContext.BINARY32;
    }
    if (floatType == CNativeType.DOUBLE.getOrdinal()) {
      return BinaryMathContext.BINARY64;
    }
    if (floatType == CNativeType.LONG_LONG.getOrdinal()) {
      return new BinaryMathContext(64, 15);
    }
    throw new IllegalArgumentException();
  }

  // TODO: Refactor and move the next 3 methods beck into CFloat (or maybe even FloatingPointNumber)
  /** Returns the number of bits in the exponent. */
  public int sizeExponent() {
    return Long.numberOfTrailingZeros(format.maxExponent + 1) + 1;
  }

  /** Returns the bias of the exponent. This is needed to convert from and to the IEEE format. */
  public int biasExponent() {
    return (1 << (sizeExponent() - 1)) - 1;
  }

  /** Returns the number of bits in the significand. */
  public int sizeSignificand() {
    return format.precision;
  }

  private BigFloat toBigFloat(CFloatWrapper floatWrapper) {
    long signMask = 1L << sizeExponent();
    long exponentMask = signMask - 1;

    // Extract bits for sign, exponent and mantissa from the wrapper
    long signBit = floatWrapper.getExponent() & signMask;
    long exponentBits = floatWrapper.getExponent() & exponentMask;
    long mantissaBits = floatWrapper.getMantissa();

    // Shift the exponent and convert the values
    boolean sign = signBit != 0;
    long exponent = exponentBits - biasExponent();
    BigInteger mantissa = BigInteger.valueOf(mantissaBits);

    // Check that the value is "normal" (= not 0, Inf or NaN) and add the missing 1 to the mantissa
    if (exponentBits != 0 && exponentBits != exponentMask) {
      BigInteger leadingOne = BigInteger.ONE.shiftLeft(format.precision - 1);
      mantissa = mantissa.add(leadingOne);
    }

    return new BigFloat(sign, mantissa, exponent, format);
  }

  private CFloatWrapper fromBigFloat(BigFloat floatValue) {
    // TODO: This method should probably use mpfr_set_z_2exp (unfortunately not in BigFloat)
    long signBit = (floatValue.sign() ? 1L : 0) << sizeExponent();
    long exponentBits =
        floatValue.exponent(format.minExponent, format.maxExponent) + biasExponent();

    // We consider NaN the default value and in this case we set the mantissa to "10..."
    BigInteger mantissa = BigInteger.ONE.shiftLeft(format.precision - 2);

    // If the value is not NaN we get the actual mantissa.
    if (!floatValue.isNaN()) {
      mantissa = floatValue.significand(format.minExponent, format.maxExponent);
      // Delete the leading "1." if the number is normal (= not subnormal or zero), and the format
      // is not 'long double'
      if (exponentBits != 0 && !format.equals(new BinaryMathContext(64, 15))) {
        mantissa = mantissa.clearBit(format.precision - 1);
      }
    }
    return new CFloatWrapper(signBit | exponentBits, mantissa.longValueExact());
  }

  private BigFloat parseBigFloat(String repr) {
    if ("nan".equals(repr)) {
      return BigFloat.NaN(format.precision);
    }
    if ("-inf".equals(repr)) {
      return BigFloat.negativeInfinity(format.precision);
    }
    if ("inf".equals(repr)) {
      return BigFloat.positiveInfinity(format.precision);
    }
    return new BigFloat(repr, format);
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
    return value.toString("%.6Re").replace(",", ".");
  }

  @Override
  public CFloat add(CFloat pSummand) {
    return new MpFloat(value.add(toBigFloat(pSummand.getWrapper()), format), format);
  }

  @Override
  public CFloat add(CFloat... pSummands) {
    BigFloat result = value;
    for (CFloat f : pSummands) {
      result = result.add(toBigFloat(f.getWrapper()), format);
    }
    return new MpFloat(result, format);
  }

  @Override
  public CFloat multiply(CFloat pFactor) {
    return new MpFloat(value.multiply(toBigFloat(pFactor.getWrapper()), format), format);
  }

  @Override
  public CFloat multiply(CFloat... pFactor) {
    BigFloat result = value;
    for (CFloat f : pFactor) {
      result = result.multiply(toBigFloat(f.getWrapper()), format);
    }
    return new MpFloat(result, format);
  }

  @Override
  public CFloat subtract(CFloat pSubtrahend) {
    return new MpFloat(value.subtract(toBigFloat(pSubtrahend.getWrapper()), format), format);
  }

  @Override
  public CFloat divideBy(CFloat pDivisor) {
    return new MpFloat(value.divide(toBigFloat(pDivisor.getWrapper()), format), format);
  }

  @Override
  public CFloat ln() {
    return new MpFloat(value.log(format), format);
  }

  @Override
  public CFloat exp() {
    return new MpFloat(value.exp(format), format);
  }

  @Override
  public CFloat powTo(CFloat exponent) {
    return new MpFloat(value.pow(toBigFloat(exponent.getWrapper()), format), format);
  }

  @Override
  public CFloat powToIntegral(int exponent) {
    // FIXME: Not implement in BigFloat (..but MPFR has mpfr_pow_sj)
    throw new UnsupportedOperationException();
  }

  @Override
  public CFloat sqrt() {
    return new MpFloat(value.sqrt(format), format);
  }

  @Override
  public CFloat round() {
    BigFloat posValue = value.abs();
    BigFloat above = posValue.rint(format.withRoundingMode(RoundingMode.CEILING));
    BigFloat below = posValue.rint(format.withRoundingMode(RoundingMode.FLOOR));
    BigFloat tie = above.add(below, format).divide(new BigFloat(2, format), format);

    BigFloat rounded = posValue.greaterThanOrEqualTo(tie) ? above : below;
    return new MpFloat(value.sign() ? rounded.negate() : rounded, format);
  }

  @Override
  public CFloat trunc() {
    BinaryMathContext toTrunc =
        new BinaryMathContext(sizeSignificand(), sizeExponent(), RoundingMode.DOWN);
    return new MpFloat(value.rint(toTrunc), format);
  }

  @Override
  public CFloat ceil() {
    BinaryMathContext toCeil =
        new BinaryMathContext(sizeSignificand(), sizeExponent(), RoundingMode.CEILING);
    return new MpFloat(value.rint(toCeil), format);
  }

  @Override
  public CFloat floor() {
    BinaryMathContext toFloor =
        new BinaryMathContext(sizeSignificand(), sizeExponent(), RoundingMode.FLOOR);
    return new MpFloat(value.rint(toFloor), format);
  }

  @Override
  public CFloat abs() {
    return new MpFloat(value.abs(), format);
  }

  @Override
  public boolean isZero() {
    return value.isNegativeZero() || value.isPositiveZero();
  }

  @Override
  public boolean isOne() {
    return value.abs().compareTo(new BigFloat("1.0", format)) == 0;
  }

  @Override
  public boolean isNan() {
    return value.isNaN();
  }

  @Override
  public boolean isInfinity() {
    return value.isInfinite();
  }

  @Override
  public boolean isNegative() {
    return value.sign();
  }

  @Override
  public CFloat copySignFrom(CFloat source) {
    // MPFR actually has mpfr_copysign for this, but it seems to be missing from BigFloat
    boolean negative = toBigFloat(source.getWrapper()).sign();
    return new MpFloat(negative ? value.abs().negate() : value.abs(), format);
  }

  @Override
  public CFloat castTo(CNativeType toType) {
    BinaryMathContext ldouble = new BinaryMathContext(64, 15);
    return switch (toType) {
      case SINGLE ->
          new MpFloat(value.round(BinaryMathContext.BINARY32), BinaryMathContext.BINARY32);
      case DOUBLE ->
          new MpFloat(value.round(BinaryMathContext.BINARY64), BinaryMathContext.BINARY64);
      case LONG_DOUBLE -> new MpFloat(value.round(ldouble), ldouble);
      default -> throw new IllegalArgumentException();
    };
  }

  @Override
  public Number castToOther(CNativeType toType) {
    return switch (toType) {
      case CHAR -> value.byteValue();
      case SHORT -> value.shortValue();
      case INT -> value.intValue();
      case LONG -> value.longValue();
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
    if (format.equals(BinaryMathContext.BINARY32)) {
      return CNativeType.SINGLE.getOrdinal();
    }
    if (format.equals(BinaryMathContext.BINARY64)) {
      return CNativeType.DOUBLE.getOrdinal();
    }
    if (format.equals(new BinaryMathContext(64, 15))) {
      return CNativeType.LONG_DOUBLE.getOrdinal();
    }
    throw new IllegalArgumentException();
  }

  @Override
  public BigFloat toBigFloat() {
    return value;
  }

  @Override
  public boolean greaterThan(CFloat other) {
    BigFloat that = toBigFloat(other.getWrapper());
    if (value.isNaN() || that.isNaN()) {
      return false;
    }
    return value.compareTo(that) > 0;
  }
}
