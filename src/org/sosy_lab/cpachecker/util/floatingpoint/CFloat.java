/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.util.floatingpoint;

/**
 * This interface is used to implement classes which provide floating point arithmetic according to
 * close to hardware level C compilations.
 */
public interface CFloat {

  /**
   * Add another {@link CFloat} to <code>this</code> and return the resulting {@link CFloat}.
   * <p>
   * It is recommended to return a fresh {@link CFloat} object to adhere to the general mathematical
   * idea of immutability of numbers.
   *
   * @param pSummand the other {@link CFloat} instance to add
   * @return the resulting {@link CFloat}
   */
  CFloat add(CFloat pSummand);

  /**
   * Add multiple {@link CFloat} objects to <code>this</code> and return the resulting
   * {@link CFloat}.
   * <p>
   * The performance of this method is highly dependent on if the implementation just loops
   * {@link CFloat#add(CFloat)} or uses some sort of optimized procedure. However, it should be
   * taken into consideration, that C compilations in general do not guarantee a commutative
   * addition of multiple floating point numbers, due to over-/underflows introduced by large
   * differences in the exponent.
   *
   * @param pSummands the other {@link CFloat} instances to add
   * @return the resulting {@link CFloat}
   */
  CFloat add(CFloat... pSummands);

  /**
   * Multiply <code>this</code> with another {@link CFloat} object and return the resulting
   * {@link CFloat} instance.
   * <p>
   * It is recommended to return a fresh {@link CFloat} object to adhere to the general mathematical
   * idea of immutability of numbers.
   *
   * @param pFactor the other {@link CFloat} instance to multiply
   * @return the resulting {@link CFloat}
   */
  CFloat multiply(CFloat pFactor);

  /**
   * Multiply <code>this</code> with multiple {@link CFloat} objects and return the resulting
   * {@link CFloat}.
   * <p>
   * The performance of this method is highly dependent on if the implementation just loops
   * {@link CFloat#multiply(CFloat)} or uses some sort of optimized procedure. However, it should be
   * taken into consideration, that C compilations in general do not guarantee a commutative
   * multiplication of multiple floating point numbers, due to over-/underflows introduced by large
   * differences in the exponent.
   *
   * @param pFactors the other {@link CFloat} instances to multiply
   * @return the resulting {@link CFloat}
   */
  CFloat multiply(CFloat... pFactors);

  /**
   * Subtract another {@link CFloat} object from <code>this</code> and return the resulting
   * {@link CFloat} instance.
   * <p>
   * It is recommended to return a fresh {@link CFloat} object to adhere to the general mathematical
   * idea of immutability of numbers.
   *
   * @param pSubtrahend the other {@link CFloat} instance to subtract
   * @return the resulting {@link CFloat}
   */
  CFloat subtract(CFloat pSubtrahend);

  /**
   * Divide <code>this</code> by another {@link CFloat} object and return the resulting
   * {@link CFloat} instance.
   * <p>
   * It is recommended to return a fresh {@link CFloat} object to adhere to the general mathematical
   * idea of immutability of numbers.
   *
   * @param pDivisor the other {@link CFloat} instance to divide by
   * @return the resulting {@link CFloat}
   */
  CFloat divideBy(CFloat pDivisor);

  /**
   * Compute the power of <code>this</code> to another {@link CFloat} instance.
   * <p>
   * It is recommended to return a fresh {@link CFloat} object to adhere to the general mathematical
   * idea of immutability of numbers.
   *
   * @param exponent the other {@link CFloat} instance to compute the power to
   * @return the resulting {@link CFloat} object with the value of <code>this</code> to the power of
   *         <code>exponent</code>
   */
  CFloat powTo(CFloat exponent);

  /**
   * /** Compute the power of <code>this</code> to an integral exponent.
   * <p>
   * It is recommended to return a fresh {@link CFloat} object to adhere to the general mathematical
   * idea of immutability of numbers.
   *
   * @param exponent integral number to compute the power to
   * @return the resulting {@link CFloat} object with the value of <code>this</code> to the power of
   *         <code>exponent</code>
   */
  CFloat powToIntegral(int exponent);

  /**
   * Compute the square root of <code>this</code>.
   * <p>
   * Note that in general only the square root of a positive number is computable.
   * <p>
   * Usually the result should be positive but that might differ according to implementations.
   *
   * @return a {@link CFloat} instance representing the value of the square root of
   *         <code>this</code>
   */
  CFloat sqrt();

  /**
   * Return a {@link CFloat} object representing the value of <code>this</code> rounded to the
   * closest integral number. If two integral numbers are equally close to <code>this</code>,
   * {@link CFloat#round} returns the one further from zero.
   *
   * @return a {@link CFloat} object representing the value of <code>this</code> rounded to the
   *         closest integral number
   */
  CFloat round();

  /**
   * Return a {@link CFloat} object representing the value of <code>this</code> rounded to the
   * closest integral number, that is not greater (in absolute value) than <code>this</code>.
   * <p>
   * Effectively the fractional part of <code>this</code> is dropped, truncated respectively.
   *
   * @return a {@link CFloat} object representing the value of <code>this</code> rounded to the
   *         closest integral value towards zero
   */
  CFloat trunc();

  /**
   * Return a {@link CFloat} object representing the value of <code>this</code> rounded to the
   * closest integral value towards positive infinity.
   *
   * @return a {@link CFloat} object representing the value of <code>this</code> rounded to the
   *         closest integral value towards positive infinity
   */
  CFloat ceil();

  /**
   * Return a {@link CFloat} object representing the value of <code>this</code> rounded to the
   * closest integral value towards negative infinity.
   *
   * @return a {@link CFloat} object representing the value of <code>this</code> rounded to the
   *         closest integral value towards negative infinity
   */
  CFloat floor();

  /**
   * Return the absolute value of <code>this</code>, so in general just drop the sign-bit if it is
   * set.
   *
   * @return a {@link CFloat} representing the absolute value of <code>this</code>
   */
  CFloat abs();

  /**
   * Determine whether <code>this</code> has an absolute value of 0.
   * <p>
   * In general it should not matter for this method whether the sign-bit is set. Implementations
   * which differ from that interpretation should clearly state so.
   *
   * @return whether <code>this</code> has an absolute value of 0 or not
   */
  boolean isZero();

  /**
   * Determine whether <code>this</code> has an absolute value of 1.
   * <p>
   * In general it should not matter for this method whether the sign-bit is set. Implementations
   * which differ from that interpretation should clearly state so.
   *
   * @return whether <code>this</code> has an absolute value of 1 or not
   */
  boolean isOne();

  /**
   * Determine whether <code>this</code> is NaN or not.
   *
   * @return whether <code>this</code> is NaN or not
   */
  default boolean isNan() {
    return false;
  }

  /**
   * Determine whether <code>this</code> has an infinite value or not.
   *
   * @return whether <code>this</code> has an infinite value or not
   */
  default boolean isInfinity() {
    return false;
  }

  /**
   * Determine whether the sign-bit is set.
   *
   * @return whether the sign-bit is set
   */
  boolean isNegative();

  /**
   * Copy the sign-bit of another {@link CFloat} to <code>this</code> and return the resulting
   * {@link CFloat}.
   * <p>
   * It is recommended for implementations to copy the sign-bit into a fresh {@link CFloat} instance
   * and to return this new object while letting <code>this</code> untouched, to adhere to the
   * general mathematical idea of immutability of numbers.
   *
   * @param source the other {@link CFloat} instance
   * @return a {@link CFloat} instance with the absolute value of <code>this</code> and the sign of
   *         <code>source</code>
   */
  CFloat copySignFrom(CFloat source);

  /**
   * Try to cast <code>this</code> to some floating point number type.
   * <p>
   * Note, however, that due to precision changes the value of the object resulting from the cast is
   * not necessarily the same as the value of the original object.
   *
   * @param toType the target floating point number type
   * @return a new {@link CFloat} instance with the type <code>toType</code> and (approximately) the
   *         value of <code>this</code>
   */
  CFloat castTo(int toType);

  /**
   * Try to cast <code>this</code> to another number type, more precisely some implementation of the
   * {@link Number} interface.
   *
   * @param toType the target number type
   * @return a new {@link Number} instance with (approximately) the value of <code>this</code>
   */
  Number castToOther(int toType);

  /**
   * Somehow create a {@link CFloatWrapper} instance holding an exponent and mantissa representing
   * the value <code>this</code> (in case of NaN, -NaN, subnormal numbers, and zeroes there is not
   * necessarily a unique representation).
   * <p>
   * Implementations should take care to create a real copy as to prevent accidental changes to the
   * value of <code>this</code> via the returned {@link CFloatWrapper}.
   *
   * @return a {@link CFloatWrapper} instance containing a bit representation of the value of
   *         <code>this</code>
   */
  CFloatWrapper copyWrapper();

  /**
   * Return the API representation of the floating point number type of <code>this</code>.
   *
   * @return the type of <code>this</code>
   */
  int getType();

  /**
   * Compare <code>this</code> with another {@link CFloat} object and return whether
   * <code>this</code> is greater (according to floating point semantics) than the other
   * {@link CFloat} instance.
   * <p>
   * Note that usually any such comparison involving <code>NaN</code> objects results in
   * <code>false</code>
   *
   * @param other the other {@link CFloat} instance to compare <code>this</code> to
   * @return whether <code>this</code> is greater than <code>other</code>
   */
  boolean greaterThan(CFloat other);
}
