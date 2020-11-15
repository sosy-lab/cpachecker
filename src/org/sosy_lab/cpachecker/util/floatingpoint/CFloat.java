// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.floatingpoint;

/**
 * This abstract class is used to implement classes which provide floating point arithmetic
 * according to close to hardware level C compilations.
 */
public abstract class CFloat {

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
   * Add multiple {@link CFloat} objects to <code>this</code> and return the resulting {@link
   * CFloat}.
   *
   * <p>The performance of this method is highly dependent on if the implementation just loops
   * {@link CFloat#add(CFloat)} or uses some sort of optimized procedure. However, it should be
   * taken into consideration, that C compilations in general do not guarantee a commutative
   * addition of multiple floating point numbers, due to over-/underflows introduced by large
   * differences in the exponent.
   *
   * @param pSummands the other {@link CFloat} instances to add
   * @return the resulting {@link CFloat}
   */
  public abstract CFloat add(final CFloat... pSummands);

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
   * Multiply <code>this</code> with multiple {@link CFloat} objects and return the resulting {@link
   * CFloat}.
   *
   * <p>The performance of this method is highly dependent on if the implementation just loops
   * {@link CFloat#multiply(CFloat)} or uses some sort of optimized procedure. However, it should be
   * taken into consideration, that C compilations in general do not guarantee a commutative
   * multiplication of multiple floating point numbers, due to over-/underflows introduced by large
   * differences in the exponent.
   *
   * @param pFactors the other {@link CFloat} instances to multiply
   * @return the resulting {@link CFloat}
   */
  public abstract CFloat multiply(CFloat... pFactors);

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
  public boolean isNan() {
    return false;
  }

  /**
   * Determine whether <code>this</code> has an infinite value or not.
   *
   * @return whether <code>this</code> has an infinite value or not
   */
  public boolean isInfinity() {
    return false;
  }

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
  public abstract CFloat castTo(final int toType);

  /**
   * Try to cast <code>this</code> to another number type, more precisely some implementation of the
   * {@link Number} interface.
   *
   * @param toType the target number type
   * @return a new {@link Number} instance with (approximately) the value of <code>this</code>
   */
  public abstract Number castToOther(final int toType);

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
  public abstract int getType();

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

  public final long getExponent() {
    return getWrapper().getExponent();
  }

  public final long getMantissa() {
    return getWrapper().getMantissa();
  }

  public final int getNormalizedMantissaLength() {
    int length = 0;

    switch (getType()) {
      case CFloatNativeAPI.FP_TYPE_SINGLE:
        length = 24;
        break;
      case CFloatNativeAPI.FP_TYPE_DOUBLE:
        length = 53;
        break;
      case CFloatNativeAPI.FP_TYPE_LONG_DOUBLE:
        length = 64;
        break;
      default:
        throw new RuntimeException("Unimplemented floating point type: " + getType());
    }

    return length;
  }

  public final long getOverflowHighBitsMask() {
    long bits = 0L;
    switch (getType()) {
      case CFloatNativeAPI.FP_TYPE_SINGLE:
        bits = 0b11111111_11111111_11111110_00000000_00000000_00000000_00000000_00000000L;
        break;
      case CFloatNativeAPI.FP_TYPE_DOUBLE:
        bits = 0b11111111_11111111_11111111_11111111_11111111_11111111_11110000_00000000L;
        break;
      case CFloatNativeAPI.FP_TYPE_LONG_DOUBLE:
        bits = 0b11111111_11111111_11111111_11111111_11111111_11111111_11111111_11111111L;
        break;
      default:
        throw new RuntimeException("Unimplemented floating point type: " + getType());
    }

    return bits;
  }

  public final int getExponentLength() {
    int res = -1;

    switch (getType()) {
      case CFloatNativeAPI.FP_TYPE_SINGLE:
        res = 8;
        break;
      case CFloatNativeAPI.FP_TYPE_DOUBLE:
        res = 11;
        break;
      case CFloatNativeAPI.FP_TYPE_LONG_DOUBLE:
        res = 15;
        break;
      default:
        throw new IllegalArgumentException("Unimplemented floating point type: " + getType());
    }

    return res;
  }

  public final long getBias() {
    long bias = 0L;
    switch (getType()) {
      case CFloatNativeAPI.FP_TYPE_SINGLE:
      case CFloatNativeAPI.FP_TYPE_DOUBLE:
      case CFloatNativeAPI.FP_TYPE_LONG_DOUBLE:
        bias = getExponentMask() / 2;
        break;
      default:
        throw new RuntimeException("Unimplemented floating point type: " + getType());
    }

    return bias;
  }

  public final int getMantissaLength() {
    int res = -1;

    switch (getType()) {
      case CFloatNativeAPI.FP_TYPE_SINGLE:
        res = 23;
        break;
      case CFloatNativeAPI.FP_TYPE_DOUBLE:
        res = 52;
        break;
      case CFloatNativeAPI.FP_TYPE_LONG_DOUBLE:
        res = 64;
        break;
      default:
        throw new IllegalArgumentException("Unimplemented floating point type: " + getType());
    }

    return res;
  }

  public final long getSignBitMask() {
    long signBit = 0L;
    switch (getType()) {
      case CFloatNativeAPI.FP_TYPE_SINGLE:
        signBit = 0b00000000_00000000_00000000_00000000_00000000_00000000_00000001_00000000L;
        break;
      case CFloatNativeAPI.FP_TYPE_DOUBLE:
        signBit = 0b00000000_00000000_00000000_00000000_00000000_00000000_00001000_00000000L;
        break;
      case CFloatNativeAPI.FP_TYPE_LONG_DOUBLE:
        signBit = 0b00000000_00000000_00000000_00000000_00000000_00000000_10000000_00000000L;
        break;
      default:
        throw new RuntimeException("Unimplemented floating point type: " + getType());
    }

    return signBit;
  }

  public final long getExponentMask() {
    long exp = 0L;
    switch (getType()) {
      case CFloatNativeAPI.FP_TYPE_SINGLE:
        exp = 0b00000000_00000000_00000000_00000000_00000000_00000000_00000000_11111111L;
        break;
      case CFloatNativeAPI.FP_TYPE_DOUBLE:
        exp = 0b00000000_00000000_00000000_00000000_00000000_00000000_00000111_11111111L;
        break;
      case CFloatNativeAPI.FP_TYPE_LONG_DOUBLE:
        exp = 0b00000000_00000000_00000000_00000000_00000000_00000000_01111111_11111111L;
        break;
      default:
        throw new RuntimeException("Unimplemented floating point type: " + getType());
    }

    return exp;
  }

  public final CFloatWrapper round(CFloatWrapper pWrapper, long pOverflow) {
    // TODO: currently only rounding mode NEAREST_TIE_TO_EVEN; implement others
    CFloatWrapper rWrapper = pWrapper.copy();
    if (pOverflow != 0) {
      long man = rWrapper.getMantissa();
      long exp = rWrapper.getExponent();

      boolean isNormalized =
          (getType() == CFloatNativeAPI.FP_TYPE_LONG_DOUBLE && (man & getNormalizationMask()) != 0);

      if ((getHighestOrderOverflowBitMask() & pOverflow) != 0) {
        if (((getLowerOrderOverflowBitsMask() & pOverflow) != 0) || ((1 & man) != 0)) {
          long nMan = (man + 1) & getNormalizedMantissaMask();

          if ((getType() != CFloatNativeAPI.FP_TYPE_LONG_DOUBLE
                  && (nMan & getNormalizationMask()) != 0)
              || (getType() == CFloatNativeAPI.FP_TYPE_LONG_DOUBLE
                  && (nMan & getNormalizationMask()) == 0
                  && isNormalized)) {
            nMan >>>= 1;
            nMan ^= getNormalizationMask();
            nMan &= getNormalizedMantissaMask();
            exp--;
          }

          rWrapper.setExponent(exp);
          rWrapper.setMantissa(nMan);
        }
      }
    }

    return rWrapper;
  }

  public final long getLowerOrderOverflowBitsMask() {
    long bits = 0L;
    switch (getType()) {
      case CFloatNativeAPI.FP_TYPE_SINGLE:
        bits = 0b01111111_11111111_11111110_00000000_00000000_00000000_00000000_00000000L;
        break;
      case CFloatNativeAPI.FP_TYPE_DOUBLE:
        bits = 0b01111111_11111111_11111111_11111111_11111111_11111111_11110000_00000000L;
        break;
      case CFloatNativeAPI.FP_TYPE_LONG_DOUBLE:
        bits = 0b01111111_11111111_11111111_11111111_11111111_11111111_11111111_11111111L;
        break;
      default:
        throw new RuntimeException("Unimplemented floating point type: " + getType());
    }

    return bits;
  }

  public final long getHighestOrderOverflowBitMask() {
    return 0b10000000_00000000_00000000_00000000_00000000_00000000_00000000_00000000L;
  }

  public final long getMantissaMask() {
    long man = 0L;
    switch (getType()) {
      case CFloatNativeAPI.FP_TYPE_SINGLE:
        man = 0b00000000_00000000_00000000_00000000_00000000_01111111_11111111_11111111L;
        break;
      case CFloatNativeAPI.FP_TYPE_DOUBLE:
        man = 0b00000000_00001111_11111111_11111111_11111111_11111111_11111111_11111111L;
        break;
      case CFloatNativeAPI.FP_TYPE_LONG_DOUBLE:
        man = 0b01111111_11111111_11111111_11111111_11111111_11111111_11111111_11111111L;
        break;
      default:
        throw new RuntimeException("Unimplemented floating point type: " + getType());
    }

    return man;
  }

  public final long getNormalizationMask() {
    long oneBit = 0L;
    switch (getType()) {
      case CFloatNativeAPI.FP_TYPE_SINGLE:
        oneBit = 0b00000000_00000000_00000000_00000000_00000000_10000000_00000000_00000000L;
        break;
      case CFloatNativeAPI.FP_TYPE_DOUBLE:
        oneBit = 0b00000000_00010000_00000000_00000000_00000000_00000000_00000000_00000000L;
        break;
      case CFloatNativeAPI.FP_TYPE_LONG_DOUBLE:
        oneBit = 0b10000000_00000000_00000000_00000000_00000000_00000000_00000000_00000000L;
        break;
      default:
        throw new RuntimeException("Unimplemented floating point type: " + getType());
    }

    return oneBit;
  }

  public final long getNormalizedMantissaMask() {
    long man = 0L;
    switch (getType()) {
      case CFloatNativeAPI.FP_TYPE_SINGLE:
      case CFloatNativeAPI.FP_TYPE_DOUBLE:
        return getMantissaMask();
      case CFloatNativeAPI.FP_TYPE_LONG_DOUBLE:
        man = 0b11111111_11111111_11111111_11111111_11111111_11111111_11111111_11111111L;
        break;
      default:
        throw new RuntimeException("Unimplemented floating point type: " + getType());
    }

    return man;
  }
}
