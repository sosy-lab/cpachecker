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

import static com.google.common.primitives.Ints.max;

@Deprecated
public class CFloatNative implements CFloat {
  private final CFloatWrapper WRAPPER;
  private final int TYPE;

  public CFloatNative(String rep, int type) {
    this.WRAPPER = CFloatNativeAPI.createFp(rep, type);
    this.TYPE = type;
  }

  public CFloatNative(CFloatWrapper wrapper, int type) {
    this.WRAPPER = wrapper;
    this.TYPE = type;
  }

  /* (non-Javadoc)
   * @see org.sosy_lab.cpachecker.util.floatingpoint.CFloat#add(org.sosy_lab.cpachecker.util.floatingpoint.CFloatNative)
   */
  @Override
  public CFloat add(CFloat summand) {
    CFloatWrapper newFloat =
        CFloatNativeAPI.addFp(WRAPPER, TYPE, summand.copyWrapper(), summand.getType());
    return new CFloatNative(newFloat, max(TYPE, summand.getType()));
  }

  /* (non-Javadoc)
   * @see org.sosy_lab.cpachecker.util.floatingpoint.CFloat#add(org.sosy_lab.cpachecker.util.floatingpoint.CFloatNative)
   */
  @Override
  public CFloat add(CFloat... summands) {
    if (summands.length < 1) {
      return this;
    }
    if (summands.length < 2) {
      return add(summands[0]);
    }

    int index = 0;
    int maxType = -1;
    CFloatWrapper[] wrappers = new CFloatWrapper[summands.length];
    int[] types = new int[summands.length + 1];

    maxType = constructParametersForMultiOperation(index, maxType, wrappers, types, summands);

    CFloatWrapper newFloat = CFloatNativeAPI.addManyFp(WRAPPER, types, wrappers);
    return new CFloatNative(newFloat, maxType);
  }

  /* (non-Javadoc)
   * @see org.sosy_lab.cpachecker.util.floatingpoint.CFloat#multiply(org.sosy_lab.cpachecker.util.floatingpoint.CFloatNative)
   */
  @Override
  public CFloat multiply(CFloat factor) {
    CFloatWrapper newFloat =
        CFloatNativeAPI.multiplyFp(WRAPPER, TYPE, factor.copyWrapper(), factor.getType());
    return new CFloatNative(newFloat, max(TYPE, factor.getType()));
  }

  /* (non-Javadoc)
   * @see org.sosy_lab.cpachecker.util.floatingpoint.CFloat#multiply(org.sosy_lab.cpachecker.util.floatingpoint.CFloatNative)
   */
  @Override
  public CFloat multiply(CFloat... factors) {
    if (factors.length < 1) {
      return this;
    }
    if (factors.length < 2) {
      return add(factors[0]);
    }

    int index = 0;
    int maxType = -1;
    CFloatWrapper[] wrappers = new CFloatWrapper[factors.length];
    int[] types = new int[factors.length + 1];

    maxType = constructParametersForMultiOperation(index, maxType, wrappers, types, factors);

    CFloatWrapper newFloat = CFloatNativeAPI.multiplyManyFp(WRAPPER, types, wrappers);
    return new CFloatNative(newFloat, maxType);
  }

  /* (non-Javadoc)
   * @see org.sosy_lab.cpachecker.util.floatingpoint.CFloat#subtract(org.sosy_lab.cpachecker.util.floatingpoint.CFloatNative)
   */
  @Override
  public CFloat subtract(CFloat subtrahend) {
    CFloatWrapper newFloat =
        CFloatNativeAPI.subtractFp(WRAPPER, TYPE, subtrahend.copyWrapper(), subtrahend.getType());

    return new CFloatNative(newFloat, max(TYPE, subtrahend.getType()));
  }

  /* (non-Javadoc)
   * @see org.sosy_lab.cpachecker.util.floatingpoint.CFloat#divideBy(org.sosy_lab.cpachecker.util.floatingpoint.CFloatNative)
   */
  @Override
  public CFloatNative divideBy(CFloat divisor) {
    CFloatWrapper newFloat =
        CFloatNativeAPI.divideFp(WRAPPER, TYPE, divisor.copyWrapper(), divisor.getType());

    return new CFloatNative(newFloat, max(TYPE, divisor.getType()));
  }

  /* (non-Javadoc)
   * @see org.sosy_lab.cpachecker.util.floatingpoint.CFloat#powTo(org.sosy_lab.cpachecker.util.floatingpoint.CFloatNative)
   */
  @Override
  public CFloat powTo(CFloat exponent) {
    CFloatWrapper newFloat =
        CFloatNativeAPI.powFp(WRAPPER, TYPE, exponent.copyWrapper(), exponent.getType());

    return new CFloatNative(newFloat, max(TYPE, exponent.getType()));
  }

  /* (non-Javadoc)
   * @see org.sosy_lab.cpachecker.util.floatingpoint.CFloat#powToIntegral(int)
   */
  @Override
  public CFloat powToIntegral(int exponent) {
    CFloatWrapper newFloat = CFloatNativeAPI.powIntegralFp(WRAPPER, exponent, TYPE);

    return new CFloatNative(newFloat, TYPE);
  }

  /* (non-Javadoc)
   * @see org.sosy_lab.cpachecker.util.floatingpoint.CFloat#sqrt()
   */
  @Override
  public CFloat sqrt() {
    CFloatWrapper newFloat = CFloatNativeAPI.sqrtFp(WRAPPER, TYPE);

    return new CFloatNative(newFloat, TYPE);
  }

  /* (non-Javadoc)
   * @see org.sosy_lab.cpachecker.util.floatingpoint.CFloat#round()
   */
  @Override
  public CFloat round() {
    CFloatWrapper newFloat = CFloatNativeAPI.roundFp(WRAPPER, TYPE);

    return new CFloatNative(newFloat, TYPE);
  }

  /* (non-Javadoc)
   * @see org.sosy_lab.cpachecker.util.floatingpoint.CFloat#trunc()
   */
  @Override
  public CFloat trunc() {
    CFloatWrapper newFloat = CFloatNativeAPI.truncFp(WRAPPER, TYPE);

    return new CFloatNative(newFloat, TYPE);
  }

  /* (non-Javadoc)
   * @see org.sosy_lab.cpachecker.util.floatingpoint.CFloat#ceil()
   */
  @Override
  public CFloat ceil() {
    CFloatWrapper newFloat = CFloatNativeAPI.ceilFp(WRAPPER, TYPE);

    return new CFloatNative(newFloat, TYPE);
  }

  /* (non-Javadoc)
   * @see org.sosy_lab.cpachecker.util.floatingpoint.CFloat#floor()
   */
  @Override
  public CFloat floor() {
    CFloatWrapper newFloat = CFloatNativeAPI.floorFp(WRAPPER, TYPE);

    return new CFloatNative(newFloat, TYPE);
  }

  /* (non-Javadoc)
   * @see org.sosy_lab.cpachecker.util.floatingpoint.CFloat#abs()
   */
  @Override
  public CFloat abs() {
    CFloatWrapper newFloat = CFloatNativeAPI.absFp(WRAPPER, TYPE);

    return new CFloatNative(newFloat, TYPE);
  }

  /* (non-Javadoc)
   * @see org.sosy_lab.cpachecker.util.floatingpoint.CFloat#isZero()
   */
  @Override
  public boolean isZero() {
    return CFloatNativeAPI.isZeroFp(WRAPPER, TYPE);
  }

  /* (non-Javadoc)
   * @see org.sosy_lab.cpachecker.util.floatingpoint.CFloat#isOne()
   */
  @Override
  public boolean isOne() {
    return CFloatNativeAPI.isOneFp(WRAPPER, TYPE);
  }

  /* (non-Javadoc)
   * @see org.sosy_lab.cpachecker.util.floatingpoint.CFloat#isNan()
   */
  @Override
  public boolean isNan() {
    return CFloatNativeAPI.isNanFp(WRAPPER, TYPE);
  }

  /* (non-Javadoc)
   * @see org.sosy_lab.cpachecker.util.floatingpoint.CFloat#isInfinity()
   */
  @Override
  public boolean isInfinity() {
    return CFloatNativeAPI.isInfinityFp(WRAPPER, TYPE);
  }

  /* (non-Javadoc)
   * @see org.sosy_lab.cpachecker.util.floatingpoint.CFloat#isNegative()
   */
  @Override
  public boolean isNegative() {
    return CFloatNativeAPI.isNegativeFp(WRAPPER, TYPE);
  }

  /* (non-Javadoc)
   * @see org.sosy_lab.cpachecker.util.floatingpoint.CFloat#copySignFrom(org.sosy_lab.cpachecker.util.floatingpoint.CFloatNative)
   */
  @Override
  public CFloat copySignFrom(CFloat source) {
    if (TYPE != source.getType()) {
      throw new IllegalArgumentException(
          "Type "
              + TYPE
              + " of first argument and type "
              + source.getType()
              + " of second argument must not be different.");
    }
    CFloatWrapper newFloat = CFloatNativeAPI.copySignFp(WRAPPER, source.copyWrapper(), TYPE);

    return new CFloatNative(newFloat, TYPE);
  }

  /* (non-Javadoc)
   * @see org.sosy_lab.cpachecker.util.floatingpoint.CFloat#castTo(int)
   */
  @Override
  public CFloat castTo(int toType) {
    CFloatWrapper newFloat = CFloatNativeAPI.castFpFromTo(WRAPPER, TYPE, toType);

    return new CFloatNative(newFloat, toType);
  }

  public static CFloat castOtherTo(Number value, int fromType, int toType) {
    CFloatWrapper newFloat = CFloatNativeAPI.castOtherToFp(value, fromType, toType);

    return new CFloatNative(newFloat, toType);
  }

  /* (non-Javadoc)
   * @see org.sosy_lab.cpachecker.util.floatingpoint.CFloat#castToOther(int)
   */
  @Override
  public Number castToOther(int toType) {
    return CFloatNativeAPI.castFpToOther(WRAPPER, TYPE, toType);
  }

  private int constructParametersForMultiOperation(
      int index,
      int maxType,
      CFloatWrapper[] wrappers,
      int[] types,
      CFloat... summands) {
    types[0] = TYPE;
    for (CFloat f : summands) {
      wrappers[index] = f.copyWrapper();
      maxType = max(maxType, f.getType());
      types[++index] = f.getType();
    }
    return maxType;
  }

  @Override
  public String toString() {
    return CFloatNativeAPI.printFp(WRAPPER, TYPE).replaceAll("(\\.[0-9]+?)0*$", "$1");
  }

  /* (non-Javadoc)
   * @see org.sosy_lab.cpachecker.util.floatingpoint.CFloat#copyWrapper()
   */
  @Override
  public CFloatWrapper copyWrapper() {
    return WRAPPER.copy();
  }

  @Override
  public int getType() {
    return TYPE;
  }
}
