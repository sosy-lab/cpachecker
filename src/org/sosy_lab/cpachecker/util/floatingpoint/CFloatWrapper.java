// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.floatingpoint;

/**
 * This class is used to wrap the bit representation of C like floating point numbers into two
 * <code>long</code> objects.
 *
 * <p>It is on the one hand meant as an interface object between the {@link CFloatNativeAPI} and its
 * underlying C-library, on the other as a means to easier differentiate between exponent and
 * significant of a floating point number.
 *
 * <p>In its current implementation the biggest type that can be modeled is a 128-bit extended
 * double precision floating point number (in C called <code>long double</code>).
 */
class CFloatWrapper {

  private long exponent;
  private long mantissa;

  CFloatWrapper() {
    // no-op
  }

  CFloatWrapper(long pExp, long pMan) {
    exponent = pExp;
    mantissa = pMan;
  }

  /**
   * Get the exponent
   *
   * <p>The exponent returned will be <code>biased</code>. See {@link
   * org.sosy_lab.cpachecker.util.floatingpoint.FloatValue.Format#bias()} for how biased exponents
   * work in IEEE-754.
   */
  long getExponent() {
    return exponent;
  }

  /**
   * Set the exponent
   *
   * <p>The exponent is expected to be <code>biased</code>. See {@link
   * org.sosy_lab.cpachecker.util.floatingpoint.FloatValue.Format#bias()} for how biased exponents
   * work in IEEE-754.
   */
  void setExponent(long exponent) {
    this.exponent = exponent;
  }

  /**
   * Get the mantissa
   *
   * <p>For single- and double-precision values the leading bit of the mantissa (the so-called
   * "hidden bit") is not included in the value returned by this function. For "extended" precision
   * values the entire mantissa is returned as the format does not use a "hidden" bit.
   *
   * <p>"Hidden bit" refers to the leading bit of the significand. Since it is always <code>1</code>
   * in normalized binary floating point numbers, it can be dropped to save space.
   */
  long getMantissa() {
    return mantissa;
  }

  /**
   * Set the mantissa
   *
   * <p>For single- and double-precision values the leading bit of the mantissa (the so-called
   * "hidden bit") must not be included in the argument. For "extended" precision values the entire
   * mantissa must be used as there is no "hidden" bit in this format.
   *
   * <p>"Hidden bit" refers to the leading bit of the significand. Since it is always <code>1</code>
   * in normalized binary floating point numbers, it can be dropped to save space.
   */
  void setMantissa(long mantissa) {
    this.mantissa = mantissa;
  }

  /**
   * Create and return a copy of <code>this</code>, containing the exact same bit-masks.
   *
   * @return a fresh {@link CFloatWrapper} instance, containing the same bit-masks as <code>this
   *     </code>
   */
  CFloatWrapper copy() {
    return new CFloatWrapper(exponent, mantissa);
  }
}
