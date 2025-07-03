// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.floatingpoint;

import java.util.Optional;
import org.sosy_lab.cpachecker.util.floatingpoint.CFloatNativeAPI.CFloatType;
import org.sosy_lab.cpachecker.util.floatingpoint.CFloatNativeAPI.CIntegerType;

/**
 * This abstract class is used to implement classes which provide floating point arithmetic
 * according to close to hardware level C compilations.
 */
abstract class CFloat implements Comparable<CFloat> {

  /**
   * Add another {@link CFloat} to <code>this</code> and return the resulting {@link CFloat}.
   *
   * <p>It is recommended to return a fresh {@link CFloat} object to adhere to the general
   * mathematical idea of immutability of numbers.
   *
   * @param pSummand the other {@link CFloat} instance to add
   * @return the resulting {@link CFloat}
   */
  public abstract CFloat add(final CFloat pSummand);

  /**
   * Multiply <code>this</code> with another {@link CFloat} object and return the resulting {@link
   * CFloat} instance.
   *
   * <p>It is recommended to return a fresh {@link CFloat} object to adhere to the general
   * mathematical idea of immutability of numbers.
   *
   * @param pFactor the other {@link CFloat} instance to multiply
   * @return the resulting {@link CFloat}
   */
  public abstract CFloat multiply(final CFloat pFactor);

  /**
   * Subtract another {@link CFloat} object from <code>this</code> and return the resulting {@link
   * CFloat} instance.
   *
   * <p>It is recommended to return a fresh {@link CFloat} object to adhere to the general
   * mathematical idea of immutability of numbers.
   *
   * @param pSubtrahend the other {@link CFloat} instance to subtract
   * @return the resulting {@link CFloat}
   */
  public abstract CFloat subtract(final CFloat pSubtrahend);

  /**
   * Divide <code>this</code> by another {@link CFloat} object and return the resulting {@link
   * CFloat} instance.
   *
   * <p>It is recommended to return a fresh {@link CFloat} object to adhere to the general
   * mathematical idea of immutability of numbers.
   *
   * @param pDivisor the other {@link CFloat} instance to divide by
   * @return the resulting {@link CFloat}
   */
  public abstract CFloat divideBy(final CFloat pDivisor);

  public abstract CFloat modulo(final CFloat pDivisor);

  public abstract CFloat remainder(final CFloat pDivisor);

  /** The natural logorithm ln(x) */
  public abstract CFloat ln();

  /** The exponential function e^x */
  public abstract CFloat exp();

  /**
   * Compute the power of <code>this</code> to another {@link CFloat} instance.
   *
   * <p>It is recommended to return a fresh {@link CFloat} object to adhere to the general
   * mathematical idea of immutability of numbers.
   *
   * @param exponent the other {@link CFloat} instance to compute the power to
   * @return the resulting {@link CFloat} object with the value of <code>this</code> to the power of
   *     <code>exponent</code>
   */
  public abstract CFloat powTo(final CFloat exponent);

  /**
   * /** Compute the power of <code>this</code> to an integral exponent.
   *
   * <p>It is recommended to return a fresh {@link CFloat} object to adhere to the general
   * mathematical idea of immutability of numbers.
   *
   * @param exponent integral number to compute the power to
   * @return the resulting {@link CFloat} object with the value of <code>this</code> to the power of
   *     <code>exponent</code>
   */
  public abstract CFloat powToIntegral(final int exponent);

  /**
   * Compute the square root of <code>this</code>.
   *
   * <p>Note that in general only the square root of a positive number is computable.
   *
   * <p>Usually the result should be positive but that might differ according to implementations.
   *
   * @return a {@link CFloat} instance representing the value of the square root of <code>this
   *     </code>
   */
  public abstract CFloat sqrt();

  /**
   * Return a {@link CFloat} object representing the value of <code>this</code> rounded to the
   * closest integral number. If two integral numbers are equally close to <code>this</code>, {@link
   * CFloat#round} returns the one further from zero.
   *
   * @return a {@link CFloat} object representing the value of <code>this</code> rounded to the
   *     closest integral number
   */
  public abstract CFloat round();

  /**
   * Return a {@link CFloat} object representing the value of <code>this</code> rounded to the
   * closest integral number, that is not greater (in absolute value) than <code>this</code>.
   *
   * <p>Effectively the fractional part of <code>this</code> is dropped, truncated respectively.
   *
   * @return a {@link CFloat} object representing the value of <code>this</code> rounded to the
   *     closest integral value towards zero
   */
  public abstract CFloat trunc();

  /**
   * Return a {@link CFloat} object representing the value of <code>this</code> rounded to the
   * closest integral value towards positive infinity.
   *
   * @return a {@link CFloat} object representing the value of <code>this</code> rounded to the
   *     closest integral value towards positive infinity
   */
  public abstract CFloat ceil();

  /**
   * Return a {@link CFloat} object representing the value of <code>this</code> rounded to the
   * closest integral value towards negative infinity.
   *
   * @return a {@link CFloat} object representing the value of <code>this</code> rounded to the
   *     closest integral value towards negative infinity
   */
  public abstract CFloat floor();

  /**
   * Return the absolute value of <code>this</code>, so in general just drop the sign-bit if it is
   * set.
   *
   * @return a {@link CFloat} representing the absolute value of <code>this</code>
   */
  public abstract CFloat abs();

  /**
   * Determine whether <code>this</code> has an absolute value of 0.
   *
   * <p>In general it should not matter for this method whether the sign-bit is set. Implementations
   * which differ from that interpretation should clearly state so.
   *
   * @return whether <code>this</code> has an absolute value of 0 or not
   */
  public abstract boolean isZero();

  /**
   * Determine whether <code>this</code> has an absolute value of 1.
   *
   * <p>In general it should not matter for this method whether the sign-bit is set. Implementations
   * which differ from that interpretation should clearly state so.
   *
   * @return whether <code>this</code> has an absolute value of 1 or not
   */
  public abstract boolean isOne();

  /**
   * Determine whether <code>this</code> is NaN or not.
   *
   * @return whether <code>this</code> is NaN or not
   */
  public abstract boolean isNan();

  /**
   * Determine whether <code>this</code> has an infinite value or not.
   *
   * @return whether <code>this</code> has an infinite value or not
   */
  public abstract boolean isInfinity();

  /**
   * Determine whether the sign-bit is set.
   *
   * @return whether the sign-bit is set
   */
  public abstract boolean isNegative();

  /**
   * Copy the sign-bit of another {@link CFloat} to <code>this</code> and return the resulting
   * {@link CFloat}.
   *
   * <p>It is recommended for implementations to copy the sign-bit into a fresh {@link CFloat}
   * instance and to return this new object while letting <code>this</code> untouched, to adhere to
   * the general mathematical idea of immutability of numbers.
   *
   * @param source the other {@link CFloat} instance
   * @return a {@link CFloat} instance with the absolute value of <code>this</code> and the sign of
   *     <code>source</code>
   */
  public abstract CFloat copySignFrom(final CFloat source);

  /**
   * Try to cast <code>this</code> to some floating point number type.
   *
   * <p>Note, however, that due to precision changes the value of the object resulting from the cast
   * is not necessarily the same as the value of the original object.
   *
   * @param toType the target floating point number type
   * @return a new {@link CFloat} instance with the type <code>toType</code> and (approximately) the
   *     value of <code>this</code>
   */
  public abstract CFloat castTo(final CFloatType toType);

  /**
   * Try to cast <code>this</code> to another number type, more precisely some implementation of the
   * {@link Number} interface.
   *
   * @param toType the target number type
   * @return a new {@link Number} instance with (approximately) the value of <code>this</code>
   */
  public abstract Optional<Number> castToOther(final CIntegerType toType);

  /**
   * Somehow create a {@link CFloatWrapper} instance holding an exponent and mantissa representing
   * the value <code>this</code> (in case of NaN, -NaN, subnormal numbers, and zeroes there is not
   * necessarily a unique representation).
   *
   * <p>Implementations should take care to create a real copy as to prevent accidental changes to
   * the value of <code>this</code> via the returned {@link CFloatWrapper}.
   *
   * @return a {@link CFloatWrapper} instance containing a bit representation of the value of <code>
   *     this</code>
   */
  public abstract CFloatWrapper copyWrapper();

  /**
   * The sole meaning of this method is to be able to get to the exponent and mantissa of an
   * instance of {@link CFloat} without creating discard-objects via {@link CFloat#copyWrapper()}.
   *
   * @return the {@link CFloatWrapper} member of this instance
   */
  protected abstract CFloatWrapper getWrapper();

  /**
   * Return the API representation of the floating point number type of <code>this</code>.
   *
   * @return the type of <code>this</code>
   */
  public abstract CFloatType getType();

  public abstract boolean equalTo(final CFloat other);

  public abstract boolean lessOrGreater(final CFloat other);

  /**
   * Compare <code>this</code> with another {@link CFloat} object and return whether <code>this
   * </code> is greater (according to floating point semantics) than the other {@link CFloat}
   * instance.
   *
   * <p>Note that usually any such comparison involving <code>NaN</code> objects results in <code>
   * false</code>
   *
   * @param other the other {@link CFloat} instance to compare <code>this</code> to
   * @return whether <code>this</code> is greater than <code>other</code>
   */
  public abstract boolean greaterThan(final CFloat other);

  public abstract boolean greaterOrEqual(final CFloat other);

  public abstract boolean lessThan(final CFloat other);

  public abstract boolean lessOrEqual(final CFloat other);

  /**
   * Compare two floating point values
   *
   * <p>Uses the total order predicate from the 754-2008 IEEE standard (§5.10) for the comparison:
   *
   * <pre>
   * -Nan < -Inf < ... < -0 < +0 < .. < +Inf < +Nan</pre>
   */
  @Override
  public abstract int compareTo(final CFloat other);

  private static final int FLOAT32_EXP_BITS = 8;
  private static final int FLOAT32_SIG_BITS = 23;

  private static final int FLOAT64_EXP_BITS = 11;
  private static final int FLOAT64_SIG_BITS = 52;

  private static final int FLOAT_EXTENDED_EXP_BITS = 15;

  public final long getSignBitMask() {
    return switch (getType()) {
      case SINGLE -> 1L << FLOAT32_EXP_BITS;
      case DOUBLE -> 1L << FLOAT64_EXP_BITS;
      case LONG_DOUBLE -> 1L << FLOAT_EXTENDED_EXP_BITS;
      default -> throw new RuntimeException("Unimplemented floating point type: " + getType());
    };
  }

  public final long getExponentMask() {
    return getSignBitMask() - 1;
  }

  public final long getMantissaMask() {
    return switch (getType()) {
      case SINGLE -> (1L << FLOAT32_SIG_BITS) - 1;
      case DOUBLE -> (1L << FLOAT64_SIG_BITS) - 1;
      case LONG_DOUBLE ->
          0b01111111_11111111_11111111_11111111_11111111_11111111_11111111_11111111L;
      default -> throw new RuntimeException("Unimplemented floating point type: " + getType());
    };
  }

  public final long getNormalizedMantissaMask() {
    return switch (getType()) {
      case SINGLE, DOUBLE -> getMantissaMask();
      case LONG_DOUBLE ->
          // We need to inlcude the leading bit as "Extended precision" stores the hidden bit there
          0b11111111_11111111_11111111_11111111_11111111_11111111_11111111_11111111L;
      default -> throw new RuntimeException("Unimplemented floating point type: " + getType());
    };
  }
}
