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
import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

// TODO: Add support for more rounding modes
// TODO: Add more functions (like sin(x), etc)
// TODO: Make castToOther return an Optional
//  This is needed when the target type for castToOther is too small for the integer value of the
//  float. See the comment before toByte() for more details.
// TODO: Add support for unsigned types

/**
 * Java based implementation of multi-precision floating point values with correct rounding.
 *
 * <p>Values are created with a fixed precision and an exponent range. The nested class {@link
 * Format} is used to provide these parameters and supports predefined formats for the most commonly
 * used bit sizes. Binary operations will upcast their arguments if the precisions don't match.
 *
 * <p>We guarantee "correct rounding" for all operations, that is the result is always the same as
 * if the calculation had been performed with infinite precision before rounding down to the
 * precision of the final result. This can easily be ensured for the "algebraic" operations in this
 * class, that is addition, subtraction, multiplication, division and the square root. Here worst
 * case bounds exist on the number of extra digits that need to be calculated before the number can
 * always be rounded correctly. The same is not true for transcendental functions where such bounds
 * are unknown and may not even exist. This problem is known as <a
 * href="https://en.wikipedia.org/wiki/Rounding#Table-maker's_dilemma">"Table-maker's dilemma"</a>.
 * Luckily for the transcendental functions {@link FloatValue#exp()}, {@link FloatValue#ln()} and
 * {@link FloatValue#pow(FloatValue)}) from this class it can be shown that only a finite number of
 * extra digits are needed for correct rounding. This follows from Lindemann’s theorem that e^z is
 * transcendental for every nonzero algebraic complex number z. Since floating point numbers are
 * algebraic, the result of the calculation can never fall exactly on a floating point value, or the
 * break-point between two floating point values. It is therefore enough to repeat the calculation
 * with increasing precision until the result no longer falls on a break-point and can be correctly
 * rounded. This approach is know as <a href="https://dl.acm.org/doi/pdf/10.1145/114697.116813">
 * Ziv's technique</a> and requires a "rounding test" that decides if the value calculated in the
 * current iteration can be correctly rounded. Such test depend on the rounding mode used, but for
 * "round-to-nearest-ties-to-even" (the standard rounding mode in IEEE 754, and what is being used
 * by this implementation) it can be enough to simply look for patterns of the form 01+ or 10+ at
 * the end of the significand. These last few digits are exactly on the break-point in the current
 * iteration and can still change when the calculation is repeated with a higher precision.
 * Everything before that is stable, however, and we implement the rounding test by checking if
 * there are enough stable bits to round down to the final precision of the result.
 *
 * @see <a href="https://link.springer.com/book/10.1007/978-3-319-76526-6">Handbook of
 *     Floating-Point Arithmetic (12.1.1 The Table Maker’s Dilemma, 12.4.1 Lindemann’s theorem,
 *     11.6.3 Rounding test)</a>
 */
public class FloatValue extends Number {
  @Serial private static final long serialVersionUID = 293351032085106407L;

  /**
   * Map with the pre-calculated values of k!
   *
   * <p>We need these values in {@link FloatValue#lookupExpTable(int)} to calculate the Taylor
   * expansion for e^x. Storing the values in this global maps allows us to avoid costly
   * recalculations. This is thread-safe as we use {@link java.util.concurrent.ConcurrentHashMap}
   * for the Map and the calculation of k! is side effect free.
   */
  private static final Map<Integer, BigInteger> FACTORIALS = new ConcurrentHashMap<>();

  /** Names for the constants used in {@link ConstantsKey} */
  enum ConstantsName {
    SQRT_INITIAL_T,
    EXP_TABLE,
    LN_TABLE,
    CONSTANT_LN
  }

  /**
   * Key for the {@link FloatValue#CONSTANTS} Map.
   *
   * @param format Specified the precision and exponent range of the format that the constant was
   *     calculated for.
   * @param f The name of the constant.
   * @param arg An index for the name.
   */
  private record ConstantsKey(Format format, ConstantsName f, int arg) {}

  /**
   * Map with pre-calculated constants.
   *
   * <p>The constants are needed by various methods in this class and have to be calculated once for
   * every precision. This map holds the results to avoid costly recalculations. To access one the
   * constants the methods {@link FloatValue#lookupSqrtT1()}, {@link FloatValue#lookupSqrtT2()},
   * {@link FloatValue#lookupExpTable(int)}, {@link FloatValue#lookupLnTable(int)} and {@link
   * FloatValue#lookupLn2()} should be used.
   */
  private static final Map<ConstantsKey, FloatValue> CONSTANTS = new ConcurrentHashMap<>();

  /**
   * Lookup the value 48/17 from the constant table.
   *
   * <p>Required as the initial value for Newton's method in {@link FloatValue#sqrt()}
   */
  private FloatValue lookupSqrtT1() {
    return CONSTANTS.computeIfAbsent(
        new ConstantsKey(format, ConstantsName.SQRT_INITIAL_T, 1),
        (ConstantsKey val) -> fromInteger(format, 48).divideSlow(fromInteger(format, 17)));
  }

  /**
   * Lookup the value 32/17 from the constant table.
   *
   * <p>Required as the initial value for Newton's method in {@link FloatValue#sqrt()}
   */
  private FloatValue lookupSqrtT2() {
    return CONSTANTS.computeIfAbsent(
        new ConstantsKey(format, ConstantsName.SQRT_INITIAL_T, 2),
        (ConstantsKey val) -> fromInteger(format, 32).divideSlow(fromInteger(format, 17)));
  }

  /**
   * Lookup the value 1/k! from the constant table.
   *
   * <p>Required by {@link FloatValue#exp()} to speed up the expansion of the Taylor series.
   */
  private FloatValue lookupExpTable(int k) {
    ConstantsKey key = new ConstantsKey(format, ConstantsName.EXP_TABLE, k);
    if (!CONSTANTS.containsKey(key)) {
      CONSTANTS.put(key, one(format).divide_(fromInteger(format, factorial(k, FACTORIALS))));
    }
    return CONSTANTS.get(key);
  }

  /**
   * Lookup the value 1/k from the constant table.
   *
   * <p>Required by {@link FloatValue#ln()} to speed up the expansion of the Taylor series.
   */
  private FloatValue lookupLnTable(int k) {
    ConstantsKey key = new ConstantsKey(format, ConstantsName.LN_TABLE, k);
    if (!CONSTANTS.containsKey(key)) {
      CONSTANTS.put(key, one(format).divide_(fromInteger(format, k)));
    }
    return CONSTANTS.get(key);
  }

  /**
   * Lookup the value ln(2) from the constant table.
   *
   * <p>Required by {@link FloatValue#ln()} to rewrite ln(x)=ln(a*2^k) as ln(a) + k*ln(2)
   */
  private FloatValue lookupLn2() {
    ConstantsKey key = new ConstantsKey(format, ConstantsName.CONSTANT_LN, 2);
    if (!CONSTANTS.containsKey(key)) {
      FloatValue r = fromInteger(format, 2).sqrt_().subtract(one(format)).ln1p();
      CONSTANTS.put(key, r.withExponent(r.exponent + 1));
    }
    return CONSTANTS.get(key);
  }

  /** Format, defines the precision of the value and the allowed exponent range. */
  private final Format format;

  /** Sign of the value, `true` if negative */
  private final boolean sign;

  /** Exponent of the value. */
  private final long exponent;

  /** Significand of the value. */
  private final BigInteger significand;

  /**
   * Defines the precision and the exponent range of a {@link FloatValue} value.
   *
   * <p>The precision of a FloatValue is equivalent to the length of its significand. Here the
   * 'hidden bit' is not counted. The exponent range can be derived from the width of the exponent
   * field.
   */
  public record Format(int expBits, int sigBits) implements Serializable {
    @Serial private static final long serialVersionUID = -6677404553596078315L;

    public Format {
      // Check that the arguments are valid
      Preconditions.checkArgument(
          expBits >= 2 && expBits <= 25, "Exponent field must be between 2 and 25 bits wide");
      Preconditions.checkArgument(
          sigBits >= 0, "Significand field must not have negative bit width");
    }

    /**
     * An 8bit floating-point format
     *
     * <p>There is no standardized IEEE 754-2008 format for 8bit values. We reserve 4 bits for the
     * exponent and use the remaining 4 for the significand, but other choices would be possible.
     *
     * @see <a href="https://en.wikipedia.org/wiki/Minifloat">Wikipedia</a>
     */
    static final Format Float8 = new Format(4, 3);

    /**
     * Half-precision floating-point format
     *
     * <p>16bit binary floating point format (<b>"binary16"</b>) as defined in the IEEE 754-2008
     * standard. Uses 4 bits for the 5 bits for the exponent and 11 for the significand.
     *
     * @see <a
     *     href="https://en.wikipedia.org/wiki/Half-precision_floating-point_format">Wikipedia</a>
     */
    public static final Format Float16 = new Format(5, 10);

    /**
     * Single-precision floating-point format
     *
     * <p>32bit binary floating point format (<b>"binary32"</b>) as defined in the IEEE 754-2008
     * standard. Uses 8 bits for the exponent and 24 for the significand.
     *
     * @see <a
     *     href="https://en.wikipedia.org/wiki/Single-precision_floating-point_format">Wikipedia</a>
     */
    public static final Format Float32 = new Format(8, 23);

    /**
     * Double-precision floating-point format
     *
     * <p>64bit binary floating point format (<b>"binary64"</b>) as defined in the IEEE 754-2008
     * standard. Uses 11 bits for the exponent and 53 for the significand.
     *
     * @see <a
     *     href="https://en.wikipedia.org/wiki/Double-precision_floating-point_format">Wikipedia</a>
     */
    public static final Format Float64 = new Format(11, 52);

    /**
     * Quadruple-precision floating-point format
     *
     * <p>128bit binary floating point format (<b>"binary128"</b>) as defined in the IEEE 754-2008
     * standard. Uses 15 bits for the exponent and 113 for the significand.
     *
     * @see <a
     *     href="https://en.wikipedia.org/wiki/Quadruple-precision_floating-point_format"f>Wikipedia</a>
     */
    public static final Format Float128 = new Format(15, 112);

    /**
     * Octuple-precision floating-point format
     *
     * <p>256bit binary floating point format ("<b>binary128</b>") as defined in the IEEE 754-2008
     * standard. Uses 19 bits for the exponent and 237 for the significand.
     *
     * @see <a
     *     href="https://en.wikipedia.org/wiki/Octuple-precision_floating-point_format">Wikipedia</a>
     */
    public static final Format Float256 = new Format(19, 236);

    /**
     * Extended-precision floating-point format
     *
     * <p>80bit binary floating point format as used by the x87 FPU. Uses 15 bits for the exponent
     * and 64 for the significand.
     *
     * @see <a href="https://en.wikipedia.org/wiki/Extended_precision">Wikipedia</a>
     */
    public static final Format Extended = new Format(15, 63);

    /**
     * The exponent 'bias' of a FloatValue value in this format.
     *
     * <p>In the IEEE 754 standard a `bias` is used to store negative exponents as unsigned numbers.
     * This bias can be calculated from the size of the exponent field as is done by this method.
     * The value needs to be added to the exponent to convert it to the IEEE representation.
     * Subtracting it from the IEEE representation will return the exponent to its unbiased form.
     *
     * @see <a href="https://en.wikipedia.org/wiki/Exponent_bias">Wikipedia</a>
     */
    long bias() {
      return (1L << (expBits - 1)) - 1;
    }

    /** The largest exponent supported by this format */
    public long minExp() {
      long rawExp = 1;
      return rawExp - bias();
    }

    /** The smallest (= largest negative) exponent supported by this format */
    public long maxExp() {
      long rawExp = (1L << expBits) - 2;
      return rawExp - bias();
    }

    /** Return a higher precision format meant to be used for intermediate results. */
    private Format intermediatePrecision() {
      // We need precision m = 2p + 2 for division, and an additional 2 bits for powInt
      // See Table 12.6 in "Handbook of Floating-Point Arithmetic"
      // FIXME: Find the exact formulas
      return new Format(Float256.expBits, 2 * (1 + sigBits) + 4);
    }

    /**
     * Returns the same format but with an (effectively) unlimited exponent field.
     *
     * <p>Can be used to effectively avoid having to deal with subnormal numbers as they can be
     * written in their normalized form in this larger format.
     */
    private Format withUnlimitedExponent() {
      return new Format(25, sigBits);
    }

    /**
     * Least upper bound of the two formats.
     *
     * <p>Used when implementing binary operations on FloatValue values, where a common format large
     * enough for both arguments needs to be found.
     */
    Format sup(Format pOther) {
      int newExp = Math.max(expBits, pOther.expBits);
      int newSig = Math.max(sigBits, pOther.sigBits);
      return new Format(newExp, newSig);
    }

    /**
     * Construct a Format for a {@link CType}.
     *
     * <p>Throws a {@link IllegalArgumentException} if the {@link CType} is not a floating point
     * type.
     */
    public static Format fromCType(CType pType) {
      if (pType.equals(CNumericTypes.FLOAT)) {
        return Format.Float32;
      } else if (pType.equals(CNumericTypes.DOUBLE)) {
        return Format.Float64;
      } else if (pType.equals(CNumericTypes.LONG_DOUBLE)) {
        return Format.Extended;
      } else {
        throw new IllegalArgumentException(
            String.format("`%s` is not a floating point type", pType));
      }
    }
  }

  /**
   * Create a floating point value.
   *
   * @param pFormat Format, defines the precision and the width of the exponent field
   * @param pSign Sign for the value, `true` if the value is negative
   * @param pExponent Exponent, without the IEEE bias
   * @param pSignificand Significand, including the leading bit that is hidden in the IEEE format
   */
  public FloatValue(Format pFormat, boolean pSign, long pExponent, BigInteger pSignificand) {
    // Exponent range
    Preconditions.checkArgument(
        pExponent >= pFormat.minExp() - 1 && pExponent <= pFormat.maxExp() + 1,
        "Exponent out of range");
    // Normal number
    Preconditions.checkArgument(
        pExponent == pFormat.minExp() - 1
            || pExponent == pFormat.maxExp() + 1
            || pSignificand.bitLength() == pFormat.sigBits + 1,
        "Significand has wrong length for the specified format");
    // Subnormal number, Inf and Nan
    Preconditions.checkArgument(
        (pExponent > pFormat.minExp() - 1 && pExponent < pFormat.maxExp() + 1)
            || pSignificand.bitLength() < pFormat.sigBits + 1,
        "Significand has wrong length for the specified format");
    format = pFormat;
    sign = pSign;
    exponent = pExponent;
    significand = pSignificand;
  }

  /** Return the {@link Format} of this value */
  public Format getFormat() {
    return format;
  }

  /**
   * The value "NaN", or "not a number"
   *
   * <p>NaN has many representations, and we always return the "canonical" representation that only
   * has the highest bit of the significand set to one. The sign bit is zero, but can be set by
   * {@link FloatValue#negate()}. The methods {@link FloatValue#abs()} and {@link
   * FloatValue#isNegative()} still as expected and {@link FloatValue#withPrecision(Format)} always
   * preserves the sign of NaN.
   *
   * <p>Users should not depend on any exact representation of NaN and use the method {@link
   * FloatValue#isNan()} instead.
   */
  public static FloatValue nan(Format pFormat) {
    return new FloatValue(
        pFormat, false, pFormat.maxExp() + 1, BigInteger.ONE.shiftLeft(pFormat.sigBits - 1));
  }

  /** Positive infinity. */
  public static FloatValue infinity(Format pFormat) {
    return new FloatValue(pFormat, false, pFormat.maxExp() + 1, BigInteger.ZERO);
  }

  /** Largest value that can be represented in this format */
  public static FloatValue maxValue(Format pFormat) {
    BigInteger allOnes = BigInteger.ONE.shiftLeft(pFormat.sigBits + 1).subtract(BigInteger.ONE);
    return new FloatValue(pFormat, false, pFormat.maxExp(), allOnes);
  }

  /* Constant 1.0 */
  public static FloatValue one(Format pFormat) {
    return new FloatValue(pFormat, false, 0, BigInteger.ONE.shiftLeft(pFormat.sigBits));
  }

  /** Smallest normal value that can be represented in this format. */
  public static FloatValue minNormal(Format pFormat) {
    return new FloatValue(
        pFormat, false, pFormat.minExp(), BigInteger.ONE.shiftLeft(pFormat.sigBits));
  }

  /**
   * Smallest absolute value (other than zero) that can be represented in this format.
   *
   * <p>Note that this value will be sub-normal. To get the smallest normal value use {@link
   * FloatValue#minNormal} instead.
   */
  public static FloatValue minValue(Format pFormat) {
    return new FloatValue(pFormat, false, pFormat.minExp() - 1, BigInteger.ONE);
  }

  /** Positive zero */
  public static FloatValue zero(Format pFormat) {
    return new FloatValue(pFormat, false, pFormat.minExp() - 1, BigInteger.ZERO);
  }

  /** Negative zero */
  public static FloatValue negativeZero(Format pFormat) {
    return new FloatValue(pFormat, true, pFormat.minExp() - 1, BigInteger.ZERO);
  }

  /** Constant -1.0 */
  public static FloatValue negativeOne(Format pFormat) {
    return new FloatValue(pFormat, true, 0, BigInteger.ONE.shiftLeft(pFormat.sigBits));
  }

  /** Negative infinity */
  public static FloatValue negativeInfinity(Format pFormat) {
    return new FloatValue(pFormat, true, pFormat.maxExp() + 1, BigInteger.ZERO);
  }

  /** Rue if the value is NaN */
  public boolean isNan() {
    return (exponent == format.maxExp() + 1) && (significand.compareTo(BigInteger.ZERO) > 0);
  }

  /** True if the value is infinite */
  public boolean isInfinite() {
    return (exponent == format.maxExp() + 1) && significand.equals(BigInteger.ZERO);
  }

  /** True if the value is equal to zero or negative zero. */
  public boolean isZero() {
    return (exponent == format.minExp() - 1) && significand.equals(BigInteger.ZERO);
  }

  /** True if the value is +1.0 */
  public boolean isOne() {
    return one(format).equals(this);
  }

  /** True if the value is -1.0 */
  public boolean isNegativeOne() {
    return negativeOne(format).equals(this);
  }

  /**
   * True if the value is negative
   *
   * <p>This includes the special case of -0.0
   */
  public boolean isNegative() {
    return sign;
  }

  /** True if the value is an integer, that is if there is no fractional part */
  public boolean isInteger() {
    BigInteger intValue = toInteger();
    return equals(fromInteger(format, intValue));
  }

  /** True if the value is an odd integer number */
  private boolean isOddInteger() {
    BigInteger intValue = toInteger();
    return equals(fromInteger(format, intValue)) && intValue.testBit(0);
  }

  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    return pOther instanceof FloatValue other
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
  private static BigInteger truncate(BigInteger pSignificand, int pBits) {
    BigInteger mask = BigInteger.ONE.shiftLeft(pBits).subtract(BigInteger.ONE);
    BigInteger r = pSignificand.and(mask);
    BigInteger l = pSignificand.shiftRight(pBits);
    return r.equals(BigInteger.ZERO) ? l : l.setBit(0);
  }

  /**
   * Supported rounding modes for {@link FloatValue#roundToInteger(RoundingMode)}.
   *
   * <p>All other operations currently use {@link RoundingMode#NEAREST_EVEN} by default.
   */
  public enum RoundingMode {
    /** Round to nearest, ties away from zero */
    NEAREST_AWAY,
    /** Round to nearest, ties to even */
    NEAREST_EVEN,
    /** Round toward +∞ */
    CEILING,
    /** Round toward -∞ */
    FLOOR,
    /** Round toward */
    TRUNCATE
  }

  /**
   * Round the significand.
   *
   * <p>We expect the significand to be followed by 3 grs bits.
   */
  private static BigInteger applyRounding(
      RoundingMode pRoundingMode, boolean pNegative, BigInteger pSignificand) {
    long grs = pSignificand.and(new BigInteger("111", 2)).longValue();
    pSignificand = pSignificand.shiftRight(3);
    BigInteger plusOne = pSignificand.add(BigInteger.ONE);
    return switch (pRoundingMode) {
      case NEAREST_AWAY -> (grs >= 4) ? plusOne : pSignificand;
      case NEAREST_EVEN ->
          ((grs == 4 && pSignificand.testBit(0)) || grs > 4) ? plusOne : pSignificand;
      case CEILING -> (grs > 0 && !pNegative) ? plusOne : pSignificand;
      case FLOOR -> (grs > 0 && pNegative) ? plusOne : pSignificand;
      case TRUNCATE -> pSignificand;
    };
  }

  /** Clone the value with a new exponent. */
  private FloatValue withExponent(long pExponent) {
    Format precision = format.withUnlimitedExponent();
    FloatValue expPart =
        new FloatValue(
            precision, false, pExponent - exponent, BigInteger.ONE.shiftLeft(format.sigBits));
    return this.withPrecision(precision).multiply(expPart).withPrecision(format);
  }

  /** Clone the value with a new sign. */
  private FloatValue withSign(boolean pSign) {
    return new FloatValue(format, pSign, exponent, significand);
  }

  /**
   * Convert the value to a different precision.
   *
   * <p>Uses "round to nearest, ties to even" for rounding when the value can not be represented
   * exactly in the new format.
   */
  public FloatValue withPrecision(Format pTargetFormat) {
    if (format.equals(pTargetFormat)) {
      return this;
    } else if (isNan()) {
      return sign ? nan(pTargetFormat).negate() : nan(pTargetFormat);
    } else if (isInfinite()) {
      return sign ? negativeInfinity(pTargetFormat) : infinity(pTargetFormat);
    } else if (isZero()) {
      return sign ? negativeZero(pTargetFormat) : zero(pTargetFormat);
    }

    long resultExponent = Math.max(exponent, format.minExp());
    BigInteger resultSignificand = significand;

    // Normalization
    // If the number is subnormal shift it upward and adjust the exponent
    int shift = (format.sigBits + 1) - resultSignificand.bitLength();
    if (shift > 0) {
      resultSignificand = resultSignificand.shiftLeft(shift);
      resultExponent -= shift;
    }

    // Return infinity if the exponent is too large for the new encoding
    if (resultExponent > pTargetFormat.maxExp()) {
      return sign ? negativeInfinity(pTargetFormat) : infinity(pTargetFormat);
    }
    // Return zero if the exponent is below the subnormal range
    if (resultExponent < pTargetFormat.minExp() - (pTargetFormat.sigBits + 1)) {
      return sign ? negativeZero(pTargetFormat) : zero(pTargetFormat);
    }

    // Extend the significand with 3 grs bits
    resultSignificand = resultSignificand.shiftLeft(pTargetFormat.sigBits + 3);

    // Use the lowest possible exponent and move the rest into the significand by shifting
    // it to the right.
    int leading = 0;
    if (resultExponent < pTargetFormat.minExp()) {
      leading = (int) Math.abs(pTargetFormat.minExp() - resultExponent);
      resultExponent = pTargetFormat.minExp() - 1;
    }

    // Truncate the value and round the result
    resultSignificand = truncate(resultSignificand, format.sigBits + leading);
    resultSignificand = applyRounding(RoundingMode.NEAREST_EVEN, sign, resultSignificand);

    // Normalize if rounding caused an overflow
    if (resultSignificand.testBit(pTargetFormat.sigBits + 1)) {
      resultSignificand = resultSignificand.shiftRight(1); // The last bit is zero
      resultExponent += 1;
    }
    if (leading > 0 && resultSignificand.testBit(pTargetFormat.sigBits)) {
      // Just fix the exponent if the value was subnormal before the overflow
      resultExponent += 1;
    }

    // Return infinity if this caused the exponent to leave the range
    if (resultExponent > pTargetFormat.maxExp()) {
      return sign ? negativeInfinity(pTargetFormat) : infinity(pTargetFormat);
    }
    return new FloatValue(pTargetFormat, sign, resultExponent, resultSignificand);
  }

  /** Returns the absolute value */
  public FloatValue abs() {
    return new FloatValue(format, false, exponent, significand);
  }

  /** Negates the value */
  public FloatValue negate() {
    return new FloatValue(format, !sign, exponent, significand);
  }

  /**
   * Equality
   *
   * <p>Ignores the sign of the zero and returns `false` if one of the operands is NaN. Use {@link
   * FloatValue#equals(Object)} for a bitwise comparison of the values.
   */
  public boolean equalTo(FloatValue pNumber) {
    Format precision = format.sup(pNumber.format);

    FloatValue arg1 = this.withPrecision(precision);
    FloatValue arg2 = pNumber.withPrecision(precision);

    if (arg1.isNan() || arg2.isNan()) {
      return false;
    } else if (arg1.isZero()) {
      return arg2.isZero();
    } else {
      return arg1.equals(arg2);
    }
  }

  /**
   * Inequality ("less than or greater")
   *
   * <p>Returns `false` if one of the operands is NaN. Otherwise behaves like the negation of {@link
   * FloatValue#equalTo(FloatValue)}
   */
  public boolean notEqualTo(FloatValue pNumber) {
    if (isNan() || pNumber.isNan()) {
      return false;
    }
    return !equalTo(pNumber);
  }

  /** Strictly greater than */
  public boolean greaterThan(FloatValue pNumber) {
    // Find a common precision and convert both arguments to this precision
    Format precision = format.sup(pNumber.format);

    FloatValue arg1 = this.withPrecision(precision);
    FloatValue arg2 = pNumber.withPrecision(precision);

    if (arg1.isNan() || arg2.isNan()) {
      return false;
    } else if (arg1.isInfinite() && !arg1.isNegative()) {
      // inf > b, holds unless b=inf
      return !(arg2.isInfinite() && !arg2.isNegative());
    } else if (arg1.isInfinite() && arg1.isNegative() && arg2.isInfinite() && arg2.isNegative()) {
      // -inf > b, holds unless b=-inf
      return false;
    } else {
      FloatValue r = arg1.subtract(arg2);
      if (r.isZero()) {
        return false;
      }
      return !r.isNegative();
    }
  }

  /** Greater or equal to */
  public boolean greaterOrEqual(FloatValue pNumber) {
    return greaterThan(pNumber) || equalTo(pNumber);
  }

  /** Strictly less than */
  public boolean lessThan(FloatValue pNumber) {
    if (isNan() || pNumber.isNan()) {
      return false;
    }
    return !greaterOrEqual(pNumber);
  }

  /** Less than or equal to */
  public boolean lessOrEqual(FloatValue pNumber) {
    return pNumber.greaterOrEqual(this);
  }

  /** Addition */
  public FloatValue add(FloatValue pNumber) {
    // Find a common precision and convert both arguments to this precision
    Format precision = format.sup(pNumber.format);

    // Make sure the first argument has the larger (or equal) exponent
    FloatValue arg1;
    FloatValue arg2;
    if (exponent >= pNumber.exponent) {
      arg1 = this.withPrecision(precision);
      arg2 = pNumber.withPrecision(precision);
    } else {
      arg1 = pNumber.withPrecision(precision);
      arg2 = this.withPrecision(precision);
    }

    // Handle special cases:
    if (arg1.isNan() || arg2.isNan()) {
      // (1) Either argument is NaN
      return nan(precision);
    } else if (arg1.isInfinite() && arg2.isInfinite()) {
      // (2) Both arguments are infinite
      if (arg1.isNegative() && arg2.isNegative()) {
        return negativeInfinity(precision);
      } else if (!arg1.isNegative() && !arg2.isNegative()) {
        return infinity(precision);
      } else {
        return nan(precision);
      }
    } else if (arg1.isInfinite()) {
      // (3) Only one argument is infinite
      // No need to check m as it can't be larger, and one of the args is finite
      return arg1;
    } else if (arg1.isZero() && arg2.isZero()) {
      // (4) Both arguments are zero (or negative zero)
      return (arg1.isNegative() && arg2.isNegative()) ? negativeZero(precision) : zero(precision);
    } else if (arg1.isZero() || arg2.isZero()) {
      // (5) Only one of the arguments is zero (or negative zero)
      return arg1.isZero() ? arg2 : arg1;
    }

    // Get the exponents without the IEEE bias. Note that for subnormal numbers the stored exponent
    // needs to be increased by one.
    long exponent1 = Math.max(arg1.exponent, precision.minExp());
    long exponent2 = Math.max(arg2.exponent, precision.minExp());

    // Calculate the difference between the exponents. If it is larger than the mantissa size we can
    // skip the add and return immediately.
    int exp_diff = (int) (exponent1 - exponent2);
    if (exp_diff >= precision.sigBits + 3) {
      return arg1;
    }

    // Get the significands and apply the sign
    BigInteger significand1 = arg1.sign ? arg1.significand.negate() : arg1.significand;
    BigInteger significand2 = arg2.sign ? arg2.significand.negate() : arg2.significand;

    // Expand the significand with (empty) guard, round and sticky bits
    significand1 = significand1.shiftLeft(3);
    significand2 = significand2.shiftLeft(3);

    // Shift the number with the smaller exponent to the exponent of the other number.
    significand2 = truncate(significand2, exp_diff);

    // Add the two significands
    BigInteger result = significand1.add(significand2);

    // Extract the sign and value of the significand from the result
    boolean resultSign = result.signum() < 0;
    BigInteger resultSignificand = result.abs();

    // The result has the same exponent as the larger of the two arguments
    long resultExponent = exponent1;

    // Normalize
    // (1) Significand is too large: shift to the right by one bit
    //     This can happen if two numbers with equal exponent are added:
    //     f.ex 1.0e3 + 1.0e3 = 10.0e3
    //     (here we normalize the result to 1.0e4)
    if (resultSignificand.testBit(precision.sigBits + 4)) {
      resultSignificand = truncate(resultSignificand, 1);
      resultExponent += 1;
    }

    // (2) Significand is too small: shift left unless the number is subnormal
    //     This can happen if digits have canceled out:
    //     f.ex 1.01001e2 + (-1.01e2) = 0.00001e2
    //     (here we normalize to 1.0e-3)
    int leading = (precision.sigBits + 4) - resultSignificand.bitLength();
    int maxValue = (int) (resultExponent - precision.minExp()); // p.minExp() <= exponent
    if (leading > maxValue) {
      // If the exponent would get too small only shift to the left until the minimal exponent is
      // reached and return a subnormal number.
      resultSignificand = resultSignificand.shiftLeft(maxValue);
      resultExponent = precision.minExp() - 1;
    } else {
      resultSignificand = resultSignificand.shiftLeft(leading);
      resultExponent -= leading;
    }

    // Round the result according to the grs bits
    resultSignificand = applyRounding(RoundingMode.NEAREST_EVEN, resultSign, resultSignificand);

    // Shift the significand to the right if rounding has caused an overflow
    if (resultSignificand.bitLength() > precision.sigBits + 1) {
      resultSignificand = resultSignificand.shiftRight(1); // The dropped bit is zero
      resultExponent += 1;
    }

    // Check if the result is zero
    if (resultSignificand.equals(BigInteger.ZERO)) {
      return resultSign ? negativeZero(precision) : zero(precision);
    }
    // Return infinity if there is an overflow.
    if (resultExponent > precision.maxExp()) {
      return resultSign ? negativeInfinity(precision) : infinity(precision);
    }

    // Otherwise return the number
    return new FloatValue(precision, resultSign, resultExponent, resultSignificand);
  }

  /** Subtraction */
  public FloatValue subtract(FloatValue pNumber) {
    return add(pNumber.negate());
  }

  /** Multiplication */
  public FloatValue multiply(FloatValue pNumber) {
    // Find a common precision and convert both arguments to this precision
    Format precision = format.sup(pNumber.format);

    // Make sure the first argument has the larger (or equal) exponent
    FloatValue arg1;
    FloatValue arg2;
    if (exponent >= pNumber.exponent) {
      arg1 = this.withPrecision(precision);
      arg2 = pNumber.withPrecision(precision);
    } else {
      arg1 = pNumber.withPrecision(precision);
      arg2 = this.withPrecision(precision);
    }

    // Handle special cases:
    if (arg1.isNan() || arg2.isNan()) {
      // (1) Either argument is NaN
      return nan(precision);
    } else if (arg1.isInfinite()) {
      // (2) One of the argument is infinite
      // No need to check m as it can't be larger, and one of the args is finite
      if (arg2.isZero()) {
        // Return NaN if we're trying to multiply infinity by zero
        return nan(precision);
      } else {
        return (arg1.isNegative() ^ arg2.isNegative())
            ? negativeInfinity(precision)
            : infinity(precision);
      }
    } else if (arg1.isZero() || arg2.isZero()) {
      // (3) One of the arguments is zero (or negative zero)
      return (arg1.isNegative() ^ arg2.isNegative()) ? negativeZero(precision) : zero(precision);
    }

    // Calculate the sign of the result
    boolean resultSign = sign ^ pNumber.sign;

    // Get the exponents without the IEEE bias. Note that for subnormal numbers the stored exponent
    // needs to be increased by one.
    long exponent1 = Math.max(arg1.exponent, precision.minExp());
    long exponent2 = Math.max(arg2.exponent, precision.minExp());

    // Calculate the exponent of the result by adding the exponents of the two arguments.
    // If the calculated exponent is out of range we can return infinity (or zero) immediately.
    long resultExponent = exponent1 + exponent2;
    if (resultExponent > precision.maxExp()) {
      return resultSign ? negativeInfinity(precision) : infinity(precision);
    }
    if (resultExponent < precision.minExp() - precision.sigBits - 2) {
      return resultSign ? negativeZero(precision) : zero(precision);
    }

    // Multiply the significands
    BigInteger significand1 = arg1.significand;
    BigInteger significand2 = arg2.significand;

    BigInteger result = significand1.multiply(significand2);

    // Add guard, round and sticky bits
    BigInteger resultSignificand = result.shiftLeft(3);

    // Normalize
    // (1) Significand is too large: shift to the right by one bit
    //     This can happen if two numbers with significand greater 1 are multiplied:
    //     f.ex 1.1e3 x 1.1e1 = 10.01e4
    //     (here we normalize the result to 1.001e5)
    if (resultSignificand.testBit(2 * precision.sigBits + 4)) {
      resultSignificand = resultSignificand.shiftRight(1);
      resultExponent += 1;
    }

    // (2) Significand is too small: shift left unless the number is subnormal
    //     This can happen if one of the numbers was subnormal:
    //     f.ex 1.0e3 x 0.1e-1 = 0.10e2
    //     (here we normalize to 1.0e1)
    int shift = (2 * precision.sigBits + 4) - resultSignificand.bitLength();
    if (shift > 0) {
      resultSignificand = resultSignificand.shiftLeft(shift);
      resultExponent -= shift;
    }

    // Otherwise use the lowest possible exponent and move the rest into the significand by shifting
    // it to the right. Here we calculate haw many digits we need to shift:
    int leading = 0;
    if (resultExponent < precision.minExp()) {
      leading = (int) Math.abs(precision.minExp() - resultExponent);
      resultExponent = precision.minExp() - 1;
    }

    // Truncate the value:
    // The significand now has length 2*|precision of the significand| + 3 where 3 are the grs bits
    // at the end. We need to shift by at least |precision of the significand| bits.
    // If one of the factors was subnormal the results may have leading zeroes as well, and we need
    // to shift further by 'leading' bits.
    resultSignificand = truncate(resultSignificand, precision.sigBits + leading);

    // Round the result
    resultSignificand = applyRounding(RoundingMode.NEAREST_EVEN, resultSign, resultSignificand);

    // Shift the significand to the right if rounding has caused an overflow
    if (resultSignificand.bitLength() > precision.sigBits + 1) {
      resultSignificand = resultSignificand.shiftRight(1); // The dropped bit is zero
      resultExponent += 1;
    }
    if (leading > 0 && resultSignificand.bitLength() > precision.sigBits) {
      // Just fix the exponent if the value was subnormal before the overflow
      resultExponent += 1;
    }

    // Return infinity if there is an overflow.
    if (resultExponent > precision.maxExp()) {
      return resultSign ? negativeInfinity(precision) : infinity(precision);
    }

    // Otherwise return the number
    return new FloatValue(precision, resultSign, resultExponent, resultSignificand);
  }

  /**
   * Multiply two numbers and return the exact result.
   *
   * <p>This variant of {@link FloatValue#multiply} skips the rounding steps at the end and returns
   * directly. The result may have between p and 2p+1 bits.
   */
  private FloatValue multiplyExact(FloatValue pNumber) {
    // Find a common precision and convert both arguments to this precision
    Format precision = format.sup(pNumber.format);

    // Make sure the first argument has the larger (or equal) exponent
    FloatValue arg1;
    FloatValue arg2;
    if (exponent >= pNumber.exponent) {
      arg1 = this.withPrecision(precision);
      arg2 = pNumber.withPrecision(precision);
    } else {
      arg1 = pNumber.withPrecision(precision);
      arg2 = this.withPrecision(precision);
    }

    // Handle special cases:
    if (arg1.isNan() || arg2.isNan()) {
      // (1) Either argument is NaN
      return nan(precision);
    } else if (arg1.isInfinite()) {
      // (2) One of the argument is infinite
      // No need to check m as it can't be larger, and one of the args is finite
      if (arg2.isZero()) {
        // Return NaN if we're trying to multiply infinity by zero
        return nan(precision);
      } else {
        return (arg1.isNegative() ^ arg2.isNegative())
            ? negativeInfinity(precision)
            : infinity(precision);
      }
    } else if (arg1.isZero() || arg2.isZero()) {
      // (3) One of the arguments is zero (or negative zero)
      return (arg1.isNegative() ^ arg2.isNegative()) ? negativeZero(precision) : zero(precision);
    }

    // We assume both arguments are normal
    Preconditions.checkArgument(
        arg1.exponent >= precision.minExp() && arg2.exponent >= precision.minExp());

    // Calculate the sign of the result
    boolean resultSign = sign ^ pNumber.sign;

    // Get the exponents without the IEEE bias. Note that for subnormal numbers the stored exponent
    // needs to be increased by one.
    long exponent1 = arg1.exponent;
    long exponent2 = arg2.exponent;

    // Calculate the exponent of the result by adding the exponents of the two arguments.
    // If the calculated exponent is out of range we can return infinity (or zero) immediately.
    long resultExponent = exponent1 + exponent2;
    if (resultExponent > precision.maxExp()) {
      return resultSign ? negativeInfinity(precision) : infinity(precision);
    }
    if (resultExponent < precision.minExp() - precision.sigBits - 2) {
      return resultSign ? negativeZero(precision) : zero(precision);
    }

    // Multiply the significands
    BigInteger significand1 = arg1.significand;
    BigInteger significand2 = arg2.significand;

    BigInteger resultSignificand = significand1.multiply(significand2);

    // Normalize if the significand is too large:
    if (resultSignificand.testBit(2 * precision.sigBits + 4)) {
      resultExponent += 1;
    }

    // Return infinity if there is an overflow.
    if (resultExponent > precision.maxExp()) {
      return resultSign ? negativeInfinity(precision) : infinity(precision);
    }

    // Otherwise return the number
    return new FloatValue(
        new Format(precision.expBits, resultSignificand.bitLength() - 1),
        resultSign,
        resultExponent,
        resultSignificand);
  }

  private FloatValue squared() {
    return this.multiply(this);
  }

  /** The power function a^x for integer exponents x */
  public FloatValue powInt(BigInteger exp) {
    return withPrecision(format.intermediatePrecision()).powFast(exp).withPrecision(format);
  }

  /**
   * Calculate the power function a^x where x is an integer.
   *
   * <p>Uses fast exponentiation to calculate the result.
   */
  private FloatValue powFast(BigInteger x) {
    if (x.compareTo(BigInteger.ZERO) < 0) {
      // If x is negative we calculate 1/a^-x
      return one(format).divide(this.powFast(x.abs()));
    }
    FloatValue r = one(format);
    for (int s = x.bitLength() - 1; s >= 0; s--) {
      r = x.testBit(s) ? this.multiply(r.squared()) : r.squared();
    }
    return r;
  }

  /** Division */
  public FloatValue divide(FloatValue pNumber) {
    // Convert arguments to a common precision
    Format precision = format.sup(pNumber.format);
    // TODO: Replace with precision.intermediatePrecision()
    //   Currently this does not seem to work as we get incorrectly rounded results for Float8
    precision = /*precision.intermediatePrecision();*/
        new Format(Format.Float256.expBits, 2 * (1 + precision.sigBits) + 2);
    FloatValue arg1 = this.withPrecision(precision);
    FloatValue arg2 = pNumber.withPrecision(precision);

    return arg1.divide_(arg2).withPrecision(format);
  }

  /**
   * Divide the value by another number.
   *
   * <p>This version of divide is slower and does not check for corner cases. It is still needed to
   * calculate the constants that are needed for the other, faster version of divide that uses
   * Newton's method.
   *
   * <p>This method assumes that both arguments use the same precision.
   */
  private FloatValue divideSlow(FloatValue pNumber) {
    // Calculate the sign of the result
    boolean resultSign = sign ^ pNumber.sign;

    // Get the exponents without the IEEE bias. Note that for subnormal numbers the stored exponent
    // needs to be increased by one.
    long exponent1 = Math.max(exponent, format.minExp());
    long exponent2 = Math.max(pNumber.exponent, format.minExp());

    // Normalize both arguments.
    BigInteger significand1 = significand;
    int shift1 = (format.sigBits + 1) - significand1.bitLength();
    if (shift1 > 0) {
      significand1 = significand1.shiftLeft(shift1);
      exponent1 -= shift1;
    }

    BigInteger significand2 = pNumber.significand;
    int shift2 = (format.sigBits + 1) - significand2.bitLength();
    if (shift2 > 0) {
      significand2 = significand2.shiftLeft(shift2);
      exponent2 -= shift2;
    }

    // Calculate the exponent of the result by subtracting the exponent of the divisor from the
    // exponent of the dividend.
    long resultExponent = exponent1 - exponent2;

    // Calculate how many digits need to be calculated. If the result is <1 we need one additional
    // digit to normalize it.
    int digits = (1 + 2 * format.sigBits) + 3; // 2p-1 (+ 3 grs bits)
    if (significand1.compareTo(significand2) < 0) {
      digits += 1;
      resultExponent -= 1;
    }

    // If the exponent of the result is beyond the exponent range, skip the calculation and return
    // infinity immediately.
    if (resultExponent > format.maxExp()) {
      return resultSign ? negativeInfinity(format) : infinity(format);
    }
    // If it is below the subnormal range, return zero immediately.
    if (resultExponent < format.minExp() - (format.sigBits + 1)) {
      return resultSign ? negativeZero(format) : zero(format);
    }

    // Divide the significands
    BigInteger resultSignificand = BigInteger.ZERO;
    for (int i = 0; i < digits; i++) {
      // Calculate the next digit of the result
      resultSignificand = resultSignificand.shiftLeft(1);
      if (significand1.compareTo(significand2) >= 0) {
        significand1 = significand1.subtract(significand2);
        resultSignificand = resultSignificand.add(BigInteger.ONE);
      }

      // Shift the dividend
      significand1 = significand1.shiftLeft(1);
    }

    // Fix the exponent if it is too small. Use the lowest possible exponent for the format and then
    // move the rest into the significand by shifting it to the right.
    int leading = 0;
    if (resultExponent < format.minExp()) {
      leading = (int) Math.abs(format.minExp() - resultExponent);
      resultExponent = format.minExp() - 1;
    }

    // Truncate the value and round the result
    resultSignificand = truncate(resultSignificand, format.sigBits + leading);
    resultSignificand = applyRounding(RoundingMode.NEAREST_EVEN, resultSign, resultSignificand);

    // Shift the significand to the right if rounding has caused an overflow
    if (resultSignificand.bitLength() > format.sigBits + 1) {
      resultSignificand = resultSignificand.shiftRight(1); // Last bit is zero
      resultExponent += 1;
    }

    // Return the number
    return new FloatValue(format, resultSign, resultExponent, resultSignificand);
  }

  private double lb(double pNumber) {
    return Math.log(pNumber) / Math.log(2);
  }

  private FloatValue divide_(FloatValue pNumber) {
    FloatValue arg1 = this;
    FloatValue arg2 = pNumber;

    boolean resultSign = arg1.isNegative() ^ arg2.isNegative(); // Sign of the result

    // Handle special cases:
    if (arg1.isNan() || arg2.isNan()) {
      // (1) Either argument is NaN
      return nan(format);
    } else if (arg1.isZero()) {
      // (2) Dividend is zero
      if (arg2.isZero()) {
        // Divisor is zero or infinite
        return resultSign ? nan(format).negate() : nan(format);
      } else {
        return resultSign ? negativeZero(format) : zero(format);
      }
    } else if (arg1.isInfinite()) {
      // (3) Dividend is infinite
      if (arg2.isInfinite()) {
        // Divisor is infinite
        return resultSign ? nan(format).negate() : nan(format);
      } else {
        return resultSign ? negativeInfinity(format) : infinity(format);
      }
    } else if (arg2.isZero()) {
      // (4) Divisor is zero (and dividend is finite)
      return resultSign ? negativeInfinity(format) : infinity(format);
    } else if (arg2.isInfinite()) {
      // (5) Divisor is infinite (and dividend is finite)
      return resultSign ? negativeZero(format) : zero(format);
    }

    // Extract exponents and significand bits
    int exponent1 = (int) Math.max(arg1.exponent, format.minExp());
    int exponent2 = (int) Math.max(arg2.exponent, format.minExp());

    // Shift numerator and divisor by pulling out common factors in the exponent.
    // This will put the divisor in the range of 0.5 to 1.0
    FloatValue n = new FloatValue(format, false, exponent1 - (exponent2 + 1), arg1.significand);
    FloatValue d = new FloatValue(format, false, -1, arg2.significand);

    // Calculate how many iterations are needed
    int bound = (int) Math.ceil(lb((format.sigBits + 2) / lb(17)));

    // Set the initial value to 48/32 - 32/17*D
    FloatValue x = lookupSqrtT1().subtract(lookupSqrtT2().multiply(d));

    for (int i = 0; i < bound; i++) {
      // X(i+1) = X(i)*(2 - D*X(i))
      x = x.multiply(fromInteger(format, 2).subtract(d.multiply(x)));
    }

    // Multiply 1/D with N
    FloatValue r = x.multiply(n);

    // Set the sign bit and return the result
    return r.withSign(arg1.sign ^ arg2.sign);
  }

  /** Square root */
  public FloatValue sqrt() {
    // The calculation will be done in a higher precision and the result is then rounded down.
    // 2p+2 bits are enough for the inverse square root.
    // See Table 12.6 in "Handbook of Floating-Point Arithmetic"
    return withPrecision(format.intermediatePrecision()).sqrt_().withPrecision(format);
  }

  private FloatValue sqrt_() {
    if (isZero()) {
      return isNegative() ? negativeZero(format) : zero(format);
    } else if (isNan() || isNegative()) {
      return nan(format);
    } else if (isInfinite()) {
      return infinity(format);
    }

    // Get the exponent and the significand
    long resultExponent = Math.max(exponent, format.minExp());
    BigInteger resultSignificand = significand;

    // Normalize the argument
    int shift = (format.sigBits + 1) - resultSignificand.bitLength();
    if (shift > 0) {
      resultSignificand = resultSignificand.shiftLeft(shift);
      resultExponent -= shift;
    }

    // Range reduction:
    // sqrt(f * 2^2m) = sqrt(f)*2^m
    FloatValue f = new FloatValue(format, sign, resultExponent % 2, resultSignificand);

    // Define constants
    // TODO: These constants should be declared only once for each supported precision
    FloatValue c1d2 = new FloatValue(format, false, -1, BigInteger.ONE.shiftLeft(format.sigBits));
    FloatValue c3d2 =
        new FloatValue(format, false, 0, BigInteger.valueOf(3).shiftLeft(format.sigBits - 1));

    // Initial value (0.5 will always converge)
    FloatValue x = c1d2;

    boolean done = false;
    List<FloatValue> partial = new ArrayList<>();
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
    FloatValue r = fromInteger(format, 2).powInt(BigInteger.valueOf(resultExponent / 2));
    return x.multiply(r);
  }

  /**
   * Strip invalid digits from the significand.
   *
   * <p>With round-to-nearest-ties-to-even invalid bits at the end of the significand follow one of
   * two patterns:
   *
   * <ul>
   *   <li>10+
   *   <li>01+
   * </ul>
   *
   * After truncating these bits from the end all other digits are equal to the digits of the
   * infinite actual number.
   *
   * <p>This method assumes that the number is transcendental.
   */
  private FloatValue validPart() {
    if (isZero() || isNan() || isInfinite()) {
      return this;
    } else if (BigInteger.ONE.shiftLeft(format.sigBits).equals(significand)) {
      return this;
    }
    BigInteger resultSignificand = significand;
    boolean last = resultSignificand.testBit(0);

    // Search for the pattern
    int trailing = 1;
    do {
      resultSignificand = resultSignificand.shiftRight(1);
      trailing++;
    } while (resultSignificand.testBit(0) == last);

    resultSignificand = resultSignificand.shiftRight(1);

    // Return zero if we lost all our bits
    if (resultSignificand.equals(BigInteger.ZERO)) {
      return sign ? negativeZero(format) : zero(format);
    }

    return new FloatValue(
        new Format(format.expBits, trailing > format.sigBits ? 0 : (format.sigBits - trailing)),
        sign,
        exponent,
        resultSignificand);
  }

  /** The minimal distance between two floating point values with the same exponent. */
  private FloatValue oneUlp() {
    if (exponent < format.minExp()) {
      return sign ? minValue(format).negate() : minValue(format);
    } else {
      return one(format).withExponent(exponent - format.sigBits);
    }
  }

  /** Returns the next larger floating point number. */
  private FloatValue plus1Ulp() {
    return add(oneUlp());
  }

  /** Returns the floating point number immediately below this number. */
  private FloatValue minus1Ulp() {
    return add(oneUlp().negate());
  }

  /** Compare two floating point numbers for equality when rounded to a lower precision p. * */
  private static boolean equalModuloP(Format pFormat, FloatValue pArg1, FloatValue pArg2) {
    return pArg1.withPrecision(pFormat).equals(pArg2.withPrecision(pFormat));
  }

  /**
   * Check if a floating point number is stable in precision p.
   *
   * <p>Needed as part of the "rounding test": We have to make sure that the number is not too close
   * to a break point before rounding.
   */
  private boolean isStable(FloatValue pValue) {
    if (pValue.format.sigBits == 0) {
      return false;
    } else {
      return equalModuloP(format, pValue, pValue.plus1Ulp());
    }
  }

  /**
   * Returns a list of intermediate formats with increasing precision.
   *
   * <p>For transcendental functions like ln, exp or pow the calculation has to be repeated with
   * increasing precision until enough (valid) digits are available to round the value correctly.
   * For this we define a list of intermediate formats that was specially optimized for the
   * precision of the input format. The idea is that we only have to try 2-3 different precisions
   * before the right one is found, which is much more efficient than increasing the precision one
   * bit at a time.
   */
  private ImmutableList<Format> intermediatePrecisions() {
    ImmutableList.Builder<Format> builder = ImmutableList.builder();
    if (format.equals(Format.Float8)) {
      //      0.1    0.2    0.3    0.4    0.5    0.6    0.7    0.8    0.9    1.0
      // ln     1      1      1      1      1      4      5      6      7     10
      // exp    1      1      1      4      4      5      5      6      7     13
      // pow    6     13     13     13     13     16     16     16     16     16
      Format m0 = new Format(11, format.sigBits + 7);
      Format m1 = new Format(11, format.sigBits + 19);
      builder.add(m0, m1);
    } else if (format.equals(Format.Float16)) {
      //      0.1    0.2    0.3    0.4    0.5    0.6    0.7    0.8    0.9    1.0
      // ln     1      1      1      1      1      9     10     11     12     31
      // exp    1      1      1      6      8      9     10     12     13     26
      // pow    7     10     13     15     17     19     20     22     23     25
      Format m0 = new Format(11, format.sigBits + 13);
      Format m1 = new Format(11, format.sigBits + 31);
      builder.add(m0, m1, m1.intermediatePrecision());
    } else if (format.equals(Format.Float32)) {
      //      0.1    0.2    0.3    0.4    0.5    0.6    0.7    0.8    0.9    1.0
      // ln     1      1      1      1     18     19     20     21     22     35
      // exp    1      1      1      1     15     25     25     25     26     41
      // pow   13     18     22     25     29     31     34     36     39     41
      Format m0 = new Format(15, format.sigBits + 26);
      Format m1 = new Format(15, format.sigBits + 41);
      builder.add(m0, m1, m1.intermediatePrecision());
    } else if (format.equals(Format.Float64)) {
      //      0.1    0.2    0.3    0.4    0.5    0.6    0.7    0.8    0.9    1.0
      // ln     1      1      1      1     44     45     46     47     48     62
      // exp    1      1      1      1     28     54     54     55     55     68
      // pow   31     39     44     49     53     57     61     64     67     70
      Format m0 = new Format(Format.Float256.expBits, format.sigBits + 55);
      Format m1 = new Format(Format.Float256.expBits, format.sigBits + 70);
      builder.add(m0, m1, m1.intermediatePrecision());
    } else {
      builder.add(
          format.intermediatePrecision(), format.intermediatePrecision().intermediatePrecision());
    }
    return builder.build();
  }

  /** The exponential function e^x */
  public FloatValue exp() {
    return expWithStats(null);
  }

  /**
   * Calculates e^x and records how many extra bits of precision were needed.
   *
   * @param pExpStats Histogram with the number of extra bits used in all calls to exp()
   */
  FloatValue expWithStats(@Nullable Map<Integer, Integer> pExpStats) {
    if (isZero()) {
      return one(format);
    } else if (isNan()) {
      return nan(format);
    } else if (isInfinite()) {
      return isNegative() ? zero(format) : infinity(format);
    } else if (!abs().greaterThan(minNormal(format))) {
      // Return one immediately if the argument is close to zero
      return one(format);
    }

    FloatValue r = nan(format);
    boolean done = false;

    for (Format p : intermediatePrecisions()) {
      if (!done) {
        Format precision = new Format(p.expBits, p.sigBits - format.sigBits);
        FloatValue x = withPrecision(precision);

        // TODO: Call exp_ only once and *then* check if we're too close to a break point
        FloatValue v1 = x.plus1Ulp().withPrecision(p).exp_();
        FloatValue v2 = x.minus1Ulp().withPrecision(p).exp_();

        if (equalModuloP(format, v1, v2) && isStable(v1.validPart())) {
          done = true;
          r = v1;

          // Update statistics
          if (pExpStats != null) {
            Integer k = p.sigBits - format.sigBits;
            pExpStats.put(k, pExpStats.getOrDefault(k, 0) + 1);
          }
        }
      }
    }
    return r.withPrecision(format);
  }

  /**
   * Calculate the factorial of k
   *
   * <p>We use dynamic programming and store intermediate results in the Map from the second
   * argument. This speeds up the calculation of k! in {@link FloatValue#lookupExpTable(int)} as
   * results can be reused across multiple function calls.
   */
  private static BigInteger factorial(int k, Map<Integer, BigInteger> pFacMap) {
    return pFacMap.computeIfAbsent(
        k,
        (Integer arg1) -> {
          if (k == 0 || k == 1) {
            return BigInteger.ONE;
          }
          return factorial(k - 1, pFacMap).multiply(BigInteger.valueOf(k));
        });
  }

  /**
   * Internal version of {@link FloatValue#exp()} that does not extend the precision.
   *
   * <p>Use in the implementation of {@link FloatValue#pow(FloatValue)} where the caller makes sure
   * that enough precision is used.
   */
  private FloatValue exp_() {
    return expImpl(false);
  }

  /**
   * Calculates e^x - 1.
   *
   * <p>This method is used in the implementation of {@link FloatValue#pow(FloatValue)} where it
   * helps to avoid precision loss if the argument is close to zero.
   */
  private FloatValue expMinus1() {
    // Can be computed by simply skipping the first term of the Taylor expansion
    return expImpl(true);
  }

  /**
   * Helper method that calculates e^x or e^x - 1, depending on the argument.
   *
   * @param pSkipTerm1 Subtract one calculate e^x - 1 if true
   */
  private FloatValue expImpl(boolean pSkipTerm1) {
    if (isNan()) {
      return nan(format);
    } else if (isInfinite()) {
      return isNegative() ? zero(format) : infinity(format);
    }

    FloatValue x = this;
    FloatValue r = zero(format); // Series expansion after k terms.

    // Range reduction: exp(a * 2^k) = exp(a)^2k
    if (exponent > 0) {
      x = x.withExponent(0);
    }

    boolean done = false;
    int k = pSkipTerm1 ? 1 : 0; // Skip the first term of the Taylor expansion to compute e^x - 1

    while (!done) {
      FloatValue s = r;

      // r(k+1) = r(k) +  x^k/k!
      FloatValue a = x.powFast(BigInteger.valueOf(k));
      FloatValue b = lookupExpTable(k);

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

  /** The natural logarithm ln(x) */
  public FloatValue ln() {
    return lnWithStats(null);
  }

  /**
   * Calculates ln(x) and records how many extra bits of precision were needed.
   *
   * @param pLnStats Histogram with the number of extra bits used in all calls to ln()
   */
  FloatValue lnWithStats(@Nullable Map<Integer, Integer> pLnStats) {
    if (isZero()) {
      return negativeInfinity(format);
    } else if (isOne()) {
      return zero(format);
    }

    FloatValue r = nan(format);
    boolean done = false;

    for (Format p : intermediatePrecisions()) {
      if (!done) {
        Format precision = new Format(p.expBits, p.sigBits - format.sigBits);
        FloatValue x = withPrecision(precision);

        // TODO: Call ln only once and *then* check if we're too close to a break point
        FloatValue x1 = x.plus1Ulp().withPrecision(p);
        FloatValue x2 = x.minus1Ulp().withPrecision(p);

        FloatValue v1 = x1.ln_();
        FloatValue v2 = x2.ln_();

        if (equalModuloP(format, v1, v2) && isStable(v1.validPart())) {
          done = true;
          r = v1;

          // Update statistics
          if (pLnStats != null) {
            Integer k = p.sigBits - format.sigBits;
            pLnStats.put(k, pLnStats.getOrDefault(k, 0) + 1);
          }
        }
      }
    }
    return r.withPrecision(format);
  }

  private FloatValue ln_() {
    if (isZero()) {
      return negativeInfinity(format);
    } else if (isNan() || isNegative()) {
      return nan(format);
    } else if (isInfinite()) {
      return infinity(format);
    } else if (isOne()) {
      return zero(format);
    }

    // ln(x) = ln(a * 2^k) = ln a + ln 2^k = ln a + k * ln 2
    FloatValue a = withExponent(-1);
    FloatValue lna = a.subtract(one(format)).ln1p();

    FloatValue nln2 = fromInteger(format, (int) exponent + 1).multiply(lookupLn2());
    return lna.add(nln2);
  }

  private FloatValue ln1p() {
    FloatValue x = this;
    FloatValue r = zero(format);

    int k = 1;
    boolean done = false;
    while (!done) {
      FloatValue r0 = r;

      // r(k+1) = r(k) +  x^k/k
      FloatValue a = x.powFast(BigInteger.valueOf(k));
      FloatValue b = lookupLnTable(k);
      FloatValue v = a.multiply(b);

      r = r.add(k % 2 == 0 ? v.negate() : v);

      // Abort if we have enough precision
      done = r.equals(r0);
      k++;
    }
    return r;
  }

  /** The power function a^x */
  public FloatValue pow(FloatValue pExponent) {
    return powWithStats(pExponent, null);
  }

  /**
   * Calculates a^x and records how many extra bits of precision were needed.
   *
   * @param pPowStats Histogram with the number of extra bits used in all calls to pow()
   */
  FloatValue powWithStats(FloatValue pExponent, @Nullable Map<Integer, Integer> pPowStats) {
    // Find a common precision and convert both arguments to this precision
    Format precision = format.sup(pExponent.format);

    FloatValue arg1 = this.withPrecision(precision);
    FloatValue arg2 = pExponent.withPrecision(precision);

    // Handle special cases
    // See https://en.cppreference.com/w/c/numeric/math/pow for the full definition
    if (arg1.isOne() || arg2.isZero()) {
      // pow(+1, exponent) returns 1 for any exponent, even when exponent is NaN
      // pow(base, ±0) returns 1 for any base, even when base is NaN
      return one(precision);
    } else if (arg1.isNan() || arg2.isNan()) {
      // except where specified above, if any argument is NaN, NaN is returned
      return nan(precision);
    } else if (arg1.isZero() && arg2.isNegative() && arg2.isOddInteger()) {
      // pow(+0, exponent), where exponent is a negative odd integer
      // returns +∞ and raises FE_DIVBYZERO
      // pow(-0, exponent), where exponent is a negative odd integer
      // returns -∞ and raises FE_DIVBYZERO
      return arg1.isNegative() ? negativeInfinity(precision) : infinity(precision);
    } else if (arg1.isZero() && arg2.isNegative()) {
      // pow(±0, -∞) returns +∞ and may raise FE_DIVBYZERO(until C23)
      // pow(±0, exponent), where exponent is negative, finite, and is an even integer or a
      // non-integer, returns +∞ and raises FE_DIVBYZERO
      return infinity(precision);
    } else if (arg1.isZero() && !arg2.isNegative()) {
      // pow(+0, exponent), where exponent is a positive odd integer, returns +0
      // pow(-0, exponent), where exponent is a positive odd integer, returns -0
      // pow(±0, exponent), where exponent is positive non-integer or a positive even integer,
      // returns +0
      return arg2.isOddInteger() ? arg1 : zero(precision);
    } else if (arg1.isNegativeOne() && arg2.isInfinite()) {
      // pow(-1, ±∞) returns 1
      return one(precision);
    } else if (arg1.isInfinite() && isNegative()) {
      // pow(-∞, exponent) returns -0 if exponent is a negative odd integer
      // pow(-∞, exponent) returns +0 if exponent is a negative non-integer or negative even integer
      // pow(-∞, exponent) returns -∞ if exponent is a positive odd integer
      // pow(-∞, exponent) returns +∞ if exponent is a positive non-integer or positive even integer
      FloatValue power = arg2.isNegative() ? zero(precision) : infinity(precision);
      return arg2.isOddInteger() ? power.negate() : power;
    } else if (arg1.isInfinite()) {
      // pow(+∞, exponent) returns +0 for any negative exponent
      // pow(+∞, exponent) returns +∞ for any positive exponent
      return arg2.isNegative() ? zero(precision) : infinity(precision);
    } else if (arg2.isInfinite() && arg2.isNegative()) {
      // pow(base, -∞) returns +∞ for any |base|<1
      // pow(base, -∞) returns +0 for any |base|>1
      return arg1.abs().greaterThan(one(precision)) ? zero(precision) : infinity(precision);
    } else if (arg2.isInfinite()) {
      // pow(base, +∞) returns +0 for any |base|<1
      // pow(base, +∞) returns +∞ for any |base|>1
      return arg1.abs().greaterThan(one(precision)) ? infinity(precision) : zero(precision);
    } else if (arg1.isNegative() && !arg2.isInteger()) {
      // pow(base, exponent)
      // returns NaN and raises FE_INVALID if base is finite and negative and exponent is finite and
      // non-integer.
      return nan(precision);
    } else if (arg2.isInteger()) {
      // pow(base, exponent) where exponent is integer: calculate with powInt
      return arg1.powInt(arg2.toInteger());
    } else if (arg2.equals(
        new FloatValue(precision, false, -1, BigInteger.ONE.shiftLeft(precision.sigBits)))) {
      // pow(base, exponent) where exponent=1/2: calculate sqrt(a) instead
      // TODO: Also include a^3/2 in this check?
      return arg1.sqrt();
    } else {
      FloatValue r = arg1.abs().pow_(pExponent, pPowStats);
      if (arg1.isNegative()) {
        // Fix the sign if `a` was negative and x an integer
        r = r.withSign(arg2.isOddInteger());
      }
      if (r.isNan()) {
        // If the result is NaN we assume that the real result is a floating point number (or a
        // break
        // point between two floating point number). Unlike exp and log, pow has many arguments
        // where
        // this is the case. Examples include a^0, a^1, a^k (where k is an integer), or a^0.5 (where
        // 'a') is a square number.
        // We still check some of the trivial cases earlier for performance reasons.
        // The more general check is more costly, however, so it is only performed after the search
        // in the main algorithm has failed.
        r = arg1.powExact(arg2);
      }
      return r;
    }
  }

  /** Check if the argument is a square number and, if so, return its square root. */
  private Optional<FloatValue> sqrtExact() {
    FloatValue a = this;
    FloatValue r = a.sqrt();
    FloatValue b = r.multiplyExact(r);

    FloatValue x = b.withPrecision(format).withPrecision(b.format);
    FloatValue y = b.subtract(x);

    return y.isZero() ? Optional.of(r) : Optional.empty();
  }

  /** Handle cases in pow where a^x is a floating point number or a breakpoint. */
  private FloatValue powExact(FloatValue exp) {
    Format precision = format.withUnlimitedExponent();
    FloatValue arg1 = this.withPrecision(precision);
    FloatValue arg2 = exp.withPrecision(precision);

    FloatValue r = nan(format);
    boolean done = false;

    while (!done && !arg2.isInfinite()) { // TODO: Derive better bounds based on the exponent range
      // Rewrite a^x with a=b^2 and x=y/2 as b^y until we're left with an integer exponent
      Optional<FloatValue> val = arg1.sqrtExact();
      if (val.isEmpty()) {
        // Abort if 'a' is not a square number
        break;
      }
      arg1 = val.orElseThrow();
      arg2 = arg2.withExponent(arg2.exponent + 1);

      if (arg2.isInteger()) {
        done = true;
        r = arg1.powInt(arg2.toInteger());
      }
    }
    return r.withPrecision(format);
  }

  private FloatValue pow_(FloatValue pExponent, @Nullable Map<Integer, Integer> pPowStats) {
    FloatValue r = nan(format);
    boolean done = false;

    for (Format p : intermediatePrecisions()) {
      if (!done) {
        // a^x = exp(x * ln a)
        Format precision = new Format(p.expBits, p.sigBits - format.sigBits);

        FloatValue a = this.withPrecision(p);
        FloatValue x = pExponent.withPrecision(p);

        // The next call calculates ln with the current precision.
        // TODO: Figure out why this is enough?
        FloatValue lna = a.ln_();
        FloatValue xlna = x.multiply(lna).withPrecision(precision);

        // Check if we call e^x with x close to zero
        boolean nearZero = !xlna.abs().greaterThan(minNormal(format));

        // Calculate a bound for the value of e^(x * ln a)
        // TODO: Call exp only once and *then* check if we're too close to a break point
        FloatValue xlna1 = xlna.plus1Ulp().withPrecision(p);
        FloatValue xlna2 = xlna.minus1Ulp().withPrecision(p);

        FloatValue exlna1 = nearZero ? xlna1.expMinus1() : xlna1.exp_();
        FloatValue exlna2 = nearZero ? xlna2.expMinus1() : xlna2.exp_();

        // Proceed if the result is stable in the original precision
        // If the result was close to zero we have to use an extended format that allows larger
        // exponents. Otherwise, the values are too small and will be flushed to zero.
        Format p0 = format.withUnlimitedExponent();

        if (equalModuloP(nearZero ? p0 : format, exlna1, exlna2) && isStable(exlna1.validPart())) {
          done = true;
          r = nearZero ? one(p).add(exlna1) : exlna1;

          // Update statistics
          if (pPowStats != null) {
            pPowStats.put(precision.sigBits, pPowStats.getOrDefault(0, precision.sigBits) + 1);
          }
        }
      }
    }
    return r.withPrecision(format);
  }

  /**
   * Round to the next integer (under the given rounding mode)
   *
   * <p>The result is again a floating point value. To convert directly to integer use {@link
   * FloatValue#toInteger()} or the methods {@link FloatValue#byteValue()}, {@link
   * FloatValue#shortValue()}, {@link FloatValue#intValue()} and {@link FloatValue#longValue()}
   */
  public FloatValue roundToInteger(RoundingMode pRoundingMode) {
    if (isInfinite()) {
      // If the argument is infinite, just return it
      return this;
    } else if (isNan()) {
      // For -NaN we drop the sign to make the implementation in line with MPFR
      return this.abs();
    } else if (exponent > format.sigBits) {
      // If the exponent is large enough we already have an integer and can return immediately
      return this;
    }

    // Get the significand and add grs bits
    BigInteger resultSignificand = significand;
    resultSignificand = resultSignificand.shiftLeft(3);

    // Shift the fractional part to the right and then round the result
    resultSignificand = truncate(resultSignificand, (int) (format.sigBits - exponent));
    resultSignificand = applyRounding(pRoundingMode, sign, resultSignificand);

    // Recalculate the exponent
    int resultExponent = resultSignificand.bitLength() - 1;

    // Normalize the significand if there was an overflow. The last bit is then always zero and can
    // simply be dropped.
    resultSignificand = resultSignificand.shiftLeft(format.sigBits - resultExponent);

    // Check if the result is zero
    if (resultSignificand.equals(BigInteger.ZERO)) {
      return isNegative() ? negativeZero(format) : zero(format);
    }
    // Check if we need to round to infinity
    if (resultExponent > format.maxExp()) {
      return isNegative() ? negativeInfinity(format) : infinity(format);
    }
    return new FloatValue(format, sign, resultExponent, resultSignificand);
  }

  /**
   * Cast an integer value to a {@link FloatValue}
   *
   * <p>Will return +/- infinity if the integer is too large for the chosen float type.
   */
  public static FloatValue fromInteger(Format pFormat, BigInteger pNumber) {
    // Return +0.0 for input 0
    if (pNumber.equals(BigInteger.ZERO)) {
      return zero(pFormat);
    }

    // Get the sign and calculate the exponent
    boolean sign = pNumber.signum() < 0;
    int exponent = pNumber.abs().bitLength() - 1;

    // Truncate the number while carrying over the grs bits.
    BigInteger significand = pNumber.abs().shiftLeft(pFormat.sigBits + 3);
    significand = truncate(significand, exponent);

    // Round the result
    significand = applyRounding(RoundingMode.NEAREST_EVEN, sign, significand);

    // Shift the significand to the right if rounding has caused an overflow
    if (significand.bitLength() > pFormat.sigBits + 1) {
      significand = significand.shiftRight(1); // The dropped bit is zero
      exponent += 1;
    }

    // Return infinity if there is an overflow.
    if (exponent > pFormat.maxExp()) {
      return sign ? negativeInfinity(pFormat) : infinity(pFormat);
    }

    return new FloatValue(pFormat, sign, exponent, significand);
  }

  private static FloatValue fromInteger(Format pFormat, int pNumber) {
    return fromInteger(pFormat, BigInteger.valueOf(pNumber));
  }

  /**
   * Cast the value to a BigInteger.
   *
   * <p>If the value is not already an integer the fractional part will be cut off. This is
   * equivalent to rounding with {@link RoundingMode#TRUNCATE RoundingMode.TRUNCATE}. If a different
   * rounding mode is desired use {@link FloatValue#roundToInteger} first.
   */
  public BigInteger toInteger() {
    if (exponent < -1) {
      return BigInteger.ZERO;
    }
    // Shift the significand to truncate the fractional part. For large exponents the expression
    // 'format.sigBits - exponent' will become negative, and the shift is to the left, adding
    // additional zeroes.
    BigInteger resultSignificand = significand.shiftRight((int) (format.sigBits - exponent));
    if (sign) {
      resultSignificand = resultSignificand.negate();
    }
    return resultSignificand;
  }

  /**
   * Cast the value to a byte.
   *
   * <p>Note that the result of this operation is undefined if the integer value of the float is too
   * large for the target type. According to the C99 standard:
   *
   * <pre>F.4 Floating to integer conversion
   *  If the floating value is infinite or NaN or if the integral part of the floating value exceeds
   *  the range of the integer type, then the ‘‘invalid’’ floating-point exception is raised and the
   *  resulting value is unspecified. Whether conversion of non-integer floating values whose
   *  integral part is within the range of the integer type raises the ‘‘inexact’’ floating-point
   *  exception is unspecified</pre>
   *
   * However, gcc does not always set the inexact flag correctly (see <a
   * href="https://gcc.gnu.org/bugzilla/show_bug.cgi?id=27682">this</a> bugreport) and the check has
   * to be performed manually. It is also possible to check the range in advance, but this again has
   * to be done by the programmer.
   *
   * <p>We therefore try to emulate the default behaviour of gcc, which is to return a special
   * indefinite value if the real value is out of range. For signed integers this indefinite value
   * is 0x80000000 for int and 0x8000000000000000 for long. Conversion to byte or short happens in
   * two steps: first the float is converted to a 32bit integer, and then this value is truncated.
   * The indefinite value is therefore 0 in both cases.
   */
  @Override
  public byte byteValue() {
    BigInteger integerValue = toInteger();
    BigInteger maxPositive =
        BigInteger.valueOf(format.equals(Format.Extended) ? Byte.MAX_VALUE : Integer.MAX_VALUE);
    BigInteger maxNegative =
        BigInteger.valueOf(format.equals(Format.Extended) ? Byte.MIN_VALUE : Integer.MIN_VALUE);
    if (isNan()
        || isInfinite()
        || (integerValue.compareTo(maxPositive) > 0)
        || integerValue.compareTo(maxNegative) < 0) {
      return (format.equals(Format.Extended)
              && integerValue.compareTo(BigInteger.valueOf(Short.MAX_VALUE)) <= 0
              && integerValue.compareTo(BigInteger.valueOf(Short.MIN_VALUE)) >= 0)
          ? integerValue.byteValue()
          : 0;
    }
    return integerValue.byteValue();
  }

  /**
   * Cast the value to a short.
   *
   * <p>See {@link FloatValue#byteValue()} for some notes.
   */
  @Override
  public short shortValue() {
    BigInteger integerValue = toInteger();
    BigInteger maxPositive =
        BigInteger.valueOf(format.equals(Format.Extended) ? Short.MAX_VALUE : Integer.MAX_VALUE);
    BigInteger maxNegative =
        BigInteger.valueOf(format.equals(Format.Extended) ? Short.MIN_VALUE : Integer.MIN_VALUE);
    if (isNan()
        || isInfinite()
        || (integerValue.compareTo(maxPositive) > 0)
        || integerValue.compareTo(maxNegative) < 0) {
      integerValue = format.equals(Format.Extended) ? maxNegative : BigInteger.ZERO;
    }
    return integerValue.shortValue();
  }

  /**
   * Cast the value to an int.
   *
   * <p>See {@link FloatValue#byteValue()} for some notes.
   */
  @Override
  public int intValue() {
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

  /**
   * Cast the value to a long.
   *
   * <p>See {@link FloatValue#byteValue()} for some notes.
   */
  @Override
  public long longValue() {
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

  /** Convert a `float` to {@link FloatValue} */
  public static FloatValue fromFloat(float pFloat) {
    Format format = Format.Float32;
    if (Float.isNaN(pFloat)) {
      boolean sign = (Float.floatToRawIntBits(pFloat) & 0x80000000) != 0;
      return sign ? FloatValue.nan(format).negate() : FloatValue.nan(format);
    } else if (Float.isInfinite(pFloat)) {
      return (pFloat < 0.0) ? FloatValue.negativeInfinity(format) : FloatValue.infinity(format);
    } else {
      int floatBits = Float.floatToIntBits(pFloat);

      // Get the sign
      boolean sign = ((floatBits >> 23) & 0x100) != 0;

      // Extract exponent and significand
      int expBits = (floatBits >> 23) & 0xFF;
      int sigBits = floatBits & 0x7FFFFF;

      // Add the hidden bit to the significand
      if (expBits != 0) {
        sigBits += 1 << 23;
      }

      // Remove the bias from the exponent
      long exp = expBits - format.bias();

      return new FloatValue(Format.Float32, sign, exp, BigInteger.valueOf(sigBits));
    }
  }

  @Override
  public float floatValue() {
    FloatValue f = withPrecision(Format.Float32);
    if (f.isNan()) {
      return f.isNegative() ? Float.intBitsToFloat(0xFFC00000) : Float.NaN;
    } else if (f.isInfinite()) {
      return f.isNegative() ? Float.NEGATIVE_INFINITY : Float.POSITIVE_INFINITY;
    } else {
      int sigBits = f.significand.clearBit(Format.Float32.sigBits).intValue();
      int expBits = (int) (f.exponent + Format.Float32.bias());
      if (f.sign) {
        expBits += 0x100;
      }
      return Float.intBitsToFloat((expBits << 23) | sigBits);
    }
  }

  /** Convert a `double` to {@link FloatValue#byteValue()} */
  public static FloatValue fromDouble(double pDouble) {
    Format format = Format.Float64;
    if (Double.isNaN(pDouble)) {
      boolean sign = (Double.doubleToRawLongBits(pDouble) & 0x8000000000000000L) != 0;
      return sign ? FloatValue.nan(format).negate() : FloatValue.nan(format);
    } else if (Double.isInfinite(pDouble)) {
      return (pDouble < 0.0) ? FloatValue.negativeInfinity(format) : FloatValue.infinity(format);
    } else {
      long doubleBits = Double.doubleToLongBits(pDouble);

      // Get the sign
      boolean sign = ((doubleBits >> 52) & 0x800) != 0;

      // Extract exponent and significand
      long expBits = (doubleBits >> 52) & 0x7FF;
      long sigBits = doubleBits & 0xFFFFFFFFFFFFFL;

      // Add the hidden bit to the significand
      if (expBits != 0) {
        sigBits += 1L << 52;
      }

      // Remove the bias from the exponent
      long exp = expBits - format.bias();

      return new FloatValue(Format.Float64, sign, exp, BigInteger.valueOf(sigBits));
    }
  }

  @Override
  public double doubleValue() {
    FloatValue d = withPrecision(Format.Float64);
    if (d.isNan()) {
      return d.isNegative() ? Double.longBitsToDouble(0xFFF8000000000000L) : Double.NaN;
    } else if (d.isInfinite()) {
      return d.isNegative() ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
    } else {
      long sigBits = d.significand.clearBit(Format.Float64.sigBits).longValue();
      long expBits = d.exponent + Format.Float64.bias();
      if (d.sign) {
        expBits += 0x800;
      }
      return Double.longBitsToDouble((expBits << 52) | sigBits);
    }
  }

  /**
   * Create a floating point value from its hexadecimal string representation.
   *
   * <p>Converting from base 16 to base 2 is a special case as the two radixes are "commensurable",
   * that is, they are both powers of another integer. In this case this integer is simply 2 and the
   * two bases can then be expressed as 2^1 and 2^4. Intuitively this means that the conversion can
   * be done one digit at a time as each hexadecimal digit can simply be expanded into 4 bits to
   * create the base2 representation.
   *
   * @see <a href="https://dl.acm.org/doi/pdf/10.1145/93542.93557">How to Read Floating Point
   *     Numbers Accurately</a>
   */
  private static FloatValue fromHexadecimal(
      Format pFormat, boolean pSign, String pDigits, int pExpValue) {
    FloatValue r = fromInteger(pFormat, new BigInteger(pDigits, 16));
    int finalExp = pExpValue - 4 * (pDigits.length() - 1);
    r = r.withExponent(r.exponent + finalExp);
    return pSign ? r.negate() : r;
  }

  private static FloatValue makeValue(Format pFormat, BigInteger u, BigInteger v, int k) {
    Format precision = pFormat.withUnlimitedExponent();
    BigInteger q = u.divide(v);
    BigInteger r1 = u.subtract(q.multiply(v));
    BigInteger r2 = v.subtract(r1);
    FloatValue z = fromInteger(precision, q);
    z = z.withExponent(z.exponent + k);
    boolean isEven = q.mod(BigInteger.TWO).equals(BigInteger.ZERO);
    if (r1.compareTo(r2) > 0 || (r1.compareTo(r2) == 0 && !isEven)) { // Round up
      z = z.plus1Ulp();
    }
    return z.withPrecision(pFormat);
  }

  private static FloatValue fromDecimal_(Format pFormat, BigInteger u, BigInteger v) {
    int b1 = u.bitLength();
    int b2 = v.bitLength();

    // Consider the bit length of the numerator and the denominator and pull out 2^k
    int k = -b2 + (b1 - pFormat.sigBits);
    if (k < 0) {
      u = u.shiftLeft(Math.abs(k));
    } else {
      v = v.shiftLeft(k);
    }

    // Fine tune for the actual value. If the quotient is already between 1.0 and 2.0 we're done.
    // Otherwise, multiply by 2 once more to fix the exponent.
    BigInteger x = u.divide(v);
    if (x.bitLength() < pFormat.sigBits + 1) {
      u = u.shiftLeft(1);
      k--;
    }

    return makeValue(pFormat, u, v, k);
  }

  /**
   * Create a floating point value from its decimal string representation.
   *
   * <p>We use <b>AlgorithmM</b> from <a href="https://dl.acm.org/doi/pdf/10.1145/93548.93557">How
   * to read floating point numbers accurately</a> to ensure correct rounding.
   */
  private static FloatValue fromDecimal(
      Format pFormat, boolean pSign, String pDigits, int pExpValue) {
    int k = pExpValue - (pDigits.length() - 1);
    BigInteger f = new BigInteger(pDigits);
    BigInteger e = BigInteger.TEN.pow(Math.abs(k));
    FloatValue r =
        k > 0 ? fromDecimal_(pFormat, f.multiply(e), BigInteger.ONE) : fromDecimal_(pFormat, f, e);
    return pSign ? r.negate() : r;
  }

  /**
   * Parse input string as a floating point number
   *
   * <p>Valid input strings must match the following grammar:
   *
   * <pre>
   * floating-constant ::=
   *     decimal-floating-constant
   *   | hexadecimal-floating-constant
   *
   * decimal-floating-constant ::=
   *     [sign] fractional-constant [exponent-part]
   *   | [sign] digit-sequence exponent-part
   *
   * hexadecimal-floating-constant ::=
   *     `0x` hexadecimal-fractional-constant binary-exponent-part
   *   | `0x` hexadecimal-digit-sequence binary-exponent-part
   *
   * fractional-constant ::=
   *     digit* `.` digit+
   *   | digit+ `.`
   *
   * exponent-part ::=
   *     `e` [sign] digit+
   *   | `E` [sign] digit+
   *
   * hexadecimal-fractional-constant ::=
   *     hexadecimal-digit* `.` hexadecimal-digit+
   *   | hexadecimal-digit+ `.`
   *
   * binary-exponent-part ::=
   *     `p` [sign] digit+
   *   | `P` [sign] digit+
   *
   * sign ::= `+` | `-`
   *
   * digit ::= `0` ... `9`
   * hexadecimal-digit ::= digit | `A` ... `F` | `a` ... `f`
   * </pre>
   */
  public static FloatValue fromString(Format pFormat, String pInput) {
    // TODO: Add error handling for broken inputs.
    if ("inf".equals(pInput)) {
      return infinity(pFormat);
    } else if ("-inf".equals(pInput)) {
      return negativeInfinity(pFormat);
    } else if ("nan".equals(pInput)) {
      return nan(pFormat);
    } else if ("-nan".equals(pInput)) {
      return nan(pFormat).negate();
    }
    pInput = pInput.toLowerCase(Locale.getDefault());

    boolean sign = false;

    // Determine the sign
    char pre = pInput.charAt(0);
    if (pre == '+' || pre == '-') {
      pInput = pInput.substring(1);
      sign = pre == '-';
    }

    // Check if it's a hex literal
    boolean isHexLiteral = false;
    if (pInput.startsWith("0x")) {
      isHexLiteral = true;
      pInput = pInput.substring(2);
    }

    // Split off the exponent part (if there is one)
    int sep = isHexLiteral ? pInput.indexOf('p') : pInput.indexOf('e');
    String digits = sep > -1 ? pInput.substring(0, sep) : pInput;
    String exponent = sep > -1 ? pInput.substring(sep + 1) : "0";

    int expValue = Integer.parseInt(exponent);

    // Get the fractional part of the number (and add ".0" if it has none)
    int radix = digits.indexOf('.');
    if (radix == -1) {
      radix = digits.length();
      digits = digits + ".0";
    }

    // Normalize the mantissa, then fix the exponent
    expValue = (isHexLiteral ? 4 : 1) * (radix - 1) + expValue;
    digits = digits.substring(0, radix) + digits.substring(radix + 1);

    // Return zero if the significand is all zeroes
    if (digits.matches("0+")) {
      return sign ? negativeZero(pFormat) : zero(pFormat);
    }

    // Convert the value to a binary float representation
    if (isHexLiteral) {
      return fromHexadecimal(pFormat, sign, digits, expValue);
    } else {
      return fromDecimal(pFormat, sign, digits, expValue);
    }
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
   * </pre>
   */
  private int neededDigits() {
    return 1 + (int) Math.ceil((format.sigBits + 1) * Math.log(2) / Math.log(10));
  }

  /**
   * Print the floating point value
   *
   * <p>The output will be in scientific format with the number of digits printed given by the
   * formula <code>1 + ceil(p * log_10(2))</code> where <code>p</code> is the precision of the
   * value. This length guarantees that <code>fromString(toString(x)) = x</code> always holds. For
   * single-precision values the number of digits is 9, for double-precision it is 17 and for
   * extended-precision 21 digits are needed.
   */
  @Override
  public String toString() {
    if (isNan()) {
      return isNegative() ? "-nan" : "nan";
    } else if (isInfinite()) {
      return isNegative() ? "-inf" : "inf";
    }

    // Get the exponent and the significand
    BigInteger resultSignificand = significand;
    long resultExponent = Math.max(exponent, format.minExp());

    // Normalize the value if it is subnormal
    int shift = (format.sigBits + 1) - resultSignificand.bitLength();
    if (shift > 0) {
      resultSignificand = resultSignificand.shiftLeft(shift);
      resultExponent -= shift;
    }

    // Shift the exponent to make the significand an integer
    resultExponent -= format.sigBits;

    // p is the number of decimal digits needed to recover the original number if the output of
    // toString is read back with fromString
    int p = neededDigits();

    // We define an extended format that has enough precision to hold the intermediate result
    // during the conversion from binary to decimal representation
    int precision = 2 * p + 2; // FIXME: Make sure this is enough

    // Build a term for the exponent in decimal representation
    MathContext rm = new MathContext(precision, java.math.RoundingMode.HALF_EVEN);
    BigDecimal r = new BigDecimal(BigInteger.ONE.shiftLeft(Math.abs((int) resultExponent)));
    if (resultExponent < 0) {
      r = BigDecimal.ONE.divide(r, rm);
    }

    // Convert the significand to BigDecimal and multiply with the decimal exponent term
    BigDecimal a = new BigDecimal(resultSignificand);
    BigDecimal b = a.multiply(r);

    // Round the result down to p significand digits
    BigDecimal rounded =
        b.plus(new MathContext(p, java.math.RoundingMode.HALF_EVEN)).stripTrailingZeros();

    // Print the output string
    String repr = String.format("%." + (p - 1) + "e", rounded);

    // Add the sign if necessary
    return isNegative() ? "-" + repr : repr;
  }

  /** Print the number in its base2 string representation. */
  public String toBinaryString() {
    if (isNan()) {
      return isNegative() ? "-nan" : "nan";
    } else if (isInfinite()) {
      return isNegative() ? "-inf" : "inf";
    } else if (isZero()) {
      return isNegative() ? "-0.0" : "0.0";
    } else {
      String bits = significand.toString(2);
      bits = "0".repeat(format.sigBits + 1 - bits.length()) + bits;
      return "%s%s.%s e%d".formatted(sign ? "-" : "", bits.charAt(0), bits.substring(1), exponent);
    }
  }

  /** Returns the exponent of this value */
  long getExponent() {
    return exponent;
  }

  /** Returns the significand of this value */
  BigInteger getSignificand() {
    return significand;
  }
}