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
import org.sosy_lab.cpachecker.util.floatingpoint.CFloatNativeAPI.CFloatType;
import org.sosy_lab.cpachecker.util.floatingpoint.CFloatNativeAPI.CIntegerType;
import org.sosy_lab.cpachecker.util.floatingpoint.FloatValue.Format;

/**
 * MPFR based implementation of the {@link CFloat} interface.
 *
 * <p>Uses the BigFloat class from mpfr-java to call MPFR through a JNI interface. MPFR provides
 * arbitrary precision floating point operations with correct rounding.
 */
class MpfrFloat extends CFloat {
  private final CFloatWrapper wrapper;

  private final BinaryMathContext format;
  private final BigFloat value;

  public static final BinaryMathContext BINARY_EXTENDED = new BinaryMathContext(64, 15);

  public MpfrFloat(BigFloat pValue, BinaryMathContext pMathContext) {
    format = pMathContext;
    value = pValue;
    wrapper = fromBigFloat(value);
  }

  public MpfrFloat(BigFloat pValue, Format pFormat) {
    format = new BinaryMathContext(pFormat.sigBits() + 1, pFormat.expBits());
    value = pValue;
    wrapper = fromBigFloat(value);
  }

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
    BigInteger mantissa = new BigInteger(Long.toUnsignedString(mantissaBits));

    // Check whether the value is "normal" (= not 0, Inf or NaN) and add the missing 1 to the
    // mantissa. This step is skipped for "extended precision" values as the format does not use a
    // hidden bit.
    if (!format.equals(BINARY_EXTENDED) && exponentBits != 0 && exponentBits != exponentMask) {
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
    return new CFloatWrapper(signBit | exponentBits, mantissa.longValue());
  }

  private BigFloat parseBigFloat(String repr) {
    if ("nan".equals(repr)) {
      return BigFloat.NaN(format.precision);
    } else if ("-inf".equals(repr)) {
      return BigFloat.negativeInfinity(format.precision);
    } else if ("inf".equals(repr)) {
      return BigFloat.positiveInfinity(format.precision);
    } else {
      return new BigFloat(repr, format);
    }
  }

  @Override
  public String toString() {
    if (isNan()) {
      return isNegative() ? "-nan" : "nan";
    } else if (isInfinity()) {
      return isNegative() ? "-inf" : "inf";
    } else {
      return value.toString().replace(",", ".");
    }
  }

  @Override
  public CFloat add(CFloat pSummand) {
    return new MpfrFloat(value.add(toBigFloat(pSummand.getWrapper()), format), format);
  }

  @Override
  public CFloat add(CFloat... pSummands) {
    BigFloat result = value;
    for (CFloat f : pSummands) {
      result = result.add(toBigFloat(f.getWrapper()), format);
    }
    return new MpfrFloat(result, format);
  }

  @Override
  public CFloat multiply(CFloat pFactor) {
    return new MpfrFloat(value.multiply(toBigFloat(pFactor.getWrapper()), format), format);
  }

  @Override
  public CFloat multiply(CFloat... pFactor) {
    BigFloat result = value;
    for (CFloat f : pFactor) {
      result = result.multiply(toBigFloat(f.getWrapper()), format);
    }
    return new MpfrFloat(result, format);
  }

  @Override
  public CFloat subtract(CFloat pSubtrahend) {
    return new MpfrFloat(value.subtract(toBigFloat(pSubtrahend.getWrapper()), format), format);
  }

  @Override
  public CFloat divideBy(CFloat pDivisor) {
    return new MpfrFloat(value.divide(toBigFloat(pDivisor.getWrapper()), format), format);
  }

  @Override
  public CFloat ln() {
    return new MpfrFloat(value.log(format), format);
  }

  @Override
  public CFloat exp() {
    return new MpfrFloat(value.exp(format), format);
  }

  @Override
  public CFloat powTo(CFloat exponent) {
    return new MpfrFloat(value.pow(toBigFloat(exponent.getWrapper()), format), format);
  }

  @Override
  public CFloat powToIntegral(int exponent) {
    return new MpfrFloat(value.pow(parseBigFloat(String.valueOf(exponent)), format), format);
  }

  @Override
  public CFloat sqrt() {
    return new MpfrFloat(value.sqrt(format), format);
  }

  @Override
  public CFloat round() {
    if (isNan()) {
      return this.abs();
    }
    BigFloat posValue = value.abs();
    BigFloat above = posValue.rint(format.withRoundingMode(RoundingMode.CEILING));
    BigFloat below = posValue.rint(format.withRoundingMode(RoundingMode.FLOOR));
    BigFloat half = new BigFloat("0.5", format);
    BigFloat tie = above.multiply(half, format).add(below.multiply(half, format), format);

    BigFloat rounded = posValue.greaterThanOrEqualTo(tie) ? above : below;
    return new MpfrFloat(value.sign() ? rounded.negate() : rounded, format);
  }

  @Override
  public CFloat trunc() {
    BinaryMathContext toTrunc =
        new BinaryMathContext(sizeSignificand(), sizeExponent(), RoundingMode.DOWN);
    return new MpfrFloat(value.rint(toTrunc), format);
  }

  @Override
  public CFloat ceil() {
    BinaryMathContext toCeil =
        new BinaryMathContext(sizeSignificand(), sizeExponent(), RoundingMode.CEILING);
    return new MpfrFloat(value.rint(toCeil), format);
  }

  @Override
  public CFloat floor() {
    BinaryMathContext toFloor =
        new BinaryMathContext(sizeSignificand(), sizeExponent(), RoundingMode.FLOOR);
    return new MpfrFloat(value.rint(toFloor), format);
  }

  @Override
  public CFloat abs() {
    return new MpfrFloat(value.abs(), format);
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
    return new MpfrFloat(negative ? value.abs().negate() : value.abs(), format);
  }

  @Override
  public CFloat castTo(CFloatType toType) {
    BinaryMathContext ldouble = new BinaryMathContext(64, 15);
    return switch (toType) {
      case SINGLE ->
          new MpfrFloat(value.round(BinaryMathContext.BINARY32), BinaryMathContext.BINARY32);
      case DOUBLE ->
          new MpfrFloat(value.round(BinaryMathContext.BINARY64), BinaryMathContext.BINARY64);
      case LONG_DOUBLE -> new MpfrFloat(value.round(ldouble), ldouble);
      default -> throw new IllegalArgumentException();
    };
  }

  @Override
  public Number castToOther(CIntegerType toType) {
    return switch (toType) {
      case CHAR -> value.byteValue();
      case SHORT -> value.shortValue();
      case INT -> value.intValue();
      case LONG -> value.longValue();
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
  public CFloatType getType() {
    if (format.equals(BinaryMathContext.BINARY32)) {
      return CFloatType.SINGLE;
    } else if (format.equals(BinaryMathContext.BINARY64)) {
      return CFloatType.DOUBLE;
    } else if (format.equals(new BinaryMathContext(64, 15))) {
      return CFloatType.LONG_DOUBLE;
    } else {
      throw new IllegalArgumentException();
    }
  }

  @Override
  public boolean equalTo(CFloat other) {
    BigFloat that = toBigFloat(other.getWrapper());
    return value.equalTo(that);
  }

  @Override
  public boolean notEqualTo(CFloat other) {
    BigFloat that = toBigFloat(other.getWrapper());
    if (value.isNaN() || that.isNaN()) {
      return false;
    }
    return value.notEqualTo(that);
  }

  public BigFloat toBigFloat() {
    return value;
  }

  @Override
  public boolean greaterThan(CFloat other) {
    BigFloat that = toBigFloat(other.getWrapper());
    return value.greaterThan(that);
  }

  @Override
  public boolean greaterOrEqual(CFloat other) {
    BigFloat that = toBigFloat(other.getWrapper());
    return value.greaterThanOrEqualTo(that);
  }

  @Override
  public boolean lessThan(CFloat other) {
    BigFloat that = toBigFloat(other.getWrapper());
    return value.lessThan(that);
  }

  @Override
  public boolean lessOrEqual(CFloat other) {
    BigFloat that = toBigFloat(other.getWrapper());
    return value.lessThanOrEqualTo(that);
  }
}
