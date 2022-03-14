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
public class CFloatNativeAPI {
  private CFloatNativeAPI() {}

  static {
    NativeLibraries.loadLibrary("FloatingPoints");
  }

  public enum CNativeType {
    SINGLE(0),
    DOUBLE(1),
    LONG_DOUBLE(2),
    CHAR(3),
    SHORT(4),
    INT(5),
    LONG(6),
    LONG_LONG(7),
    UCHAR(8),
    USHORT(9),
    UINT(10),
    ULONG(11),
    ULONG_LONG(12);

    private final int ordinal;

    CNativeType(final int pOrdinal) {
      ordinal = pOrdinal;
    }

    public int getOrdinal() {
      return ordinal;
    }
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

  public static final int FP_TYPE_SINGLE = 0;
  public static final int FP_TYPE_DOUBLE = 1;
  public static final int FP_TYPE_LONG_DOUBLE = 2;

  public static final int TYPE_CHAR = 3;
  public static final int TYPE_SHORT = 4;
  public static final int TYPE_INT = 5;
  public static final int TYPE_LONG = 6;
  public static final int TYPE_LONG_LONG = 7;
  public static final int TYPE_UCHAR = 8;
  public static final int TYPE_USHORT = 9;
  public static final int TYPE_UINT = 10;
  public static final int TYPE_ULONG = 11;
  public static final int TYPE_ULONG_LONG = 12;

  static {
    ZERO_SINGLE = new CFloatImpl(createFp("0.0", FP_TYPE_SINGLE), FP_TYPE_SINGLE);
    ONE_SINGLE = new CFloatImpl(createFp("1.0", FP_TYPE_SINGLE), FP_TYPE_SINGLE);
    TEN_SINGLE = new CFloatImpl(createFp("10.0", FP_TYPE_SINGLE), FP_TYPE_SINGLE);

    ZERO_DOUBLE = new CFloatImpl(createFp("0.0", FP_TYPE_DOUBLE), FP_TYPE_DOUBLE);
    ONE_DOUBLE = new CFloatImpl(createFp("1.0", FP_TYPE_DOUBLE), FP_TYPE_DOUBLE);
    TEN_DOUBLE = new CFloatImpl(createFp("10.0", FP_TYPE_DOUBLE), FP_TYPE_DOUBLE);

    ZERO_LONG_DOUBLE = new CFloatImpl(createFp("0.0", FP_TYPE_LONG_DOUBLE), FP_TYPE_LONG_DOUBLE);
    ONE_LONG_DOUBLE = new CFloatImpl(createFp("1.0", FP_TYPE_LONG_DOUBLE), FP_TYPE_LONG_DOUBLE);
    TEN_LONG_DOUBLE = new CFloatImpl(createFp("10.0", FP_TYPE_LONG_DOUBLE), FP_TYPE_LONG_DOUBLE);
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

  public static native CFloatWrapper addManyFp(
      CFloatWrapper fp1, int[] fp_types, CFloatWrapper... fps);

  public static native CFloatWrapper multiplyManyFp(
      CFloatWrapper fp1, int[] fp_types, CFloatWrapper... fps);

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

  public static native CFloatWrapper copySignFp(CFloatWrapper fp1, CFloatWrapper fp2, int fp_type);

  public static native CFloatWrapper castFpFromTo(CFloatWrapper fp, int fp_from_type, int to_type);

  public static native CFloatWrapper castOtherToFp(Number value, int from_type, int to_fp_type);

  public static native Number castFpToOther(CFloatWrapper fp, int fp_from_type, int to_type);
}
