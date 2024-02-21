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
    private final int expBits;
    private final int sigBits;

    public Format(int pExpBits, int pSigBits) {
      expBits = pExpBits;
      sigBits = pSigBits;
    }

    static Format FLOAT = new Format(8, 23);
    static Format DOUBLE = new Format(11, 52);

    @Override
    public boolean equals(Object other) {
      if (this == other) {
        return true;
      }
      if (other instanceof Format that) {
        return this.expBits == that.expBits && this.sigBits == that.sigBits;
      }
      return false;
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
    return (value.exponent == format.maxExp() + 1) && (value.significand.equals(BigInteger.ZERO));
  }

  public boolean isZero() {
    return (value.exponent == format.minExp() - 1) && (value.significand.equals(BigInteger.ZERO));
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
    if (n.isInfinite()) { // No need to check m as it can't be larger and one is finite
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

  public MyFloat addImpl(MyFloat number) {
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
    if (grs == 4 && significand_.testBit(0) || grs > 4) {
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
    throw new UnsupportedOperationException();
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
