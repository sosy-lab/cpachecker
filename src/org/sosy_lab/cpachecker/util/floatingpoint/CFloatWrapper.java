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
    this.exponent = pExp;
    this.mantissa = pMan;
  }

  long getExponent() {
    return exponent;
  }

  void setExponent(long exponent) {
    this.exponent = exponent;
  }

  long getMantissa() {
    return mantissa;
  }

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
