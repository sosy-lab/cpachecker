// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.floatingpoint;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.kframework.mpfr.BigFloat;
import org.kframework.mpfr.BinaryMathContext;
import org.sosy_lab.common.NativeLibraries;

// TODO: Add support for more rounding modes
// TODO: Add more functions (like sin(x), etc)
// TODO: Refactor and split off format as its own class
//  This would allow us to created some of the constants (and tables) only once per bit size.
//  On the other hand it may complicate the interface.
// TODO: Make castToOther return an Optional
//  This is needed when the target type for castToOther is too small for the integer value of the
//  float. See the comment before toByte() for more details.
// TODO: Add support for unsigned types

/**
 * Java based implementation of multi-precision floating point values with correct rounding.
 *
 * <p>"Correct rounding" means that the result is the same as if the calculation had been performed
 * with infinite precision before rounding and returning the value. All FloatP values have a given
 * precision and exponent range that has to be chosen by the user. Operations on multiple arguments
 * expect the precision of the arguments to match. The nested class {@link Format} is used to
 * represent the precision and exponent range of a FloatP value.
 *
 * <p>{@link CFloatImpl} wraps this class to provide an adapter for the {@link CFloat} interface.
 */
public class FloatP {
  static {
    NativeLibraries.loadLibrary("mpfr_java");
  }

  /**
   * Defines the precision and the exponent range of a {@link FloatP} value.
   *
   * <p>The precision of a FloatP is equivalent to the length of its significand. Here the 'hidden
   * bit' is not counted. The exponent range can be derived from the width of the exponent field.
   */
  public static class Format {
    private final int expBits;
    private final int sigBits;

    public Format(int pExpBits, int pSigBits) {
      // Check that the arguments are valid. We expect the format to be at least as big as Float8.
      Preconditions.checkArgument(
          pExpBits >= 0 && pExpBits <= 64, "Exponent field must be between 0 and 64 bits wide.");
      Preconditions.checkArgument(
          pSigBits >= 0, "Significand field must not have negative bit width.");

      expBits = pExpBits;
      sigBits = pSigBits;
    }

    public static final Format Float8 = new Format(4, 3);
    public static final Format Float16 = new Format(5, 10);
    public static final Format Float32 = new Format(8, 23);
    public static final Format Float64 = new Format(11, 52);
    public static final Format Float128 = new Format(15, 112);
    public static final Format Float256 = new Format(19, 236);

    public int getExpBits() {
      return expBits;
    }

    public int getSigBits() {
      return sigBits;
    }

    @Override
    public boolean equals(Object other) {
      if (this == other) {
        return true;
      }
      return other instanceof Format that && expBits == that.expBits && sigBits == that.sigBits;
    }

    @Override
    public int hashCode() {
      return Objects.hash(expBits, sigBits);
    }

    /**
     * The exponent 'bias' of a FloatP value in this format.
     *
     * <p>Useful when converting to the IEEE representation of the value where an unsigned integer
     * is used to represent the exponent. Adding the bias converts to IEEE, subtracting it returns
     * the value to our representation.
     */
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

    /** Returns the next bigger format meant to be used for intermediate results. */
    public Format extended() {
      // TODO: Add support for arbitrary sizes
      if (equals(Format.Float8)) {
        return Float32;
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
      return new Format(Float256.expBits, 2 * sigBits + 1);
    }

    /**
     * Least upper bound of the two formats.
     *
     * <p>Used when implementing binary operations on FloatP values, where a common format large
     * enough for both arguments needs to be found.
     */
    public Format sup(Format other) {
      int newExp = Math.max(expBits, other.expBits);
      int newSig = Math.max(sigBits, other.sigBits);
      return new Format(newExp, newSig);
    }
  }

  // Format, defines the precision of the value and the allowed exponent range
  private final Format format;

  // Sign, exponent and significand of the float value
  private final boolean sign;
  private final long exponent;
  private final BigInteger significand;

  /**
   * Create a floating point value.
   *
   * @param pFormat Format, defines the precision and the width of the exponent field
   * @param pSign Sign of the value
   * @param pExponent Exponent, without the IEEE bias
   * @param pSignificand Significand, including the leading bit that is hidden in the IEEE format
   */
  public FloatP(Format pFormat, boolean pSign, long pExponent, BigInteger pSignificand) {
    format = pFormat;
    sign = pSign;
    exponent = pExponent;
    significand = pSignificand;
  }

  public Format getFormat() {
    return format;
  }

  public static FloatP nan(Format pFormat) {
    return new FloatP(
        pFormat, false, pFormat.maxExp() + 1, BigInteger.ONE.shiftLeft(pFormat.sigBits - 1));
  }

  public static FloatP infinity(Format pFormat) {
    return new FloatP(pFormat, false, pFormat.maxExp() + 1, BigInteger.ZERO);
  }

  public static FloatP maxValue(Format pFormat) {
    BigInteger allOnes = BigInteger.ONE.shiftLeft(pFormat.sigBits + 1).subtract(BigInteger.ONE);
    return new FloatP(pFormat, false, pFormat.maxExp(), allOnes);
  }

  public static FloatP constant(Format pFormat, BigInteger number) {
    return fromInteger(pFormat, number);
  }

  public static FloatP constant(Format pFormat, int number) {
    return fromInteger(pFormat, BigInteger.valueOf(number));
  }

  public static FloatP one(Format pFormat) {
    return new FloatP(pFormat, false, 0, BigInteger.ONE.shiftLeft(pFormat.sigBits));
  }

  /** Smallest normal value that can be represented in this format. */
  public static FloatP minNormal(Format pFormat) {
    return new FloatP(pFormat, false, pFormat.minExp(), BigInteger.ONE.shiftLeft(pFormat.sigBits));
  }

  /**
   * Smallest absolute value (other than zero) that can be represented in this format.
   *
   * <p>Note that this value will be sub-normal. To get the smallest normal value use {@link
   * FloatP#minNormal} instead.
   */
  public static FloatP minValue(Format pFormat) {
    return new FloatP(pFormat, false, pFormat.minExp() - 1, BigInteger.ONE);
  }

  public static FloatP zero(Format pFormat) {
    return new FloatP(pFormat, false, pFormat.minExp() - 1, BigInteger.ZERO);
  }

  public static FloatP negativeZero(Format pFormat) {
    return new FloatP(pFormat, true, pFormat.minExp() - 1, BigInteger.ZERO);
  }

  public static FloatP negativeOne(Format pFormat) {
    return new FloatP(pFormat, true, 0, BigInteger.ONE.shiftLeft(pFormat.sigBits));
  }

  public static FloatP negativeInfinity(Format pFormat) {
    return new FloatP(pFormat, true, pFormat.maxExp() + 1, BigInteger.ZERO);
  }

  public boolean isNan() {
    return (exponent == format.maxExp() + 1) && (significand.compareTo(BigInteger.ZERO) > 0);
  }

  public boolean isInfinite() {
    return (exponent == format.maxExp() + 1) && significand.equals(BigInteger.ZERO);
  }

  /** True if the value is equal to zero (or negative zero). */
  public boolean isZero() {
    return (exponent == format.minExp() - 1) && significand.equals(BigInteger.ZERO);
  }

  public boolean isOne() {
    return one(format).equals(this);
  }

  public boolean isNegativeOne() {
    return negativeOne(format).equals(this);
  }

  public boolean isNegative() {
    return sign;
  }

  public boolean isInteger() {
    BigInteger intValue = toInteger();
    return equals(fromInteger(format, intValue));
  }

  public boolean isOddInteger() {
    BigInteger intValue = toInteger();
    return equals(fromInteger(format, intValue)) && intValue.testBit(0);
  }

  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    return pOther instanceof FloatP other
        && format.equals(other.format)
        && sign == other.sign
        && exponent == other.exponent
        && significand.equals(other.significand);
  }

  @Override
  public int hashCode() {
    return Objects.hash(format, sign, exponent, significand);
  }

  /** Shift the significand to the right while preserving the sticky bit. */
  private static BigInteger truncate(BigInteger pSignificand, int bits) {
    BigInteger mask = BigInteger.ONE.shiftLeft(bits).subtract(BigInteger.ONE);
    BigInteger r = pSignificand.and(mask);
    BigInteger l = pSignificand.shiftRight(bits);
    return r.equals(BigInteger.ZERO) ? l : l.setBit(0);
  }

  public enum RoundingMode {
    NEAREST_AWAY, // Round to nearest, ties away from zero
    NEAREST_EVEN, // Round to nearest, ties to even
    CEILING, // Round toward +∞
    FLOOR, // Round toward -∞
    TRUNCATE // Round toward 0
  }

  /* Round the significand.<p>
   * We expect the significand to be followed by 3 grs bits.
   */
  private BigInteger applyRounding(RoundingMode rm, boolean negative, BigInteger pSignificand) {
    long grs = pSignificand.and(new BigInteger("111", 2)).longValue();
    pSignificand = pSignificand.shiftRight(3);
    BigInteger plusOne = pSignificand.add(BigInteger.ONE);
    return switch (rm) {
      case NEAREST_AWAY -> (grs >= 4) ? plusOne : pSignificand;
      case NEAREST_EVEN ->
          ((grs == 4 && pSignificand.testBit(0)) || grs > 4) ? plusOne : pSignificand;
      case CEILING -> (grs > 0 && !negative) ? plusOne : pSignificand;
      case FLOOR -> (grs > 0 && negative) ? plusOne : pSignificand;
      case TRUNCATE -> pSignificand;
    };
  }

  /** Clone the value with a new exponent. */
  private FloatP withExponent(long pExponent) {
    if (pExponent > format.maxExp()) {
      return sign ? negativeInfinity(format) : infinity(format);
    }
    if (pExponent < format.minExp()) {
      throw new IllegalArgumentException(); // FIXME: Handle subnormal numbers
    }
    return new FloatP(format, sign, pExponent, significand);
  }

  /** Clone the value with a new sign. */
  private FloatP withSign(boolean pSign) {
    return new FloatP(format, pSign, exponent, significand);
  }

  /**
   * Convert the value to a different precision. Uses "round to nearest, ties to even" for rounding
   * when the value can not be represented exactly in the new format.
   */
  public FloatP withPrecision(Format targetFormat) {
    if (format.equals(targetFormat)) {
      return this;
    }
    if (isNan()) {
      return sign ? nan(targetFormat).negate() : nan(targetFormat);
    }
    if (isInfinite()) {
      return sign ? negativeInfinity(targetFormat) : infinity(targetFormat);
    }
    if (isZero()) {
      return sign ? negativeZero(targetFormat) : zero(targetFormat);
    }

    long exponent_ = Math.max(exponent, format.minExp());
    BigInteger significand_ = significand;

    // Normalization
    // If the number is subnormal shift it upward and adjust the exponent
    int shift = (format.sigBits + 1) - significand_.bitLength();
    if (shift > 0) {
      significand_ = significand_.shiftLeft(shift);
      exponent_ -= shift;
    }

    // Return infinity if the exponent is too large for the new encoding
    if (exponent_ > targetFormat.maxExp()) {
      return sign ? negativeInfinity(targetFormat) : infinity(targetFormat);
    }
    // Return zero if the exponent is below the subnormal range
    if (exponent_ < targetFormat.minExp() - (targetFormat.sigBits + 1)) {
      return sign ? negativeZero(targetFormat) : zero(targetFormat);
    }

    // Extend the significand with 3 grs bits
    significand_ = significand_.shiftLeft(targetFormat.sigBits + 3);

    // Use the lowest possible exponent and move the rest into the significand by shifting
    // it to the right.
    int leading = 0;
    if (exponent_ < targetFormat.minExp()) {
      leading = (int) Math.abs(targetFormat.minExp() - exponent_);
      exponent_ = targetFormat.minExp() - 1;
    }

    // Truncate the value and round the result
    significand_ = truncate(significand_, format.sigBits + leading);
    significand_ = applyRounding(RoundingMode.NEAREST_EVEN, sign, significand_);

    // Normalize if rounding caused an overflow
    if (significand_.testBit(targetFormat.sigBits + 1)) {
      significand_ = significand_.shiftRight(1); // The last bit is zero
      exponent_ += 1;
    }

    // Return infinity if this caused the exponent to leave the range
    if (exponent_ > targetFormat.maxExp()) {
      return sign ? negativeInfinity(targetFormat) : infinity(targetFormat);
    }
    return new FloatP(targetFormat, sign, exponent_, significand_);
  }

  public FloatP abs() {
    return new FloatP(format, false, exponent, significand);
  }

  public FloatP negate() {
    return new FloatP(format, !sign, exponent, significand);
  }

  public boolean greaterThan(FloatP number) {
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
    FloatP r = this.subtract(number);
    if (r.isZero()) {
      return false;
    }
    return !r.isNegative();
  }

  public FloatP add(FloatP number) {
    // Make sure the first argument has the larger (or equal) exponent
    FloatP a = number;
    FloatP b = this;
    if (exponent >= number.exponent) {
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
    long exponent1 = Math.max(a.exponent, format.minExp());
    long exponent2 = Math.max(b.exponent, format.minExp());

    // Calculate the difference between the exponents. If it is larger than the mantissa size we can
    // skip the add and return immediately.
    int exp_diff = (int) (exponent1 - exponent2);
    if (exp_diff >= format.sigBits + 3) {
      return a;
    }

    // Get the significands and apply the sign
    BigInteger significand1 = a.sign ? a.significand.negate() : a.significand;
    BigInteger significand2 = b.sign ? b.significand.negate() : b.significand;

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
    return new FloatP(format, sign_, exponent_, significand_);
  }

  public FloatP subtract(FloatP number) {
    return add(number.negate());
  }

  public FloatP multiply(FloatP number) {
    // Make sure the first argument has the larger (or equal) exponent
    FloatP a = number;
    FloatP b = this;
    if (exponent >= number.exponent) {
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
    boolean sign_ = sign ^ number.sign;

    // Get the exponents without the IEEE bias. Note that for subnormal numbers the stored exponent
    // needs to be increased by one.
    long exponent1 = Math.max(a.exponent, format.minExp());
    long exponent2 = Math.max(b.exponent, format.minExp());

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
    BigInteger significand1 = a.significand;
    BigInteger significand2 = b.significand;

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
    return new FloatP(format, sign_, exponent_, significand_);
  }

  /**
   * Multiply two numbers and return the exact result.
   *
   * <p>This variant of {@link FloatP#multiply} skips the rounding steps at the end and returns
   * directly. The result may have between p and 2p+1 bits.
   */
  private FloatP multiplyExact(FloatP number) {
    // Make sure the first argument has the larger (or equal) exponent
    FloatP a = number;
    FloatP b = this;
    if (exponent >= number.exponent) {
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

    // We assume both arguments are normal
    Preconditions.checkArgument(a.exponent >= format.minExp() && b.exponent >= format.minExp());

    // Calculate the sign of the result
    boolean sign_ = sign ^ number.sign;

    // Get the exponents without the IEEE bias. Note that for subnormal numbers the stored exponent
    // needs to be increased by one.
    long exponent1 = a.exponent;
    long exponent2 = b.exponent;

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
    BigInteger significand1 = a.significand;
    BigInteger significand2 = b.significand;

    BigInteger significand_ = significand1.multiply(significand2);

    // Normalize if the significand is too large:
    if (significand_.testBit(2 * format.sigBits + 4)) {
      exponent_ += 1;
    }

    // Return infinity if there is an overflow.
    if (exponent_ > format.bias()) {
      return sign_ ? negativeInfinity(format) : infinity(format);
    }

    // Otherwise return the number
    return new FloatP(
        new Format(format.expBits, significand_.bitLength() - 1), sign_, exponent_, significand_);
  }

  private FloatP squared() {
    return this.multiply(this);
  }

  private final Map<BigInteger, FloatP> powMap = new HashMap<>();

  /** Calculate the power function a^x where x is an integer. */
  public FloatP powInt(BigInteger exp) {
    return withPrecision(format.extended()).powInt_(exp).withPrecision(format);
  }

  private FloatP powInt_(BigInteger exp) {
    if (!powMap.containsKey(exp)) {
      FloatP x = this;
      if (exp.compareTo(BigInteger.ZERO) < 0) {
        // TODO: Find a bound for the number of extra bits needed
        Format ext = new Format(format.expBits, 2 * format.sigBits + 1);
        x = one(ext).divide_(x.withPrecision(ext));
      }
      FloatP r = x.powFast(exp.abs());
      powMap.put(exp, r);
    }
    return powMap.get(exp);
  }

  private FloatP powFast(BigInteger exp) {
    if (exp.equals(BigInteger.ZERO)) {
      return one(format);
    }
    if (exp.equals(BigInteger.ONE)) {
      return this;
    }
    FloatP r = powInt_(exp.divide(BigInteger.valueOf(2))).squared();
    FloatP p = exp.mod(BigInteger.valueOf(2)).equals(BigInteger.ZERO) ? one(format) : this;
    return p.multiply(r);
  }

  public FloatP divide(FloatP number) {
    FloatP a = this.withPrecision(format.extended());
    FloatP b = number.withPrecision(format.extended());
    return a.divide_(b).withPrecision(format);
  }

  /* Divide the value by another number.
   * This version of divide is slower and does not check for corner cases. It is still needed to
   * calculate the constants that are needed for the other, faster version of divide that uses
   * Newton's method.
   */
  private FloatP divideSlow(FloatP number) {
    // Calculate the sign of the result
    boolean sign_ = sign ^ number.sign;

    // Get the exponents without the IEEE bias. Note that for subnormal numbers the stored exponent
    // needs to be increased by one.
    long exponent1 = Math.max(exponent, format.minExp());
    long exponent2 = Math.max(number.exponent, format.minExp());

    // Normalize both arguments.
    BigInteger significand1 = significand;
    int shift1 = (format.sigBits + 1) - significand1.bitLength();
    if (shift1 > 0) {
      significand1 = significand1.shiftLeft(shift1);
      exponent1 -= shift1;
    }

    BigInteger significand2 = number.significand;
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
    return new FloatP(format, sign_, exponent_, significand_);
  }

  // These constants are needed for division with Newton's approach.
  private static final FloatP c48 = constant(Format.Float256, 48);
  private static final FloatP c32 = constant(Format.Float256, 32);
  private static final FloatP c17 = constant(Format.Float256, 17);

  // Here we have to use the slow divide for the constants.
  private static final FloatP c48d17 = c48.divideSlow(c17);
  private static final FloatP c32d17 = c32.divideSlow(c17);

  private double lb(double number) {
    return Math.log(number) / Math.log(2);
  }

  private FloatP divide_(FloatP number) {
    FloatP a = this;
    FloatP b = number;

    boolean sign_ = a.isNegative() ^ b.isNegative(); // Sign of the result

    // Handle special cases:
    // (1) Either argument is NaN
    if (a.isNan() || b.isNan()) {
      return nan(format);
    }
    // (2) Dividend is zero
    if (a.isZero()) {
      if (b.isZero()) {
        // Divisor is zero or infinite
        return sign_ ? nan(format).negate() : nan(format);
      }
      return sign_ ? negativeZero(format) : zero(format);
    }
    // (3) Dividend is infinite
    if (a.isInfinite()) {
      if (b.isInfinite()) {
        // Divisor is infinite
        return sign_ ? nan(format).negate() : nan(format);
      }
      return sign_ ? negativeInfinity(format) : infinity(format);
    }
    // (4) Divisor is zero (and dividend is finite)
    if (b.isZero()) {
      return sign_ ? negativeInfinity(format) : infinity(format);
    }
    // (5) Divisor is infinite (and dividend is finite)
    if (b.isInfinite()) {
      return sign_ ? negativeZero(format) : zero(format);
    }

    // Extract exponents and significand bits
    int exponent1 = (int) Math.max(a.exponent, format.minExp());
    int exponent2 = (int) Math.max(b.exponent, format.minExp());

    BigInteger significand1 = a.significand;
    BigInteger significand2 = b.significand;

    // Shift numerator and divisor by pulling out common factors in the exponent.
    // This will put the divisor in the range of 0.5 to 1.0
    FloatP n = new FloatP(format, false, exponent1 - (exponent2 + 1), significand1);
    FloatP d = new FloatP(format, false, -1, significand2);

    // Calculate how many iterations are needed
    int bound = (int) Math.ceil(lb((format.sigBits + 2) / lb(17)));

    // Set the initial value to 48/32 - 32/17*D
    FloatP t1 = c48d17.withPrecision(format);
    FloatP t2 = c32d17.withPrecision(format);

    FloatP x = t1.subtract(t2.multiply(d));

    for (int i = 0; i < bound; i++) {
      // X(i+1) = X(i)*(2 - D*X(i))
      x = x.multiply(constant(format, 2).subtract(d.multiply(x)));
    }

    // Multiply 1/D with N
    FloatP r = x.multiply(n);

    // Set the sign bit and return the result
    return r.withSign(a.sign ^ b.sign);
  }

  public FloatP sqrt() {
    // The calculation will be done in a higher precision and the result is then rounded down.
    // 2p+2 bits are enough for the inverse square root.
    // See Table 12.6 in "Handbook of Floating-Point Arithmetic"
    Format extended = new Format(format.expBits, 2 * format.sigBits + 2);
    return withPrecision(extended).sqrt_().withPrecision(format);
  }

  private FloatP sqrt_() {
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
    long exponent_ = Math.max(exponent, format.minExp());
    BigInteger significand_ = significand;

    // Normalize the argument
    int shift = (format.sigBits + 1) - significand_.bitLength();
    if (shift > 0) {
      significand_ = significand_.shiftLeft(shift);
      exponent_ -= shift;
    }

    // Range reduction:
    // sqrt(f * 2^2m) = sqrt(f)*2^m
    FloatP f = new FloatP(format, sign, exponent_ % 2, significand_);

    // Define constants
    // TODO: These constants should be declared only once for each supported precision
    FloatP c1d2 = new FloatP(format, false, -1, BigInteger.ONE.shiftLeft(format.sigBits));
    FloatP c3d2 = new FloatP(format, false, 0, BigInteger.valueOf(3).shiftLeft(format.sigBits - 1));

    // Initial value (0.5 will always converge)
    FloatP x = c1d2;

    boolean done = false;
    List<FloatP> partial = new ArrayList<>();
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
    FloatP r = constant(format, 2).powInt(BigInteger.valueOf(exponent_ / 2));
    return x.multiply(r);
  }

  // Strip invalid digits from the significand. This assumes that the number is transcendental.
  private FloatP validPart() {
    // In round to nearest invalid digits follow one of two patterns:
    // 1(0)+ or 1(0)+
    // After truncating these bits from the end all other digits are equal to the digits of the
    // infinite actual number.
    if (isZero() || isNan() || isInfinite()) {
      return this;
    }
    if (BigInteger.ONE.shiftLeft(format.sigBits).equals(significand)) {
      return this;
    }
    BigInteger significand_ = significand;
    boolean last = significand_.testBit(0);

    // Search for the pattern
    int trailing = 1;
    do {
      significand_ = significand_.shiftRight(1);
      trailing++;
    } while (significand_.testBit(0) == last);

    significand_ = significand_.shiftRight(1);
    return new FloatP(
        new Format(format.expBits, trailing > format.sigBits ? 0 : (format.sigBits - trailing)),
        sign,
        exponent,
        significand_);
  }

  /** The minimal distance between two floating point values with the same exponent. */
  private FloatP oneUlp() {
    BigInteger significand_ = BigInteger.ONE.shiftLeft(format.sigBits);
    long exponent_ = exponent - format.sigBits;
    return new FloatP(format, sign, exponent_, significand_);
  }

  /** Returns the next larger floating point number. */
  private FloatP plus1Ulp() {
    return add(oneUlp());
  }

  /** Returns the floating point number immediately below this number. */
  private FloatP minus1Ulp() {
    return add(oneUlp().negate());
  }

  /** Compare two floating point numbers for equality when rounded to a lower precision p. * */
  private static boolean equalModuloP(Format format, FloatP a, FloatP b) {
    return a.withPrecision(format).equals(b.withPrecision(format));
  }

  /**
   * Check if a floating point number is stable in precision p.
   *
   * <p>Needed as part of the "rounding test": we have to make sure that the number is not too close
   * to a break point before rounding.
   */
  private boolean isStable(FloatP r) {
    if (r.format.sigBits == 0) {
      return false;
    }
    return equalModuloP(format, r, r.plus1Ulp());
  }

  // Statistics for exp(...)
  static final Map<Integer, Integer> expStats = new HashMap<>();

  /**
   * Returns a list of formats for the calculation of exp(x).
   *
   * <p>For transcendental functions like exp(x) the calculation has to be repeated with increasing
   * precision until enough (valid) digits are available to round the value correctly. This method
   * returns a list of intermediate formats with increasing precision that was specially optimized
   * for the precision of the input. The idea is that we only have to try 2-3 different precisions
   * before the right one is found, which is much more efficient than increasing the precision one
   * bit at a time.
   */
  private ImmutableList<Format> expExtFormats() {
    if (format.equals(Format.Float8)) {
      //      0.1    0.2    0.3    0.4    0.5    0.6    0.7    0.8    0.9    1.0
      // p      1      1      1      4      4      5      5      6      7     13
      Format p = new Format(11, format.sigBits + 13);
      return ImmutableList.of(p);
    }
    if (format.equals(Format.Float16)) {
      //      0.1    0.2    0.3    0.4    0.5    0.6    0.7    0.8    0.9    1.0
      // p      1      1      1      6      8      9     10     12     13     26
      Format p1 = new Format(11, format.sigBits + 13);
      Format p2 = new Format(11, format.sigBits + 26);
      return ImmutableList.of(p1, p2);
    }
    if (format.equals(Format.Float32)) {
      //      0.1    0.2    0.3    0.4    0.5    0.6    0.7    0.8    0.9    1.0
      // p      1      1      1      1     15     25     25     25     26     41
      Format p = new Format(15, format.sigBits + 41);
      return ImmutableList.of(p, p.extended());
    }
    if (format.equals(Format.Float64)) {
      //      0.1    0.2    0.3    0.4    0.5    0.6    0.7    0.8    0.9    1.0
      // p     1      1      1      1     28     54     54     55     55     68
      Format p = new Format(Format.Float256.expBits, format.sigBits + 60);
      return ImmutableList.of(p, p.extended());
    }
    Format p = new Format(Format.Float256.expBits, 2 * format.sigBits);
    return ImmutableList.of(p, p.extended());
  }

  public FloatP exp() {
    if (isZero()) {
      return one(format);
    }
    if (isNan()) {
      return nan(format);
    }
    if (!abs().greaterThan(minNormal(format))) {
      // Return one immediately if the argument is close to zero
      return one(format);
    }

    FloatP r = nan(format);
    boolean done = false;

    for (Format p : expExtFormats()) {
      if (!done) {
        Format p_ext = new Format(p.expBits, p.sigBits - format.sigBits);
        FloatP x = withPrecision(p_ext);

        // TODO: Call exp_ only once and *then* check if we're too close to a break point
        FloatP v1 = x.plus1Ulp().withPrecision(p).exp_();
        FloatP v2 = x.minus1Ulp().withPrecision(p).exp_();

        if (equalModuloP(format, v1, v2) && isStable(v1.validPart())) {
          done = true;
          r = v1;

          // Update statistics
          Integer k = p.sigBits - format.sigBits;
          expStats.put(k, expStats.getOrDefault(k, 0) + 1);
        }
      }
    }
    return r.withPrecision(format);
  }

  private static final Map<Integer, BigInteger> facMap = new HashMap<>();

  private static BigInteger fac(int k) {
    return facMap.computeIfAbsent(k, FloatP::fac_);
  }

  private static BigInteger fac_(Integer k) {
    if (k == 0 || k == 1) {
      return BigInteger.ONE;
    }
    return fac(k - 1).multiply(BigInteger.valueOf(k));
  }

  private static ImmutableMap<Integer, FloatP> mkExpTable(Format pFormat) {
    ImmutableMap.Builder<Integer, FloatP> builder = ImmutableMap.builder();
    builder.put(0, one(pFormat));
    for (int k = 1; k < 100; k++) { // TODO: Find a bound that depends on the precision
      // Calculate 1/k! and store the values in the table
      builder.put(k, one(pFormat).divide_(constant(pFormat, fac(k))));
    }
    return builder.buildOrThrow();
  }

  // Table contains terms 1/k! for 1..100
  private static final ImmutableMap<Integer, FloatP> expTable = mkExpTable(Format.Float256);

  private FloatP exp_() {
    return expImpl(0);
  }

  private FloatP expm1_() {
    return expImpl(1);
  }

  private FloatP expImpl(int k) { // k is the first term of the expansion
    if (isNan()) {
      return nan(format);
    }
    if (isInfinite()) {
      return isNegative() ? zero(format) : infinity(format);
    }

    if (!format.sup(Format.Float256).equals(Format.Float256)) {
      // We created expTable with 256 bit precision. If the required precision is larger than that
      // the table would have to be recalculated.
      // TODO: Support arbitrary precisions by rebuilding the table when necessary
      throw new IllegalArgumentException();
    }

    FloatP x = this;
    FloatP r = zero(format); // Series expansion after k terms.

    // Range reduction: exp(a * 2^k) = exp(a)^2k
    if (exponent > 0) {
      x = x.withExponent(0);
    }

    boolean done = false;
    while (!done) {
      FloatP s = r;

      // Check that expTable was created with enough terms
      if (!expTable.containsKey(k)) {
        // We populated lnTable with the first 100 terms of the taylor expansion. If more is needed
        // an exception is thrown.
        // TODO: Support arbitrary precisions by rebuilding the table when necessary
        throw new IllegalArgumentException();
      }

      // r(k+1) = r(k) +  x^k/k!
      FloatP a = x.powInt_(BigInteger.valueOf(k));
      FloatP b = expTable.get(k).withPrecision(format);

      r = r.add(a.multiply(b));

      // Abort if we have enough precision
      done = r.equals(s);
      k++;
    }

    // Square the result to recover the exponent
    for (int i = 0; i < exponent; i++) {
      r = r.squared();
    }
    return r;
  }

  static final Map<Integer, Integer> lnStats = new HashMap<>();

  private ImmutableList<Format> lnExtFormats() {
    if (format.equals(Format.Float8)) {
      //      0.1    0.2    0.3    0.4    0.5    0.6    0.7    0.8    0.9    1.0
      // p      1      1      1      1      1      4      5      6      7     10
      Format p = new Format(11, format.sigBits + 13);
      return ImmutableList.of(p);
    }
    if (format.equals(Format.Float16)) {
      //      0.1    0.2    0.3    0.4    0.5    0.6    0.7    0.8    0.9    1.0
      // p      1      1      1      1      1      9     10     11     12     31
      Format p1 = new Format(11, format.sigBits + 12);
      Format p2 = new Format(11, format.sigBits + 31);
      return ImmutableList.of(p1, p2);
    }
    if (format.equals(Format.Float32)) {
      //      0.1    0.2    0.3    0.4    0.5    0.6    0.7    0.8    0.9    1.0
      // p      1      1      1      1     18     19     20     21     22     35
      Format p = new Format(15, format.sigBits + 22);
      return ImmutableList.of(p, p.extended());
    }
    if (format.equals(Format.Float64)) {
      //      0.1    0.2    0.3    0.4    0.5    0.6    0.7    0.8    0.9    1.0
      // p      1      1      1      1     44     45     46     47     48     62
      Format p = new Format(Format.Float256.expBits, format.sigBits + 48);
      return ImmutableList.of(p, p.extended());
    }

    // FIXME: Why is broken for 32bit exponents?
    Format p = new Format(/*32*/ Format.Float256.expBits, 2 * format.sigBits);
    return ImmutableList.of(p, p.extended());
  }

  public FloatP ln() {
    if (isZero()) {
      return negativeInfinity(format);
    }
    if (isOne()) {
      return zero(format);
    }

    FloatP r = nan(format);
    boolean done = false;

    for (Format p : lnExtFormats()) {
      if (!done) {
        Format p_ext = new Format(p.expBits, p.sigBits - format.sigBits);
        FloatP x = withPrecision(p_ext);

        // TODO: Call ln only once and *then* check if we're too close to a break point
        FloatP x1 = x.plus1Ulp().withPrecision(p);
        FloatP x2 = x.minus1Ulp().withPrecision(p);

        FloatP v1 = x1.ln_();
        FloatP v2 = x2.ln_();

        if (equalModuloP(format, v1, v2) && isStable(v1.validPart())) {
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

  private static ImmutableMap<Integer, FloatP> mkLnTable(Format pFormat) {
    ImmutableMap.Builder<Integer, FloatP> builder = ImmutableMap.builder();
    for (int k = 1; k < 1000; k++) { // TODO: Find a bound that depends on the precision
      // Calculate 1/k and store the values in the table
      builder.put(k, one(pFormat).divide_(constant(pFormat, k)));
    }
    return builder.buildOrThrow();
  }

  // Table contains terms 1/k for k=1..100
  private static final ImmutableMap<Integer, FloatP> lnTable = mkLnTable(Format.Float256);

  private static FloatP make_ln2(Format pFormat) {
    FloatP r = constant(pFormat, 2).sqrt_().subtract(one(pFormat)).ln1p();
    return r.withExponent(r.exponent + 1);
  }

  private static final FloatP const_ln2 = make_ln2(Format.Float256);

  private FloatP ln_() {
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

    // ln(x) = ln(a * 2^k) = ln a + ln 2^k = ln a + k * ln 2
    FloatP a = withExponent(-1);
    FloatP lna = a.subtract(one(format)).ln1p();

    FloatP ln2 = const_ln2.withPrecision(format);
    FloatP nln2 = constant(format, (int) exponent + 1).multiply(ln2);
    return lna.add(nln2);
  }

  private FloatP ln1p() {
    if (!format.sup(Format.Float256).equals(Format.Float256)) {
      // We created expTable with 256 bit precision. If the required precision is larger than that
      // the table would have to be recalculated.
      // TODO: Support arbitrary precisions by rebuilding the table when necessary
      throw new IllegalArgumentException();
    }
    FloatP x = this;
    FloatP r = zero(format);

    int k = 1;
    boolean done = false;
    while (!done) {
      FloatP r0 = r;

      // Check that lnTable was created with enough terms
      if (!lnTable.containsKey(k)) {
        // We populated lnTable with the first 1000 terms of the taylor expansion. If more is needed
        // an exception is thrown.
        // TODO: Support arbitrary precisions by rebuilding the table when necessary
        throw new IllegalArgumentException();
      }

      // r(k+1) = r(k) +  x^k/k
      FloatP a = x.powInt_(BigInteger.valueOf(k));
      FloatP b = a.multiply(lnTable.get(k).withPrecision(format));

      r = r.add(k % 2 == 0 ? b.negate() : b);

      // Abort if we have enough precision
      done = r.equals(r0);
      k++;
    }
    return r;
  }

  public FloatP pow(FloatP pExponent) {
    FloatP a = this;
    FloatP x = pExponent;

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
      FloatP power = x.isNegative() ? zero(format) : infinity(format);
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
      return a.powInt(x.toInteger());
    }
    FloatP c1d2 = new FloatP(format, false, -1, BigInteger.ONE.shiftLeft(format.sigBits));
    if (x.equals(c1d2)) {
      // TODO: Also include a^3/2 in this check?
      return a.sqrt();
    }
    FloatP r = a.abs().pow_(pExponent);
    if (a.isNegative()) {
      // Fix the sign if `a` was negative and x an integer
      r = r.withSign(x.isOddInteger());
    }
    if (r.isNan()) {
      // If the result is NaN we assume that the real result is a floating point number (or a break
      // point between two floating point number). Unlike exp and log, pow has many arguments where
      // this is the case. Examples include a^0, a^1, a^k (where k is an integer), or a^0.5 (where
      // 'a') is a square number.
      // We still check some of the trivial cases earlier for performance reasons.
      // The more general check is more costly, however, so it is only performed after the search
      // in the main algorithm has failed.
      r = a.powExact(x);
    }
    return r;
  }

  /** Check if the argument is a square number and, if so, return its square root. */
  private Optional<FloatP> sqrtExact() {
    FloatP a = this;
    FloatP r = a.sqrt();
    FloatP b = r.multiplyExact(r);

    FloatP x = b.withPrecision(format).withPrecision(b.format);
    FloatP y = b.subtract(x);

    return y.isZero() ? Optional.of(r) : Optional.empty();
  }

  /** Handle cases in pow where a^x is a floating point number or a breakpoint. */
  private FloatP powExact(FloatP exp) {
    Format p = new Format(Format.Float256.expBits, format.sigBits);
    FloatP a = this.withPrecision(p);
    FloatP x = exp.withPrecision(p);

    FloatP r = nan(format);
    boolean done = false;

    while (!done && !x.isInfinite()) { // TODO: Derive better bounds based on the exponent range
      // Rewrite a^x with a=b^2 and x=y/2 as b^y until we're left with an integer exponent
      Optional<FloatP> val = a.sqrtExact();
      if (val.isEmpty()) {
        // Abort if 'a' is not a square number
        break;
      }
      a = val.orElseThrow();
      x = x.withExponent(x.exponent + 1);

      if (x.isInteger()) {
        done = true;
        r = a.powInt(x.toInteger());
      }
    }
    return r.withPrecision(format);
  }

  static final Map<Integer, Integer> powStats = new HashMap<>();

  private ImmutableList<Format> powExtFormats() {
    if (format.equals(Format.Float8)) {
      //    0.1    0.2    0.3    0.4    0.5    0.6    0.7    0.8    0.9    1.0
      // p    6     13     13     13     13     16     16     16     16     16
      Format p = new Format(11, format.sigBits + 19);
      return ImmutableList.of(p); // exhaustive, so we only need one
    }
    if (format.equals(Format.Float16)) {
      //    0.1    0.2    0.3    0.4    0.5    0.6    0.7    0.8    0.9    1.0
      // p    7     10     13     15     17     19     20     22     23     25
      Format p = new Format(11, format.sigBits + 25);
      return ImmutableList.of(p, p.extended());
    }
    if (format.equals(Format.Float32)) {
      //    0.1    0.2    0.3    0.4    0.5    0.6    0.7    0.8    0.9    1.0
      // p   13     18     22     25     29     31     34     36     39     41
      Format p = new Format(15, format.sigBits + 41);
      return ImmutableList.of(p, p.extended());
    }
    if (format.equals(Format.Float64)) {
      //    0.1    0.2    0.3    0.4    0.5    0.6    0.7    0.8    0.9    1.0
      // p   31     39     44     49     53     57     61     64     67     70
      Format p = new Format(Format.Float256.expBits, format.sigBits + 71);
      return ImmutableList.of(p, p.extended());
    }

    Format p = new Format(Format.Float256.expBits, 2 * format.sigBits);
    return ImmutableList.of(p, p.extended());
  }

  private FloatP pow_(FloatP pExponent) {
    FloatP r = nan(format);
    boolean done = false;

    for (Format p : powExtFormats()) {
      if (!done) {
        // a^x = exp(x * ln a)
        Format ext = new Format(p.expBits, p.sigBits - format.sigBits);

        FloatP a = this.withPrecision(p);
        FloatP x = pExponent.withPrecision(p);

        // The next call calculates ln with the current precision.
        // TODO: Figure out why this is enough?
        FloatP lna = a.ln_();
        FloatP xlna = x.multiply(lna).withPrecision(ext);

        // Check if we call e^x with x close to zero
        boolean nearZero = !xlna.abs().greaterThan(minNormal(format));

        // Calculate a bound for the value of e^(x * ln a)
        // TODO: Call exp only once and *then* check if we're too close to a break point
        FloatP xlna1 = xlna.plus1Ulp().withPrecision(p);
        FloatP xlna2 = xlna.minus1Ulp().withPrecision(p);

        FloatP exlna1 = nearZero ? xlna1.expm1_() : xlna1.exp_();
        FloatP exlna2 = nearZero ? xlna2.expm1_() : xlna2.exp_();

        // Proceed if the result is stable in the original precision
        // If the result was close to zero we have to use an extended format that allows larger
        // exponents. Otherwise, the values are too small and will be flushed to zero.
        Format p0 = new Format(Format.Float256.expBits, format.sigBits);

        if (equalModuloP(nearZero ? p0 : format, exlna1, exlna2) && isStable(exlna1.validPart())) {
          done = true;
          r = nearZero ? one(p).add(exlna1) : exlna1;

          // Update statistics
          powStats.put(ext.sigBits, powStats.getOrDefault(0, ext.sigBits) + 1);
        }
      }
    }
    return r.withPrecision(format);
  }

  public FloatP roundToInteger(RoundingMode rm) {
    // If the argument is infinite, just return it
    if (isInfinite()) {
      return this;
    }
    // For -NaN we drop the sign to make the implementation in line with MPFR
    if (isNan()) {
      return this.abs();
    }
    // If the exponent is large enough we already have an integer and can return immediately
    if (exponent > format.sigBits) {
      return this;
    }

    // Get the significand and add grs bits
    BigInteger significand_ = significand;
    significand_ = significand_.shiftLeft(3);

    // Shift the fractional part to the right and then round the result
    significand_ = truncate(significand_, (int) (format.sigBits - exponent));
    significand_ = applyRounding(rm, sign, significand_);

    // Recalculate the exponent
    int exponent_ = significand_.bitLength() - 1;

    // Normalize the significand if there was an overflow. The last bit is then always zero and can
    // simply be dropped.
    significand_ = significand_.shiftLeft(format.sigBits - exponent_);

    // Check if the result is zero
    if (significand_.equals(BigInteger.ZERO)) {
      return isNegative() ? negativeZero(format) : zero(format);
    }
    // Check if we need to round to infinity
    if (exponent_ > format.maxExp()) {
      return isNegative() ? negativeInfinity(format) : infinity(format);
    }
    return new FloatP(format, sign, exponent_, significand_);
  }

  private static FloatP fromInteger(Format format, BigInteger number) {
    // Return +0.0 for input 0
    if (number.equals(BigInteger.ZERO)) {
      return new FloatP(format, false, 0, BigInteger.ZERO);
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

    return new FloatP(format, sign, exponent, significand);
  }

  private BigInteger toInteger() {
    if (exponent < -1) {
      return BigInteger.ZERO;
    }
    // Shift the significand to truncate the fractional part. For large exponents the expression
    // 'format.sigBits - exponent' will become negative, and the shift is to the left, adding
    // additional zeroes.
    BigInteger significand_ = significand.shiftRight((int) (format.sigBits - exponent));
    if (sign) {
      significand_ = significand_.negate();
    }
    return significand_;
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
    return new BigFloat(sign, significand, exponent, BinaryMathContext.BINARY32).floatValueExact();
  }

  public double toDouble() {
    if (isNan()) {
      return Double.NaN;
    }
    if (isInfinite()) {
      return isNegative() ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
    }
    return new BigFloat(sign, significand, exponent, BinaryMathContext.BINARY64).doubleValueExact();
  }

  public static FloatP fromBigFloat(Format p, BigFloat pValue) {
    if (pValue.isNaN()) {
      return pValue.sign() ? nan(p).negate() : nan(p);
    }
    if (pValue.isInfinite()) {
      return pValue.sign() ? negativeInfinity(p) : infinity(p);
    }
    long exp = pValue.exponent(p.minExp(), p.maxExp());
    BigInteger sig = pValue.significand(p.minExp(), p.maxExp());
    return new FloatP(p, pValue.sign(), exp, sig);
  }

  public BigFloat toBigFloat() {
    BinaryMathContext context = new BinaryMathContext(format.sigBits + 1, format.expBits);
    if (isNan()) {
      return isNegative()
          ? BigFloat.NaN(context.precision).negate()
          : BigFloat.NaN(context.precision);
    }
    if (isInfinite()) {
      return isNegative()
          ? BigFloat.negativeInfinity(context.precision)
          : BigFloat.positiveInfinity(context.precision);
    }
    return new BigFloat(sign, significand, exponent, context);
  }

  private static ImmutableList<Format> fromStringExtFormats(Format format) {
    // Whereas for the transcendental functions we can find one extended format that will work for
    // all inputs, here we need to use provide several as the rewrite in buildValue only works for
    // powers of ten.
    // TODO: Find a better solution

    if (format.equals(Format.Float8)) {
      //      0.1    0.2    0.3    0.4    0.5    0.6    0.7    0.8    0.9    1.0
      // p      3      3      3      3      4      4      4      4      4      9
      Format p = new Format(11, format.sigBits + 12);
      return ImmutableList.of(p);
    }
    if (format.equals(Format.Float16)) {
      //      0.1    0.2    0.3    0.4    0.5    0.6    0.7    0.8    0.9    1.0
      // p      3      3      3      3      4      4      4      4      5     12
      Format p1 = new Format(11, format.sigBits + 3);
      Format p2 = new Format(11, format.sigBits + 4);
      Format p3 = new Format(11, format.sigBits + 5);
      Format p4 = new Format(11, format.sigBits + 12);
      return ImmutableList.of(p1, p2, p3, p4);
    }
    if (format.equals(Format.Float32)) {
      //      0.1    0.2    0.3    0.4    0.5    0.6    0.7    0.8    0.9    1.0
      // p      3      3      3      3      4      4      4      4      5     11
      ImmutableList.Builder<Format> builder = ImmutableList.builder();
      builder.add(new Format(15, format.sigBits + 3));
      builder.add(new Format(15, format.sigBits + 4));
      builder.add(new Format(15, format.sigBits + 5));
      builder.add(new Format(15, format.sigBits + 15));
      for (int i = 16; i < 32; i++) {
        builder.add(new Format(15, format.sigBits + i));
      }
      return builder.build();
    }
    if (format.equals(Format.Float64)) {
      //      0.1    0.2    0.3    0.4    0.5    0.6    0.7    0.8    0.9    1.0
      // p      3      3      3      4      4      4      4      5      6     10
      ImmutableList.Builder<Format> builder = ImmutableList.builder();
      builder.add(new Format(15, format.sigBits + 3));
      builder.add(new Format(15, format.sigBits + 4));
      builder.add(new Format(15, format.sigBits + 5));
      builder.add(new Format(15, format.sigBits + 6));
      builder.add(new Format(15, format.sigBits + 12));
      for (int i = 13; i < 32; i++) {
        builder.add(new Format(15, format.sigBits + i));
      }
      return builder.build();
    }

    ImmutableList.Builder<Format> builder = ImmutableList.builder();
    for (int i = 1; i < 100; i++) {
      builder.add(new Format(Format.Float256.expBits, format.sigBits + i));
    }
    return builder.build();
  }

  // Statistics for fromString(...)
  static final Map<Integer, Integer> fromStringStats = new HashMap<>();

  public static FloatP fromString(Format p, String input) {
    // TODO: Add error handling for broken inputs.
    if ("inf".equals(input)) {
      return infinity(p);
    }
    if ("-inf".equals(input)) {
      return negativeInfinity(p);
    }
    if ("nan".equals(input)) {
      return nan(p);
    }

    // Split off the exponent part (if there is one)
    int sep = Math.max(input.indexOf('e'), input.indexOf('E'));
    String digits = sep > -1 ? input.substring(0, sep) : input;
    String exponent = sep > -1 ? input.substring(sep + 1) : "0";

    boolean sign = false;

    // Determine the sign
    char pre = digits.charAt(0);
    if (!Character.isDigit(pre)) {
      Preconditions.checkArgument(pre == '+' || pre == '-');
      digits = digits.substring(1);
      sign = pre == '-';
    }

    int expValue = Integer.parseInt(exponent);

    // Get the fractional part of the number (and add ".0" if it has none)
    int radix = digits.indexOf('.');
    if (radix == -1) {
      radix = digits.length();
      digits = digits + ".0";
    }

    // Normalize the mantissa, then fix the exponent
    expValue = (radix - 1) + expValue;
    digits = digits.substring(0, radix) + digits.substring(radix + 1);

    BigInteger mantissa = new BigInteger(digits);
    if (mantissa.equals(BigInteger.ZERO)) {
      return sign ? negativeZero(p) : zero(p);
    }

    boolean done = false;
    FloatP r = nan(p);

    for (Format ext : fromStringExtFormats(p)) {
      if (!done) {
        int diff = ext.sigBits - p.sigBits;

        FloatP f = constant(ext, mantissa.multiply(BigInteger.TEN.pow(diff)));
        FloatP e =
            constant(ext, 10).powInt(BigInteger.valueOf(expValue - (digits.length() - 1) - diff));

        FloatP val1 = f.plus1Ulp().multiply(e);
        FloatP val2 = f.minus1Ulp().multiply(e);

        if (equalModuloP(p, val1, val2)) {
          done = true;
          r = sign ? val1.negate() : val1;

          // Update statistics
          fromStringStats.put(diff, fromStringStats.getOrDefault(diff, 0) + 1);
        }
      }
    }
    return r.withPrecision(p);
  }

  /**
   * The number of digits needed to represent this value as a string.
   *
   * <p>We borrow the definition from MPFR:
   *
   * <pre>
   * "Return the minimal integer m such that any number of p bits, when output with m digits
   *  in radix b with rounding to nearest, can be recovered exactly when read again, still
   *  with rounding to nearest."
   *  </pre>
   */
  private int neededDigits() {
    return 1 + (int) Math.ceil((format.sigBits + 1) * Math.log(2) / Math.log(10));
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

    // Get the exponent and the significand
    BigInteger significand_ = significand;
    long exponent_ = Math.max(exponent, format.minExp());

    // Normalize the value if it is subnormal
    int shift = (format.sigBits + 1) - significand_.bitLength();
    if (shift > 0) {
      significand_ = significand_.shiftLeft(shift);
      exponent_ -= shift;
    }

    // Shift the exponent to make the significand an integer
    exponent_ -= format.sigBits;

    // p is the number of decimal digits needed to recover the original number if the output of
    // toString is read back with fromString
    int p = neededDigits();

    // We define an extended format that has enough precision to hold the intermediate result
    // during the conversion from binary to decimal representation
    int ext = 2 * p + 2; // FIXME: Make sure this is enough

    // Build a term for the exponent in decimal representation
    MathContext rm = new MathContext(ext, java.math.RoundingMode.HALF_EVEN);
    BigDecimal r = new BigDecimal(BigInteger.ONE.shiftLeft(Math.abs((int) exponent_)));
    if (exponent_ < 0) {
      r = BigDecimal.ONE.divide(r, rm);
    }

    // Convert the significand to BigDecimal and multiply with the decimal exponent term
    BigDecimal a = new BigDecimal(significand_);
    BigDecimal b = a.multiply(r);

    // Round the result down to p significand digits
    BigDecimal rounded =
        b.plus(new MathContext(p, java.math.RoundingMode.HALF_EVEN)).stripTrailingZeros();

    // Print the output string
    String repr = String.format("%." + p + "e", rounded);
    repr = repr.replaceAll("(\\.0+e)|(0+e)", "e"); // Drop trailing zeroes

    // Add the sign if necessary
    return isNegative() ? "-" + repr : repr;
  }

  /** Print the number in base2 representation. */
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
    String bits = significand.toString(2);
    bits = "0".repeat(format.sigBits + 1 - bits.length()) + bits;
    return "%s%s.%s e%d".formatted(sign ? "-" : "", bits.charAt(0), bits.substring(1), exponent);
  }

  long extractExpBits() {
    return exponent + format.bias();
  }

  BigInteger extractSigBits() {
    return significand;
  }
}
