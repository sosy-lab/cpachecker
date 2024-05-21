// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.floatingpoint;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.primitives.Ints.max;

import com.google.common.base.Preconditions;
import org.sosy_lab.cpachecker.util.floatingpoint.CFloatNativeAPI.CNativeType;
import org.sosy_lab.cpachecker.util.floatingpoint.FloatValue.Format;

/**
 * C based implementation of the {@link CFloat} interface.
 *
 * <p>This implementation calls a native code library via JNI to perform its operations directly in
 * C.
 */
@Deprecated
class CFloatNative extends CFloat {
  private final CFloatWrapper wrapper;
  private final int type;

  public CFloatNative(String rep, int pType) {
    wrapper = CFloatNativeAPI.createFp(rep, pType);
    type = pType;
  }

  public CFloatNative(String rep, Format pFormat) {
    // TODO: Add support for 80bit x87 floats
    int pType = -1;
    if (pFormat.equals(Format.Float32)) {
      pType = CNativeType.SINGLE.getOrdinal();
    }
    if (pFormat.equals(Format.Float64)) {
      pType = CNativeType.DOUBLE.getOrdinal();
    }
    checkArgument(pType >= 0);
    wrapper = CFloatNativeAPI.createFp(rep, pType);
    type = pType;
  }

  public CFloatNative(CFloatWrapper pWrapper, int pType) {
    wrapper = pWrapper;
    type = pType;
  }

  @Override
  public CFloat add(CFloat summand) {
    CFloatWrapper newFloat =
        CFloatNativeAPI.addFp(wrapper, type, summand.copyWrapper(), summand.getType());
    return new CFloatNative(newFloat, max(type, summand.getType()));
  }

  public CFloat add3(CFloat summand1, CFloat summand2) {
    CFloatWrapper newFloat =
        CFloatNativeAPI.add3Fp(
            wrapper,
            type,
            summand1.copyWrapper(),
            summand1.getType(),
            summand2.copyWrapper(),
            summand2.getType());
    return new CFloatNative(newFloat, max(type, summand1.getType(), summand1.getType()));
  }

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

    CFloatWrapper newFloat = CFloatNativeAPI.addManyFp(wrapper, types, wrappers);
    return new CFloatNative(newFloat, maxType);
  }

  @Override
  public CFloat multiply(CFloat factor) {
    CFloatWrapper newFloat =
        CFloatNativeAPI.multiplyFp(wrapper, type, factor.copyWrapper(), factor.getType());
    return new CFloatNative(newFloat, max(type, factor.getType()));
  }

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

    CFloatWrapper newFloat = CFloatNativeAPI.multiplyManyFp(wrapper, types, wrappers);
    return new CFloatNative(newFloat, maxType);
  }

  @Override
  public CFloat subtract(CFloat subtrahend) {
    CFloatWrapper newFloat =
        CFloatNativeAPI.subtractFp(wrapper, type, subtrahend.copyWrapper(), subtrahend.getType());

    return new CFloatNative(newFloat, max(type, subtrahend.getType()));
  }

  @Override
  public CFloatNative divideBy(CFloat divisor) {
    CFloatWrapper newFloat =
        CFloatNativeAPI.divideFp(wrapper, type, divisor.copyWrapper(), divisor.getType());

    return new CFloatNative(newFloat, max(type, divisor.getType()));
  }

  @Override
  public CFloat ln() {
    return new CFloatNative(CFloatNativeAPI.logFp(wrapper, type), type);
  }

  @Override
  public CFloat exp() {
    return new CFloatNative(CFloatNativeAPI.expFp(wrapper, type), type);
  }

  @Override
  public CFloat powTo(CFloat exponent) {
    CFloatWrapper newFloat =
        CFloatNativeAPI.powFp(wrapper, type, exponent.copyWrapper(), exponent.getType());

    return new CFloatNative(newFloat, max(type, exponent.getType()));
  }

  @Override
  public CFloat powToIntegral(int exponent) {
    // FIXME: Add support for negative integer exponent in floatingPoints.c
    Preconditions.checkArgument(0 <= exponent, "Negative exponents not supported");

    CFloatWrapper newFloat = CFloatNativeAPI.powIntegralFp(wrapper, exponent, type);
    return new CFloatNative(newFloat, type);
  }

  @Override
  public CFloat sqrt() {
    CFloatWrapper newFloat = CFloatNativeAPI.sqrtFp(wrapper, type);

    return new CFloatNative(newFloat, type);
  }

  @Override
  public CFloat round() {
    CFloatWrapper newFloat = CFloatNativeAPI.roundFp(wrapper, type);

    return new CFloatNative(newFloat, type);
  }

  @Override
  public CFloat trunc() {
    CFloatWrapper newFloat = CFloatNativeAPI.truncFp(wrapper, type);

    return new CFloatNative(newFloat, type);
  }

  @Override
  public CFloat ceil() {
    CFloatWrapper newFloat = CFloatNativeAPI.ceilFp(wrapper, type);

    return new CFloatNative(newFloat, type);
  }

  @Override
  public CFloat floor() {
    CFloatWrapper newFloat = CFloatNativeAPI.floorFp(wrapper, type);

    return new CFloatNative(newFloat, type);
  }

  @Override
  public CFloat abs() {
    CFloatWrapper newFloat = CFloatNativeAPI.absFp(wrapper, type);

    return new CFloatNative(newFloat, type);
  }

  @Override
  public boolean isZero() {
    return CFloatNativeAPI.isZeroFp(wrapper, type);
  }

  @Override
  public boolean isOne() {
    return CFloatNativeAPI.isOneFp(wrapper, type);
  }

  @Override
  public boolean isNan() {
    return CFloatNativeAPI.isNanFp(wrapper, type);
  }

  @Override
  public boolean isInfinity() {
    return CFloatNativeAPI.isInfinityFp(wrapper, type);
  }

  @Override
  public boolean isNegative() {
    return CFloatNativeAPI.isNegativeFp(wrapper, type);
  }

  @Override
  public CFloat copySignFrom(CFloat source) {
    if (type != source.getType()) {
      throw new IllegalArgumentException(
          "Type "
              + type
              + " of first argument and type "
              + source.getType()
              + " of second argument must not be different.");
    }
    CFloatWrapper newFloat = CFloatNativeAPI.copySignFp(wrapper, source.copyWrapper(), type);

    return new CFloatNative(newFloat, type);
  }

  @Override
  public CFloat castTo(CNativeType toType) {
    CFloatWrapper newFloat = CFloatNativeAPI.castFpFromTo(wrapper, type, toType.getOrdinal());

    return new CFloatNative(newFloat, toType.getOrdinal());
  }

  public static CFloat castOtherTo(Number value, int fromType, int toType) {
    CFloatWrapper newFloat = CFloatNativeAPI.castOtherToFp(value, fromType, toType);

    return new CFloatNative(newFloat, toType);
  }

  @Override
  public Number castToOther(CNativeType toType) {
    if (toType == CNativeType.CHAR) {
      return CFloatNativeAPI.castFpToByte(wrapper, type);
    }
    if (toType == CNativeType.SHORT) {
      return CFloatNativeAPI.castFpToShort(wrapper, type);
    }
    if (toType == CNativeType.INT) {
      return CFloatNativeAPI.castFpToInt(wrapper, type);
    }
    if (toType == CNativeType.LONG) {
      return CFloatNativeAPI.castFpToLong(wrapper, type);
    }
    throw new IllegalArgumentException();
  }

  private int constructParametersForMultiOperation(
      int index, int maxType, CFloatWrapper[] wrappers, int[] types, CFloat... summands) {
    types[0] = type;
    for (CFloat f : summands) {
      wrappers[index] = f.copyWrapper();
      maxType = max(maxType, f.getType());
      types[++index] = f.getType();
    }
    return maxType;
  }

  @Override
  public String toString() {
    return CFloatNativeAPI.printFp(wrapper, type);
  }

  @Override
  public CFloatWrapper copyWrapper() {
    return wrapper.copy();
  }

  @Override
  public int getType() {
    return type;
  }

  @Override
  public boolean greaterThan(CFloat pFloat) {
    return CFloatNativeAPI.isGreaterFp(wrapper, type, pFloat.copyWrapper(), pFloat.getType());
  }

  @Override
  protected CFloatWrapper getWrapper() {
    return wrapper;
  }
}
