// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.floatingpoint;

import static com.google.common.primitives.Ints.max;

/**
 * This class is used to increase performance and debugging capabilities. Since <code>inf</code> and
 * <code>-inf</code> are special numbers, some operations including them or performed on them,
 * evaluate in a specific manner that, generally, can be computed much easier than the usual
 * floating point operations and therefore terminate some operations faster.
 *
 * <p>Also, the usual check for infinity uses some computations which can be saved by a default
 * return of <code>true</code> when the object already is known to be an infinity.
 */
public class CFloatInf extends CFloat {

  private boolean negative;
  private final int type;

  public CFloatInf() {
    this(false, CFloatNativeAPI.FP_TYPE_SINGLE);
  }

  public CFloatInf(int type) {
    this(false, type);
  }

  public CFloatInf(boolean pNegative, int pType) {
    negative = pNegative;
    type = pType;
  }

  @Override
  public CFloat add(CFloat pSummand) {
    int maxType = max(type, pSummand.getType());
    if (pSummand.isInfinity() && negative != pSummand.isNegative()) {
      return new CFloatNaN(true, maxType);
    }

    return new CFloatInf(negative, maxType);
  }

  @Override
  public CFloat add(CFloat... pSummands) {
    int maxType = type;
    boolean nanResult = false;
    for (CFloat summand : pSummands) {
      maxType = max(maxType, summand.getType());
      if (summand.isInfinity() && negative != summand.isNegative()) {
        nanResult = true;
      }
    }

    if (nanResult) {
      return new CFloatNaN(true, maxType);
    }

    return new CFloatInf(negative, maxType);
  }

  @Override
  public CFloat multiply(CFloat pFactor) {
    int maxType = max(type, pFactor.getType());
    return new CFloatInf(negative, maxType);
  }

  @Override
  public CFloat multiply(CFloat... pFactors) {
    int maxType = type;
    int sign = negative ? -1 : 1;

    for (CFloat factor : pFactors) {
      maxType = max(maxType, factor.getType());
      sign *= factor.isNegative() ? -1 : 1;
    }
    return new CFloatInf(sign < 0, maxType);
  }

  @Override
  public CFloat subtract(CFloat pSubtrahend) {
    int maxType = max(type, pSubtrahend.getType());
    if (pSubtrahend.isInfinity() && negative == pSubtrahend.isNegative()) {
      return new CFloatNaN(true, maxType);
    }

    return new CFloatInf(negative, maxType);
  }

  @Override
  public CFloat divideBy(CFloat pDivisor) {
    int maxType = max(type, pDivisor.getType());
    int sign = (negative ? -1 : 1) * (pDivisor.isNegative() ? -1 : 1);
    return new CFloatInf(sign < 0, maxType);
  }

  @Override
  public CFloat powTo(CFloat pExponent) {
    return new CFloatInf(negative, type);
  }

  @Override
  public CFloat powToIntegral(int pExponent) {
    boolean neg = negative;
    if (negative) {
      if (pExponent % 2 == 0) {
        neg = false;
      }
    }
    return new CFloatInf(neg, type);
  }

  @Override
  public CFloat sqrt() {
    if (negative) {
      return new CFloatNaN(true, type);
    }
    return new CFloatInf(negative, type);
  }

  @Override
  public CFloat round() {
    return new CFloatInf(negative, type);
  }

  @Override
  public CFloat trunc() {
    return new CFloatInf(negative, type);
  }

  @Override
  public CFloat ceil() {
    return new CFloatInf(negative, type);
  }

  @Override
  public CFloat floor() {
    return new CFloatInf(negative, type);
  }

  @Override
  public CFloat abs() {
    return new CFloatInf(false, type);
  }

  @Override
  public boolean isZero() {
    return false;
  }

  @Override
  public boolean isOne() {
    return false;
  }

  @Override
  public boolean isNegative() {
    return negative;
  }

  @Override
  public CFloat copySignFrom(CFloat pSource) {
    return new CFloatInf(pSource.isNegative(), type);
  }

  @Override
  public CFloat castTo(int pToType) {
    return new CFloatInf(negative, pToType);
  }

  @Override
  public Number castToOther(int pToType) {
    // TODO Determine how to handle this
    // XXX: effectively return pToType.MIN_VALUE
    return null;
  }

  @Override
  public CFloatWrapper copyWrapper() {
    CFloatWrapper result = null;
    switch (type) {
      case CFloatNativeAPI.FP_TYPE_SINGLE:
      case CFloatNativeAPI.FP_TYPE_DOUBLE:
        result = new CFloatWrapper(getExponentMask() ^ (negative ? getSignBitMask() : 0L), 0L);
        break;
      case CFloatNativeAPI.FP_TYPE_LONG_DOUBLE:
        result =
            new CFloatWrapper(
                getExponentMask() ^ (negative ? getSignBitMask() : 0L), getNormalizationMask());
        break;
      default:
        throw new RuntimeException("Unimplemented floating point type: " + type);
    }
    return result;
  }

  @Override
  public int getType() {
    return type;
  }

  @Override
  public boolean isInfinity() {
    return true;
  }

  @Override
  public String toString() {
    return (negative ? "-" : "") + "inf";
  }

  @Override
  public boolean greaterThan(CFloat pFloat) {
    if (pFloat.isNan() || (pFloat.isInfinity() && !pFloat.isNegative())) {
      return false;
    }
    return !isNegative();
  }

  @Override
  protected CFloatWrapper getWrapper() {
    return copyWrapper();
  }
}
