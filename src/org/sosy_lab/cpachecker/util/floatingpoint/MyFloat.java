// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.floatingpoint;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.kframework.mpfr.BigFloat;
import org.kframework.mpfr.BinaryMathContext;
import org.sosy_lab.common.NativeLibraries;

public class MyFloat {
  static {
    NativeLibraries.loadLibrary("mpfr_java");
  }

  // Defines the width of the significand and the exponent range
  public static class Format {
    private final Integer expBits;
    private final Integer sigBits;

    public Format(int pExpBits, int pSigBits) {
      expBits = pExpBits;
      sigBits = pSigBits;
    }

    public static final Format Float16 = new Format(5, 10);
    public static final Format Float32 = new Format(8, 23);
    public static final Format Float64 = new Format(11, 52);
    public static final Format Float128 = new Format(15, 112);
    public static final Format Float256 = new Format(19, 236);

    public Integer getExpBits() {
      return expBits;
    }

    public Integer getSigBits() {
      return sigBits;
    }

    @Override
    public boolean equals(Object other) {
      if (this == other) {
        return true;
      }
      return other instanceof Format that
          && expBits.equals(that.expBits)
          && sigBits.equals(that.sigBits);
    }

    @Override
    public int hashCode() {
      return Objects.hash(expBits, sigBits);
    }

    public long bias() {
      return (1L << (expBits - 1)) - 1;
    }

    public long minExp() {
      long rawExp = 1;
      return rawExp - bias();
    }

    public long maxExp() {
      long rawExp = (1L << expBits) - 2;
      return rawExp - bias();
    }

    // Returns the next bigger format meant to be used for intermediate results.
    public Format extended() {
      // TODO: Add support for arbitrary sizes
      if (equals(new Format(3, 4))) {
        return Float16;
      }
      if (equals(Format.Float16)) {
        return Float32;
      }
      if (equals(Format.Float32)) {
        return Float64;
      }
      if (equals(Format.Float64)) {
        return Float128;
      }
      if (equals(Format.Float128)) {
        return Float256;
      }
      throw new IllegalStateException();
    }
  }

  // Stores a floating point value
  public static class FpValue {
    private final boolean sign;
    private final long exponent;
    private final BigInteger significand;

    /**
     * Build a new floating point value
     *
     * @param pSign Sign of the float
     * @param pExponent Exponent, without the IEEE bias
     * @param pSignificand Significand, including the leading bit that is hidden in the IEEE format
     */
    public FpValue(boolean pSign, long pExponent, BigInteger pSignificand) {
      sign = pSign;
      exponent = pExponent;
      significand = pSignificand;
    }

    @Override
    public boolean equals(Object pOther) {
      if (this == pOther) {
        return true;
      }
      return pOther instanceof FpValue other
          && sign == other.sign
          && exponent == other.exponent
          && significand.equals(other.significand);
    }

    @Override
    public int hashCode() {
      return Objects.hash(sign, exponent, significand);
    }
  }

  private final Format format;
  private final FpValue value;

  public MyFloat(Format pFormat, FpValue pValue) {
    format = pFormat;
    value = pValue;
  }

  public MyFloat(Format pFormat, BigInteger pValue) {
    format = pFormat;
    value = fromInteger(pValue);
  }

  public MyFloat(Format pFormat, boolean pSign, long pExponent, BigInteger pSignificand) {
    format = pFormat;
    value = new FpValue(pSign, pExponent, pSignificand);
  }

  public Format getFormat() {
    return format;
  }

  public static MyFloat nan(Format pFormat) {
    FpValue value =
        new FpValue(false, pFormat.maxExp() + 1, BigInteger.ONE.shiftLeft(pFormat.sigBits - 1));
    return new MyFloat(pFormat, value);
  }

  public static MyFloat infinity(Format pFormat) {
    FpValue value = new FpValue(false, pFormat.maxExp() + 1, BigInteger.ZERO);
    return new MyFloat(pFormat, value);
  }

  public static MyFloat maxValue(Format pFormat) {
    BigInteger allOnes = BigInteger.ONE.shiftLeft(pFormat.sigBits + 1).subtract(BigInteger.ONE);
    FpValue value = new FpValue(false, pFormat.maxExp(), allOnes);
    return new MyFloat(pFormat, value);
  }

  public static MyFloat constant(Format pFormat, BigInteger number) {
    return new MyFloat(pFormat, number);
  }

  public static MyFloat constant(Format pFormat, int number) {
    return new MyFloat(pFormat, BigInteger.valueOf(number));
  }

  public static MyFloat one(Format pFormat) {
    FpValue value = new FpValue(false, 0, BigInteger.ONE.shiftLeft(pFormat.sigBits));
    return new MyFloat(pFormat, value);
  }

  public static MyFloat minNormal(Format pFormat) {
    FpValue value = new FpValue(false, pFormat.minExp(), BigInteger.ONE.shiftLeft(pFormat.sigBits));
    return new MyFloat(pFormat, value);
  }

  public static MyFloat minValue(Format pFormat) {
    FpValue value = new FpValue(false, pFormat.minExp() - 1, BigInteger.ONE);
    return new MyFloat(pFormat, value);
  }

  public static MyFloat zero(Format pFormat) {
    FpValue value = new FpValue(false, pFormat.minExp() - 1, BigInteger.ZERO);
    return new MyFloat(pFormat, value);
  }

  public static MyFloat negativeZero(Format pFormat) {
    FpValue value = new FpValue(true, pFormat.minExp() - 1, BigInteger.ZERO);
    return new MyFloat(pFormat, value);
  }

  public static MyFloat negativeOne(Format pFormat) {
    FpValue value = new FpValue(true, 0, BigInteger.ONE.shiftLeft(pFormat.sigBits));
    return new MyFloat(pFormat, value);
  }

  public static MyFloat negativeInfinity(Format pFormat) {
    FpValue value = new FpValue(true, pFormat.maxExp() + 1, BigInteger.ZERO);
    return new MyFloat(pFormat, value);
  }

  public boolean isNan() {
    return !value.sign
        && (value.exponent == format.maxExp() + 1)
        && (value.significand.compareTo(BigInteger.ZERO) > 0);
  }

  public boolean isInfinite() {
    return (value.exponent == format.maxExp() + 1) && value.significand.equals(BigInteger.ZERO);
  }

  public boolean isZero() {
    boolean b1 = value.exponent == format.minExp() - 1;
    boolean b2 = value.significand.equals(BigInteger.ZERO);
    return b1
        && b2; // (value.exponent == format.minExp() - 1) &&
               // value.significand.equals(BigInteger.ZERO);
  }

  public boolean isOne() {
    return one(format).equals(this);
  }

  public boolean isNegativeOne() {
    return negativeOne(format).equals(this);
  }

  public boolean isNegative() {
    return value.sign;
  }

  public boolean isInteger() {
    BigInteger intValue = toInteger();
    return value.equals(fromInteger(intValue));
  }

  public boolean isOddInteger() {
    BigInteger intValue = toInteger();
    return value.equals(fromInteger(intValue)) && intValue.testBit(0);
  }

  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    return pOther instanceof MyFloat other
        && format.equals(other.format)
        && value.equals(other.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(format, value);
  }

  // Shift the significand to the right while preserving the sticky bit
  private BigInteger truncate(BigInteger significand, int bits) {
    BigInteger mask = BigInteger.ONE.shiftLeft(bits).subtract(BigInteger.ONE);
    BigInteger r = significand.and(mask);
    BigInteger l = significand.shiftRight(bits);
    return r.equals(BigInteger.ZERO) ? l : l.setBit(0);
  }

  public enum RoundingMode {
    NEAREST_AWAY, // Round to nearest, ties away from zero
    NEAREST_EVEN, // Round to nearest, ties to even
    CEILING, // Round toward +∞
    FLOOR, // Round toward -∞
    TRUNCATE // Round toward 0
  }

  // Round the significand
  // For internal use only. We expect the significand to be followed by 3 grs bits
  private BigInteger applyRounding(RoundingMode rm, boolean negative, BigInteger significand) {
    long grs = significand.and(new BigInteger("111", 2)).longValue();
    significand = significand.shiftRight(3);
    BigInteger plusOne = significand.add(BigInteger.ONE);
    return switch (rm) {
      case NEAREST_AWAY -> (grs >= 4) ? plusOne : significand;
      case NEAREST_EVEN ->
          ((grs == 4 && significand.testBit(0)) || grs > 4) ? plusOne : significand;
      case CEILING -> (grs > 0 && !negative) ? plusOne : significand;
      case FLOOR -> (grs > 0 && negative) ? plusOne : significand;
      case TRUNCATE -> significand;
    };
  }

  // Copy the value with a new exponent
  private MyFloat withExponent(long pExponent) {
    return new MyFloat(format, value.sign, pExponent, value.significand);
  }

  // Copy the value with a new sign
  private MyFloat withSign(boolean pSign) {
    return new MyFloat(format, pSign, value.exponent, value.significand);
  }

  // Convert the value to a different precision (uses round to nearest, ties to even for now)
  public MyFloat withPrecision(Format targetFormat) {
    if (format.equals(targetFormat)) {
      return this;
    }
    if (isNan()) {
      return nan(targetFormat);
    }
    if (isInfinite()) {
      return value.sign ? negativeInfinity(targetFormat) : infinity(targetFormat);
    }
    if (isZero()) {
      return value.sign ? negativeZero(targetFormat) : zero(targetFormat);
    }

    long exponent = Math.max(value.exponent, format.minExp());
    BigInteger significand = value.significand;

    // Normalization
    // If the number is subnormal shift it upward and adjust the exponent
    int shift = (format.sigBits + 1) - significand.bitLength();
    if (shift > 0) {
      significand = significand.shiftLeft(shift);
      exponent -= shift;
    }

    // Return infinity if the exponent is too large for the new encoding
    if (exponent > targetFormat.maxExp()) {
      return value.sign ? negativeInfinity(targetFormat) : infinity(targetFormat);
    }
    // Return zero if the exponent is below the subnormal range
    if (exponent < targetFormat.minExp() - (targetFormat.sigBits + 1)) {
      return value.sign ? negativeZero(targetFormat) : zero(targetFormat);
    }

    // Extend the significand with 3 grs bits
    significand = significand.shiftLeft(targetFormat.sigBits + 3);

    // Use the lowest possible exponent and move the rest into the significand by shifting
    // it to the right.
    int leading = 0;
    if (exponent < targetFormat.minExp()) {
      leading = (int) Math.abs(targetFormat.minExp() - exponent);
      exponent = targetFormat.minExp() - 1;
    }

    // Truncate the value and round the result
    significand = truncate(significand, format.sigBits + leading);
    significand = applyRounding(RoundingMode.NEAREST_EVEN, value.sign, significand);

    // Normalize if rounding caused an overflow
    if (significand.testBit(targetFormat.sigBits + 1)) {
      significand = significand.shiftRight(1); // The last bit is zero
      exponent += 1;
    }

    // Return infinity if this caused the exponent to leave the range
    if (exponent > targetFormat.maxExp()) {
      return value.sign ? negativeInfinity(targetFormat) : infinity(targetFormat);
    }
    return new MyFloat(targetFormat, value.sign, exponent, significand);
  }

  public MyFloat abs() {
    return new MyFloat(format, false, value.exponent, value.significand);
  }

  public MyFloat negate() {
    if (isNan()) {
      return nan(format);
    }
    return new MyFloat(format, new FpValue(!value.sign, value.exponent, value.significand));
  }

  public boolean greaterThan(MyFloat number) {
    if (this.isNan() || number.isNan()) {
      return false;
    }
    if (this.isInfinite() && !this.isNegative()) {
      // inf > x = true, unless x=inf
      if (number.isInfinite() && !number.isNegative()) {
        return false;
      }
      return true;
    }
    if (this.isInfinite() && this.isNegative() && number.isInfinite() && number.isNegative()) {
      // -inf > x = true, unless x=-inf
      return false;
    }
    MyFloat r = this.subtract(number);
    if (r.isZero()) {
      return false;
    }
    return !r.isNegative();
  }

  public MyFloat add(MyFloat number) {
    // Make sure the first argument has the larger (or equal) exponent
    MyFloat a = number;
    MyFloat b = this;
    if (value.exponent >= number.value.exponent) {
      a = this;
      b = number;
    }

    // Handle special cases:
    // (1) Either argument is NaN
    if (a.isNan() || b.isNan()) {
      return nan(format);
    }
    // (2) Both arguments are infinite
    if (a.isInfinite() && b.isInfinite()) {
      if (a.isNegative() && b.isNegative()) {
        return negativeInfinity(format);
      }
      if (!a.isNegative() && !b.isNegative()) {
        return infinity(format);
      }
      return nan(format);
    }
    // (3) Only one argument is infinite
    if (a.isInfinite()) { // No need to check m as it can't be larger, and one of the args is finite
      return a;
    }
    // (4) Both arguments are zero (or negative zero)
    if (a.isZero() && b.isZero()) {
      return (a.isNegative() && b.isNegative()) ? negativeZero(format) : zero(format);
    }
    // (5) Only one of the arguments is zero (or negative zero)
    if (a.isZero() || b.isZero()) {
      return a.isZero() ? b : a;
    }

    // Get the exponents without the IEEE bias. Note that for subnormal numbers the stored exponent
    // needs to be increased by one.
    long exponent1 = Math.max(a.value.exponent, format.minExp());
    long exponent2 = Math.max(b.value.exponent, format.minExp());

    // Calculate the difference between the exponents. If it is larger than the mantissa size we can
    // skip the add and return immediately.
    int exp_diff = (int) (exponent1 - exponent2);
    if (exp_diff >= format.sigBits + 3) {
      return a;
    }

    // Get the significands and apply the sign
    BigInteger significand1 = a.value.sign ? a.value.significand.negate() : a.value.significand;
    BigInteger significand2 = b.value.sign ? b.value.significand.negate() : b.value.significand;

    // Expand the significand with (empty) guard, round and sticky bits
    significand1 = significand1.shiftLeft(3);
    significand2 = significand2.shiftLeft(3);

    // Shift the number with the smaller exponent to the exponent of the other number.
    significand2 = truncate(significand2, exp_diff);

    // Add the two significands
    BigInteger result = significand1.add(significand2);

    // Extract the sign and value of the significand from the result
    boolean sign_ = result.signum() < 0;
    BigInteger significand_ = result.abs();

    // The result has the same exponent as the larger of the two arguments
    long exponent_ = exponent1;

    // Normalize
    // (1) Significand is too large: shift to the right by one bit
    //     This can happen if two numbers with equal exponent are added:
    //     f.ex 1.0e3 + 1.0e3 = 10.0e3
    //     (here we normalize the result to 1.0e4)
    if (significand_.testBit(format.sigBits + 4)) {
      significand_ = truncate(significand_, 1);
      exponent_ += 1;
    }

    // (2) Significand is too small: shift left unless the number is subnormal
    //     This can happen if digits have canceled out:
    //     f.ex 1.01001e2 + (-1.01e2) = 0.00001e2
    //     (here we normalize to 1.0e-3)
    int leading = (format.sigBits + 4) - significand_.bitLength();
    int maxValue = (int) (exponent_ - format.minExp()); // format.minExp() <= exponent
    if (leading > maxValue) {
      // If the exponent would get too small only shift to the left until the minimal exponent is
      // reached and return a subnormal number.
      significand_ = significand_.shiftLeft(maxValue);
      exponent_ = format.minExp() - 1;
    } else {
      significand_ = significand_.shiftLeft(leading);
      exponent_ -= leading;
    }

    // Round the result according to the grs bits
    significand_ = applyRounding(RoundingMode.NEAREST_EVEN, sign_, significand_);

    // Shift the significand to the right if rounding has caused an overflow
    if (significand_.bitLength() > format.sigBits + 1) {
      significand_ = significand_.shiftRight(1); // The dropped bit is zero
      exponent_ += 1;
    }

    // Check if the result is zero
    if (significand_.equals(BigInteger.ZERO)) {
      return sign_ ? negativeZero(format) : zero(format);
    }
    // Return infinity if there is an overflow.
    if (exponent_ > format.bias()) {
      return sign_ ? negativeInfinity(format) : infinity(format);
    }

    // Otherwise return the number
    return new MyFloat(format, new FpValue(sign_, exponent_, significand_));
  }

  public MyFloat subtract(MyFloat number) {
    return add(number.negate());
  }

  public MyFloat multiply(MyFloat number) {
    // Make sure the first argument has the larger (or equal) exponent
    MyFloat a = number;
    MyFloat b = this;
    if (value.exponent >= number.value.exponent) {
      a = this;
      b = number;
    }

    // Handle special cases:
    // (1) Either argument is NaN
    if (a.isNan() || b.isNan()) {
      return nan(format);
    }
    // (2) One of the argument is infinite
    if (a.isInfinite()) { // No need to check m as it can't be larger, and one of the args is finite
      if (b.isZero()) {
        // Return NaN if we're trying to multiply infinity by zero
        return nan(format);
      }
      return (a.isNegative() ^ b.isNegative()) ? negativeInfinity(format) : infinity(format);
    }
    // (3) One of the arguments is zero (or negative zero)
    if (a.isZero() || b.isZero()) {
      return (a.isNegative() ^ b.isNegative()) ? negativeZero(format) : zero(format);
    }

    // Calculate the sign of the result
    boolean sign_ = value.sign ^ number.value.sign;

    // Get the exponents without the IEEE bias. Note that for subnormal numbers the stored exponent
    // needs to be increased by one.
    long exponent1 = Math.max(a.value.exponent, format.minExp());
    long exponent2 = Math.max(b.value.exponent, format.minExp());

    // Calculate the exponent of the result by adding the exponents of the two arguments.
    // If the calculated exponent is out of range we can return infinity (or zero) immediately.
    long exponent_ = exponent1 + exponent2;
    if (exponent_ > format.maxExp()) {
      return sign_ ? negativeInfinity(format) : infinity(format);
    }
    if (exponent_ < format.minExp() - format.sigBits - 2) {
      return sign_ ? negativeZero(format) : zero(format);
    }

    // Multiply the significands
    BigInteger significand1 = a.value.significand;
    BigInteger significand2 = b.value.significand;

    BigInteger result = significand1.multiply(significand2);

    // Add guard, round and sticky bits
    BigInteger significand_ = result.shiftLeft(3);

    // Normalize
    // (1) Significand is too large: shift to the right by one bit
    //     This can happen if two numbers with significand greater 1 are multiplied:
    //     f.ex 1.1e3 x 1.1e1 = 10.01e4
    //     (here we normalize the result to 1.001e5)
    if (significand_.testBit(2 * format.sigBits + 4)) {
      significand_ = significand_.shiftRight(1);
      exponent_ += 1;
    }

    // (2) Significand is too small: shift left unless the number is subnormal
    //     This can happen if one of the numbers was subnormal:
    //     f.ex 1.0e3 x 0.1e-1 = 0.10e2
    //     (here we normalize to 1.0e1)
    int shift = (2 * format.sigBits + 4) - significand_.bitLength();
    if (shift > 0) {
      significand_ = significand_.shiftLeft(shift);
      exponent_ -= shift;
    }

    // Otherwise use the lowest possible exponent and move the rest into the significand by shifting
    // it to the right. Here we calculate haw many digits we need to shift:
    int leading = 0;
    if (exponent_ < format.minExp()) {
      leading = (int) Math.abs(format.minExp() - exponent_);
      exponent_ = format.minExp() - 1;
    }

    // Truncate the value:
    // The significand now has length 2*|precision of the significand| + 3 where 3 are the grs bits
    // at the end. We need to shift by at least |precision of the significand| bits.
    // If one of the factors was subnormal the results may have leading zeroes as well, and we need
    // to shift further by 'leading' bits.
    significand_ = truncate(significand_, format.sigBits + leading);

    // Round the result
    significand_ = applyRounding(RoundingMode.NEAREST_EVEN, sign_, significand_);

    // Shift the significand to the right if rounding has caused an overflow
    if (significand_.bitLength() > format.sigBits + 1) {
      significand_ = significand_.shiftRight(1); // The dropped bit is zero
      exponent_ += 1;
    }

    // Return infinity if there is an overflow.
    if (exponent_ > format.bias()) {
      return sign_ ? negativeInfinity(format) : infinity(format);
    }

    // Otherwise return the number
    return new MyFloat(format, new FpValue(sign_, exponent_, significand_));
  }

  private MyFloat squared() {
    return this.multiply(this);
  }

  private final Map<BigInteger, MyFloat> powMap = new HashMap<>();

  public MyFloat powInt(BigInteger exp) {
    return withPrecision(format.extended()).powInt_(exp).withPrecision(format);
  }

  private MyFloat powInt_(BigInteger exp) {
    if (!powMap.containsKey(exp)) {
      MyFloat r = powFast(exp.abs());
      if (exp.compareTo(BigInteger.ZERO) < 0) {
        r = one(format).divide_(r);
      }
      powMap.put(exp, r);
    }
    return powMap.get(exp);
  }

  private MyFloat powFast(BigInteger exp) {
    if (exp.equals(BigInteger.ZERO)) {
      return one(format);
    }
    if (exp.equals(BigInteger.ONE)) {
      return this;
    }
    MyFloat r = powInt_(exp.divide(BigInteger.valueOf(2))).squared();
    MyFloat p = exp.mod(BigInteger.valueOf(2)).equals(BigInteger.ZERO) ? one(format) : this;
    return p.multiply(r);
  }

  public MyFloat divide(MyFloat number) {
    MyFloat a = this.withPrecision(format.extended());
    MyFloat b = number.withPrecision(format.extended());
    return a.divide_(b).withPrecision(format);
  }

  // This version of divide is slower and does not check for corner cases. It is still needed to
  // calculate the constants that are needed for the other, faster version of divide that uses
  // Newton's method.
  private MyFloat divideSlow(MyFloat number) {
    // Calculate the sign of the result
    boolean sign_ = value.sign ^ number.value.sign;

    // Get the exponents without the IEEE bias. Note that for subnormal numbers the stored exponent
    // needs to be increased by one.
    long exponent1 = Math.max(value.exponent, format.minExp());
    long exponent2 = Math.max(number.value.exponent, format.minExp());

    // Normalize both arguments.
    BigInteger significand1 = value.significand;
    int shift1 = (format.sigBits + 1) - significand1.bitLength();
    if (shift1 > 0) {
      significand1 = significand1.shiftLeft(shift1);
      exponent1 -= shift1;
    }

    BigInteger significand2 = number.value.significand;
    int shift2 = (format.sigBits + 1) - significand2.bitLength();
    if (shift2 > 0) {
      significand2 = significand2.shiftLeft(shift2);
      exponent2 -= shift2;
    }

    // Calculate the exponent of the result by subtracting the exponent of the divisor from the
    // exponent of the dividend.
    long exponent_ = exponent1 - exponent2;

    // Calculate how many digits need to be calculated. If the result is <1 we need one additional
    // digit to normalize it.
    int digits = (1 + 2 * format.sigBits) + 3; // 2p-1 (+ 3 grs bits)
    if (significand1.compareTo(significand2) < 0) {
      digits += 1;
      exponent_ -= 1;
    }

    // If the exponent of the result is beyond the exponent range, skip the calculation and return
    // infinity immediately.
    if (exponent_ > format.maxExp()) {
      return sign_ ? negativeInfinity(format) : infinity(format);
    }
    // If it is below the subnormal range, return zero immediately.
    if (exponent_ < format.minExp() - (format.sigBits + 1)) {
      return sign_ ? negativeZero(format) : zero(format);
    }

    // Divide the significands
    BigInteger significand_ = BigInteger.ZERO;
    for (int i = 0; i < digits; i++) {
      // Calculate the next digit of the result
      significand_ = significand_.shiftLeft(1);
      if (significand1.compareTo(significand2) >= 0) {
        significand1 = significand1.subtract(significand2);
        significand_ = significand_.add(BigInteger.ONE);
      }

      // Shift the dividend
      significand1 = significand1.shiftLeft(1);
    }

    // Fix the exponent if it is too small. Use the lowest possible exponent for the format and then
    // move the rest into the significand by shifting it to the right.
    int leading = 0;
    if (exponent_ < format.minExp()) {
      leading = (int) Math.abs(format.minExp() - exponent_);
      exponent_ = format.minExp() - 1;
    }

    // Truncate the value and round the result
    significand_ = truncate(significand_, format.sigBits + leading);
    significand_ = applyRounding(RoundingMode.NEAREST_EVEN, sign_, significand_);

    // Shift the significand to the right if rounding has caused an overflow
    if (significand_.bitLength() > format.sigBits + 1) {
      significand_ = significand_.shiftRight(1); // Last bit is zero
      exponent_ += 1;
    }

    // Return the number
    return new MyFloat(format, new FpValue(sign_, exponent_, significand_));
  }

  // These constants are needed for division with Newton's approach.
  private static final MyFloat c48 = constant(Format.Float256, 48);
  private static final MyFloat c32 = constant(Format.Float256, 32);
  private static final MyFloat c17 = constant(Format.Float256, 17);

  // Here we have to use the slow divide for the constants.
  private static final MyFloat c48d17 = c48.divideSlow(c17);
  private static final MyFloat c32d17 = c32.divideSlow(c17);

  private double lb(double number) {
    return Math.log(number) / Math.log(2);
  }

  private MyFloat divide_(MyFloat number) {
    MyFloat a = this;
    MyFloat b = number;

    // Handle special cases:
    // (1) Either argument is NaN
    if (a.isNan() || b.isNan()) {
      return nan(format);
    }
    // (2) Dividend is zero
    if (a.isZero()) {
      if (b.isZero()) {
        // Divisor is zero or infinite
        return nan(format);
      }
      return (a.isNegative() ^ b.isNegative()) ? negativeZero(format) : zero(format);
    }
    // (3) Dividend is infinite
    if (a.isInfinite()) {
      if (b.isInfinite()) {
        // Divisor is infinite
        return nan(format);
      }
      return (a.isNegative() ^ b.isNegative()) ? negativeInfinity(format) : infinity(format);
    }
    // (4) Divisor is zero (and dividend is finite)
    if (b.isZero()) {
      return (a.isNegative() ^ b.isNegative()) ? negativeInfinity(format) : infinity(format);
    }
    // (5) Divisor is infinite (and dividend is finite)
    if (b.isInfinite()) {
      return (a.isNegative() ^ b.isNegative()) ? negativeZero(format) : zero(format);
    }

    // Extract exponents and significand bits
    int exponent1 = (int) Math.max(a.value.exponent, format.minExp());
    int exponent2 = (int) Math.max(b.value.exponent, format.minExp());

    BigInteger significand1 = a.value.significand;
    BigInteger significand2 = b.value.significand;

    // Shift numerator and divisor by pulling out common factors in the exponent.
    // This will put the divisor in the range of 0.5 to 1.0
    MyFloat n = new MyFloat(format, false, exponent1 - (exponent2 + 1), significand1);
    MyFloat d = new MyFloat(format, false, -1, significand2);

    // Calculate how many iterations are needed
    int bound = (int) Math.ceil(lb((format.sigBits + 2) / lb(17)));

    // Set the initial value to 48/32 - 32/17*D
    MyFloat t1 = c48d17.withPrecision(format);
    MyFloat t2 = c32d17.withPrecision(format);

    MyFloat x = t1.subtract(t2.multiply(d));

    for (int i = 0; i < bound; i++) {
      // X(i+1) = X(i)*(2 - D*X(i))
      x = x.multiply(constant(format, 2).subtract(d.multiply(x)));
    }

    // Multiply 1/D with N and round down to single precision
    MyFloat r = x.multiply(n);

    // Set the sign bit and return the result
    return r.withSign(a.value.sign ^ b.value.sign);
  }

  public MyFloat sqrt() {
    // The calculation will be done in a higher precision and the result is then rounded down.
    // 2p+2 bits are enough for the inverse square root.
    // See Table 12.6 in "Handbook of Floating-Point Arithmetic"
    Format extended = new Format(format.expBits, 2*format.sigBits + 2);
    return withPrecision(extended).sqrt_().withPrecision(format);
  }

  private MyFloat sqrt_() {
    if (isZero()) {
      return isNegative() ? negativeZero(format) : zero(format);
    }
    if (isNan() || isNegative()) {
      return nan(format);
    }
    if (isInfinite()) {
      return infinity(format);
    }

    // Get the exponent and the significand
    long exponent = Math.max(value.exponent, format.minExp());
    BigInteger significand = value.significand;

    // Range reduction:
    // sqrt(f * 2^2m) = sqrt(f)*2^m
    MyFloat f = new MyFloat(format, value.sign, exponent % 2, significand);

    // Define constants
    // TODO: These constants should be declared only once for each supported precision
    MyFloat c1d2 = new MyFloat(format, false, -1, BigInteger.ONE.shiftLeft(format.sigBits));
    MyFloat c3d2 =
        new MyFloat(format, false, 0, BigInteger.valueOf(3).shiftLeft(format.sigBits - 1));

    // Initial value (0.5 will always converge)
    MyFloat x = c1d2;

    boolean done = false;
    List<MyFloat> partial = new ArrayList<>();
    while (!done) {
      partial.add(x);
      // x_n+1 = x_n * (3/2 - 1/2 * f * x_n^2)
      x = x.multiply(c3d2.subtract(c1d2.multiply(f).multiply(x.squared())));

      // Abort once we have enough precision
      done = partial.contains(x);
    }

    // Multiply the inverse square root with f again to get the square root itself.
    x = x.multiply(f);

    // Restore the exponent by multiplying with 2^m
    MyFloat r = one(format).withExponent(exponent / 2);
    return x.multiply(r);
  }

  // Strip invalid digits from the significand. This assumes that the number is transcendent.
  private MyFloat validPart() {
    // In round to nearest invalid digits follow one of two patterns:
    // 1(0)+ or 1(0)+
    // After truncating these bits from the end all other digits are equal to the digits of the
    // infinite actual number.
    if (isZero() || isNan() || isInfinite()) {
      return this;
    }
    if (BigInteger.ONE.shiftLeft(format.sigBits).equals(value.significand)) {
      return this;
    }
    BigInteger significand = value.significand;
    boolean last = significand.testBit(0);

    // Search for the pattern
    int trailing = 1;
    do {
      significand = significand.shiftRight(1);
      trailing++;
    } while (significand.testBit(0) == last);

    significand = significand.shiftRight(1);
    return new MyFloat(
        new Format(format.expBits, format.sigBits - trailing),
        value.sign,
        value.exponent,
        significand);
  }

  // The minimal distance between two float with the same exponent
  private MyFloat oneUlp() {
    BigInteger significand = BigInteger.ONE.shiftLeft(format.sigBits);
    long exponent = value.exponent - format.sigBits;
    return new MyFloat(format, value.sign, exponent, significand);
  }

  // Returns the next larger floating point number
  private MyFloat plus1Ulp() {
    return add(oneUlp());
  }

  // Returns the floating point number immediately below this number
  private MyFloat minus1Ulp() {
    return add(oneUlp().negate());
  }

  // Compare two m-float numbers for equality when rounded to lower precision p
  private boolean equalModuloP(MyFloat a, MyFloat b) {
    return a.withPrecision(format).equals(b.withPrecision(format));
  }

  // Check if an m-float number is stable in precision p
  private boolean isStable(MyFloat r) {
    if (r.format.sigBits == 0) {
      return false;
    }
    return equalModuloP(r, r.plus1Ulp());
  }

  // Statistics for exp(...)
  // public static final Map<Integer, Integer> expStats = new HashMap<>();

  public MyFloat exp() {
    if (isZero()) {
      return one(format);
    }

    ImmutableList.Builder<Format> builder = ImmutableList.builder();
    if (format.equals(Format.Float16)) {
      builder.add(new Format(8, format.sigBits + 2));
      builder.add(new Format(8, format.sigBits + 16));
      builder.add(new Format(8, format.sigBits + 26));
    } else {
      builder.add(new Format(15, format.sigBits + 10));
      builder.add(format.extended());
      builder.add(format.extended().extended());
    }
    ImmutableList<Format> formats = builder.build();

    MyFloat r = nan(format);
    boolean done = false;

    for (Format p : formats) {
      if (!done) {
        Format p_ext = new Format(p.expBits, p.sigBits - format.sigBits);
        MyFloat x = withPrecision(p_ext);

        boolean isTiny = x.exp_().subtract(one(p_ext)).isZero();

        MyFloat x1 = x.plus1Ulp().withPrecision(p);
        MyFloat x2 = x.minus1Ulp().withPrecision(p);

        MyFloat v1 = isTiny ? x1.expm1() : x1.exp_();
        MyFloat v2 = isTiny ? x2.expm1() : x2.exp_();

        if (equalModuloP(v1, v2)) {
          done = true;
          r = isTiny ? one(p).add(v1) : v1;

          /*
          // Update statistics
          Integer k = p.sigBits - format.sigBits;
          expStats.put(k, expStats.getOrDefault(k, 0) + 1);
          */
        }
      }
    }
    return r.withPrecision(format);
  }

  private static final Map<Integer, BigInteger> facMap = new HashMap<>();

  private static BigInteger fac(int k) {
    return facMap.computeIfAbsent(k, MyFloat::fac_);
  }

  private static BigInteger fac_(Integer k) {
    if (k == 0 || k == 1) {
      return BigInteger.ONE;
    }
    return fac(k - 1).multiply(BigInteger.valueOf(k));
  }

  private static Map<Integer, MyFloat> mkExpTable(Format pFormat) {
    ImmutableMap.Builder<Integer, MyFloat> builder = ImmutableMap.builder();
    builder.put(0, one(pFormat));
    for (int k = 1; k < 100; k++) {
      // Calculate 1/k! and store the values in the table
      builder.put(k, one(pFormat).divide_(constant(pFormat, fac(k))));
    }
    return builder.buildOrThrow();
  }

  // Table contains terms 1/k! for 1..100
  private static final Map<Integer, MyFloat> expTable = mkExpTable(Format.Float256);

  private MyFloat exp_() { return expImpl(0); }
  public MyFloat expm1() { return expImpl(1); }

  private MyFloat expImpl(int k) { // k is the first term of the expansion
    if (isNan()) {
      return nan(format);
    }
    if (isInfinite()) {
      return isNegative() ? zero(format) : infinity(format);
    }

    MyFloat x = this;
    MyFloat r = zero(format); // Series expansion after k terms.

    // Range reduction: exp(a * 2^k) = exp(a)^2k
    if (value.exponent > 0) {
      x = x.withExponent(0);
    }

    boolean done = false;
    while(!done) {
      MyFloat s = r;

      // r(k+1) = r(k) +  x^k/k!
      MyFloat a = x.powInt_(BigInteger.valueOf(k));
      MyFloat b = expTable.get(k).withPrecision(format); // FIXME: Make sure k < expTable size

      r = r.add(a.multiply(b));

      // Abort if we have enough precision
      done = r.equals(s);
      k++;
    }

    // Square the result to recover the exponent
    for (int i = 0; i < value.exponent; i++) {
      r = r.squared();
    }
    return r;
  }

  public static final Map<Integer, Integer> lnStats = new HashMap<>();

  public MyFloat ln() {
    if (isZero()) {
      return negativeInfinity(format);
    }
    if (isOne()) {
      return zero(format);
    }

    ImmutableList.Builder<Format> builder = ImmutableList.builder();
    if (format.equals(new Format(3, 4))) {
      builder.add(new Format(8, format.sigBits + 2));
      builder.add(new Format(8, format.sigBits + 9));
      builder.add(new Format(8, format.sigBits + 12));
    }
    if (format.equals(Format.Float16)) {
      builder.add(new Format(8, format.sigBits + 1));
      builder.add(new Format(8, format.sigBits + 14));
      builder.add(new Format(8, format.sigBits + 60));
    } else {
      builder.add(new Format(15, format.sigBits + 1));
      builder.add(format.extended());
      builder.add(format.extended().extended());
    }
    ImmutableList<Format> formats = builder.build();

    MyFloat r = nan(format);
    boolean done = false;

    for (Format p : formats) {
      if (!done) {
        Format p_ext = new Format(p.expBits, p.sigBits - format.sigBits);
        MyFloat x = withPrecision(p_ext);

        MyFloat x1 = x.plus1Ulp().withPrecision(p);
        MyFloat x2 = x.minus1Ulp().withPrecision(p);

        MyFloat v1 = x1.ln_pre(false, false);
        MyFloat v2 = x2.ln_pre(false, false);

        if (equalModuloP(v1, v2)) {
          done = true;
          r = v1;

          // Update statistics
          Integer k = p.sigBits - format.sigBits;
          lnStats.put(k, lnStats.getOrDefault(k, 0) + 1);
        }
      }
    }
    return r.withPrecision(format);
  }

  private static Map<Integer, MyFloat> mkLnTable(Format pFormat) {
    ImmutableMap.Builder<Integer, MyFloat> builder = ImmutableMap.builder();
    for (int k = 1; k < 100; k++) {
      // Calculate 1/k and store the values in the table
      builder.put(k, one(pFormat).divide_(constant(pFormat, k)));
    }
    return builder.buildOrThrow();
  }

  // Table contains terms 1/k for k=1..100
  private static final Map<Integer, MyFloat> lnTable = mkLnTable(Format.Float256);

  private MyFloat ln_pre(boolean useSqrt, boolean useNewton) {
    if (isZero()) {
      return negativeInfinity(format);
    }
    if (isNan() || isNegative()) {
      return nan(format);
    }
    if (isInfinite()) {
      return infinity(format);
    }
    if (isOne()) {
      return zero(format);
    }
    return useSqrt ? ln_pre1(useNewton) : ln_pre2(useNewton);
  }

  private MyFloat ln_pre1(boolean useNewton) {
    // TODO: These constants should be declared only once for each supported precision
    MyFloat c1d2 = new MyFloat(format, false, -1, BigInteger.ONE.shiftLeft(format.sigBits));
    MyFloat c3d2 =
        new MyFloat(format, false, 0, BigInteger.valueOf(3).shiftLeft(format.sigBits - 1));

    MyFloat x = this;

    int preprocess = 0;
    while (x.greaterThan(c3d2) || c1d2.greaterThan(x)) {
      x = x.sqrt_();
      preprocess++;
    }

    MyFloat r = useNewton ? x.lnNewton_() : x.subtract(one(format)).ln1p();
    return r.withExponent(r.value.exponent + preprocess);
  }

  private static MyFloat const_ln2 = make_ln2(Format.Float256);

  private static MyFloat make_ln2(Format pFormat) {
    MyFloat r = constant(pFormat, 2).sqrt_().subtract(one(pFormat)).ln1p();
    return r.withExponent(r.value.exponent + 1);
  }

  private MyFloat ln_pre2(boolean useNewton) {
    // ln(x) = ln(a * 2^k) = ln a + ln 2^k = ln a + k * ln 2
    MyFloat a = withExponent(-1);
    MyFloat lna = useNewton ? a.lnNewton_() : a.subtract(one(format)).ln1p();

    MyFloat ln2 = const_ln2.withPrecision(format);
    MyFloat nln2 = constant(format, (int) value.exponent + 1).multiply(ln2);

    return lna.add(nln2);
  }

  public MyFloat ln1p() {
    MyFloat x = this;
    MyFloat r = zero(format);

    for (int k = 1; k < 40; k++) { // fill the cache with values
      x.powInt_(BigInteger.valueOf(k));
    }

    // We calculate the sum backwards to avoid rounding errors
    for (int k = 39; k >= 1; k--) { // TODO: Find a proper bound for the number of iterations.
      // r(k+1) = r(k) +  x^k/k
      MyFloat a = x.powInt_(BigInteger.valueOf(k));
      MyFloat b = a.multiply(lnTable.get(k).withPrecision(format));

      r = r.add(k % 2 == 0 ? b.negate() : b);
    }
    return r;
  }

  private MyFloat lnNewton_() {
    MyFloat x = this;

    // Initial value: first term of taylor series for ln
    MyFloat r = x.subtract(one(format));

    boolean done = false;
    List<MyFloat> partial = new ArrayList<>();
    while (!done) {
      partial.add(r);

      //  r(n+1) = r(n) + 2 * (x - e^r(n)) / (x + e^r(n))
      MyFloat exp_y = r.exp_();
      MyFloat t1 = x.subtract(exp_y);
      MyFloat t2 = x.add(exp_y);
      r = r.add(constant(format, 2).multiply(t1.divide_(t2)));

      // Abort once we have enough precision
      done = partial.contains(r);
    }
    return r;
  }

  public MyFloat pow(MyFloat exponent) {
    MyFloat a = this;
    MyFloat x = exponent;

    // Handle special cases
    // See https://en.cppreference.com/w/c/numeric/math/pow for the full definition

    if (a.isOne() || x.isZero()) {
      // pow(+1, exponent) returns 1 for any exponent, even when exponent is NaN
      // pow(base, ±0) returns 1 for any base, even when base is NaN
      return one(format);
    }
    if (a.isNan() || x.isNan()) {
      // except where specified above, if any argument is NaN, NaN is returned
      return nan(format);
    }
    if (a.isZero() && x.isNegative() && x.isOddInteger()) {
      // pow(+0, exponent), where exponent is a negative odd integer, returns +∞ and raises
      // FE_DIVBYZERO
      // pow(-0, exponent), where exponent is a negative odd integer, returns -∞ and raises
      // FE_DIVBYZERO
      return a.isNegative() ? negativeInfinity(format) : infinity(format);
    }
    if (a.isZero() && x.isNegative()) {
      // pow(±0, -∞) returns +∞ and may raise FE_DIVBYZERO(until C23)
      // pow(±0, exponent), where exponent is negative, finite, and is an even integer or a
      // non-integer, returns +∞ and raises FE_DIVBYZERO
      return infinity(format);
    }
    if (a.isZero() && !x.isNegative()) {
      // pow(+0, exponent), where exponent is a positive odd integer, returns +0
      // pow(-0, exponent), where exponent is a positive odd integer, returns -0
      // pow(±0, exponent), where exponent is positive non-integer or a positive even integer,
      // returns +0
      return x.isOddInteger() ? a : zero(format);
    }
    if (a.isNegativeOne() && x.isInfinite()) {
      // pow(-1, ±∞) returns 1
      return one(format);
    }
    if (a.isInfinite() && isNegative()) {
      // pow(-∞, exponent) returns -0 if exponent is a negative odd integer
      // pow(-∞, exponent) returns +0 if exponent is a negative non-integer or negative even integer
      // pow(-∞, exponent) returns -∞ if exponent is a positive odd integer
      // pow(-∞, exponent) returns +∞ if exponent is a positive non-integer or positive even integer
      MyFloat power = x.isNegative() ? zero(format) : infinity(format);
      return x.isOddInteger() ? power.negate() : power;
    }
    if (a.isInfinite()) {
      // pow(+∞, exponent) returns +0 for any negative exponent
      // pow(+∞, exponent) returns +∞ for any positive exponent
      return x.isNegative() ? zero(format) : infinity(format);
    }
    if (x.isInfinite() && x.isNegative()) {
      // pow(base, -∞) returns +∞ for any |base|<1
      // pow(base, -∞) returns +0 for any |base|>1
      return a.abs().greaterThan(one(format)) ? zero(format) : infinity(format);
    }
    if (x.isInfinite()) {
      // pow(base, +∞) returns +0 for any |base|<1
      // pow(base, +∞) returns +∞ for any |base|>1
      return a.abs().greaterThan(one(format)) ? infinity(format) : zero(format);
    }
    if (a.isNegative() && !x.isInteger()) {
      // pow(base, exponent) returns NaN and raises FE_INVALID if base is finite and negative and
      // exponent is finite and non-integer.
      return nan(format);
    }
    if (x.isInteger()) {
      // FIXME: Fix the rounding test and remove this workaround
      return a.powInt(x.toInteger());
    }
    MyFloat c1d2 = new MyFloat(format, false, -1, BigInteger.ONE.shiftLeft(format.sigBits));
    if (x.equals(c1d2)) {
      // FIXME: Remove this workaround
      return a.sqrt();
    }
    MyFloat r = a.abs().pow_(exponent);
    if (a.isNegative()) {
      // Fix the sign if `a` was negative and x an integer
      r = r.withSign(x.isOddInteger());
    }
    return r;
  }

  private static MyFloat prefix(MyFloat lo, MyFloat hi) {
    BigInteger sig1 = lo.value.significand;
    BigInteger sig2 = hi.value.significand;

    int diff = 0;
    while (!sig1.equals(sig2)) {
      sig1 = sig1.shiftRight(1);
      sig2 = sig2.shiftRight(1);
      diff++;
    }
    Format format = new Format(lo.format.expBits, lo.format.sigBits - diff);
    return new MyFloat(format, lo.value.sign, lo.value.exponent, sig1);
  }

  // Largest value that will round down to the valid part of the number
  private MyFloat above() {
    MyFloat t = validPart();
    int diff = format.sigBits - t.format.sigBits;
    BigInteger sig1 = t.value.significand.shiftLeft(diff);
    BigInteger sig2 = BigInteger.ONE;
    if (sig1.testBit(0)) {
      sig2 = sig2.shiftLeft(diff - 1).subtract(BigInteger.ONE);
    } else {
      sig2 = sig2.shiftLeft(diff - 1);
    }
    return new MyFloat(format, value.sign, value.exponent, sig1.add(sig2));
  }

  // Smallest value that will round up to the valid part of the number
  private MyFloat below() {
    MyFloat t = validPart().minus1Ulp();
    int diff = format.sigBits - t.format.sigBits;
    BigInteger sig1 = t.value.significand.shiftLeft(diff);
    BigInteger sig2 = BigInteger.ONE;
    if (sig1.testBit(0)) {
      sig2 = sig2.shiftLeft(diff - 1);
    } else {
      sig2 = sig2.shiftLeft(diff - 1).add(BigInteger.ONE);
    }
    return new MyFloat(format, value.sign, value.exponent, sig1.add(sig2));
  }

  private MyFloat pow_(MyFloat exponent) {
    // a^x = exp(x * ln a)
    Format fp1 = new Format(20, format.sigBits + 20);
    Format fp2 = format.extended();
    Format fp3 = new Format(32, 200);

    ImmutableList<Format> formats = ImmutableList.of(fp1, fp2, fp3);

    MyFloat r = nan(format);
    boolean done = false;

    for (Format p : formats) {
      if (!done) {
        Format p_ext = new Format(p.expBits, p.sigBits + 3);

        MyFloat a = this.withPrecision(p_ext);
        MyFloat x = exponent.withPrecision(p_ext);

        // Calculate ln(a) with extra precision, then multiply with x and round down
        MyFloat lna = a.ln_pre(false, false);
        MyFloat xlna = x.multiply(lna).withPrecision(p);

        // Probe to see if the result rounds to one. If so we'll use expm1 to avoid losing
        // significand digits.
        // TODO: Avoid the call and check the argument instead
        boolean collapses = xlna.exp_().subtract(one(p)).isZero();

        // Get an interval for the true value of exp(..)
        // TODO: Find a way to avoid calling exp() multiple times
        MyFloat hi = collapses ? xlna.above().expm1() : xlna.above().exp_();
        MyFloat lo = collapses ? xlna.below().expm1() : xlna.below().exp_();

        // Find the valid digits
        MyFloat val = prefix(hi, lo).validPart();

        // Check if they're enough to round
        if (isStable(val)) {
          done = true;
          r = collapses ? one(val.format).add(val) : val;
        }
      }
    }
    return r.withPrecision(format);
  }

  public MyFloat roundToInteger(RoundingMode rm) {
    // If we have a special value, just return it
    if (isNan() || isInfinite()) {
      return this;
    }
    // If the exponent is large enough we already have an integer and can return immediately
    if (value.exponent > format.sigBits) {
      return this;
    }

    // Get the significand and add grs bits
    BigInteger significand = value.significand;
    significand = significand.shiftLeft(3);

    // Shift the fractional part to the right and then round the result
    significand = truncate(significand, (int) (format.sigBits - value.exponent));
    significand = applyRounding(rm, value.sign, significand);

    // Recalculate the exponent
    int exponent = significand.bitLength() - 1;

    // Normalize the significand if there was an overflow. The last bit is then always zero and can
    // simply be dropped.
    significand = significand.shiftLeft(format.sigBits - exponent);

    // Check if the result is zero
    if (significand.equals(BigInteger.ZERO)) {
      return isNegative() ? negativeZero(format) : zero(format);
    }
    // Check if we need to round to infinity
    if (exponent > format.maxExp()) {
      return isNegative() ? negativeInfinity(format) : infinity(format);
    }
    return new MyFloat(format, value.sign, exponent, significand);
  }

  private FpValue fromInteger(BigInteger number) {
    // Return +0.0 for input 0
    if (number.equals(BigInteger.ZERO)) {
      return new FpValue(false, 0, BigInteger.ZERO);
    }

    // Get the sign and calculate the exponent
    boolean sign = number.signum() < 0;
    int exponent = number.abs().bitLength() - 1;

    // Truncate the number while carrying over the grs bits.
    BigInteger significand = number.abs().shiftLeft(format.sigBits + 3);
    significand = truncate(significand, exponent);

    // Round the result according to the grs bits
    long grs = significand.and(new BigInteger("111", 2)).longValue();
    significand = significand.shiftRight(3);
    if ((grs == 4 && significand.testBit(0)) || grs > 4) {
      significand = significand.add(BigInteger.ONE);
    }

    return new FpValue(sign, exponent, significand);
  }

  private BigInteger toInteger() {
    if (value.exponent < -1) {
      return BigInteger.ZERO;
    }
    // Shift the significand to truncate the fractional part. For large exponents the expression
    // 'format.sigBits - value.exponent' will become negative, and the shift is to the left, adding
    // additional zeroes.
    BigInteger significand = value.significand.shiftRight((int) (format.sigBits - value.exponent));
    if (value.sign) {
      significand = significand.negate();
    }
    return significand;
  }

  // NOTE: toByte, toShort, toInt and toLong depend on undefined behaviour
  // According to the C99 standard:
  // "F.4 Floating to integer conversion
  //  If the floating value is infinite or NaN or if the integral part of the floating value exceeds
  //  the range of the integer type, then the ‘‘invalid’’ floating-point exception is raised and the
  //  resulting value is unspecified. Whether conversion of non-integer floating values whose
  //  integral part is within the range of the integer type raises the ‘‘inexact’’ floating-point
  //  exception is unspecified"
  // However gcc does not always set the inexact flag correctly
  // (see https://gcc.gnu.org/bugzilla/show_bug.cgi?id=27682) and the check has to be performed
  // manually. It is also possible to check the range in advance, but this again has to be done by
  // the programmer.
  // We therefore try to emulate the default behaviour of gcc, which is to return a special
  // indefinite value if the real value is out of range. For signed integers this indefinite value
  // is 0x80000000 for int and 0x8000000000000000 for long. Conversion to byte or short happens in
  // two steps: first the float is converted to a 32bit integer, and then this value is truncated.
  // The indefinite value is therefore 0 in both cases.

  public byte toByte() {
    BigInteger integerValue = toInteger();
    BigInteger maxPositive = BigInteger.valueOf(Integer.MAX_VALUE);
    BigInteger maxNegative = BigInteger.valueOf(Integer.MIN_VALUE);
    if (isNan()
        || isInfinite()
        || (integerValue.compareTo(maxPositive) > 0)
        || integerValue.compareTo(maxNegative) < 0) {
      return 0;
    }
    return integerValue.byteValue();
  }

  public short toShort() {
    BigInteger integerValue = toInteger();
    BigInteger maxPositive = BigInteger.valueOf(Integer.MAX_VALUE);
    BigInteger maxNegative = BigInteger.valueOf(Integer.MIN_VALUE);
    if (isNan()
        || isInfinite()
        || (integerValue.compareTo(maxPositive) > 0)
        || integerValue.compareTo(maxNegative) < 0) {
      return 0;
    }
    return integerValue.shortValue();
  }

  public int toInt() {
    BigInteger integerValue = toInteger();
    BigInteger maxPositive = BigInteger.valueOf(Integer.MAX_VALUE);
    BigInteger maxNegative = BigInteger.valueOf(Integer.MIN_VALUE);
    if (isNan()
        || isInfinite()
        || (integerValue.compareTo(maxPositive) > 0)
        || integerValue.compareTo(maxNegative) < 0) {
      return maxNegative.intValue();
    }
    return integerValue.intValue();
  }

  public long toLong() {
    BigInteger integerValue = toInteger();
    BigInteger maxPositive = BigInteger.valueOf(Long.MAX_VALUE);
    BigInteger maxNegative = BigInteger.valueOf(Long.MIN_VALUE);
    if (isNan()
        || isInfinite()
        || (integerValue.compareTo(maxPositive) > 0)
        || integerValue.compareTo(maxNegative) < 0) {
      return maxNegative.longValue();
    }
    return integerValue.longValue();
  }

  public float toFloat() {
    if (isNan()) {
      return Float.NaN;
    }
    if (isInfinite()) {
      return isNegative() ? Float.NEGATIVE_INFINITY : Float.POSITIVE_INFINITY;
    }
    return new BigFloat(value.sign, value.significand, value.exponent, BinaryMathContext.BINARY32)
        .floatValueExact();
  }

  public double toDouble() {
    if (isNan()) {
      return Double.NaN;
    }
    if (isInfinite()) {
      return isNegative() ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
    }
    return new BigFloat(value.sign, value.significand, value.exponent, BinaryMathContext.BINARY64)
        .doubleValueExact();
  }

  public BigFloat toBigFloat() {
    BinaryMathContext context = new BinaryMathContext(format.sigBits + 1, format.expBits);
    if (isNan()) {
      return BigFloat.NaN(context.precision);
    }
    if (isInfinite()) {
      return isNegative()
          ? BigFloat.negativeInfinity(context.precision)
          : BigFloat.positiveInfinity(context.precision);
    }
    return new BigFloat(value.sign, value.significand, value.exponent, context);
  }

  @Override
  public String toString() {
    if (isNan()) {
      return "nan";
    }
    if (isInfinite()) {
      return isNegative() ? "-inf" : "inf";
    }
    if (isZero()) {
      return isNegative() ? "-0.0" : "0.0";
    }
    // TODO: Return more digits if the bitwidth allows it
    return String.format("%.6e", withPrecision(Format.Float64).toDouble());
  }

  public String toBinaryString() {
    if (isNan()) {
      return "nan";
    }
    if (isInfinite()) {
      return isNegative() ? "-inf" : "inf";
    }
    if (isZero()) {
      return isNegative() ? "-0.0" : "0.0";
    }
    String bits = value.significand.toString(2);
    return "%s%s.%s e%d"
        .formatted(value.sign ? "-" : "", bits.charAt(0), bits.substring(1), value.exponent);
  }
}