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

public class CFloatUtil {

  private CFloatUtil() {
    // do not instantiate
  }

  public static long getSignBitMask(int pType) {
    long signBit = 0L;
    switch (pType) {
      case CFloatNativeAPI.FP_TYPE_SINGLE:
        signBit = 0b00000000_00000000_00000000_00000000_00000000_00000000_00000001_00000000L;
        break;
      case CFloatNativeAPI.FP_TYPE_DOUBLE:
        signBit = 0b00000000_00000000_00000000_00000000_00000000_00000000_00001000_00000000L;
        break;
      case CFloatNativeAPI.FP_TYPE_LONG_DOUBLE:
        signBit = 0b00000000_00000000_00000000_00000000_00000000_00000000_10000000_00000000L;
        break;
      default:
        throw new RuntimeException("Unimplemented floating point type: " + pType);
    }

    return signBit;
  }

  public static long getExponentMask(int pType) {
    long exp = 0L;
    switch (pType) {
      case CFloatNativeAPI.FP_TYPE_SINGLE:
        exp = 0b00000000_00000000_00000000_00000000_00000000_00000000_00000000_11111111L;
        break;
      case CFloatNativeAPI.FP_TYPE_DOUBLE:
        exp = 0b00000000_00000000_00000000_00000000_00000000_00000000_00000111_11111111L;
        break;
      case CFloatNativeAPI.FP_TYPE_LONG_DOUBLE:
        exp = 0b00000000_00000000_00000000_00000000_00000000_00000000_01111111_11111111L;
        break;
      default:
        throw new RuntimeException("Unimplemented floating point type: " + pType);
    }

    return exp;
  }

  public static long getMantissaMask(int pType) {
    long man = 0L;
    switch (pType) {
      case CFloatNativeAPI.FP_TYPE_SINGLE:
        man = 0b00000000_00000000_00000000_00000000_00000000_01111111_11111111_11111111L;
        break;
      case CFloatNativeAPI.FP_TYPE_DOUBLE:
        man = 0b00000000_00001111_11111111_11111111_11111111_11111111_11111111_11111111L;
        break;
      case CFloatNativeAPI.FP_TYPE_LONG_DOUBLE:
        man = 0b01111111_11111111_11111111_11111111_11111111_11111111_11111111_11111111L;
        break;
      default:
        throw new RuntimeException("Unimplemented floating point type: " + pType);
    }

    return man;
  }

  public static long getNormalizationMask(int pType) {
    long oneBit = 0L;
    switch (pType) {
      case CFloatNativeAPI.FP_TYPE_SINGLE:
        oneBit = 0b00000000_00000000_00000000_00000000_00000000_10000000_00000000_00000000L;
        break;
      case CFloatNativeAPI.FP_TYPE_DOUBLE:
        oneBit = 0b00000000_00010000_00000000_00000000_00000000_00000000_00000000_00000000L;
        break;
      case CFloatNativeAPI.FP_TYPE_LONG_DOUBLE:
        oneBit = 0b10000000_00000000_00000000_00000000_00000000_00000000_00000000_00000000L;
        break;
      default:
        throw new RuntimeException("Unimplemented floating point type: " + pType);
    }

    return oneBit;
  }

  public static long getNormalizedMantissaMask(int pType) {
    long man = 0L;
    switch (pType) {
      case CFloatNativeAPI.FP_TYPE_SINGLE:
      case CFloatNativeAPI.FP_TYPE_DOUBLE:
        return getMantissaMask(pType);
      case CFloatNativeAPI.FP_TYPE_LONG_DOUBLE:
        man = 0b11111111_11111111_11111111_11111111_11111111_11111111_11111111_11111111L;
        break;
      default:
        throw new RuntimeException("Unimplemented floating point type: " + pType);
    }

    return man;
  }

  public static long getBias(int pType) {
    long bias = 0L;
    switch (pType) {
      case CFloatNativeAPI.FP_TYPE_SINGLE:
      case CFloatNativeAPI.FP_TYPE_DOUBLE:
      case CFloatNativeAPI.FP_TYPE_LONG_DOUBLE:
        bias = getExponentMask(pType) / 2;
        break;
      default:
        throw new RuntimeException("Unimplemented floating point type: " + pType);
    }

    return bias;
  }

  public static int getMantissaLength(int pType) {
    int res = -1;

    switch (pType) {
      case CFloatNativeAPI.FP_TYPE_SINGLE:
        res = 23;
        break;
      case CFloatNativeAPI.FP_TYPE_DOUBLE:
        res = 52;
        break;
      case CFloatNativeAPI.FP_TYPE_LONG_DOUBLE:
        res = 64;
        break;
      default:
        throw new IllegalArgumentException("Unimplemented floating point type: " + pType);
    }

    return res;
  }

  public static int getExponentLength(int pType) {
    int res = -1;

    switch (pType) {
      case CFloatNativeAPI.FP_TYPE_SINGLE:
        res = 8;
        break;
      case CFloatNativeAPI.FP_TYPE_DOUBLE:
        res = 11;
        break;
      case CFloatNativeAPI.FP_TYPE_LONG_DOUBLE:
        res = 15;
        break;
      default:
        throw new IllegalArgumentException("Unimplemented floating point type: " + pType);
    }

    return res;
  }
}
