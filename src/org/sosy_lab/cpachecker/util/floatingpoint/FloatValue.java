// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.floatingpoint;

import com.google.common.base.Ascii;
import com.google.common.base.CharMatcher;
import com.google.common.base.Preconditions;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multiset;
import com.google.errorprone.annotations.Immutable;
import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.rationals.Rational;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.java_smt.api.FloatingPointNumber;

/**
 * Java based implementation of multi-precision floating point values with correct rounding.
 *
 * <p>Values are created with a fixed precision and an exponent range. The nested class {@link
 * Format} is used to provide these parameters and supports predefined formats for the most commonly
 * used bit sizes. Binary operations expect the {@link Format} of their arguments to match and will
 * throw an exception otherwise. Use {@link Format#matchWith(Format)} and {@link
 * FloatValue#withPrecision(Format)} to upcast your arguments before the call.
 *
 * <h3>Rounding</h3>
 *
 * <p>With floating point values rounding is necessary after almost all operations to match the
 * precision of the result. We guarantee "correct rounding" for all operations, that is the result
 * is always the same as if the calculation had been performed with infinite precision before
 * rounding down to the precision of the final result. See the <b>Background</b> section at the
 * bottom for more information on how correct rounding can be guaranteed.
 *
 * <p>We use {@link RoundingMode#NEAREST_EVEN} as the rounding mode in all operations and currently
 * don't support any other rounding modes. This is with the exception of {@link
 * #round(RoundingMode)}, which can be used with any of the {@link RoundingMode RoundingModes} to
 * convert floating point values to integers.
 *
 * <h3>Comparison operators</h3>
 *
 * There are different ways to compare floating point values and the right choice often depends on
 * the application. We follow the discussion <a
 * href="https://download.java.net/java/early_access/valhalla/docs/api/java.base/java/lang/Double.html#equivalenceRelation">here</a>
 * use the terms "numeric, "bit-wise" and "representational" to define different types of
 * equivalence:
 *
 * <ul>
 *   <li>"Numerical equivalence"
 *       <p>Same as for primitive floats in Java: <code>+0 == -0</code> and <code>NaN != b</code>
 *       for all <code>b</code> (including <code>NaN</code> itself). We use this order for the
 *       following comparison predicates:
 *       <ul>
 *         <li>{@link #equalTo(FloatValue)}
 *         <li>{@link #lessOrGreater(FloatValue)}
 *         <li>{@link #lessOrEqual(FloatValue)}
 *         <li>{@link #lessThan(FloatValue)}
 *         <li>{@link #greaterOrEqual(FloatValue)}
 *         <li>{@link #greaterThan(FloatValue)}
 *       </ul>
 *   <li>"Representation equivalence"
 *       <p>Same as for boxed floats in Java: <code>+0 != -0</code> and <code>NaN == NaN</code> for
 *       any two <code>NaN</code> values ignoring both the sign and the payload. We use this order
 *       for {@link #equals(Object)} and {@link #compareTo(FloatValue)} to match the behavior of
 *       {@link Float} and {@link Double} for these operations.
 *   <li>"Bit-wise equivalence"
 *       <p>Considers the actual bit pattern of the values: <code>+0 != -0</code> and two <code>NaN
 *       </code> values are consider equal if (and only if) their sign and payload match. Used in
 *       {@link #compareWithTotalOrder(FloatValue)} to implement the totalOrder predicate from the
 *       IEEE-754 standard.
 * </ul>
 *
 * <h3>Background</h3>
 *
 * <p>Correct rounding can easily be ensured for the "algebraic" operations in this class, that is
 * addition, subtraction, multiplication, division and the square root. Here worst case bounds exist
 * on the number of extra digits that need to be calculated before the number can always be rounded
 * correctly. The same is not true for transcendental functions where such bounds are unknown and
 * may not even exist. This problem is known as <a
 * href="https://en.wikipedia.org/wiki/Rounding#Table-maker's_dilemma">"Table-maker's dilemma"</a>.
 * Luckily for the transcendental functions {@link FloatValue#exp()}, {@link FloatValue#ln()} and
 * {@link FloatValue#pow(FloatValue)}) from this class it can be shown that only a finite number of
 * extra digits are needed for correct rounding. This follows from Lindemann’s theorem that e^z is
 * transcendental for every nonzero algebraic complex number z. Since floating point numbers are
 * algebraic, the result of the calculation can never fall exactly on a floating point value, or the
 * break-point between two floating point values. It is therefore enough to repeat the calculation
 * with increasing precision until the result no longer falls on a break-point and can be correctly
 * rounded. This approach is know as <a
 * href="https://dl.acm.org/doi/pdf/10.1145/114697.116813">Ziv's technique</a> and requires a
 * "rounding test" that decides if the value calculated in the current iteration can be correctly
 * rounded. Such test depend on the rounding mode used, but for "round-to-nearest-ties-to-even" (the
 * standard rounding mode in IEEE 754, and what is being used by this implementation) it can be
 * enough to simply look for patterns of the form 01+ or 10+ at the end of the significand. These
 * last few digits are exactly on the break-point in the current iteration and can still change when
 * the calculation is repeated with a higher precision. Everything before that is stable, however,
 * and we implement the rounding test by checking if there are enough stable bits to round down to
 * the final precision of the result.
 *
 * @see <a href="https://link.springer.com/book/10.1007/978-3-319-76526-6">Handbook of
 *     Floating-Point Arithmetic (12.1.1 The Table Maker’s Dilemma, 12.4.1 Lindemann’s theorem,
 *     11.6.3 Rounding test)</a>
 */
public final class FloatValue extends Number implements Comparable<FloatValue> {
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
      CONSTANTS.put(key, one(format).divideNewton(fromInteger(format, factorial(k, FACTORIALS))));
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
      CONSTANTS.put(key, one(format).divideNewton(fromInteger(format, k)));
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
      FloatValue r = fromInteger(format, 2).sqrtNewton().subtract(one(format)).ln1p();
      CONSTANTS.put(key, r.withExponent(r.exponent + 1));
    }
    return CONSTANTS.get(key);
  }

  /** Format, defines the precision of the value and the allowed exponent range. */
  private final Format format;

  /** Sign of the value, <code>true</code> if negative */
  private final boolean sign;

  /**
   * Exponent of the value.
   *
   * <p>The exponent is stored without the IEEE bias and can therefore be negative. Use {@link
   * Format#minExp()} and {@link Format#maxExp()} to get the range of possible exponents. For zero
   * and subnormal numbers the exponent will be <code>Format.minExp() - 1</code>, and for Infinity
   * and NaN the exponent is always <code>Format.maxExp() + 1</code>.
   */
  private final long exponent;

  /**
   * Significand of the value.
   *
   * <p>For values that are not zero, subnormal, infinity or NaN the significand is stored with an
   * additional hidden bit in its first place.
   *
   * <p>"Hidden bit" refers to the leading bit of the significand. Since it is always <code>1</code>
   * in normalized binary floating point numbers, it can be dropped to save space. In our internal
   * representation we always include the "hidden" bit as it simplifies calculations. However, when
   * converting from/to other representations, as with {@link #fromDouble(double)} or {@link
   * #toFloatingPointNumber()}, care must be taken to remove/add the hidden bit as needed.
   */
  private final BigInteger significand;

  /**
   * Defines the precision and the exponent range of a {@link FloatValue} value.
   *
   * <p>The precision of a FloatValue is equivalent to the length of its significand. Here the
   * 'hidden bit' is not counted. The exponent range can be derived from the width of the exponent
   * field.
   */
  @Immutable
  public record Format(int expBits, int sigBits) implements Serializable {
    @Serial private static final long serialVersionUID = -6677404553596078315L;

    /** The minimal number of bits of the exponent field */
    private static final int MIN_EXPONENT_WIDTH = 2;

    /** The maximal number of bits of the exponent field */
    private static final int MAX_EXPONENT_WIDTH = 25;

    public Format {
      // Check that the arguments are valid
      Preconditions.checkArgument(
          expBits >= MIN_EXPONENT_WIDTH && expBits <= MAX_EXPONENT_WIDTH,
          "Exponent field must be between %s and %s bits wide",
          MIN_EXPONENT_WIDTH,
          MAX_EXPONENT_WIDTH);
      Preconditions.checkArgument(
          sigBits >= 0, "Significand field must not have negative bit width");
    }

    private static final int FLOAT8_EXP_BITS = 4;
    private static final int FLOAT8_SIG_BITS = 3;

    /**
     * An 8bit floating-point format
     *
     * <p>There is no standardized IEEE 754-2008 format for 8bit values. We reserve 4 bits for the
     * exponent and use the remaining 4 for the significand, but other choices would be possible.
     *
     * @see <a href="https://en.wikipedia.org/wiki/Minifloat">Wikipedia</a>
     */
    static final Format Float8 = new Format(FLOAT8_EXP_BITS, FLOAT8_SIG_BITS);

    private static final int FLOAT16_EXP_BITS = 5;
    private static final int FLOAT16_SIG_BITS = 10;

    /**
     * Half-precision floating-point format
     *
     * <p>16bit binary floating point format (<b>"binary16"</b>) as defined in the IEEE 754-2008
     * standard. Uses 4 bits for the 5 bits for the exponent and 11 for the significand.
     *
     * @see <a
     *     href="https://en.wikipedia.org/wiki/Half-precision_floating-point_format">Wikipedia</a>
     */
    public static final Format Float16 = new Format(FLOAT16_EXP_BITS, FLOAT16_SIG_BITS);

    private static final int FLOAT32_EXP_BITS = 8;
    private static final int FLOAT32_SIG_BITS = 23;

    private static final int FLOAT32_SIGN_MASK = (1 << FLOAT32_EXP_BITS) << FLOAT32_SIG_BITS;
    private static final int FLOAT32_EXPONENT_MASK =
        ((1 << FLOAT32_EXP_BITS) - 1) << FLOAT32_SIG_BITS;
    private static final int FLOAT32_SIGNIFICAND_MASK = (1 << FLOAT32_SIG_BITS) - 1;

    /**
     * Single-precision floating-point format
     *
     * <p>32bit binary floating point format (<b>"binary32"</b>) as defined in the IEEE 754-2008
     * standard. Uses 8 bits for the exponent and 24 for the significand.
     *
     * @see <a
     *     href="https://en.wikipedia.org/wiki/Single-precision_floating-point_format">Wikipedia</a>
     */
    public static final Format Float32 = new Format(FLOAT32_EXP_BITS, FLOAT32_SIG_BITS);

    private static final int FLOAT64_EXP_BITS = 11;
    private static final int FLOAT64_SIG_BITS = 52;

    private static final long FLOAT64_SIGN_MASK = (1L << FLOAT64_EXP_BITS) << FLOAT64_SIG_BITS;
    private static final long FLOAT64_EXPONENT_MASK =
        ((1L << FLOAT64_EXP_BITS) - 1L) << FLOAT64_SIG_BITS;
    private static final long FLOAT64_SIGNIFICAND_MASK = (1L << FLOAT64_SIG_BITS) - 1L;

    /**
     * Double-precision floating-point format
     *
     * <p>64bit binary floating point format (<b>"binary64"</b>) as defined in the IEEE 754-2008
     * standard. Uses 11 bits for the exponent and 53 for the significand.
     *
     * @see <a
     *     href="https://en.wikipedia.org/wiki/Double-precision_floating-point_format">Wikipedia</a>
     */
    public static final Format Float64 = new Format(FLOAT64_EXP_BITS, FLOAT64_SIG_BITS);

    private static final int FLOAT80_EXP_BITS = 15;
    private static final int FLOAT80_SIG_BITS = 63;

    /**
     * Extended-precision floating-point format
     *
     * <p>80bit binary floating point format as used by the x87 FPU. Uses 15 bits for the exponent
     * and 64 for the significand.
     *
     * @see <a href="https://en.wikipedia.org/wiki/Extended_precision">Wikipedia</a>
     */
    public static final Format Float80 = new Format(FLOAT80_EXP_BITS, FLOAT80_SIG_BITS);

    private static final int FLOAT128_EXP_BITS = 15;
    private static final int FLOAT128_SIG_BITS = 112;

    /**
     * Quadruple-precision floating-point format
     *
     * <p>128bit binary floating point format (<b>"binary128"</b>) as defined in the IEEE 754-2008
     * standard. Uses 15 bits for the exponent and 113 for the significand.
     *
     * @see <a
     *     href="https://en.wikipedia.org/wiki/Quadruple-precision_floating-point_format"f>Wikipedia</a>
     */
    public static final Format Float128 = new Format(FLOAT128_EXP_BITS, FLOAT128_SIG_BITS);

    private static final int FLOAT256_EXP_BITS = 19;
    private static final int FLOAT256_SIG_BITS = 236;

    /**
     * Octuple-precision floating-point format
     *
     * <p>256bit binary floating point format ("<b>binary128</b>") as defined in the IEEE 754-2008
     * standard. Uses 19 bits for the exponent and 237 for the significand.
     *
     * @see <a
     *     href="https://en.wikipedia.org/wiki/Octuple-precision_floating-point_format">Wikipedia</a>
     */
    public static final Format Float256 = new Format(FLOAT256_EXP_BITS, FLOAT256_SIG_BITS);

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
    long maxExp() {
      long rawExp = (1L << expBits) - 2;
      return rawExp - bias();
    }

    /** The smallest (= largest negative) exponent supported by this format */
    long minExp() {
      long rawExp = 1;
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
      return new Format(MAX_EXPONENT_WIDTH, sigBits);
    }

    /**
     * <code>True</code> if this format has at least as many bits for the exponent and the
     * significand as the other format.
     */
    public boolean isGreaterOrEqual(Format pOther) {
      return expBits >= pOther.expBits && sigBits >= pOther.sigBits;
    }

    /**
     * Compare two formats and returns the larger of the two
     *
     * <p>Used when implementing binary operations on FloatValue values, where a common format large
     * enough for both arguments needs to be found. Throws an {@link IllegalArgumentException} if
     * the two formats are incomparable.
     */
    public Format matchWith(Format pOther) {
      if (isGreaterOrEqual(pOther)) {
        return this;
      } else if (pOther.isGreaterOrEqual(this)) {
        return pOther;
      } else {
        throw new IllegalArgumentException("Floating point formats are incomparable");
      }
    }

    /**
     * Construct a Format for a {@link CType}.
     *
     * <p>Throws a {@link IllegalArgumentException} if the {@link CType} is not a floating point
     * type.
     */
    public static Format fromCType(MachineModel pMachineModel, CType pType) {
      if (pType instanceof CSimpleType pSimpleType) {
        return switch (pSimpleType.getType()) {
          case FLOAT -> Format.Float32;
          case DOUBLE ->
              pSimpleType.hasLongSpecifier() ? pMachineModel.getLongDoubleFormat() : Format.Float64;
          case FLOAT128 -> Format.Float128;
          default ->
              throw new IllegalArgumentException(
                  String.format("`%s` is not a floating point type", pType));
        };
      } else {
        throw new IllegalArgumentException(String.format("`%s` is not a simple type", pType));
      }
    }

    @Override
    public String toString() {
      return "Float" + (1 + expBits + sigBits) + "(p=" + sigBits + ")";
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
  FloatValue(Format pFormat, boolean pSign, long pExponent, BigInteger pSignificand) {
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
    return signedInfinity(pFormat, false);
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
    return signedZero(pFormat, false);
  }

  /** Negative zero */
  public static FloatValue negativeZero(Format pFormat) {
    return signedZero(pFormat, true);
  }

  /** Constant -1.0 */
  public static FloatValue negativeOne(Format pFormat) {
    return new FloatValue(pFormat, true, 0, BigInteger.ONE.shiftLeft(pFormat.sigBits));
  }

  /** Negative infinity */
  public static FloatValue negativeInfinity(Format pFormat) {
    return signedInfinity(pFormat, true);
  }

  /**
   * Positive or negative infinity
   *
   * <p>Returns negative infinity if <code>pSign</code> is <code>True</code> and positive infinity
   * if it is <code>False</code>
   */
  private static FloatValue signedInfinity(Format pFormat, boolean pSign) {
    return new FloatValue(pFormat, pSign, pFormat.maxExp() + 1, BigInteger.ZERO);
  }

  /**
   * Positive or negative zero
   *
   * <p>Returns <code>-0.0</code> if <code>pSign</code> is <code>True</code> and <code>0.0</code> if
   * it is <code>False</code>
   */
  private static FloatValue signedZero(Format pFormat, boolean pSign) {
    return new FloatValue(pFormat, pSign, pFormat.minExp() - 1, BigInteger.ZERO);
  }

  /** True if the value is NaN */
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

  /**
   * True if the value is <code>subnormal</code>
   *
   * <p>Subnormal numbers that start with <code>0.</code> and can't be normalized as this would
   * cause their exponent to drop below the supported range. The value zero is a special case, and
   * we consider it neither normal nor subnormal.
   */
  public boolean isSubnormal() {
    return (exponent == format.minExp() - 1) && !significand.equals(BigInteger.ZERO);
  }

  /** True if the value is an integer, that is if there is no fractional part */
  public boolean isInteger() {
    BigInteger integerValue = toInteger().orElse(BigInteger.ZERO);
    return equals(fromInteger(format, integerValue));
  }

  /** True if the value is an odd integer number */
  private boolean isOddInteger() {
    BigInteger integerValue = toInteger().orElse(BigInteger.ZERO);
    return equals(fromInteger(format, integerValue)) && integerValue.testBit(0);
  }

  /**
   * Check that the {@link Format} of two floating point numbers is the same
   *
   * <p>Throws an {@link IllegalArgumentException} if the formats don't match. Use {@link
   * FloatValue#withPrecision(Format)} to convert arguments to a common format before calling the
   * methods in this class.
   */
  private void checkMatchingPrecision(FloatValue pNumber) {
    Preconditions.checkArgument(
        format.equals(pNumber.format),
        "Format of the arguments is not the same. The first argument has format %s while the second"
            + " has format %s.",
        format,
        pNumber.format);
  }

  /**
   * Equality
   *
   * <p>Returns <code>false</code> when the second argument is not a {@link FloatValue} or if the
   * precisions of the arguments does not match. Otherwise follows the definition of "representation
   * equivalence" from <a
   * href="https://download.java.net/java/early_access/valhalla/docs/api/java.base/java/lang/Double.html#equivalenceRelation">this</a>
   * discussion:
   *
   * <ul>
   *   <li><code>+0</code> and <code>-0</code> are considered different value
   *   <li>all <code>NaNs</code> are collapsed into a single canonical <code>NaN</code> value
   *   <li>this canonical <code>NaN</code> value is considered equal to itself
   * </ul>
   *
   * This definition is different from the way that floating point values are usually compared. To
   * compare two numbers with "numeric equality" (as defined in the linked discussion) use {@link
   * FloatValue#equalTo(FloatValue)}. For "bit-wise equivalence" use {@link
   * FloatValue#compareWithTotalOrder(FloatValue)} and then compare the result to zero.
   *
   * <p>WARNING: This method expects both arguments to have the same {@link Format} and will always
   * return <code>false</code> otherwise. To compare values with different precision use {@link
   * Format#matchWith(Format)} and {@link #withPrecision(Format)} to upcast the arguments before
   * calling this method.
   */
  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    return pOther instanceof FloatValue other
        && format.equals(other.format)
        && ((isNan() && other.isNan())
            || (sign == other.sign
                && exponent == other.exponent
                && significand.equals(other.significand)));
  }

  @Override
  public int hashCode() {
    return Objects.hash(format, sign, exponent, significand);
  }

  /**
   * Shift the significand to the right while preserving the sticky bit.
   *
   * <p>"Sticky bit" refers to the least significand bit of the result. It represents the bits that
   * were dropped while truncating and will be set to <code>1</code> if any of these bits were
   * non-zero.
   *
   * <p>This method is generally followed by a call to {@link #applyRoundingBits} to round the
   * result down to the final precision.
   */
  private static BigInteger truncateWithRoundingBits(BigInteger pSignificand, int pBits) {
    BigInteger mask = BigInteger.ONE.shiftLeft(pBits).subtract(BigInteger.ONE);
    BigInteger r = pSignificand.and(mask);
    BigInteger l = pSignificand.shiftRight(pBits);
    return r.equals(BigInteger.ZERO) ? l : l.setBit(0);
  }

  /**
   * Supported rounding modes for {@link FloatValue#round(RoundingMode)}.
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
    /** Round toward zero */
    TRUNCATE
  }

  /**
   * Round the significand.
   *
   * <p>This method is meant to be called after {@link #truncateWithRoundingBits} and will round the
   * result according to the chosen rounding mode. We expect the significand to be followed by a
   * <code>guard</code>, <code>round</code> and <code>sticky</code> bit. The <code>sticky</code> bit
   * will be set by the earlier call to {@link #truncateWithRoundingBits}, the <code>guard</code>
   * and <code>round</code> come before it and are simply the next two digits of the result.
   */
  private static BigInteger applyRoundingBits(
      RoundingMode pRoundingMode, boolean pNegative, BigInteger pSignificand) {
    int grs = pSignificand.intValue() & 0b111;
    BigInteger shifted = pSignificand.shiftRight(3);
    return switch (pRoundingMode) {
      case NEAREST_AWAY -> (grs >= 0b100) ? shifted.add(BigInteger.ONE) : shifted;
      case NEAREST_EVEN ->
          ((grs == 0b100 && shifted.testBit(0)) || grs > 0b100)
              ? shifted.add(BigInteger.ONE)
              : shifted;
      case CEILING -> (grs > 0b000 && !pNegative) ? shifted.add(BigInteger.ONE) : shifted;
      case FLOOR -> (grs > 0b000 && pNegative) ? shifted.add(BigInteger.ONE) : shifted;
      case TRUNCATE -> shifted;
    };
  }

  /** Copy the value with a new exponent. */
  private FloatValue withExponent(long pExponent) {
    if (pExponent > format.maxExp()) {
      // Exponent too large for the format
      // Return Infinity right away
      return signedInfinity(format, sign);
    } else if (pExponent < format.minExp()) {
      // Exponent too small
      int diff = (int) (pExponent - format.minExp());
      if (diff > format.sigBits) {
        // Return zero if the result is below the subnormal range
        return zero(format);
      } else {
        // Otherwise shift the significand to the right and return the rounded number
        Format precision = format.withUnlimitedExponent();
        FloatValue expPart =
            new FloatValue(
                precision, false, pExponent - exponent, BigInteger.ONE.shiftLeft(format.sigBits));
        return this.withPrecision(precision).multiply(expPart).withPrecision(format);
      }
    } else {
      // Exponent within range
      return new FloatValue(format, sign, pExponent, significand);
    }
  }

  /** Copy the value with a new sign. */
  private FloatValue withSign(boolean pSign) {
    return new FloatValue(format, pSign, exponent, significand);
  }

  /** Copy the value with the sign from the argument */
  public FloatValue copySign(FloatValue pSign) {
    return withSign(pSign.sign);
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
      return signedInfinity(pTargetFormat, sign);
    } else if (isZero()) {
      return signedZero(pTargetFormat, sign);
    } else if (!isSubnormal() && pTargetFormat.isGreaterOrEqual(format)) {
      // Special case: The target format is larger and the value is not subnormal
      return new FloatValue(
          pTargetFormat,
          sign,
          exponent,
          significand.shiftLeft(pTargetFormat.sigBits - format.sigBits));
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
      return signedInfinity(pTargetFormat, sign);
    }
    // Return zero if the exponent is below the subnormal range
    if (resultExponent < pTargetFormat.minExp() - (pTargetFormat.sigBits + 1)) {
      return signedZero(pTargetFormat, sign);
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
    resultSignificand = truncateWithRoundingBits(resultSignificand, format.sigBits + leading);
    resultSignificand = applyRoundingBits(RoundingMode.NEAREST_EVEN, sign, resultSignificand);

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
      return signedInfinity(pTargetFormat, sign);
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
   * Floating point equality (<code>==</code>)
   *
   * <p>Same as the <code>==</code> operation on primitive float values. Ignores the sign of the
   * zero and returns `false` if one of the operands is NaN.
   *
   * <p>This is equivalent to "numerical equality" from <a
   * href="https://download.java.net/java/early_access/valhalla/docs/api/java.base/java/lang/Double.html#equivalenceRelation">this</a>
   * discussion. Use {@link FloatValue#equals(Object)} for "representation equality". For "bitwise
   * equality" use {@link #compareWithTotalOrder(FloatValue)} and compare the result to zero.
   */
  public boolean equalTo(FloatValue pNumber) {
    checkMatchingPrecision(pNumber);

    if (isNan() || pNumber.isNan()) {
      return false;
    } else if (isZero()) {
      return pNumber.isZero();
    } else {
      return equals(pNumber);
    }
  }

  /**
   * Less than or greater (<code><></code>)
   *
   * <p>Returns `false` if one of the operands is NaN. Otherwise behaves like the negation of {@link
   * FloatValue#equalTo(FloatValue)}
   *
   * <p>This operation uses "numerical equality" as defined in <a
   * href="https://download.java.net/java/early_access/valhalla/docs/api/java.base/java/lang/Double.html#equivalenceRelation">this</a>
   * discussion. For "representation equivalence" use {@link #compareTo(FloatValue)} and check if
   * the result is different from zero. The same can be done with {@link
   * #compareWithTotalOrder(FloatValue)} to get the result under "bitwise equality".
   */
  public boolean lessOrGreater(FloatValue pNumber) {
    checkMatchingPrecision(pNumber);
    if (isNan() || pNumber.isNan()) {
      return false;
    }
    return !equalTo(pNumber);
  }

  /**
   * Strictly greater than (<code>></code>)
   *
   * <p>Returns <code>false</code> if either number is <code>NaN</code> and considers `0.0` and
   * `-0.0` the same number.
   *
   * <p>This operation uses "numerical equality" as defined in <a
   * href="https://download.java.net/java/early_access/valhalla/docs/api/java.base/java/lang/Double.html#equivalenceRelation">this</a>
   * discussion. For "representation equivalence" use {@link #compareTo(FloatValue)} and check if
   * the result is greater than zero. The same can be done with {@link
   * #compareWithTotalOrder(FloatValue)} to get the result under "bitwise equality".
   */
  public boolean greaterThan(FloatValue pNumber) {
    checkMatchingPrecision(pNumber);

    if (isNan() || pNumber.isNan()) {
      return false;
    } else if (isZero() && pNumber.isZero()) {
      return false;
    } else {
      final FloatValue arg1;
      final FloatValue arg2;

      if (isNegative() && pNumber.isNegative()) {
        // If both values are negative, reverse the order
        arg1 = pNumber;
        arg2 = this;
      } else {
        arg1 = this;
        arg2 = pNumber;
      }

      // Comparison is now component wise
      return ComparisonChain.start()
              .compareTrueFirst(arg1.sign, arg2.sign)
              .compare(arg1.exponent, arg2.exponent)
              .compare(arg1.significand, arg2.significand)
              .result()
          > 0;
    }
  }

  /**
   * Greater or equal to (<code>>=</code>)
   *
   * <p>Returns <code>false</code> if either number is <code>NaN</code> and considers `0.0` and
   * `-0.0` the same number.
   *
   * <p>This operation uses "numerical equality" as defined in <a
   * href="https://download.java.net/java/early_access/valhalla/docs/api/java.base/java/lang/Double.html#equivalenceRelation">this</a>
   * discussion. For "representation equivalence" use {@link #compareTo(FloatValue)} and check if
   * the result is greater or equal to zero. The same can be done with {@link
   * #compareWithTotalOrder(FloatValue)} to get the result under "bitwise equality".
   */
  public boolean greaterOrEqual(FloatValue pNumber) {
    return greaterThan(pNumber) || equalTo(pNumber);
  }

  /**
   * Strictly less than (<code><</code>)
   *
   * <p>Returns <code>false</code> if either number is <code>NaN</code> and considers `0.0` and
   * `-0.0` the same number.
   *
   * <p>This operation uses "numerical equality" as defined in <a
   * href="https://download.java.net/java/early_access/valhalla/docs/api/java.base/java/lang/Double.html#equivalenceRelation">this</a>
   * discussion. For "representation equivalence" use {@link #compareTo(FloatValue)} and check if
   * the result is less than zero. The same can be done with {@link
   * #compareWithTotalOrder(FloatValue)} to get the result under "bitwise equality".
   */
  public boolean lessThan(FloatValue pNumber) {
    checkMatchingPrecision(pNumber);
    if (isNan() || pNumber.isNan()) {
      return false;
    }
    return !greaterOrEqual(pNumber);
  }

  /**
   * Less than or equal to (<code><=</code>) *
   *
   * <p>Returns <code>false</code> if either number is <code>NaN</code> and considers `0.0` and
   * `-0.0` the same number.
   *
   * <p>This operation uses "numerical equality" as defined in <a
   * href="https://download.java.net/java/early_access/valhalla/docs/api/java.base/java/lang/Double.html#equivalenceRelation">this</a>
   * discussion. For "representation equivalence" use {@link #compareTo(FloatValue)} and check if
   * the result is less or equal to zero. The same can be done with {@link
   * #compareWithTotalOrder(FloatValue)} to get the result under "bitwise equality".
   */
  public boolean lessOrEqual(FloatValue pNumber) {
    return pNumber.greaterOrEqual(this);
  }

  /**
   * Compare with total ordering
   *
   * <p>Order as defined in the IEEE 754-2008 standard for the totalOrder predicate:
   *
   * <pre>
   * -Nan < -Inf < ... < -0 < +0 < .. < +Inf < +Nan</pre>
   *
   * <p>Unlike {@link #compareTo(FloatValue)} this method considers the sign and the payload of
   * <code>NaN</code> values in its comparison. This is equivalent to what is called "bit-wise
   * equivalence" in <a
   * href="https://download.java.net/java/early_access/valhalla/docs/api/java.base/java/lang/Double.html#equivalenceRelation">this</a>
   * discussion.
   */
  public int compareWithTotalOrder(FloatValue pNumber) {
    checkMatchingPrecision(pNumber);

    if (equals(pNumber)) {
      // Check identity
      return 0;
    } else if (isNan()) {
      // Handle NaN on the left
      return isNegative() ? -1 : 1;
    } else if (pNumber.isNan()) {
      // Handle NaN on the right
      return pNumber.isNegative() ? 1 : -1;
    } else if (isZero() && pNumber.isZero()) {
      // Handle -0 < +0
      return isNegative() ? -1 : 1;
    } else {
      // Everything else is already ordered
      return lessThan(pNumber) ? -1 : 1;
    }
  }

  /**
   * Compare two floating point numbers
   *
   * <p>This method is compatible with {@link Float#compareTo(Float)} (and {@link #equals(Object)})
   * and uses the following order:
   *
   * <pre>
   * -Inf < ... < -0 < +0 < .. < +Inf < Nan</pre>
   *
   * <p>It returns <code>-1</code> if the first number is larger, <code>0</code> if they are the
   * same, and <code>1</code> if the first number is smaller. We use the same order as {@link
   * Float#compareTo}. Specifically, <code>-0</code> is considered smaller than <code>+0</code> and
   * all NaN values are collapsed into a single canonical NaN value, that is considered larger than
   * all other numbers (including <code>+Infinity</code>).
   *
   * <p>This is equivalent to what is called "representation equivalence" in <a
   * href="https://download.java.net/java/early_access/valhalla/docs/api/java.base/java/lang/Double.html#equivalenceRelation">this</a>
   * discussion. If you want to use the total order predicate from the IEEE 754 standard use {@link
   * #compareWithTotalOrder(FloatValue)} instead. In the linked discussion this order is referred to
   * as "bitwise equivalence".
   *
   * <p>WARNING: This method expects both arguments to have the same {@link Format} and will throw
   * an {@link IllegalArgumentException} otherwise. To compare values with different precision use
   * {@link Format#matchWith(Format)} and {@link #withPrecision(Format)} to upcast the arguments
   * before calling this method.
   */
  @Override
  public int compareTo(FloatValue pNumber) {
    // We need a comment here to silence the CI
    checkMatchingPrecision(pNumber);

    if (isNan()) {
      return pNumber.isNan() ? 0 : 1;
    } else if (pNumber.isNan()) {
      return -1;
    } else {
      return compareWithTotalOrder(pNumber);
    }
  }

  /**
   * Internal method for addition
   *
   * <p>Should only ever be called by {@link #add}.
   *
   * <p>Assumes that both arguments have the same {@link Format} and that the exponent of <code>arg1
   * </code> is larger or equal to the exponent of <code>arg2</code>.
   */
  private static FloatValue addImpl(Format format, FloatValue arg1, FloatValue arg2) {
    // Handle special cases:
    if (arg1.isNan() || arg2.isNan()) {
      // (1) Either argument is NaN
      return nan(format);
    } else if (arg1.isInfinite() && arg2.isInfinite()) {
      // (2) Both arguments are infinite
      return arg1.isNegative() == arg2.isNegative() ? arg1 : nan(format);
    } else if (arg1.isInfinite()) {
      // (3) Only one argument is infinite
      // No need to check arg2 as it can't be larger, and one of the args is finite
      return arg1;
    } else if (arg1.isZero() && arg2.isZero()) {
      // (4) Both arguments are zero (or negative zero)
      return signedZero(format, arg1.isNegative() && arg2.isNegative());
    } else if (arg1.isZero() || arg2.isZero()) {
      // (5) Only one of the arguments is zero (or negative zero)
      return arg1.isZero() ? arg2 : arg1;
    }

    // Get the exponents without the IEEE bias. Note that for subnormal numbers the stored exponent
    // needs to be increased by one.
    long exponent1 = Math.max(arg1.exponent, format.minExp());
    long exponent2 = Math.max(arg2.exponent, format.minExp());

    // Calculate the difference between the exponents. If it is larger than the mantissa size we can
    // skip the add and return immediately.
    int exp_diff = (int) (exponent1 - exponent2);
    if (exp_diff >= format.sigBits + 3) {
      return arg1;
    }

    // Get the significands and apply the sign
    BigInteger significand1 = arg1.sign ? arg1.significand.negate() : arg1.significand;
    BigInteger significand2 = arg2.sign ? arg2.significand.negate() : arg2.significand;

    // Expand the significand with (empty) guard, round and sticky bits
    significand1 = significand1.shiftLeft(3);
    significand2 = significand2.shiftLeft(3);

    // Shift the number with the smaller exponent to the exponent of the other number.
    significand2 = truncateWithRoundingBits(significand2, exp_diff);

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
    if (resultSignificand.testBit(format.sigBits + 4)) {
      resultSignificand = truncateWithRoundingBits(resultSignificand, 1);
      resultExponent += 1;
    }

    // (2) Significand is too small: shift left unless the number is subnormal
    //     This can happen if digits have canceled out:
    //     f.ex 1.01001e2 + (-1.01e2) = 0.00001e2
    //     (here we normalize to 1.0e-3)
    int leading = (format.sigBits + 4) - resultSignificand.bitLength();
    int maxValue = (int) (resultExponent - format.minExp()); // p.minExp() <= exponent
    if (leading > maxValue) {
      // If the exponent would get too small only shift to the left until the minimal exponent is
      // reached and return a subnormal number.
      resultSignificand = resultSignificand.shiftLeft(maxValue);
      resultExponent = format.minExp() - 1;
    } else {
      resultSignificand = resultSignificand.shiftLeft(leading);
      resultExponent -= leading;
    }

    // Round the result according to the grs bits
    resultSignificand = applyRoundingBits(RoundingMode.NEAREST_EVEN, resultSign, resultSignificand);

    // Shift the significand to the right if rounding has caused an overflow
    if (resultSignificand.bitLength() > format.sigBits + 1) {
      resultSignificand = resultSignificand.shiftRight(1); // The dropped bit is zero
      resultExponent += 1;
    }

    // Check if the result is zero
    if (resultSignificand.equals(BigInteger.ZERO)) {
      return signedZero(format, resultSign);
    }
    // Return infinity if there is an overflow.
    if (resultExponent > format.maxExp()) {
      return signedInfinity(format, resultSign);
    }

    // Otherwise return the number
    return new FloatValue(format, resultSign, resultExponent, resultSignificand);
  }

  /** Addition */
  public FloatValue add(FloatValue pNumber) {
    checkMatchingPrecision(pNumber);

    // Make sure the first argument has the larger (or equal) exponent
    FloatValue arg1;
    FloatValue arg2;
    if (exponent >= pNumber.exponent) {
      arg1 = this;
      arg2 = pNumber;
    } else {
      arg1 = pNumber;
      arg2 = this;
    }
    return addImpl(arg1.format, arg1, arg2);
  }

  /** Subtraction */
  public FloatValue subtract(FloatValue pNumber) {
    return add(pNumber.negate());
  }

  /**
   * Internal method for multiplication
   *
   * <p>Should only ever be called by {@link #multiply}.
   *
   * <p>Assumes that both arguments have the same {@link Format} and that the exponent of <code>arg1
   * </code> is larger or equal to the exponent of <code>arg2</code>.
   */
  private static FloatValue multiplyImpl(Format format, FloatValue arg1, FloatValue arg2) {
    // Handle special cases:
    if (arg1.isNan() || arg2.isNan()) {
      // (1) Either argument is NaN
      return nan(format);
    } else if (arg1.isInfinite()) {
      // (2) One of the argument is infinite
      // No need to check arg2 as it can't be larger, and one of the args is finite
      if (arg2.isZero()) {
        // Return NaN if we're trying to multiply infinity by zero
        return nan(format);
      } else {
        return signedInfinity(format, arg1.isNegative() != arg2.isNegative());
      }
    } else if (arg1.isZero() || arg2.isZero()) {
      // (3) One of the arguments is zero (or negative zero)
      return signedZero(format, arg1.isNegative() != arg2.isNegative());
    }

    // Calculate the sign of the result
    boolean resultSign = arg1.sign != arg2.sign;

    // Get the exponents without the IEEE bias. Note that for subnormal numbers the stored exponent
    // needs to be increased by one.
    long exponent1 = Math.max(arg1.exponent, format.minExp());
    long exponent2 = Math.max(arg2.exponent, format.minExp());

    // Calculate the exponent of the result by adding the exponents of the two arguments.
    // If the calculated exponent is out of range we can return infinity (or zero) immediately.
    long resultExponent = exponent1 + exponent2;
    if (resultExponent > format.maxExp()) {
      return signedInfinity(format, resultSign);
    }
    if (resultExponent < format.minExp() - format.sigBits - 2) {
      return signedZero(format, resultSign);
    }

    // Multiply the significands
    BigInteger result = arg1.significand.multiply(arg2.significand);

    // Add guard, round and sticky bits
    BigInteger resultSignificand = result.shiftLeft(3);

    // Normalize
    // (1) Significand is too large: shift to the right by one bit
    //     This can happen if two numbers with significand greater 1 are multiplied:
    //     f.ex 1.1e3 x 1.1e1 = 10.01e4
    //     (here we normalize the result to 1.001e5)
    if (resultSignificand.testBit(2 * format.sigBits + 4)) {
      resultSignificand = resultSignificand.shiftRight(1);
      resultExponent += 1;
    }

    // (2) Significand is too small: shift left unless the number is subnormal
    //     This can happen if one of the numbers was subnormal:
    //     f.ex 1.0e3 x 0.1e-1 = 0.10e2
    //     (here we normalize to 1.0e1)
    int shift = (2 * format.sigBits + 4) - resultSignificand.bitLength();
    if (shift > 0) {
      resultSignificand = resultSignificand.shiftLeft(shift);
      resultExponent -= shift;
    }

    // Otherwise use the lowest possible exponent and move the rest into the significand by shifting
    // it to the right. Here we calculate haw many digits we need to shift:
    int leading = 0;
    if (resultExponent < format.minExp()) {
      leading = (int) Math.abs(format.minExp() - resultExponent);
      resultExponent = format.minExp() - 1;
    }

    // Truncate the value:
    // The significand now has length 2*|precision of the significand| + 3 where 3 are the grs bits
    // at the end. We need to shift by at least |precision of the significand| bits.
    // If one of the factors was subnormal the results may have leading zeroes as well, and we need
    // to shift further by 'leading' bits.
    resultSignificand = truncateWithRoundingBits(resultSignificand, format.sigBits + leading);

    // Round the result
    resultSignificand = applyRoundingBits(RoundingMode.NEAREST_EVEN, resultSign, resultSignificand);

    // Shift the significand to the right if rounding has caused an overflow
    if (resultSignificand.bitLength() > format.sigBits + 1) {
      resultSignificand = resultSignificand.shiftRight(1); // The dropped bit is zero
      resultExponent += 1;
    }
    if (leading > 0 && resultSignificand.bitLength() > format.sigBits) {
      // Just fix the exponent if the value was subnormal before the overflow
      resultExponent += 1;
    }

    // Return infinity if there is an overflow.
    if (resultExponent > format.maxExp()) {
      return signedInfinity(format, resultSign);
    }

    // Otherwise return the number
    return new FloatValue(format, resultSign, resultExponent, resultSignificand);
  }

  /** Multiplication */
  public FloatValue multiply(FloatValue pNumber) {
    checkMatchingPrecision(pNumber);

    // Make sure the first argument has the larger (or equal) exponent
    FloatValue arg1;
    FloatValue arg2;
    if (exponent >= pNumber.exponent) {
      arg1 = this;
      arg2 = pNumber;
    } else {
      arg1 = pNumber;
      arg2 = this;
    }
    return multiplyImpl(arg1.format, arg1, arg2);
  }

  /**
   * Internal method for exact multiplication
   *
   * <p>Should only ever be called by {@link #multiplyExact}.
   *
   * <p>Assumes that both arguments have the same {@link Format} and that the exponent of <code>arg1
   * </code> is larger or equal to the exponent of <code>arg2</code>.
   */
  private static FloatValue multiplyExactImpl(Format format, FloatValue arg1, FloatValue arg2) {
    // Handle special cases:
    if (arg1.isNan() || arg2.isNan()) {
      // (1) Either argument is NaN
      return nan(format);
    } else if (arg1.isInfinite()) {
      // (2) One of the argument is infinite
      // No need to check arg2 as it can't be larger, and one of the args is finite
      if (arg2.isZero()) {
        // Return NaN if we're trying to multiply infinity by zero
        return nan(format);
      } else {
        return signedInfinity(format, arg1.isNegative() != arg2.isNegative());
      }
    } else if (arg1.isZero() || arg2.isZero()) {
      // (3) One of the arguments is zero (or negative zero)
      return signedZero(format, arg1.isNegative() != arg2.isNegative());
    }

    // We assume both arguments are normal
    Preconditions.checkArgument(
        arg1.exponent >= format.minExp() && arg2.exponent >= format.minExp());

    // Calculate the sign of the result
    boolean resultSign = arg1.sign != arg2.sign;

    // Calculate the exponent of the result by adding the exponents of the two arguments.
    // If the calculated exponent is out of range we can return infinity (or zero) immediately.
    long resultExponent = arg1.exponent + arg2.exponent;
    if (resultExponent > format.maxExp()) {
      return signedInfinity(format, resultSign);
    }
    if (resultExponent < format.minExp() - format.sigBits - 2) {
      return signedZero(format, resultSign);
    }

    // Multiply the significands
    BigInteger resultSignificand = arg1.significand.multiply(arg2.significand);

    // Normalize if the significand is too large:
    if (resultSignificand.testBit(2 * format.sigBits + 4)) {
      resultExponent += 1;
    }

    // Return infinity if there is an overflow.
    if (resultExponent > format.maxExp()) {
      return signedInfinity(format, resultSign);
    }

    // Otherwise return the number
    return new FloatValue(
        new Format(format.expBits, resultSignificand.bitLength() - 1),
        resultSign,
        resultExponent,
        resultSignificand);
  }

  /**
   * Multiply two numbers and return the exact result.
   *
   * <p>This variant of {@link FloatValue#multiply} skips the rounding steps at the end and returns
   * directly. The result may have between p and 2p+1 bits.
   */
  private FloatValue multiplyExact(FloatValue pNumber) {
    checkMatchingPrecision(pNumber);

    // Make sure the first argument has the larger (or equal) exponent
    FloatValue arg1;
    FloatValue arg2;
    if (exponent >= pNumber.exponent) {
      arg1 = this;
      arg2 = pNumber;
    } else {
      arg1 = pNumber;
      arg2 = this;
    }
    return multiplyExactImpl(arg1.format, arg1, arg2);
  }

  private FloatValue squared() {
    return this.multiply(this);
  }

  /**
   * The power function a^x for integer exponents x
   *
   * <p>Note that <code>a.powInt(x)</code> is not the same as multiplying <code>a</code> by itself
   * <code>x</code> times. We calculate the result of the real-valued function <code>a^x</code> and
   * then round it down to floating point precision. This means we don't suffer from the same
   * accumulated rounding errors that one would get with the naive approach.
   */
  public FloatValue powInt(BigInteger exp) {
    return withPrecision(format.intermediatePrecision()).powFast(exp).withPrecision(format);
  }

  /**
   * Calculate the power function a^x where x is an integer.
   *
   * <p>Uses fast exponentiation to calculate the result. The note about rounding from {@link
   * #powInt(BigInteger)} also applies to this function.
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
  public FloatValue divide(FloatValue pDivisor) {
    checkMatchingPrecision(pDivisor);
    Format precision = format.intermediatePrecision();
    FloatValue arg1 = this.withPrecision(precision);
    FloatValue arg2 = pDivisor.withPrecision(precision);

    return arg1.divideSlow(arg2).withPrecision(format);
  }

  /**
   * Divide the value by another number.
   *
   * <p>This version of divide is slow and only needed to calculate the constants that are needed
   * for the other, faster version of divide that uses Newton's method.
   *
   * <p>This method assumes that both arguments use the same precision.
   */
  private FloatValue divideSlow(FloatValue pDivisor) {
    // Calculate the sign of the result
    boolean resultSign = sign != pDivisor.sign;

    // Handle special cases:
    if (isNan() || pDivisor.isNan()) {
      // (1) Either argument is NaN
      return nan(format);
    } else if (isZero()) {
      // (2) Dividend is zero
      if (pDivisor.isZero()) {
        return resultSign ? nan(format).negate() : nan(format);
      } else {
        return signedZero(format, resultSign);
      }
    } else if (isInfinite()) {
      // (3) Dividend is infinite
      if (pDivisor.isInfinite()) {
        return resultSign ? nan(format).negate() : nan(format);
      } else {
        return signedInfinity(format, resultSign);
      }
    } else if (pDivisor.isZero()) {
      // (4) Divisor is zero (and dividend is finite)
      return signedInfinity(format, resultSign);
    } else if (pDivisor.isInfinite()) {
      // (5) Divisor is infinite (and dividend is finite)
      return signedZero(format, resultSign);
    }

    // Get the exponents without the IEEE bias. Note that for subnormal numbers the stored exponent
    // needs to be increased by one.
    long exponent1 = Math.max(exponent, format.minExp());
    long exponent2 = Math.max(pDivisor.exponent, format.minExp());

    // Normalize both arguments.
    BigInteger significand1 = significand;
    int shift1 = (format.sigBits + 1) - significand1.bitLength();
    if (shift1 > 0) {
      significand1 = significand1.shiftLeft(shift1);
      exponent1 -= shift1;
    }

    BigInteger significand2 = pDivisor.significand;
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
      return signedInfinity(format, resultSign);
    }
    // If it is below the subnormal range, return zero immediately.
    if (resultExponent < format.minExp() - (format.sigBits + 1)) {
      return signedZero(format, resultSign);
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
    resultSignificand = truncateWithRoundingBits(resultSignificand, format.sigBits + leading);
    resultSignificand = applyRoundingBits(RoundingMode.NEAREST_EVEN, resultSign, resultSignificand);

    // Shift the significand to the right if rounding has caused an overflow
    if (resultSignificand.bitLength() > format.sigBits + 1) {
      resultSignificand = resultSignificand.shiftRight(1); // Last bit is zero
      resultExponent += 1;
    }

    // Return the number
    return new FloatValue(format, resultSign, resultExponent, resultSignificand);
  }

  private static double lb(double pNumber) {
    return Math.log(pNumber) / Math.log(2);
  }

  private FloatValue divideNewton(FloatValue pDivisor) {
    boolean resultSign = isNegative() != pDivisor.isNegative(); // Sign of the result

    // Handle special cases:
    if (isNan() || pDivisor.isNan()) {
      // (1) Either argument is NaN
      return nan(format);
    } else if (isZero()) {
      // (2) Dividend is zero
      if (pDivisor.isZero()) {
        return resultSign ? nan(format).negate() : nan(format);
      } else {
        return signedZero(format, resultSign);
      }
    } else if (isInfinite()) {
      // (3) Dividend is infinite
      if (pDivisor.isInfinite()) {
        return resultSign ? nan(format).negate() : nan(format);
      } else {
        return signedInfinity(format, resultSign);
      }
    } else if (pDivisor.isZero()) {
      // (4) Divisor is zero (and dividend is finite)
      return signedInfinity(format, resultSign);
    } else if (pDivisor.isInfinite()) {
      // (5) Divisor is infinite (and dividend is finite)
      return signedZero(format, resultSign);
    }

    // Extract exponents and significand bits
    int exponent1 = (int) Math.max(exponent, format.minExp());
    int exponent2 = (int) Math.max(pDivisor.exponent, format.minExp());

    // Shift numerator and divisor by pulling out common factors in the exponent.
    // This will put the divisor in the range of 0.5 to 1.0
    FloatValue n = new FloatValue(format, false, exponent1 - (exponent2 + 1), significand);
    FloatValue d = new FloatValue(format, false, -1, pDivisor.significand);

    // Calculate how many iterations are needed
    int bound = (int) Math.ceil(lb((format.sigBits + 2) / lb(17)));

    // Set the initial value to 48/17 - 32/17*D
    FloatValue x = lookupSqrtT1().subtract(lookupSqrtT2().multiply(d));

    for (int i = 0; i < bound; i++) {
      // X(i+1) = X(i)*(2 - D*X(i))
      x = x.multiply(fromInteger(format, 2).subtract(d.multiply(x)));
    }

    // Multiply 1/D with N
    FloatValue r = x.multiply(n);

    // Set the sign bit and return the result
    return r.withSign(sign != pDivisor.sign);
  }

  /**
   * Calculate x modulo y
   *
   * <p>We define the modulo as <code>m = x - y*q</code> where <code>q</code> is the quotient <code>
   * x/y</code> rounded to the next integer with rounding mode {@link RoundingMode#TRUNCATE
   * RoundingMode.TRUNCATE}.
   */
  public FloatValue modulo(FloatValue pNumber) {
    FloatValue r = abs().remainder(pNumber.abs());

    // Fix the rounding and truncate the result
    if (r.isNegative()) {
      r = r.add(pNumber.abs());
    }
    return r.copySign(this);
  }

  /**
   * Calculate the remainder of the division x/y
   *
   * <p>The remainder is defined as <code>r = x - y*q</code> where <code>q</code> is the quotient
   * <code>x/y</code> rounded to the next integer with rounding mode {@link
   * RoundingMode#NEAREST_EVEN RoundingMode.NEAREST_EVEN}.
   */
  public FloatValue remainder(FloatValue pNumber) {
    // Match the format of the arguments and allow larger exponents to avoid overflows
    checkMatchingPrecision(pNumber);

    Format extendedPrecision = format.withUnlimitedExponent();

    // Use the absolute values of x and y for the calculation. The sign will be fixed later.
    FloatValue absoluteDividend = abs().withPrecision(extendedPrecision);
    FloatValue absoluteDivisor = pNumber.abs().withPrecision(extendedPrecision);

    // Handle special cases
    if (absoluteDividend.isNan() || absoluteDivisor.isNan()) {
      return nan(format);
    } else if (absoluteDividend.isInfinite() || absoluteDivisor.isZero()) {
      return nan(format);
    } else if (absoluteDividend.isZero() || absoluteDivisor.isInfinite()) {
      return this;
    }

    // Shift absoluteDivisor to the left to match absoluteDividend
    FloatValue shiftedDivisor = absoluteDivisor.withExponent(absoluteDividend.exponent);
    if (absoluteDividend.lessThan(shiftedDivisor)) {
      shiftedDivisor = absoluteDivisor.withExponent(absoluteDividend.exponent - 1);
    }

    boolean isOdd = false; // Will be set after the division of the (truncated) quotient is odd

    // Divide absoluteDividend by absoluteDivisor for the result
    FloatValue result = absoluteDividend;
    while (shiftedDivisor.greaterOrEqual(absoluteDivisor)) {
      isOdd = false;
      if (result.greaterOrEqual(shiftedDivisor)) {
        result = result.subtract(shiftedDivisor);
        isOdd = true;
      }
      shiftedDivisor = shiftedDivisor.withExponent(shiftedDivisor.exponent - 1);
    }

    // Correct by one to find the closest multiple
    FloatValue nextValue = result.subtract(absoluteDivisor).abs();
    if (nextValue.lessThan(result) || (nextValue.equalTo(result) && isOdd)) {
      // This implements "round to nearest ties to even"
      result = result.subtract(absoluteDivisor);
    }

    // Fix the sign if x was negative
    if (this.isNegative()) {
      result = result.negate();
    }
    return result.withPrecision(format);
  }

  /** Square root */
  public FloatValue sqrt() {
    // The calculation will be done in a higher precision and the result is then rounded down.
    // 2p+2 bits are enough for the inverse square root.
    // See Table 12.6 in "Handbook of Floating-Point Arithmetic"
    return withPrecision(format.intermediatePrecision()).sqrtNewton().withPrecision(format);
  }

  private FloatValue sqrtNewton() {
    if (isZero()) {
      return signedZero(format, sign);
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

    // Define constants 0.5 and 1.5
    FloatValue oneHalf =
        new FloatValue(format, false, -1, BigInteger.ONE.shiftLeft(format.sigBits));
    FloatValue threeHalves =
        new FloatValue(format, false, 0, BigInteger.valueOf(3).shiftLeft(format.sigBits - 1));

    // Initial value (0.5 will always converge)
    FloatValue x = oneHalf;

    Set<FloatValue> partial = new HashSet<>();
    do {
      partial.add(x);
      // x_n+1 = x_n * (3/2 - 1/2 * f * x_n^2)
      x = x.multiply(threeHalves.subtract(oneHalf.multiply(f).multiply(x.squared())));
    } while (!partial.contains(x));

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
      return signedZero(format, sign);
    }

    return new FloatValue(
        new Format(format.expBits, trailing > format.sigBits ? 0 : (format.sigBits - trailing)),
        sign,
        exponent,
        resultSignificand);
  }

  /**
   * Returns 1 ULP for this value.
   *
   * <p>ULP stands for "Unit in the last place" and is the minimal distance between two floating
   * point values of the same exponent. It can be calculated by copying the exponent of the value
   * and then replacing the significand by all zeroes, except for the last bit, which is set to one.
   * For instance, for the value <code>1.01001 * 2^7</code> 1 ULP is equivalent to <code>
   * 0.00001 * 2^7 = 1.00000 * 2^2</code>.
   *
   * <p>For normalized floating point numbers 1 ULP can be calculated as <code>2^k-p+1</code> where
   * <code>k</code> is the exponent of the number and <code>p</code> is the precision of the format
   * (including the 'hidden' bit). When the number is subnormal, 1 ULP is always equivalent to the
   * minimal number support by the format.
   *
   * <p>@See <a href="https://en.wikipedia.org/wiki/Unit_in_the_last_place">Wikipedia</a>
   */
  private FloatValue oneUlp() {
    if (exponent < format.minExp()) {
      return sign ? minValue(format).negate() : minValue(format);
    } else {
      return one(format).withExponent(exponent - format.sigBits);
    }
  }

  /**
   * Add 1 ULP to the value to get the next larger floating point number.
   *
   * <p>See {@link #oneUlp} for the definition of "ULP"
   */
  private FloatValue plus1Ulp() {
    return add(oneUlp());
  }

  /**
   * Subtract 1 ULP from the value to get the next smaller floating point number.
   *
   * <p>See {@link #oneUlp} for the definition of "ULP"
   */
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
    if (format.equals(Format.Float8)) {
      //      0.1    0.2    0.3    0.4    0.5    0.6    0.7    0.8    0.9    1.0
      // ln     1      1      1      1      1      4      5      6      7     10
      // exp    1      1      1      4      4      5      5      6      7     13
      // pow    6     13     13     13     13     16     16     16     16     16
      Format m0 = new Format(11, format.sigBits + 7);
      Format m1 = new Format(11, format.sigBits + 19);
      return ImmutableList.of(m0, m1);
    } else if (format.equals(Format.Float16)) {
      //      0.1    0.2    0.3    0.4    0.5    0.6    0.7    0.8    0.9    1.0
      // ln     1      1      1      1      1      9     10     11     12     31
      // exp    1      1      1      6      8      9     10     12     13     26
      // pow    7     10     13     15     17     19     20     22     23     25
      Format m0 = new Format(11, format.sigBits + 13);
      Format m1 = new Format(11, format.sigBits + 31);
      return ImmutableList.of(m0, m1, m1.intermediatePrecision());
    } else if (format.equals(Format.Float32)) {
      //      0.1    0.2    0.3    0.4    0.5    0.6    0.7    0.8    0.9    1.0
      // ln     1      1      1      1     18     19     20     21     22     35
      // exp    1      1      1      1     15     25     25     25     26     41
      // pow   13     18     22     25     29     31     34     36     39     41
      Format m0 = new Format(15, format.sigBits + 26);
      Format m1 = new Format(15, format.sigBits + 41);
      return ImmutableList.of(m0, m1, m1.intermediatePrecision());
    } else if (format.equals(Format.Float64)) {
      //      0.1    0.2    0.3    0.4    0.5    0.6    0.7    0.8    0.9    1.0
      // ln     1      1      1      1     44     45     46     47     48     62
      // exp    1      1      1      1     28     54     54     55     55     68
      // pow   31     39     44     49     53     57     61     64     67     70
      Format m0 = new Format(Format.Float256.expBits, format.sigBits + 55);
      Format m1 = new Format(Format.Float256.expBits, format.sigBits + 70);
      return ImmutableList.of(m0, m1, m1.intermediatePrecision());
    } else {
      return ImmutableList.of(
          format.intermediatePrecision(), format.intermediatePrecision().intermediatePrecision());
    }
  }

  /** The exponential function e^x */
  public FloatValue exp() {
    // Note that the results returned by this function may be different from what is returned in C
    // or Java. We always return the correctly rounded result, that is, the floating-point number
    // that is closest to the actual (real-valued) result of the function. C and Java on the other
    // hand use SEE, which does not guarantee correct rounding for transcendental functions. If we
    // want to use this function in CPAchecker we may have to consider using error intervals to make
    // sure that the values we calculate always match the behavior in C/Java.
    //
    // See https://www.gnu.org/software/libc/manual/html_node/Errors-in-Math-Functions.html for more
    // information on the error bounds of transcendental functions in gnu libc
    return expWithStats(null);
  }

  /**
   * Calculates e^x and records how many extra bits of precision were needed.
   *
   * @param pExpStats Histogram with the number of extra bits used in all calls to exp()
   */
  FloatValue expWithStats(@Nullable Multiset<Integer> pExpStats) {
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

    for (Format p : intermediatePrecisions()) {
      Format precision = new Format(p.expBits, p.sigBits - format.sigBits);
      FloatValue x = withPrecision(precision);

      // TODO: Call exp_ only once and *then* check if we're too close to a break point
      FloatValue v1 = x.plus1Ulp().withPrecision(p).exp_();
      FloatValue v2 = x.minus1Ulp().withPrecision(p).exp_();

      if (equalModuloP(format, v1, v2) && isStable(v1.validPart())) {
        // Update statistics
        if (pExpStats != null) {
          pExpStats.add(p.sigBits - format.sigBits);
        }

        // Exit the loop
        return v1.withPrecision(format);
      }
    }

    // If no result was found, the intermediate precision must have been too small. We throw an
    // exception and ask the user to report the issue.
    throw new AssertionError(
        String.format(
            "Could not calculate exp(%s) due to an internal error. Please report this as a bug.",
            this));
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
    // Note that the results returned by this function may be different from what is returned in C
    // or Java. See the comment for exp() for more details.
    return lnWithStats(null);
  }

  /**
   * Calculates ln(x) and records how many extra bits of precision were needed.
   *
   * @param pLnStats Histogram with the number of extra bits used in all calls to ln()
   */
  FloatValue lnWithStats(@Nullable Multiset<Integer> pLnStats) {
    if (isZero()) {
      return negativeInfinity(format);
    } else if (isOne()) {
      return zero(format);
    } else if (isNan() || isNegative()) {
      return nan(format);
    }

    for (Format p : intermediatePrecisions()) {
      Format precision = new Format(p.expBits, p.sigBits - format.sigBits);
      FloatValue x = withPrecision(precision);

      // TODO: Call ln only once and *then* check if we're too close to a break point
      FloatValue x1 = x.plus1Ulp().withPrecision(p);
      FloatValue x2 = x.minus1Ulp().withPrecision(p);

      FloatValue v1 = x1.ln_();
      FloatValue v2 = x2.ln_();

      if (equalModuloP(format, v1, v2) && isStable(v1.validPart())) {
        // Update statistics
        if (pLnStats != null) {
          pLnStats.add(p.sigBits - format.sigBits);
        }

        // Exit the loop
        return v1.withPrecision(format);
      }
    }
    // If no result was found, the intermediate precision must have been too small. We throw an
    // exception and ask the user to report the issue.
    throw new AssertionError(
        String.format(
            "Could not calculate ln(%s) due to an internal error. Please report this as a bug.",
            this));
  }

  private FloatValue ln_() {
    if (isZero()) {
      return negativeInfinity(format);
    } else if (isOne()) {
      return zero(format);
    } else if (isInfinite()) {
      return infinity(format);
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
    // Note that the results returned by this function may be different from what is returned in C
    // or Java. See the comment for exp() for more details.
    return powWithStats(pExponent, null);
  }

  /**
   * Calculates a^x and records how many extra bits of precision were needed.
   *
   * @param pPowStats Histogram with the number of extra bits used in all calls to pow()
   */
  FloatValue powWithStats(FloatValue pExponent, @Nullable Multiset<Integer> pPowStats) {
    checkMatchingPrecision(pExponent);

    // Handle special cases
    // See https://en.cppreference.com/w/c/numeric/math/pow for the full definition
    if (isOne() || pExponent.isZero()) {
      // pow(+1, exponent) returns 1 for any exponent, even when exponent is NaN
      // pow(base, ±0) returns 1 for any base, even when base is NaN
      return one(format);
    } else if (isNan() || pExponent.isNan()) {
      // except where specified above, if any argument is NaN, NaN is returned
      return nan(format);
    } else if (isZero() && pExponent.isNegative() && pExponent.isOddInteger()) {
      // pow(+0, exponent), where exponent is a negative odd integer
      // returns +∞ and raises FE_DIVBYZERO
      // pow(-0, exponent), where exponent is a negative odd integer
      // returns -∞ and raises FE_DIVBYZERO
      return signedInfinity(format, sign);
    } else if (isZero() && pExponent.isNegative()) {
      // pow(±0, -∞) returns +∞ and may raise FE_DIVBYZERO(until C23)
      // pow(±0, exponent), where exponent is negative, finite, and is an even integer or a
      // non-integer, returns +∞ and raises FE_DIVBYZERO
      return infinity(format);
    } else if (isZero() && !pExponent.isNegative()) {
      // pow(+0, exponent), where exponent is a positive odd integer, returns +0
      // pow(-0, exponent), where exponent is a positive odd integer, returns -0
      // pow(±0, exponent), where exponent is positive non-integer or a positive even integer,
      // returns +0
      return pExponent.isOddInteger() ? this : zero(format);
    } else if (isNegativeOne() && pExponent.isInfinite()) {
      // pow(-1, ±∞) returns 1
      return one(format);
    } else if (isInfinite() && isNegative()) {
      // pow(-∞, exponent) returns -0 if exponent is a negative odd integer
      // pow(-∞, exponent) returns +0 if exponent is a negative non-integer or negative even integer
      // pow(-∞, exponent) returns -∞ if exponent is a positive odd integer
      // pow(-∞, exponent) returns +∞ if exponent is a positive non-integer or positive even integer
      FloatValue power = pExponent.isNegative() ? zero(format) : infinity(format);
      return pExponent.isOddInteger() ? power.negate() : power;
    } else if (isInfinite()) {
      // pow(+∞, exponent) returns +0 for any negative exponent
      // pow(+∞, exponent) returns +∞ for any positive exponent
      return pExponent.isNegative() ? zero(format) : infinity(format);
    } else if (pExponent.isInfinite() && pExponent.isNegative()) {
      // pow(base, -∞) returns +∞ for any |base|<1
      // pow(base, -∞) returns +0 for any |base|>1
      return abs().greaterThan(one(format)) ? zero(format) : infinity(format);
    } else if (pExponent.isInfinite()) {
      // pow(base, +∞) returns +0 for any |base|<1
      // pow(base, +∞) returns +∞ for any |base|>1
      return abs().greaterThan(one(format)) ? infinity(format) : zero(format);
    } else if (isNegative() && !pExponent.isInteger()) {
      // pow(base, exponent)
      // returns NaN and raises FE_INVALID if base is finite and negative and exponent is finite and
      // non-integer.
      return nan(format);
    } else if (pExponent.isInteger()) {
      // pow(base, exponent) where exponent is integer: calculate with powInt
      return powInt(pExponent.toInteger().orElseThrow());
    } else if (pExponent.equals(
        new FloatValue(format, false, -1, BigInteger.ONE.shiftLeft(format.sigBits)))) {
      // pow(base, exponent) where exponent=1/2: calculate sqrt(a) instead
      return sqrt();
    } else {
      FloatValue r = abs().pow_(pExponent, pPowStats);
      if (isNegative()) {
        // Fix the sign if `a` was negative and x an integer
        r = r.withSign(pExponent.isOddInteger());
      }
      if (r.isNan()) {
        // If the result is NaN we assume that the real result is a floating point number (or a
        // break point between two floating point number). Unlike exp and log, pow has many
        // arguments where this is the case. Examples include a^0, a^1, a^k (where k is an integer),
        // or a^0.5 (where 'a') is a square number. We still check some of the trivial cases earlier
        // for performance reasons. The more general check is more costly, however, so it is only
        // performed after the search in the main algorithm has failed.
        r = powExact(pExponent);
      }
      if (r.isNan()) {
        // If the result is still NaN the precisions that were tried in the first phase must have
        // been too small. We could try again with larger precisions, but for now we simply throw an
        // exception.
        throw new AssertionError(
            String.format(
                "Could not calculate pow(%s,%s) due to an internal error. Please report this as a"
                    + " bug.",
                this, pExponent));
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
    while (!arg2.isInfinite()) {
      // Rewrite a^x with a=b^2 and x=y/2 as b^y until we're left with an integer exponent
      Optional<FloatValue> val = arg1.sqrtExact();
      if (val.isEmpty()) {
        // Abort if 'a' is not a square number
        break;
      }
      arg1 = val.orElseThrow();
      arg2 = arg2.withExponent(arg2.exponent + 1);

      if (arg2.isInteger()) {
        r = arg1.powInt(arg2.toInteger().orElseThrow());
        break;
      }
    }
    return r.withPrecision(format);
  }

  private FloatValue pow_(FloatValue pExponent, @Nullable Multiset<Integer> pPowStats) {
    FloatValue r = nan(format);

    for (Format p : intermediatePrecisions()) {
      // a^x = exp(x * ln a)
      Format precision = new Format(p.expBits, p.sigBits - format.sigBits);

      FloatValue a = this.withPrecision(p);
      FloatValue x = pExponent.withPrecision(p);

      // The next call calculates ln with the current precision.
      // TODO: Figure out why this is enough?
      FloatValue lna = a.ln_();
      FloatValue xlna = x.multiply(lna).withPrecision(precision);

      // Check if we call e^x with x close to zero
      boolean nearZero = !xlna.abs().greaterThan(minNormal(precision));

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
        // Store the result
        r = nearZero ? one(p).add(exlna1) : exlna1;

        // Update statistics
        if (pPowStats != null) {
          pPowStats.add(precision.sigBits);
        }

        // Exit the loop
        break;
      }
    }
    return r.withPrecision(format);
  }

  /**
   * Round to the next integer (under the given rounding mode)
   *
   * <p>The result is again a floating point value. To convert directly to integer use {@link
   * FloatValue#toInteger()} or the methods {@link FloatValue#intValue()} and {@link
   * FloatValue#longValue()}
   */
  public FloatValue round(RoundingMode pRoundingMode) {
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
    resultSignificand =
        truncateWithRoundingBits(resultSignificand, (int) (format.sigBits - exponent));
    resultSignificand = applyRoundingBits(pRoundingMode, sign, resultSignificand);

    // Recalculate the exponent
    int resultExponent = resultSignificand.bitLength() - 1;

    // Normalize the significand if there was an overflow. The last bit is then always zero and can
    // simply be dropped.
    resultSignificand = resultSignificand.shiftLeft(format.sigBits - resultExponent);

    // Check if the result is zero
    if (resultSignificand.equals(BigInteger.ZERO)) {
      return signedZero(format, sign);
    }
    // Check if we need to round to infinity
    if (resultExponent > format.maxExp()) {
      return signedInfinity(format, sign);
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
    significand = truncateWithRoundingBits(significand, exponent);

    // Round the result
    significand = applyRoundingBits(RoundingMode.NEAREST_EVEN, sign, significand);

    // Shift the significand to the right if rounding has caused an overflow
    if (significand.bitLength() > pFormat.sigBits + 1) {
      significand = significand.shiftRight(1); // The dropped bit is zero
      exponent += 1;
    }

    // Return infinity if there is an overflow.
    if (exponent > pFormat.maxExp()) {
      return signedInfinity(pFormat, sign);
    }

    return new FloatValue(pFormat, sign, exponent, significand);
  }

  /**
   * Cast an <code>long</code>> value to a {@link FloatValue}
   *
   * <p>Will return +/- infinity if the value is too large for the chosen float type.
   */
  public static FloatValue fromInteger(Format pFormat, long pNumber) {
    return fromInteger(pFormat, BigInteger.valueOf(pNumber));
  }

  /**
   * Convert the value to a {@link BigInteger}
   *
   * <p>If the value is not already an integer the fractional part will be cut off. This is
   * equivalent to rounding with {@link RoundingMode#TRUNCATE RoundingMode.TRUNCATE}. If a different
   * rounding mode is desired {@link FloatValue#round} may be used first:
   *
   * <pre> myNumber.round(roundingMode).toInteger()</pre>
   *
   * <p>If the value is NaN or an infinity this method will return {@link Optional#empty()}.
   */
  public Optional<BigInteger> toInteger() {
    if (isNan() || isInfinite()) {
      return Optional.empty();
    } else if (exponent < 0) {
      // Return zero straight away if the exponent is negative
      return Optional.of(BigInteger.ZERO);
    } else {
      // Shift the significand to truncate the fractional part. For large exponents the expression
      // 'format.sigBits - exponent' will become negative, and the shift is to the left, adding
      // additional zeroes.
      BigInteger resultSignificand = significand.shiftRight((int) (format.sigBits - exponent));
      if (sign) {
        resultSignificand = resultSignificand.negate();
      }
      return Optional.of(resultSignificand);
    }
  }

  /**
   * Convert the value to an int
   *
   * <p>Returns {@link OptionalInt#empty()} if the value was NaN or Infinity, or if it was too large
   * to fit into an int.
   *
   * <p>This method will truncate the number if it is not already an integer. See {@link
   * FloatValue#toInteger} for more details.
   */
  public OptionalInt toInt() {
    Optional<BigInteger> maybeInteger = toInteger();
    if (maybeInteger.isPresent()) {
      BigInteger integerValue = maybeInteger.orElseThrow();
      return BigInteger.valueOf(integerValue.intValue()).equals(integerValue)
          ? OptionalInt.of(integerValue.intValue())
          : OptionalInt.empty();
    } else {
      return OptionalInt.empty();
    }
  }

  /**
   * Convert the value to a long
   *
   * <p>Returns {@link OptionalLong#empty()} if the value was NaN or Infinity, or if it was too
   * large to fit into a long.
   *
   * <p>This method will truncate the number if it is not already an integer. See {@link
   * FloatValue#toInteger} for more details.
   */
  public OptionalLong toLong() {
    Optional<BigInteger> maybeInteger = toInteger();
    if (maybeInteger.isPresent()) {
      BigInteger integerValue = maybeInteger.orElseThrow();
      return BigInteger.valueOf(integerValue.longValue()).equals(integerValue)
          ? OptionalLong.of(integerValue.longValue())
          : OptionalLong.empty();
    } else {
      return OptionalLong.empty();
    }
  }

  /**
   * Cast the value to a {@link BigInteger}
   *
   * <p>Returns 0 if the value is NaN and HUGE_VAL for infinite values. Here HUGE_VAL is guaranteed
   * to be larger than any integer that can be represented in the format. Converting it back to
   * {@link FloatValue} with the same format will again result in infinity.
   *
   * <pre>
   *  -Infinity    -> -HUGE_VAL
   *   NaN         -> 0
   *  +Infinity    -> HUGE_VAL</pre>
   *
   * <p>This method will truncate the number if it is not already an integer. See {@link
   * FloatValue#toInteger} for more details.
   */
  public BigInteger integerValue() {
    if (isNan()) {
      return BigInteger.ZERO;
    } else {
      return toInteger()
          .orElseGet(
              () -> {
                Format extendedPrecision = new Format(format.expBits, format.sigBits + 1);
                BigInteger infinity = maxValue(extendedPrecision).toInteger().orElseThrow();
                return isNegative() ? infinity.negate() : infinity;
              });
    }
  }

  /**
   * Cast the value to a byte
   *
   * <p>Returns 0 if the value is NaN, and behaves like a cast in Java if the value is infinite or
   * too large to fit into a byte:
   *
   * <pre>
   *  -Infinity    -> -1
   * < Integer.Min -> -1
   * < Byte.Min    -> (byte)((int) value)
   *   NaN         -> 0
   * > Byte.Max    -> (byte)((int) value)
   * > Integer.Max -> -1
   *  +Infinity    -> -1</pre>
   *
   * <p>This method will truncate the number if it is not already an integer. See {@link
   * FloatValue#toInteger} for more details.
   */
  @Override
  public byte byteValue() {
    return (byte) intValue();
  }

  /**
   * Cast the value to a short
   *
   * <p>Returns 0 if the value is NaN, and behaves like a cast in Java if the value is infinite or
   * too large to fit into a short:
   *
   * <pre>
   *  -Infinity    -> -1
   * < Integer.Min -> -1
   * < Short.Min   -> (short)((int) value)
   *   NaN         -> 0
   * > Short.Max   -> (short)((int) value)
   * > Integer.Max -> -1
   *  +Infinity    -> -1</pre>
   *
   * <p>This method will truncate the number if it is not already an integer. See {@link
   * FloatValue#toInteger} for more details.
   */
  @Override
  public short shortValue() {
    return (short) intValue();
  }

  /**
   * Cast the value to an int
   *
   * <p>Returns 0 if the value is NaN, and behaves like a cast in Java if the value is infinite or
   * too large to fit into an int:
   *
   * <pre>
   *  -Infinity    -> Integer.Min
   * < Integer.Min -> Integer.Min
   *   NaN         -> 0
   * > Integer.Max -> Integer.Max
   *  +Infinity    -> Integer.Max</pre>
   *
   * <p>This method will truncate the number if it is not already an integer. See {@link
   * FloatValue#toInteger} for more details.
   */
  @Override
  public int intValue() {
    if (isNan()) {
      return 0;
    } else {
      return toInt().orElse(isNegative() ? Integer.MIN_VALUE : Integer.MAX_VALUE);
    }
  }

  /**
   * Cast the value to a long
   *
   * <p>Returns 0 if the value is NaN, and behaves like a cast in Java if the value is infinite or
   * too large to fit into a long:
   *
   * <pre>
   *  -Infinity -> Long.Min
   * < Long.Min -> Long.Min
   *   NaN      -> 0
   * > Long.Max -> Long.Max
   *  +Infinity -> Long.Max</pre>
   *
   * <p>This method will truncate the number if it is not already an integer. See {@link
   * FloatValue#toInteger} for more details.
   */
  @Override
  public long longValue() {
    if (isNan()) {
      return 0;
    } else {
      return toLong().orElse(isNegative() ? Long.MIN_VALUE : Long.MAX_VALUE);
    }
  }

  /** Convert a {@link FloatingPointNumber} to {@link FloatValue} */
  public static FloatValue fromFloatingPointNumber(FloatingPointNumber pNumber) {
    Format format = new Format(pNumber.getExponentSize(), pNumber.getMantissaSize());
    long exponent = pNumber.getExponent().longValue() - format.bias();
    BigInteger significand = pNumber.getMantissa();
    if (exponent >= format.minExp() && exponent <= format.maxExp()) {
      // If the number is "normal" (= not zero, subnormal, inf or nan), add the hidden bit to the
      // significand
      significand = significand.setBit(format.sigBits);
    }
    return new FloatValue(format, pNumber.getSign(), exponent, significand);
  }

  /** Convert this {@link FloatValue} to {@link FloatingPointNumber} */
  public FloatingPointNumber toFloatingPointNumber() {
    return FloatingPointNumber.of(
        sign,
        BigInteger.valueOf(exponent + format.bias()),
        significand.clearBit(format.sigBits()),
        format.expBits(),
        format.sigBits());
  }

  /** Convert a <code>float</code> to {@link FloatValue} */
  public static FloatValue fromFloat(float pFloat) {
    int rawBits = Float.floatToRawIntBits(pFloat);

    // Get the sign
    boolean sign = (rawBits & Format.FLOAT32_SIGN_MASK) != 0;

    if (Float.isNaN(pFloat)) {
      return sign ? nan(Format.Float32).negate() : nan(Format.Float32);
    } else if (Float.isInfinite(pFloat)) {
      return signedInfinity(Format.Float32, sign);
    } else {
      // Get the exponent and the significand
      long expBits = (rawBits & Format.FLOAT32_EXPONENT_MASK) >> Format.FLOAT32_SIG_BITS;
      long sigBits = rawBits & Format.FLOAT32_SIGNIFICAND_MASK;

      // Remove the bias from the exponent
      expBits = expBits - (int) Format.Float32.bias();

      if (expBits >= Format.Float32.minExp() && expBits <= Format.Float32.maxExp()) {
        // If the number is "normal" (= not zero, subnormal, inf or nan), add the hidden bit to the
        // significand
        sigBits |= 1 << Format.FLOAT32_SIG_BITS;
      }

      return new FloatValue(Format.Float32, sign, expBits, BigInteger.valueOf(sigBits));
    }
  }

  @Override
  public float floatValue() {
    if (!format.equals(Format.Float32)) {
      // Convert to the right precision first
      return withPrecision(Format.Float32).floatValue();
    }

    // Set the sign
    int signBit = sign ? Format.FLOAT32_SIGN_MASK : 0;
    // Add the bias to the exponent
    int expBits = (int) (exponent + Format.Float32.bias()) << Format.FLOAT32_SIG_BITS;
    // Remove the hidden bit from the significand
    int sigBits = significand.intValue() & Format.FLOAT32_SIGNIFICAND_MASK;

    return Float.intBitsToFloat(signBit | expBits | sigBits);
  }

  /** Convert a <code>double</code> to {@link FloatValue#byteValue()} */
  public static FloatValue fromDouble(double pDouble) {
    long rawBits = Double.doubleToRawLongBits(pDouble);

    // Get the sign
    boolean sign = (rawBits & Format.FLOAT64_SIGN_MASK) != 0;

    if (Double.isNaN(pDouble)) {
      return sign ? nan(Format.Float64).negate() : nan(Format.Float64);
    } else if (Double.isInfinite(pDouble)) {
      return signedInfinity(Format.Float64, sign);
    } else {
      // Get the exponent and the significand
      long expBits = (rawBits & Format.FLOAT64_EXPONENT_MASK) >> Format.FLOAT64_SIG_BITS;
      long sigBits = rawBits & Format.FLOAT64_SIGNIFICAND_MASK;

      // Remove the bias from the exponent
      expBits = expBits - Format.Float64.bias();

      if (expBits >= Format.Float64.minExp() && expBits <= Format.Float64.maxExp()) {
        // If the number is "normal" (= not zero, subnormal, inf or nan), add the hidden bit to the
        // significand
        sigBits |= 1L << Format.FLOAT64_SIG_BITS;
      }

      return new FloatValue(Format.Float64, sign, expBits, BigInteger.valueOf(sigBits));
    }
  }

  @Override
  public double doubleValue() {
    if (!format.equals(Format.Float64)) {
      // Convert to the right precision first
      return withPrecision(Format.Float64).doubleValue();
    }

    // Set the sign
    long signBit = sign ? Format.FLOAT64_SIGN_MASK : 0;
    // Add the bias to the exponent
    long expBits = (exponent + Format.Float64.bias()) << Format.FLOAT64_SIG_BITS;
    // Remove the hidden bit from the significand
    long sigBits = significand.longValue() & Format.FLOAT64_SIGNIFICAND_MASK;

    return Double.longBitsToDouble(signBit | expBits | sigBits);
  }

  /**
   * Cast an {@link Rational} value to a {@link FloatValue}
   *
   * <p>Will return +/- infinity if the rational value is too large for the chosen float type. Very
   * small numbers may flush to zero, depending on the chosen precision. This method is generally
   * imprecise as most rational values can not be represented by a finite number of (binary) digits
   * and will need to be rounded down to the target precision.
   */
  public static FloatValue fromRational(Format pFormat, Rational pRational) {
    // FIXME This will likely suffer from rounding issues. Maybe we can use something similar to
    //   fromDecimal() to handle the conversion?
    FloatValue num = FloatValue.fromInteger(pFormat, pRational.getNum());
    FloatValue den = FloatValue.fromInteger(pFormat, pRational.getDen());
    return num.divide(den);
  }

  /**
   * Convert this {@link FloatValue} to {@link Rational}
   *
   * <p>If the value is NaN or an infinity this method will return {@link Optional#empty()}.
   */
  public Optional<Rational> toRational() {
    if (isInfinite() || isNan()) {
      return Optional.empty();
    } else {
      // Shift the exponent to turn the significand into an integer value
      FloatValue unlimited = withPrecision(format.withUnlimitedExponent());
      int shiftedExponent = (int) unlimited.exponent - format.sigBits;

      // Construct the fraction
      Rational rationalValue;
      if (shiftedExponent >= 0) {
        // If the exponent is >=0 we return a*2^k/1
        rationalValue = Rational.of(significand.shiftLeft(shiftedExponent), BigInteger.ONE);
      } else {
        // Otherwise, the fraction is a/2^k
        rationalValue = Rational.of(significand, BigInteger.ONE.shiftLeft(-shiftedExponent));
      }
      return Optional.of(rationalValue);
    }
  }

  /**
   * Create a floating point value from its hexadecimal string representation.
   *
   * @param pFormat target precision
   * @param pSign `true` if there is a '-' sign in front of the number
   * @param pDigits digits of the number (with the period shifted all the way to the right)
   * @param pExpValue exponent of the number, corrected for the period shift
   */
  private static FloatValue fromHexadecimal(
      Format pFormat, boolean pSign, String pDigits, int pExpValue) {
    /*
     * Converting from base 16 to base 2 is a special case as the two radixes are "commensurable",
     * that is, they are both powers of another integer. In this case this integer is simply 2 and
     * the two bases can then be expressed as 2^1 and 2^4. Intuitively this means that the
     * conversion can be done one digit at a time as each hexadecimal digit can simply be expanded
     * into 4 bits to create the base2 representation.
     *
     * See "How to Read Floating Point Numbers Accurately" for details:
     *   https://dl.acm.org/doi/pdf/10.1145/93542.93557"
     */
    FloatValue r = fromInteger(pFormat, new BigInteger(pDigits, 16));
    int finalExp = pExpValue - 4 * (pDigits.length() - 1);
    r = r.withExponent(r.exponent + finalExp);
    return pSign ? r.negate() : r;
  }

  /**
   * Create a floating point value from its decimal string representation.
   *
   * @param pFormat target precision
   * @param pSign `true` if there is a '-' sign in front of the number
   * @param pDigits digits of the number (with the period shifted all the way to the right)
   * @param pExpValue exponent of the number, corrected for the period shift
   */
  private static FloatValue fromDecimal(
      Format pFormat, boolean pSign, String pDigits, int pExpValue) {
    /*
     * We use `AlgorithmM` from "How to read floating point numbers accurately" to ensure correct
     * rounding. See https://dl.acm.org/doi/pdf/10.1145/93548.93557 for the details.
     */
    int fixedExponent = pExpValue - (pDigits.length() - 1);
    BigInteger significand10 = new BigInteger(pDigits);
    BigInteger exponent10 = BigInteger.TEN.pow(Math.abs(fixedExponent));

    BigInteger numerator = fixedExponent > 0 ? significand10.multiply(exponent10) : significand10;
    BigInteger divisor = fixedExponent > 0 ? BigInteger.ONE : exponent10;

    // Convert from decimal to binary floating point representation
    int numeratorSize = numerator.bitLength();
    int divisorSize = divisor.bitLength();

    // Consider the bit length of the numerator and the denominator and pull out 2^k
    int exponent = -divisorSize + (numeratorSize - pFormat.sigBits);
    if (exponent < 0) {
      numerator = numerator.shiftLeft(Math.abs(exponent));
    } else {
      divisor = divisor.shiftLeft(exponent);
    }

    // Fine tune for the actual value. If the quotient is already between 1.0 and 2.0 we're done.
    // Otherwise, multiply by 2 once more to fix the exponent.
    BigInteger significand = numerator.divide(divisor);
    if (significand.bitLength() < pFormat.sigBits + 1) {
      numerator = numerator.shiftLeft(1);
      exponent--;
    }

    // Build the final value
    Format precision = pFormat.intermediatePrecision(); // TODO Check that this is enough
    FloatValue floatNumerator = fromInteger(precision, numerator);
    FloatValue floatDivisor = fromInteger(precision, divisor);

    FloatValue quotient = floatNumerator.divide(floatDivisor);
    FloatValue rounded = quotient.withExponent(quotient.exponent + exponent).withPrecision(pFormat);

    return pSign ? rounded.negate() : rounded;
  }

  /**
   * Parse input string as a floating point number
   *
   * <p>Valid input strings must match the following grammar:
   *
   * <pre>
   * signed-floating-constant ::=
   *     [`-`]`nan`
   *   | [`-`]`inf`
   *   | [sign] floating-constant
   *
   * floating-constant ::=
   *     decimal-floating-constant
   *   | hexadecimal-floating-constant
   *
   * decimal-floating-constant ::=
   *     fractional-constant [exponent-part]
   *   | digit+ exponent-part
   *
   * hexadecimal-floating-constant ::=
   *     hexadecimal-prefix hexadecimal-fractional-constant binary-exponent-part
   *   | hexadecimal-prefix hexadecimal-digit+ binary-exponent-part
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
   * digit ::= `0` ... `9`
   *
   * hexadecimal-prefix ::= `0x` | `0X`
   * hexadecimal-digit ::= digit | `A` ... `F` | `a` ... `f`
   * </pre>
   */
  public static FloatValue fromString(Format pFormat, String pInput) {
    Preconditions.checkArgument(!pInput.isEmpty());

    if (pInput.equals("inf")) {
      return infinity(pFormat);
    } else if (pInput.equals("-inf")) {
      return negativeInfinity(pFormat);
    } else if (pInput.equals("nan")) {
      return nan(pFormat);
    } else if (pInput.equals("-nan")) {
      return nan(pFormat).negate();
    }
    pInput = Ascii.toLowerCase(pInput);

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

    // Abort if we have a hexadecimal float literal with no exponent part
    int sep = isHexLiteral ? pInput.indexOf('p') : pInput.indexOf('e');
    Preconditions.checkArgument(
        sep != -1 || !isHexLiteral,
        "Hexadecimal floating point numbers need to have an exponent part");

    // Split off the exponent part (if there is one)
    String digits = sep > -1 ? pInput.substring(0, sep) : pInput;
    String exponent = sep > -1 ? pInput.substring(sep + 1) : "0";

    // Check that the exponent is a valid number and get its value
    Preconditions.checkArgument(exponent.matches("[+-]?\\d+"));
    int expValue = Integer.parseInt(exponent);

    // Abort if the significand has no digits
    Preconditions.checkArgument(
        !digits.isEmpty() && !digits.equals("."),
        "There needs to be at least one digit for the decimal part");

    // Get the fractional part of the number (and add ".0" if it has none)
    int radix = digits.indexOf('.');
    if (radix == -1) {
      Preconditions.checkArgument(sep > -1);
      radix = digits.length();
      digits = digits + ".0";
    }

    // Normalize the mantissa, then fix the exponent
    expValue = (isHexLiteral ? 4 : 1) * (radix - 1) + expValue;
    digits = digits.substring(0, radix) + digits.substring(radix + 1);

    // Return zero if the significand is all zeroes
    if (digits.matches("0+")) {
      return signedZero(pFormat, sign);
    }

    // Convert the value to a binary float representation
    if (isHexLiteral) {
      Preconditions.checkArgument(
          !digits.isEmpty()
              && CharMatcher.inRange('0', '9')
                  .or(CharMatcher.inRange('a', 'f'))
                  .matchesAllOf(digits));
      return fromHexadecimal(pFormat, sign, digits, expValue);
    } else {
      Preconditions.checkArgument(
          !digits.isEmpty() && CharMatcher.inRange('0', '9').matchesAllOf(digits));
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
   *
   * <p>The special values <code>NaN</code> and <code>Infinity</code> will be printed as <code>"nan"
   * </code> and <code>"inf"</code>. Note that negative NaN values will be printed simply as <code>
   * "-nan"</code>.
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
      return "%s%s.%se%d"
          .formatted(
              sign ? "-" : "",
              bits.charAt(0),
              bits.substring(1),
              Math.max(format.minExp(), exponent));
    }
  }

  /**
   * Returns the exponent of this value
   *
   * <p>The exponent is not stored with the IEEE bias. See {@link #exponent} and {@link
   * Format#bias()}.
   */
  long getExponentWithoutBias() {
    return exponent;
  }

  /**
   * Returns the significand of this value
   *
   * <p>For values that are not zero, subnormal, infinity or NaN, the significand includes an
   * additional hidden bit in its first place. See {@link #significand} for more information about
   * the "hidden" bit.
   */
  BigInteger getSignificandWithoutHiddenBit() {
    return significand;
  }
}
