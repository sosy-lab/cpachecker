// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.floatingpoint;

import static com.google.common.primitives.Ints.max;

import com.google.common.base.Preconditions;
import java.util.Optional;
import org.sosy_lab.cpachecker.util.floatingpoint.CFloatNativeAPI.CFloatType;
import org.sosy_lab.cpachecker.util.floatingpoint.CFloatNativeAPI.CIntegerType;

/**
 * C based implementation of the {@link CFloat} interface.
 *
 * <p>This implementation calls a native code library via JNI to perform its operations directly in
 * C.
 */
@Deprecated
class CFloatNative extends CFloat {
  private final CFloatWrapper wrapper;
  private final CFloatType type;

  public CFloatNative(String rep, CFloatType pType) {
    wrapper = CFloatNativeAPI.createFp(rep, pType.ordinal());
    type = pType;
  }

  public CFloatNative(CFloatWrapper pWrapper, CFloatType pType) {
    wrapper = pWrapper;
    type = pType;
  }

  /** Get the float type with the given ordinal */
  private CFloatType toFloatType(int ordinal) {
    return CFloatType.values()[ordinal];
  }

  @Override
  public CFloat add(CFloat summand) {
    CFloatWrapper newFloat =
        CFloatNativeAPI.addFp(
            wrapper, type.ordinal(), summand.copyWrapper(), summand.getType().ordinal());
    return new CFloatNative(
        newFloat, toFloatType(max(type.ordinal(), summand.getType().ordinal())));
  }

  public CFloat add3(CFloat summand1, CFloat summand2) {
    CFloatWrapper newFloat =
        CFloatNativeAPI.add3Fp(
            wrapper,
            type.ordinal(),
            summand1.copyWrapper(),
            summand1.getType().ordinal(),
            summand2.copyWrapper(),
            summand2.getType().ordinal());
    return new CFloatNative(
        newFloat,
        toFloatType(
            max(type.ordinal(), summand1.getType().ordinal(), summand1.getType().ordinal())));
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
    return new CFloatNative(newFloat, toFloatType(maxType));
  }

  @Override
  public CFloat multiply(CFloat factor) {
    CFloatWrapper newFloat =
        CFloatNativeAPI.multiplyFp(
            wrapper, type.ordinal(), factor.copyWrapper(), factor.getType().ordinal());
    return new CFloatNative(newFloat, toFloatType(max(type.ordinal(), factor.getType().ordinal())));
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
    return new CFloatNative(newFloat, toFloatType(maxType));
  }

  @Override
  public CFloat subtract(CFloat subtrahend) {
    CFloatWrapper newFloat =
        CFloatNativeAPI.subtractFp(
            wrapper, type.ordinal(), subtrahend.copyWrapper(), subtrahend.getType().ordinal());

    return new CFloatNative(
        newFloat, toFloatType(max(type.ordinal(), subtrahend.getType().ordinal())));
  }

  @Override
  public CFloatNative divideBy(CFloat divisor) {
    CFloatWrapper newFloat =
        CFloatNativeAPI.divideFp(
            wrapper, type.ordinal(), divisor.copyWrapper(), divisor.getType().ordinal());

    return new CFloatNative(
        newFloat, toFloatType(max(type.ordinal(), divisor.getType().ordinal())));
  }

  @Override
  public CFloat ln() {
    return new CFloatNative(CFloatNativeAPI.logFp(wrapper, type.ordinal()), type);
  }

  @Override
  public CFloat exp() {
    return new CFloatNative(CFloatNativeAPI.expFp(wrapper, type.ordinal()), type);
  }

  @Override
  public CFloat powTo(CFloat exponent) {
    CFloatWrapper newFloat =
        CFloatNativeAPI.powFp(
            wrapper, type.ordinal(), exponent.copyWrapper(), exponent.getType().ordinal());

    return new CFloatNative(
        newFloat, toFloatType(max(type.ordinal(), exponent.getType().ordinal())));
  }

  @Override
  public CFloat powToIntegral(int exponent) {
    // FIXME: Add support for negative integer exponent in floatingPoints.c
    Preconditions.checkArgument(0 <= exponent, "Negative exponents not supported");

    CFloatWrapper newFloat = CFloatNativeAPI.powIntegralFp(wrapper, exponent, type.ordinal());
    return new CFloatNative(newFloat, type);
  }

  @Override
  public CFloat sqrt() {
    CFloatWrapper newFloat = CFloatNativeAPI.sqrtFp(wrapper, type.ordinal());

    return new CFloatNative(newFloat, type);
  }

  @Override
  public CFloat round() {
    CFloatWrapper newFloat = CFloatNativeAPI.roundFp(wrapper, type.ordinal());

    return new CFloatNative(newFloat, type);
  }

  @Override
  public CFloat trunc() {
    CFloatWrapper newFloat = CFloatNativeAPI.truncFp(wrapper, type.ordinal());

    return new CFloatNative(newFloat, type);
  }

  @Override
  public CFloat ceil() {
    CFloatWrapper newFloat = CFloatNativeAPI.ceilFp(wrapper, type.ordinal());

    return new CFloatNative(newFloat, type);
  }

  @Override
  public CFloat floor() {
    CFloatWrapper newFloat = CFloatNativeAPI.floorFp(wrapper, type.ordinal());

    return new CFloatNative(newFloat, type);
  }

  @Override
  public CFloat abs() {
    CFloatWrapper newFloat = CFloatNativeAPI.absFp(wrapper, type.ordinal());

    return new CFloatNative(newFloat, type);
  }

  @Override
  public boolean isZero() {
    return CFloatNativeAPI.isZeroFp(wrapper, type.ordinal());
  }

  @Override
  public boolean isOne() {
    return CFloatNativeAPI.isOneFp(wrapper, type.ordinal());
  }

  @Override
  public boolean isNan() {
    return CFloatNativeAPI.isNanFp(wrapper, type.ordinal());
  }

  @Override
  public boolean isInfinity() {
    return CFloatNativeAPI.isInfinityFp(wrapper, type.ordinal());
  }

  @Override
  public boolean isNegative() {
    return CFloatNativeAPI.isNegativeFp(wrapper, type.ordinal());
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
    CFloatWrapper newFloat =
        CFloatNativeAPI.copySignFp(wrapper, source.copyWrapper(), type.ordinal());
    return new CFloatNative(newFloat, type);
  }

  @Override
  public CFloat castTo(CFloatType toType) {
    CFloatWrapper newFloat =
        CFloatNativeAPI.castFpFromTo(wrapper, type.ordinal(), toType.ordinal());
    return new CFloatNative(newFloat, toType);
  }

  public static CFloat castOtherTo(Number value, CIntegerType fromType, CFloatType toType) {
    CFloatWrapper r =
        switch (fromType) {
          case CHAR -> CFloatNativeAPI.castByteToFp(value.byteValue(), toType.ordinal());
          case SHORT -> CFloatNativeAPI.castShortToFp(value.shortValue(), toType.ordinal());
          case INT -> CFloatNativeAPI.castIntToFp(value.intValue(), toType.ordinal());
          case LONG -> CFloatNativeAPI.castLongToFp(value.longValue(), toType.ordinal());
          default -> throw new UnsupportedOperationException();
        };
    return new CFloatNative(r, toType);
  }

  @Override
  public Optional<Number> castToOther(CIntegerType toType) {
    long r =
        switch (toType) {
          case CHAR -> CFloatNativeAPI.castFpToByte(wrapper, type.ordinal());
          case SHORT -> CFloatNativeAPI.castFpToShort(wrapper, type.ordinal());
          case INT -> CFloatNativeAPI.castFpToInt(wrapper, type.ordinal());
          case LONG -> CFloatNativeAPI.castFpToLong(wrapper, type.ordinal());
          default -> throw new IllegalArgumentException();
        };

    CFloat v = castOtherTo(r, toType, type);
    if (!v.equalTo(trunc())) {
      // Return Optional.empty() if the value was too large for the target type
      return Optional.empty();
    }
    return Optional.of(r);
  }

  private int constructParametersForMultiOperation(
      int index, int maxType, CFloatWrapper[] wrappers, int[] types, CFloat... summands) {
    types[0] = type.ordinal();
    for (CFloat f : summands) {
      wrappers[index] = f.copyWrapper();
      maxType = max(maxType, f.getType().ordinal());
      types[++index] = f.getType().ordinal();
    }
    return maxType;
  }

  @Override
  public String toString() {
    return CFloatNativeAPI.printFp(wrapper, type.ordinal());
  }

  @Override
  public CFloatWrapper copyWrapper() {
    return wrapper.copy();
  }

  @Override
  public CFloatType getType() {
    return type;
  }

  @Override
  public boolean equalTo(CFloat other) {
    return CFloatNativeAPI.isEqualFp(
        wrapper, type.ordinal(), other.copyWrapper(), other.getType().ordinal());
  }

  @Override
  public boolean notEqualTo(CFloat other) {
    return CFloatNativeAPI.isNotEqualFp(
        wrapper, type.ordinal(), other.copyWrapper(), other.getType().ordinal());
  }

  @Override
  public boolean greaterThan(CFloat other) {
    return CFloatNativeAPI.isGreaterFp(
        wrapper, type.ordinal(), other.copyWrapper(), other.getType().ordinal());
  }

  @Override
  public boolean greaterOrEqual(CFloat other) {
    return CFloatNativeAPI.isGreaterEqualFp(
        wrapper, type.ordinal(), other.copyWrapper(), other.getType().ordinal());
  }

  @Override
  public boolean lessThan(CFloat other) {
    return CFloatNativeAPI.isLessFp(
        wrapper, type.ordinal(), other.copyWrapper(), other.getType().ordinal());
  }

  @Override
  public boolean lessOrEqual(CFloat other) {
    return CFloatNativeAPI.isLessEqualFp(
        wrapper, type.ordinal(), other.copyWrapper(), other.getType().ordinal());
  }

  @Override
  public int compareTo(CFloat other) {
    return CFloatNativeAPI.totalOrderFp(
        wrapper, type.ordinal(), other.copyWrapper(), other.getType().ordinal());
  }

  @Override
  protected CFloatWrapper getWrapper() {
    return wrapper;
  }
}
