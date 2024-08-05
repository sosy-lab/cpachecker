// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.floatingpoint;

import org.sosy_lab.common.NativeLibraries;

/**
 * This class is an JNI interface to the FloatingPoints C library.
 *
 * <p>Its purpose is to transport information about bit-masks of floating point number types between
 * Java and C via instances of the {@link CFloatWrapper} class and therefore to (in a way) more
 * easily compare expectations towards how certain floating point operations on a given system and C
 * compilation should work with the reality from inside a Java based framework.
 */
class CFloatNativeAPI {
  private CFloatNativeAPI() {}

  static {
    NativeLibraries.loadLibrary("FloatingPoints");
  }

  /** Native float types from C */
  public enum CFloatType {
    // DON'T CHANGE THIS ENUM WITHOUT UPDATING THE C HEADERS
    SINGLE,
    DOUBLE,
    LONG_DOUBLE
  }

  public enum CIntegerType {
    // DON'T CHANGE THIS ENUM WITHOUT UPDATING THE C HEADERS
    CHAR,
    SHORT,
    INT,
    LONG,
    LONG_LONG,
    UCHAR,
    USHORT,
    UINT,
    ULONG,
    ULONG_LONG
  }

  public static final CFloat ZERO_SINGLE;
  public static final CFloat ONE_SINGLE;
  public static final CFloat TEN_SINGLE;

  public static final CFloat ZERO_DOUBLE;
  public static final CFloat ONE_DOUBLE;
  public static final CFloat TEN_DOUBLE;

  public static final CFloat ZERO_LONG_DOUBLE;
  public static final CFloat ONE_LONG_DOUBLE;
  public static final CFloat TEN_LONG_DOUBLE;

  static {
    ZERO_SINGLE = new CFloatImpl(createFp("0.0", CFloatType.SINGLE.ordinal()), CFloatType.SINGLE);
    ONE_SINGLE = new CFloatImpl(createFp("1.0", CFloatType.SINGLE.ordinal()), CFloatType.SINGLE);
    TEN_SINGLE = new CFloatImpl(createFp("10.0", CFloatType.SINGLE.ordinal()), CFloatType.SINGLE);

    ZERO_DOUBLE = new CFloatImpl(createFp("0.0", CFloatType.DOUBLE.ordinal()), CFloatType.DOUBLE);
    ONE_DOUBLE = new CFloatImpl(createFp("1.0", CFloatType.DOUBLE.ordinal()), CFloatType.DOUBLE);
    TEN_DOUBLE = new CFloatImpl(createFp("10.0", CFloatType.DOUBLE.ordinal()), CFloatType.DOUBLE);

    ZERO_LONG_DOUBLE =
        new CFloatImpl(createFp("0.0", CFloatType.LONG_DOUBLE.ordinal()), CFloatType.LONG_DOUBLE);
    ONE_LONG_DOUBLE =
        new CFloatImpl(createFp("1.0", CFloatType.LONG_DOUBLE.ordinal()), CFloatType.LONG_DOUBLE);
    TEN_LONG_DOUBLE =
        new CFloatImpl(createFp("10.0", CFloatType.LONG_DOUBLE.ordinal()), CFloatType.LONG_DOUBLE);
  }

  public static native CFloatWrapper createFp(String stringRepresentation, int fp_type);

  public static native String printFp(CFloatWrapper fp, int fp_type);

  public static native CFloatWrapper addFp(
      CFloatWrapper fp1, int fp_type1, CFloatWrapper fp2, int fp_type2);

  public static native CFloatWrapper subtractFp(
      CFloatWrapper fp1, int fp_type1, CFloatWrapper fp2, int fp_type2);

  public static native CFloatWrapper multiplyFp(
      CFloatWrapper fp1, int fp_type1, CFloatWrapper fp2, int fp_type2);

  public static native CFloatWrapper divideFp(
      CFloatWrapper fp1, int fp_type1, CFloatWrapper fp2, int fp_type2);

  public static native CFloatWrapper add3Fp(
      CFloatWrapper fp1,
      int fp_type1,
      CFloatWrapper fp2,
      int fp_type2,
      CFloatWrapper fp3,
      int fp_type3);

  public static native CFloatWrapper addManyFp(
      CFloatWrapper fp1, int[] fp_types, CFloatWrapper... fps);

  public static native CFloatWrapper multiplyManyFp(
      CFloatWrapper fp1, int[] fp_types, CFloatWrapper... fps);

  public static native CFloatWrapper logFp(CFloatWrapper fp1, int fp_type1);

  public static native CFloatWrapper expFp(CFloatWrapper fp1, int fp_type1);

  public static native CFloatWrapper powFp(
      CFloatWrapper fp1, int fp_type1, CFloatWrapper fp2, int fp_type2);

  public static native CFloatWrapper powIntegralFp(CFloatWrapper fp, int exp, int fp_type);

  public static native CFloatWrapper sqrtFp(CFloatWrapper fp, int fp_type);

  public static native CFloatWrapper roundFp(CFloatWrapper fp, int fp_type);

  public static native CFloatWrapper truncFp(CFloatWrapper fp, int fp_type);

  public static native CFloatWrapper ceilFp(CFloatWrapper fp, int fp_type);

  public static native CFloatWrapper floorFp(CFloatWrapper fp, int fp_type);

  public static native CFloatWrapper absFp(CFloatWrapper fp, int fp_type);

  public static native boolean isZeroFp(CFloatWrapper fp, int fp_type);

  public static native boolean isOneFp(CFloatWrapper fp, int fp_type);

  public static native boolean isNanFp(CFloatWrapper fp, int fp_type);

  public static native boolean isInfinityFp(CFloatWrapper fp, int fp_type);

  public static native boolean isNegativeFp(CFloatWrapper fp, int fp_type);

  public static native boolean isEqualFp(
      CFloatWrapper fp1, int fp_type1, CFloatWrapper fp2, int fp_type2);

  public static native boolean isNotEqualFp(
      CFloatWrapper fp1, int fp_type1, CFloatWrapper fp2, int fp_type2);

  public static native boolean isGreaterFp(
      CFloatWrapper fp1, int fp_type1, CFloatWrapper fp2, int fp_type2);

  public static native boolean isGreaterEqualFp(
      CFloatWrapper fp1, int fp_type1, CFloatWrapper fp2, int fp_type2);

  public static native boolean isLessFp(
      CFloatWrapper fp1, int fp_type1, CFloatWrapper fp2, int fp_type2);

  public static native boolean isLessEqualFp(
      CFloatWrapper fp1, int fp_type1, CFloatWrapper fp2, int fp_type2);

  public static native int totalOrderFp(
      CFloatWrapper fp1, int fp_type1, CFloatWrapper fp2, int fp_type2);

  public static native CFloatWrapper copySignFp(CFloatWrapper fp1, CFloatWrapper fp2, int fp_type);

  public static native CFloatWrapper castFpFromTo(CFloatWrapper fp, int fp_from_type, int to_type);

  public static native CFloatWrapper castByteToFp(byte value, int to_fp_type);

  public static native CFloatWrapper castShortToFp(short value, int to_fp_type);

  public static native CFloatWrapper castIntToFp(int value, int to_fp_type);

  public static native CFloatWrapper castLongToFp(long value, int to_fp_type);

  public static native byte castFpToByte(CFloatWrapper fp, int fp_from_type);

  public static native short castFpToShort(CFloatWrapper fp, int fp_from_type);

  public static native int castFpToInt(CFloatWrapper fp, int fp_from_type);

  public static native long castFpToLong(CFloatWrapper fp, int fp_from_type);
}
