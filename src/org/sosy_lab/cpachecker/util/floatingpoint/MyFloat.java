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

    public static final Format FLOAT = new Format(8, 23);
    public static final Format DOUBLE = new Format(11, 52);

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
  }

  private final Format format;
  private final FpValue value;

  public MyFloat(Format pFormat, FpValue pValue) {
    format = pFormat;
    value = pValue;
  }

  public MyFloat(Format pFormat, boolean pSign, long pExponent, BigInteger pSignificand) {
    format = pFormat;
    value = new FpValue(pSign, pExponent, pSignificand);
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

  public static MyFloat negativeInfinity(Format pFormat) {
    FpValue value = new FpValue(true, pFormat.maxExp() + 1, BigInteger.ZERO);
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

  public boolean isNan() {
    return !value.sign
        && (value.exponent == format.maxExp() + 1)
        && (value.significand.compareTo(BigInteger.ZERO) > 0);
  }

  public boolean isInfinite() {
    return (value.exponent == format.maxExp() + 1) && value.significand.equals(BigInteger.ZERO);
  }

  public boolean isZero() {
    return (value.exponent == format.minExp() - 1) && value.significand.equals(BigInteger.ZERO);
  }

  public boolean isNegative() {
    return value.sign;
  }

  public MyFloat negate() {
    if (isNan()) {
      return MyFloat.nan(format);
    }
    return new MyFloat(format, new FpValue(!value.sign, value.exponent, value.significand));
  }

  public MyFloat add(MyFloat number) {
    // Make sure the first argument has the larger (or equal) exponent
    MyFloat n = number;
    MyFloat m = this;
    if (value.exponent >= number.value.exponent) {
      n = this;
      m = number;
    }

    // Handle special cases:
    // (1) Either argument is NaN
    if (n.isNan() || m.isNan()) {
      return MyFloat.nan(format);
    }
    // (2) Both arguments are infinite
    if (n.isInfinite() && m.isInfinite()) {
      if (n.isNegative() && m.isNegative()) {
        return MyFloat.negativeInfinity(format);
      }
      if (!n.isNegative() && !m.isNegative()) {
        return MyFloat.infinity(format);
      }
      return MyFloat.nan(format);
    }
    // (3) Only one argument is infinite
    if (n.isInfinite()) { // No need to check m as it can't be larger, and one of the args is finite
      return n;
    }
    // (4) Both arguments are zero (or negative zero)
    if (n.isZero() && m.isZero()) {
      return (n.isNegative() || m.isNegative())
          ? MyFloat.negativeZero(format)
          : MyFloat.zero(format);
    }

    // Handle regular numbers in addImpl
    return n.addImpl(m);
  }

  private MyFloat addImpl(MyFloat number) {
    // Get the exponents and add the bias to shift them above zero. Note that for subnormal numbers
    // the stored exponent needs to be increased by one.
    long exponent1 = Math.max(value.exponent, format.minExp()) + format.bias();
    long exponent2 = Math.max(number.value.exponent, format.minExp()) + format.bias();

    // Calculate the difference between the exponents. If it is larger than the mantissa size we can
    // skip the add and return immediately.
    long exp_diff = exponent1 - exponent2;
    if (Math.abs(exp_diff) > format.sigBits + 1) {
      return this;
    }

    // Get the signficands and apply the sign
    BigInteger significand1 = value.sign ? value.significand.negate() : value.significand;
    BigInteger significand2 =
        number.value.sign ? number.value.significand.negate() : number.value.significand;

    // Expand the significand with (empty) guard, round and sticky bits
    significand1 = significand1.shiftLeft(3);
    significand2 = significand2.shiftLeft(3);

    // Shift the number with the smaller exponent to the exponent of the other number.
    // Update the grs bits during the shift.
    for (int i = 0; i < exp_diff; i++) {
      BigInteger carry = significand2.and(BigInteger.ONE);
      significand2 = significand2.shiftRight(1);
      significand2 = significand2.or(carry);
    }

    // Add the two significands
    BigInteger result = significand1.add(significand2);

    // Extract the sign and value of the significand from the result
    boolean sign_ = result.signum() < 0;
    BigInteger significand_ = result.abs();

    // The result has the same exponent as the larger of the two arguments
    long exponent_ = value.exponent;

    // Normalize
    // (1) Significand is too large: shift to the right by one bit
    //     This can happen if two numbers with equal exponent are added:
    //     f.ex 1.0e3 + 1.0e3 = 10.0e3
    //     (here we normalize the result to 1.0e4)
    if (significand_.testBit(format.sigBits + 4)) {
      significand_ = significand_.shiftRight(1);
      exponent_ += 1;
    }

    // (2) Significand is too small: shift left unless the number is subnormal
    //     This can happen if digits have canceled out:
    //     f.ex 1.01001e2 + (-1.01e2) = 0.00001e2
    //     (here we normalize to 1.0e-3)
    String bits = significand_.toString(2);
    // FIXME: Handle subnormals
    int offset = (format.sigBits + 4) - bits.length();
    if (offset > 0 && exponent_ > format.minExp()) {
      significand_ = significand_.shiftLeft(offset);
      exponent_ -= offset;
    }

    // Round the result according to the grs bits
    long grs = significand_.and(new BigInteger("111", 2)).longValue();
    significand_ = significand_.shiftRight(3);
    if ((grs == 4 && significand_.testBit(0)) || grs > 4) {
      significand_ = significand_.add(BigInteger.ONE);
    }

    // Check if the result is zero
    if (significand_.equals(BigInteger.ZERO)) {
      return sign_ ? negativeZero(format) : zero(format);
    }

    // Return infinity if there is an overflow.
    if (exponent_ > format.bias()) {
      return sign_ ? MyFloat.negativeInfinity(format) : MyFloat.infinity(format);
    }

    // Otherwise return the number
    return new MyFloat(format, new FpValue(sign_, exponent_, significand_));
  }

  public MyFloat subtract(MyFloat number) {
    // We need to override the special case "0 - 0"
    if (this.isZero() && number.isZero()) {
      return MyFloat.zero(format);
    }
    // Everything else is just as for addition
    return add(number.negate());
  }

  public MyFloat multiply(MyFloat number) {
    // Make sure the first argument has the larger (or equal) exponent
    MyFloat n = number;
    MyFloat m = this;
    if (value.exponent >= number.value.exponent) {
      n = this;
      m = number;
    }

    // Handle special cases:
    // (1) Either argument is NaN
    if (n.isNan() || m.isNan()) {
      return MyFloat.nan(format);
    }
    // (2) One of the argument is infinite
    if (n.isInfinite()) { // No need to check m as it can't be larger, and one of the args is finite
      if (m.isZero()) {
        // Return NaN if we're trying to multiply infinity by zero
        return MyFloat.nan(format);
      }
      return (n.isNegative() ^ m.isNegative())
          ? MyFloat.negativeInfinity(format)
          : MyFloat.infinity(format);
    }
    // (3) One of the arguments is zero (or negative zero)
    if (n.isZero() || m.isZero()) {
      return (n.isNegative() ^ m.isNegative())
          ? MyFloat.negativeZero(format)
          : MyFloat.zero(format);
    }

    // Handle regular numbers in multiplyImpl
    return n.multiplyImpl(m);
  }

  private MyFloat multiplyImpl(MyFloat number) {
    // Calculate the sign of the result
    boolean sign_ = value.sign ^ number.value.sign;

    // Get the exponents without the IEEE bias. Note that for subnormal numbers the stored exponent
    // needs to be increased by one.
    long exponent1 = Math.max(value.exponent, format.minExp());
    long exponent2 = Math.max(number.value.exponent, format.minExp());

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
    BigInteger significand1 = value.significand;
    BigInteger significand2 = number.value.significand;

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
    String bits = significand_.toString(2);
    int shift = (2 * format.sigBits + 4) - bits.length();
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

    // Truncate the number while carrying over the grs bits.
    // The significand now has length 2*|precision of the significand| + 3 where 3 are the grs bits
    // at the end. We need to shift by at least |precision of the significand| bits. If one of the
    // factors was subnormal the results may have leading zeroes as well, and we need to shift
    // further by 'leading' bits.
    for (int i = 0; i < format.sigBits + leading; i++) {
      BigInteger carry = significand_.and(BigInteger.ONE);
      significand_ = significand_.shiftRight(1);
      significand_ = significand_.or(carry);
    }

    // Round the result according to the grs bits
    long grs = significand_.and(new BigInteger("111", 2)).longValue();
    significand_ = significand_.shiftRight(3);
    if ((grs == 4 && significand_.testBit(0)) || grs > 4) {
      significand_ = significand_.add(BigInteger.ONE);
    }

    // Return infinity if there is an overflow.
    if (exponent_ > format.bias()) {
      return sign_ ? MyFloat.negativeInfinity(format) : MyFloat.infinity(format);
    }

    // Otherwise return the number
    return new MyFloat(format, new FpValue(sign_, exponent_, significand_));
  }

  public MyFloat divide(MyFloat number) {
    throw new UnsupportedOperationException();
  }

  public float toFloat() {
    Preconditions.checkState(format.expBits == 8 && format.sigBits == 23);
    if (isNan()) {
      return Float.NaN;
    }
    if (isInfinite()) {
      return isNegative() ? Float.NEGATIVE_INFINITY : Float.POSITIVE_INFINITY;
    }
    return new BigFloat(value.sign, value.significand, value.exponent, BinaryMathContext.BINARY32)
        .floatValueExact();
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
    return String.format("%.6e", toFloat());
  }
}
