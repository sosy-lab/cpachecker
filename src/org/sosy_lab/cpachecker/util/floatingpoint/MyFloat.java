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

// Several functions here require higher precision variables for their intermediate results.
// Currently, we assume the arguments are float and then use double precision for these variables.
// TODO: Rewrite for arbitrary precision.

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

  public static MyFloat one(Format pFormat) {
    FpValue value = new FpValue(false, 0, BigInteger.ONE.shiftLeft(pFormat.sigBits));
    return new MyFloat(pFormat, value);
  }

  public static MyFloat constant(Format pFormat, BigInteger number) {
    return new MyFloat(pFormat, number);
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
    MyFloat n = this;
    MyFloat m = number;

    // Handle special cases:
    // (1) Either argument is NaN
    if (n.isNan() || m.isNan()) {
      return MyFloat.nan(format);
    }
    // (2) Dividend is zero
    if (n.isZero()) {
      if (m.isZero()) {
        // Divisor is zero or infinite
        return MyFloat.nan(format);
      }
      return (n.isNegative() ^ m.isNegative())
          ? MyFloat.negativeZero(format)
          : MyFloat.zero(format);
    }
    // (3) Dividend is infinite
    if (n.isInfinite()) {
      if (m.isInfinite()) {
        // Divisor is infinite
        return MyFloat.nan(format);
      }
      return (n.isNegative() ^ m.isNegative())
          ? MyFloat.negativeInfinity(format)
          : MyFloat.infinity(format);
    }
    // (4) Divisor is zero (and dividend is finite)
    if (m.isZero()) {
      return (n.isNegative() ^ m.isNegative())
          ? MyFloat.negativeInfinity(format)
          : MyFloat.infinity(format);
    }
    // (5) Divisor is infinite (and dividend is finite)
    if (m.isInfinite()) {
      return (n.isNegative() ^ m.isNegative())
          ? MyFloat.negativeZero(format)
          : MyFloat.zero(format);
    }

    // Handle regular numbers in divideImpl
    return n.divideNewton(m);
  }

  private MyFloat divideImpl(MyFloat number) {
    // Calculate the sign of the result
    boolean sign_ = value.sign ^ number.value.sign;

    // Get the exponents without the IEEE bias. Note that for subnormal numbers the stored exponent
    // needs to be increased by one.
    long exponent1 = Math.max(value.exponent, format.minExp());
    long exponent2 = Math.max(number.value.exponent, format.minExp());

    // Normalize both arguments.
    BigInteger significand1 = value.significand;
    String bits1 = significand1.toString(2);
    int shift1 = (format.sigBits + 1) - bits1.length();
    if (shift1 > 0) {
      significand1 = significand1.shiftLeft(shift1);
      exponent1 -= shift1;
    }

    BigInteger significand2 = number.value.significand;
    String bits2 = significand2.toString(2);
    int shift2 = (format.sigBits + 1) - bits2.length();
    if (shift2 > 0) {
      significand2 = significand2.shiftLeft(shift2);
      exponent2 -= shift2;
    }

    // Calculate the exponent of the result by subtracting the exponent of the divisor from the
    // exponent of the dividend. If the result is beyond the exponent range, skip the calculation
    // and return infinity immediately. If it is below the subnormal range, return zero immediately.
    long exponent_ = exponent1 - exponent2;
    if (exponent_ > format.maxExp()) {
      return sign_ ? negativeInfinity(format) : infinity(format);
    }
    if (exponent_ < format.minExp() - (format.sigBits + 1)) {
      return sign_ ? negativeZero(format) : zero(format);
    }

    // Calculate how many digits need to be calculated. If the result is <1 we need one additional
    // digit to normalize it.
    // TODO: Figure out how many digits we actually need
    int hack = format.sigBits;
    int digits = hack + (1 + format.sigBits) + 3; // last 3 is for grs bits
    if (significand1.compareTo(significand2) < 0) {
      digits += 1;
      exponent_ -= 1;
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
    // Here we calculate haw many digits we need to shift:
    int leading = 0;
    if (exponent_ < format.minExp()) {
      leading = (int) Math.abs(format.minExp() - exponent_);
      exponent_ = format.minExp() - 1;
    }

    // Truncate the number while carrying over the grs bits.
    for (int i = 0; i < hack + leading; i++) {
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

    // Return the number
    return new MyFloat(format, new FpValue(sign_, exponent_, significand_));
  }

  private double lb(double number) {
    return Math.log(number) / Math.log(2);
  }

  private MyFloat divideNewton(MyFloat number) {
    // TODO: Calculate these constants only once for each floating point precision
    MyFloat c48 = MyFloat.constant(Format.DOUBLE, BigInteger.valueOf(48));
    MyFloat c32 = MyFloat.constant(Format.DOUBLE, BigInteger.valueOf(32));
    MyFloat c17 = MyFloat.constant(Format.DOUBLE, BigInteger.valueOf(17));

    // Define constants t1 and t2 for the calculation of the initial value
    MyFloat t1 = c48.divideImpl(c17);
    MyFloat t2 = c32.divideImpl(c17);

    // Extract exponents and significand bits
    int exponent1 = (int) Math.max(value.exponent, format.minExp());
    int exponent2 = (int) Math.max(number.value.exponent, format.minExp());

    // Normalize both arguments
    BigInteger significand1 = value.significand;
    String bits1 = significand1.toString(2);
    int shift1 = (format.sigBits + 1) - bits1.length();
    if (shift1 > 0) {
      significand1 = significand1.shiftLeft(shift1);
      exponent1 -= shift1;
    }

    BigInteger significand2 = number.value.significand;
    String bits2 = significand2.toString(2);
    int shift2 = (format.sigBits + 1) - bits2.length();
    if (shift2 > 0) {
      significand2 = significand2.shiftLeft(shift2);
      exponent2 -= shift2;
    }

    // Extend the significands to double precision
    significand1 = significand1.shiftLeft(Format.DOUBLE.sigBits - format.sigBits);
    significand2 = significand2.shiftLeft(Format.DOUBLE.sigBits - format.sigBits);

    // Shift numerator and divisor by pulling out common factors in the exponent.
    // This will put the divisor in the range of 0.5 to 1.0
    MyFloat n = new MyFloat(Format.DOUBLE, false, exponent1 - (exponent2 + 1), significand1);
    MyFloat d = new MyFloat(Format.DOUBLE, false, -1, significand2);

    // Calculate how many iterations are needed
    int bound = (int) Math.ceil(lb((format.sigBits + 2) / lb(17)));

    // Set the initial value to 48/32 - 32/17*D
    MyFloat x = t1.subtract(t2.multiply(d));
    for (int i = 0; i < bound; i++) {
      // X(i+1) = X(i)*(2 - D*X(i))
      x =
          x.multiply(
              MyFloat.constant(Format.DOUBLE, BigInteger.valueOf(2)).subtract(d.multiply(x)));
    }

    // Multiply 1/D with N and round down to single precision
    MyFloat r = x.multiply(n).withPrecision(format);

    // Set the sign bit and return the result
    return new MyFloat(
        format, value.sign ^ number.value.sign, r.value.exponent, r.value.significand);
  }

  public MyFloat withPrecision(Format targetFormat) {
    if (isNan()) {
      return MyFloat.nan(targetFormat);
    }
    if (isInfinite()) {
      return value.sign ? MyFloat.negativeInfinity(targetFormat) : MyFloat.infinity(targetFormat);
    }

    long exponent = Math.max(value.exponent, format.minExp());
    BigInteger significand = value.significand;

    // Normalization
    // If the number is subnormal shift it upward and adjust the exponent
    String bits = significand.toString(2);
    int shift = (format.sigBits + 1) - bits.length();
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

    // Extend the significand
    significand = significand.shiftLeft(targetFormat.sigBits + 3);

    // Use the lowest possible exponent and move the rest into the significand by shifting
    // it to the right.
    // Here we calculate haw many digits we need to shift:
    int leading = 0;
    if (exponent < targetFormat.minExp()) {
      leading = (int) Math.abs(targetFormat.minExp() - exponent);
      exponent = targetFormat.minExp() - 1;
    }

    // Truncate the number while carrying over the grs bits.
    for (int i = 0; i < format.sigBits + leading; i++) {
      BigInteger carry = significand.and(BigInteger.ONE);
      significand = significand.shiftRight(1);
      significand = significand.or(carry);
    }

    // Round the result according to the grs bits
    long grs = significand.and(new BigInteger("111", 2)).longValue();
    significand = significand.shiftRight(3);
    if ((grs == 4 && significand.testBit(0)) || grs > 4) {
      significand = significand.add(BigInteger.ONE);
    }

    // Normalize if rounding caused an overflow
    if (significand.testBit(targetFormat.sigBits + 1)) {
      significand = significand.shiftRight(1); // The last bit is zero
      exponent += 1;
    }

    return new MyFloat(targetFormat, value.sign, exponent, significand);
  }

  public MyFloat squared() {
    return multiply(this);
  }

  public MyFloat sqrt() {
    if (isZero()) {
      return MyFloat.negativeZero(format);
    }
    if (isNan() || isNegative()) {
      return MyFloat.nan(format);
    }
    if (isInfinite()) {
      return MyFloat.infinity(format);
    }
    return sqrtImpl();
  }

  private MyFloat sqrtImpl() {
    // Declare some constants
    MyFloat const1_5 =
        new MyFloat(
            Format.DOUBLE, false, 0, BigInteger.valueOf(3).shiftLeft(Format.DOUBLE.sigBits - 1));
    MyFloat const0_5 =
        new MyFloat(Format.DOUBLE, false, -1, BigInteger.ONE.shiftLeft(Format.DOUBLE.sigBits));

    // Get the exponent and the significand of the argument
    long exponent = Math.max(value.exponent, format.minExp());
    BigInteger significand = value.significand;

    // Normalize the argument
    String bits = significand.toString(2);
    int shift = (format.sigBits + 1) - bits.length();
    if (shift > 0) {
      significand = significand.shiftLeft(shift);
      exponent -= shift;
    }

    // Convert the significand from float to double
    // All intermediate results have to be calculated in double precision to avoid rounding errors
    significand = significand.shiftLeft(Format.DOUBLE.sigBits - format.sigBits);

    // Pull the exponent part from the square root
    long exponent_f = exponent % 2;
    long exponent_r = exponent / 2;

    // Define f (the transformed argument) and x, the initial value for the inverse square root
    MyFloat f = new MyFloat(Format.DOUBLE, value.sign, exponent_f, significand);
    MyFloat x = const0_5; // Will converge, but might be slow. TODO: Find a better initial value.

    // Repeat the approximation until we have enough precision
    // TODO: Figure out a bound for the number of iterations
    for (int i = 0; i < 7; i++) {
      // x_n+1 = x_n * (3/2 - 1/2 * x_n^2)
      x = x.multiply(const1_5.subtract(const0_5.multiply(f).multiply(x.squared())));
    }

    // r is the exponent part that we pulled out of sqrt()
    MyFloat r =
        new MyFloat(
            Format.DOUBLE, false, exponent_r, BigInteger.ONE.shiftLeft(Format.DOUBLE.sigBits));

    // Multiply the inverse square root with f again to get the square root. Then convert the result
    // back to single precision.
    return x.multiply(f).multiply(r).withPrecision(format);
  }

  public MyFloat exp() {
    if (isNan()) {
      return MyFloat.nan(format);
    }
    if (isInfinite()) {
      return isNegative() ? MyFloat.zero(format) : MyFloat.infinity(format);
    }
    MyFloat result = expImpl();
    // Filter out NaN results that we seem to be getting for large negative x
    // TODO: Return zero immediately if the result would be too small
    return result.isNan() ? MyFloat.zero(format) : result;
  }

  private MyFloat expImpl() {
    MyFloat one = MyFloat.one(Format.DOUBLE);
    MyFloat x = this.withPrecision(Format.DOUBLE);

    MyFloat xs = one; // x^k (1 for k=0)
    MyFloat fs = one; // k!  (1 for k=0)

    MyFloat r = one;
    for (int k = 1; k < 100; k++) { // TODO: Find a proper bound for the number of iterations
      // Calculate x^n/k!
      xs = xs.multiply(x);
      fs = fs.multiply(MyFloat.constant(Format.DOUBLE, BigInteger.valueOf(k)));

      // Add it to the sum
      r = r.add(xs.divide(fs));
    }
    return r.withPrecision(format);
  }

  public MyFloat ln() {
    if (isZero()) {
      return negativeInfinity(format);
    }
    if (isNan() || isNegative()) {
      return MyFloat.nan(format);
    }
    if (isInfinite()) {
      return infinity(format);
    }
    return lnImpl();
  }

  public MyFloat lnImpl() {
    MyFloat x = this.withPrecision(Format.DOUBLE);
    int preprocess = 10;
    for (int i = 0; i < preprocess; i++) {
      x = x.sqrt();
    }
    x = x.subtract(MyFloat.one(Format.DOUBLE));

    MyFloat xs = MyFloat.one(Format.DOUBLE); // x^k (1 for k=0)
    MyFloat r = MyFloat.zero(Format.DOUBLE);

    for (int k = 1; k < 20; k++) { // TODO: Find a proper bound for the number of iterations
      // Calculate x^n/k
      xs = xs.multiply(x);
      MyFloat term = xs.divide(MyFloat.constant(Format.DOUBLE, BigInteger.valueOf(k)));

      // Add the sign and then build the sum
      r = r.add(k % 2 == 0 ? term.negate() : term);
    }
    MyFloat p =
        new MyFloat(
            Format.DOUBLE, r.value.sign, r.value.exponent + preprocess, r.value.significand);
    return p.withPrecision(format);
  }

  public MyFloat pow(MyFloat exponent) {
    // x^y = e^(y * ln x)
    return exponent.multiply(this.ln()).exp();
  }

  public MyFloat abs() {
    return new MyFloat(format, false, value.exponent, value.significand);
  }

  public boolean greaterThan(MyFloat number) {
    if (this.isNan() || number.isNan()) {
      return false;
    }
    if (this.isInfinite() && !this.isNegative()) {
      if (number.isInfinite() && !number.isNegative()) {
        return false;
      }
      return true;
    }
    if (this.isInfinite() && this.isNegative() && number.isInfinite() && number.isNegative()) {
      return false;
    }
    MyFloat r = this.subtract(number);
    if (r.isZero()) {
      return false;
    }
    return !r.isNegative();
  }

  public enum RoundingMode {
    NEAREST, // Round to nearest, ties to even
    CEILING, // Round toward +∞
    FLOOR, // Round toward -∞
    TRUNCATE // Round toward 0
  }

  private BigInteger applyRounding(
      RoundingMode rm, boolean negative, BigInteger significand, long grs) {
    BigInteger plusOne = significand.add(BigInteger.ONE);
    return switch (rm) {
      case NEAREST -> ((grs == 4 && significand.testBit(0)) || grs > 4) ? plusOne : significand;
      case CEILING -> (grs > 0 && !negative) ? plusOne : significand;
      case FLOOR -> (grs > 0 && negative) ? plusOne : significand;
      case TRUNCATE -> significand;
    };
  }

  public MyFloat roundToInteger(RoundingMode rm) {
    // If exponent is too large to leave any fractional part, return immediately
    if (value.exponent > format.sigBits) {
      return this;
    }

    // Extract significand and apply the sign, then add grs bits
    BigInteger significand =
        value.significand; // value.sign ? value.significand.negate() : value.significand;
    significand = significand.shiftLeft(3);

    // Shift away the fractional part
    int shift = (int) (format.sigBits - value.exponent);
    for (int i = 0; i < shift; i++) {
      BigInteger carry = significand.and(BigInteger.ONE);
      significand = significand.shiftRight(1);
      significand = significand.or(carry);
    }

    // Drop the grs bits and round the result according to the selected rounding mode
    long grs = significand.abs().and(new BigInteger("111", 2)).longValue();
    significand = significand.shiftRight(3);
    significand = applyRounding(rm, value.sign, significand, grs);

    // Calculate exponent and normalize the significand
    int exponent = significand.toString(2).length() - 1;
    // (May shift one bit to the right if rounding caused an overflow. This is safe as the last bit
    // is then always zero.)
    significand = significand.shiftLeft(format.sigBits - exponent);

    return new MyFloat(format, value.sign, exponent, significand);
  }

  private FpValue fromInteger(BigInteger number) {
    if (number.equals(BigInteger.ZERO)) {
      return new FpValue(false, 0, BigInteger.ZERO);
    }
    int exponent = number.toString(2).length() - 1;
    BigInteger significand = number.abs().shiftLeft(format.sigBits + 3);

    // Truncate the number while carrying over the grs bits.
    for (int i = 0; i < exponent; i++) {
      BigInteger carry = significand.and(BigInteger.ONE);
      significand = significand.shiftRight(1);
      significand = significand.or(carry);
    }

    // Round the result according to the grs bits
    long grs = significand.and(new BigInteger("111", 2)).longValue();
    significand = significand.shiftRight(3);
    if ((grs == 4 && significand.testBit(0)) || grs > 4) {
      significand = significand.add(BigInteger.ONE);
    }

    return new FpValue(number.signum() < 0, exponent, significand);
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

  public byte toByte() {
    BigInteger integerValue = toInteger();
    if (isNan()) {
      return 0;
    }
    BigInteger maxPositive = BigInteger.valueOf(Byte.MAX_VALUE);
    if ((isInfinite() && !isNegative()) || integerValue.compareTo(maxPositive) > 0) {
      return -1;
    }
    BigInteger maxNegative = BigInteger.valueOf(Byte.MIN_VALUE);
    if ((isInfinite() && isNegative()) || integerValue.compareTo(maxNegative) < 0) {
      return 0;
    }
    return integerValue.byteValue();
  }

  public short toShort() {
    BigInteger integerValue = toInteger();
    if (isNan()) {
      return 0;
    }
    BigInteger maxPositive = BigInteger.valueOf(Short.MAX_VALUE);
    if ((isInfinite() && !isNegative()) || integerValue.compareTo(maxPositive) > 0) {
      return -1;
    }
    BigInteger maxNegative = BigInteger.valueOf(Short.MIN_VALUE);
    if ((isInfinite() && isNegative()) || integerValue.compareTo(maxNegative) < 0) {
      return 0;
    }
    return integerValue.shortValue();
  }

  public int toInt() {
    BigInteger integerValue = toInteger();
    if (isNan()) {
      return 0;
    }
    BigInteger maxPositive = BigInteger.valueOf(Integer.MAX_VALUE);
    if ((isInfinite() && !isNegative()) || integerValue.compareTo(maxPositive) > 0) {
      return maxPositive.intValue();
    }
    BigInteger maxNegative = BigInteger.valueOf(Integer.MIN_VALUE);
    if ((isInfinite() && isNegative()) || integerValue.compareTo(maxNegative) < 0) {
      return maxNegative.intValue();
    }
    return integerValue.intValue();
  }

  public long toLong() {
    BigInteger integerValue = toInteger();
    if (isNan()) {
      return 0;
    }
    BigInteger maxPositive = BigInteger.valueOf(Long.MAX_VALUE);
    if ((isInfinite() && !isNegative()) || integerValue.compareTo(maxPositive) > 0) {
      return maxPositive.longValue();
    }
    BigInteger maxNegative = BigInteger.valueOf(Long.MIN_VALUE);
    if ((isInfinite() && isNegative()) || integerValue.compareTo(maxNegative) < 0) {
      return maxNegative.longValue();
    }
    return integerValue.longValue();
  }

  public float toFloat() {
    Preconditions.checkState(format.equals(Format.FLOAT));
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
    Preconditions.checkState(format.equals(Format.DOUBLE));
    if (isNan()) {
      return Double.NaN;
    }
    if (isInfinite()) {
      return isNegative() ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
    }
    return new BigFloat(value.sign, value.significand, value.exponent, BinaryMathContext.BINARY64)
        .doubleValueExact();
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
    return String.format("%.6e", format.equals(Format.FLOAT) ? toFloat() : toDouble());
  }
}
