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

import org.sosy_lab.common.NativeLibraries;

class CFloatNativeAPI {
  private CFloatNativeAPI() {}

  static {
    NativeLibraries.loadLibrary("FloatingPoints");
  }

  public static final CFloatWrapper ZERO_SINGLE;
  public static final CFloatWrapper ONE_SINGLE;
  public static final CFloatWrapper TEN_SINGLE;

  public static final CFloatWrapper ZERO_DOUBLE;
  public static final CFloatWrapper ONE_DOUBLE;
  public static final CFloatWrapper TEN_DOUBLE;

  public static final CFloatWrapper ZERO_LONG_DOUBLE;
  public static final CFloatWrapper ONE_LONG_DOUBLE;
  public static final CFloatWrapper TEN_LONG_DOUBLE;

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
    ZERO_SINGLE = createFp("0.0", FP_TYPE_SINGLE);
    ONE_SINGLE = createFp("1.0", FP_TYPE_SINGLE);
    TEN_SINGLE = createFp("10.0", FP_TYPE_SINGLE);

    ZERO_DOUBLE = createFp("0.0", FP_TYPE_DOUBLE);
    ONE_DOUBLE = createFp("1.0", FP_TYPE_DOUBLE);
    TEN_DOUBLE = createFp("10.0", FP_TYPE_DOUBLE);

    ZERO_LONG_DOUBLE = createFp("0.0", FP_TYPE_LONG_DOUBLE);
    ONE_LONG_DOUBLE = createFp("1.0", FP_TYPE_LONG_DOUBLE);
    TEN_LONG_DOUBLE = createFp("10.0", FP_TYPE_LONG_DOUBLE);
  }

  public static native CFloatWrapper createFp(String stringRepresentation, int fp_type);

  public static native String printFp(CFloatWrapper fp, int fp_type);

  public static native CFloatWrapper
      addFp(CFloatWrapper fp1, int fp_type1, CFloatWrapper fp2, int fp_type2);

  public static native CFloatWrapper
      subtractFp(CFloatWrapper fp1, int fp_type1, CFloatWrapper fp2, int fp_type2);

  public static native CFloatWrapper
      multiplyFp(CFloatWrapper fp1, int fp_type1, CFloatWrapper fp2, int fp_type2);

  public static native CFloatWrapper
      divideFp(CFloatWrapper fp1, int fp_type1, CFloatWrapper fp2, int fp_type2);

  public static native CFloatWrapper
      addManyFp(CFloatWrapper fp1, int[] fp_types, CFloatWrapper... fps);

  public static native CFloatWrapper
      multiplyManyFp(CFloatWrapper fp1, int[] fp_types, CFloatWrapper... fps);

  public static native CFloatWrapper
      powFp(CFloatWrapper fp1, int fp_type1, CFloatWrapper fp2, int fp_type2);

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
