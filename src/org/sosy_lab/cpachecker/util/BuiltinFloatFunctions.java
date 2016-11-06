/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.util;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;

/**
 * This class provides methods for checking whether a function is a specific builtin
 * for handling floats.
 * The builtin functions of gcc are used as a reference for the provided function names.
 */
public class BuiltinFloatFunctions {

  private static final String INFINITY_FLOAT = "__builtin_inff";
  private static final String HUGE_VAL_FLOAT = "__builtin_huge_valf";
  private static final String INFINITY = "__builtin_inf";
  private static final String HUGE_VAL = "__builtin_huge_val";
  private static final String INFINITY_LONG_DOUBLE = "__builtin_infl";
  private static final String HUGE_VAL_LONG_DOUBLE = "__builtin_huge_vall";

  private static final String NOT_A_NUMBER_FLOAT = "__builtin_nanf";
  private static final String NOT_A_NUMBER = "__builtin_nan";
  private static final String NOT_A_NUMBER_LONG_DOUBLE = "__builtin_nanl";

  private static final String ABSOLUTE_VAL_FLOAT  = "__builtin_fabsf";
  private static final String ABSOLUTE_VAL = "__builtin_fabs";
  private static final String ABSOLUTE_VAL_LONG_DOUBLE = "__builtin_fabsl";

  private static final String FLOOR_FLOAT = "floorf";
  private static final String FLOOR = "floor";
  private static final String FLOOR_LONG_DOUBLE = "floorl";

  private static final String CEIL_FLOAT = "ceilf";
  private static final String CEIL = "ceil";
  private static final String CEIL_LONG_DOUBLE = "ceill";

  private static final String SIGNBIT_FLOAT = "__signbitf";
  private static final String SIGNBIT = "__signbit";
  private static final String SIGNBIT_LONG_DOUBLE = "__signbitl";

  private static final String COPYSIGN_FLOAT = "copysignf";
  private static final String COPYSIGN = "copysign";
  private static final String COPYSIGN_LONG_DOUBLE = "copysignl";

  private static final String FLOAT_CLASSIFY = "__fpclassify";
  private static final String IS_FINITE = "__finite";
  private static final String IS_NAN = "__isnan";
  private static final String IS_INFINITY = "__isinf";

  private static final ImmutableList<String> possiblePrefixes =
      ImmutableList.of(
          INFINITY,
          HUGE_VAL,
          NOT_A_NUMBER,
          ABSOLUTE_VAL,
          CEIL,
          FLOOR,
          FLOAT_CLASSIFY,
          COPYSIGN,
          SIGNBIT,
          IS_FINITE,
          IS_NAN,
          IS_INFINITY);

  /**
   * Check whether a given function is a builtin function specific to floats
   * that can be further analyzed with this class.
   */
  public static boolean isBuiltinFloatFunction(String pFunctionName) {
    for (String prefix : possiblePrefixes) {
      if (isBuiltinFloatFunctionWithPrefix(pFunctionName, prefix)) {
        return true;
      }
    }

    return false;
  }

  private static boolean isBuiltinFloatFunctionWithPrefix(String pFunctionName,
      String pPrefix) {
    int length = pFunctionName.length();
    int prefixLength = pPrefix.length();
    if ((length != prefixLength) && (length != prefixLength+1)) {
      return false;
    }
    if (!pFunctionName.startsWith(pPrefix)) {
      return false;
    }
    String suffix = pFunctionName.substring(prefixLength);
    return suffix.isEmpty()
        || suffix.equals("f")
        || suffix.equals("l");
  }

  /**
   * Get the type of a builtin float function. This could be the return type or a parameter type.
   * @param pFunctionName A function name for which {@link #isBuiltinFloatFunction(String)} returns true.
   * @throws IllegalArgumentException For unhandled functions.
   */
  public static CSimpleType getTypeOfBuiltinFloatFunction(String pFunctionName) {
    for (String p : possiblePrefixes) {
      if (pFunctionName.startsWith(p)) {
        String suffix = pFunctionName.substring(p.length());

        switch (suffix) {
        case "":
          return CNumericTypes.DOUBLE;
        case "f":
          return CNumericTypes.FLOAT;
        case "l":
          return CNumericTypes.LONG_DOUBLE;
        default:
          throw new IllegalArgumentException(
              "Builtin function " + pFunctionName + " with unknown suffix");
        }
      }
    }

    throw new IllegalArgumentException("Invalid function name " + pFunctionName);
  }

  public static boolean matchesInfinityFloat(String pFunctionName) {
    return INFINITY_FLOAT.equals(pFunctionName);
  }

  public static boolean matchesInfinityDouble(String pFunctionName) {
    return INFINITY.equals(pFunctionName);
  }

  public static boolean matchesInfinityLongDouble(String pFunctionName) {
    return INFINITY_LONG_DOUBLE.equals(pFunctionName);
  }

  /**
   * Returns whether the given function name is any builtin infinity-function.
   *
   * @param pFunctionName the function name to check
   * @return <code>true</code> if the given function name is any builtin infinity-function,
   *   <code>false</code> otherwise
   */
  public static boolean matchesInfinity(String pFunctionName) {
    return isBuiltinFloatFunctionWithPrefix(pFunctionName, INFINITY);
  }

  public static boolean matchesHugeValFloat(String pFunctionName) {
    return HUGE_VAL_FLOAT.equals(pFunctionName);
  }

  public static boolean matchesHugeValDouble(String pFunctionName) {
    return HUGE_VAL.equals(pFunctionName);
  }

  public static boolean matchesHugeValLongDouble(String pFunctionName) {
    return HUGE_VAL_LONG_DOUBLE.equals(pFunctionName);
  }

  /**
   * Returns whether the given function name is any builtin huge_val-function.
   *
   * @param pFunctionName the function name to check
   * @return <code>true</code> if the given function name is any builtin huge_val-function,
   *   <code>false</code> otherwise
   */
  public static boolean matchesHugeVal(String pFunctionName) {
    return isBuiltinFloatFunctionWithPrefix(pFunctionName, HUGE_VAL);
  }

  public static boolean matchesNaNFloat(String pFunctionName) {
    return NOT_A_NUMBER_FLOAT.equals(pFunctionName);
  }

  public static boolean matchesNaNDouble(String pFunctionName) {
    return NOT_A_NUMBER.equals(pFunctionName);
  }

  public static boolean matchesNaNLongDouble(String pFunctionName) {
    return NOT_A_NUMBER_LONG_DOUBLE.equals(pFunctionName);
  }

  /**
   * Returns whether the given function name is any builtin NaN-function.
   *
   * @param pFunctionName the function name to check
   * @return <code>true</code> if the given function name is any builtin NaN-function,
   *   <code>false</code> otherwise
   */
  public static boolean matchesNaN(String pFunctionName) {
    return isBuiltinFloatFunctionWithPrefix(pFunctionName, NOT_A_NUMBER);
  }

  public static boolean matchesAbsoluteFloat(String pFunctionName) {
    return ABSOLUTE_VAL_FLOAT.equals(pFunctionName);
  }

  public static boolean matchesAbsoluteDouble(String pFunctionName) {
    return ABSOLUTE_VAL.equals(pFunctionName);
  }

  public static boolean matchesAbsoluteLongDouble(String pFunctionName) {
    return ABSOLUTE_VAL_LONG_DOUBLE.equals(pFunctionName);
  }

  public static boolean matchesCeil(String pFunctionName) {
    return matchesCeilFloat(pFunctionName)
        || matchesCeilDouble(pFunctionName)
        || matchesCeilLongDouble(pFunctionName);
  }

  public static boolean matchesCeilFloat(String pFunctionName) {
    return CEIL_FLOAT.equals(pFunctionName);
  }

  public static boolean matchesCeilDouble(String pFunctionName) {
    return CEIL.equals(pFunctionName);
  }

  public static boolean matchesCeilLongDouble(String pFunctionName) {
    return CEIL_LONG_DOUBLE.equals(pFunctionName);
  }

  public static boolean matchesFloor(String pFunctionName) {
    return matchesFloorFloat(pFunctionName)
        || matchesFloorDouble(pFunctionName)
        || matchesFloorLongDouble(pFunctionName);
  }

  public static boolean matchesFloorFloat(String pFunctionName) {
    return FLOOR_FLOAT.equals(pFunctionName);
  }

  public static boolean matchesFloorDouble(String pFunctionName) {
    return FLOOR.equals(pFunctionName);
  }

  public static boolean matchesFloorLongDouble(String pFunctionName) {
    return FLOOR_LONG_DOUBLE.equals(pFunctionName);
  }

  public static boolean matchesSignbit(String pFunctionName) {
    return matchesSignbitFloat(pFunctionName)
        || matchesSignbitDouble(pFunctionName)
        || matchesSignbitLongDouble(pFunctionName);
  }

  public static boolean matchesSignbitFloat(String pFunctionName) {
    return SIGNBIT_FLOAT.equals(pFunctionName);
  }

  public static boolean matchesSignbitDouble(String pFunctionName) {
    return SIGNBIT.equals(pFunctionName);
  }

  public static boolean matchesSignbitLongDouble(String pFunctionName) {
    return SIGNBIT_LONG_DOUBLE.equals(pFunctionName);
  }

  public static boolean matchesCopysign(String pFunctionName) {
    return matchesCopysignFloat(pFunctionName)
        || matchesCopysignDouble(pFunctionName)
        || matchesCopysignLongDouble(pFunctionName);
  }

  public static boolean matchesCopysignFloat(String pFunctionName) {
    return COPYSIGN_FLOAT.equals(pFunctionName);
  }

  public static boolean matchesCopysignDouble(String pFunctionName) {
    return COPYSIGN.equals(pFunctionName);
  }

  public static boolean matchesCopysignLongDouble(String pFunctionName) {
    return COPYSIGN_LONG_DOUBLE.equals(pFunctionName);
  }

  /**
   * Returns whether the given function name is any builtin absolute-function.
   *
   * @param pFunctionName the function name to check
   * @return <code>true</code> if the given function name is any builtin absolute-function,
   *   <code>false</code> otherwise
   */
  public static boolean matchesAbsolute(String pFunctionName) {
    return isBuiltinFloatFunctionWithPrefix(pFunctionName, ABSOLUTE_VAL);
  }

  /**
   * Returns whether the given function name is any builtin fpclassify-function.
   *
   * @param pFunctionName the function name to check
   * @return <code>true</code> if the given function name is any builtin fpclassify-function,
   *   <code>false</code> otherwise
   */
  public static boolean matchesFloatClassify(String pFunctionName) {
    return isBuiltinFloatFunctionWithPrefix(pFunctionName, FLOAT_CLASSIFY);
  }

  /**
   * Returns whether the given function name is any builtin function
   * that checks whether a float is finite.
   *
   * @param pFunctionName the function name to check
   * @return <code>true</code> if the given function name is any builtin finite-function,
   *   <code>false</code> otherwise
   */
  public static boolean matchesFinite(String pFunctionName) {
    return isBuiltinFloatFunctionWithPrefix(pFunctionName, IS_FINITE);
  }

  /**
   * Returns whether the given function name is any builtin function
   * that checks whether a float is NaN.
   *
   * @param pFunctionName the function name to check
   * @return <code>true</code> if the given function name is any builtin isnan-function,
   *   <code>false</code> otherwise
   */
  public static boolean matchesIsNaN(String pFunctionName) {
    return isBuiltinFloatFunctionWithPrefix(pFunctionName, IS_NAN);
  }

  /**
   * Returns whether the given function name is any builtin function
   * that checks whether a float is infinite.
   *
   * @param pFunctionName the function name to check
   * @return <code>true</code> if the given function name is any builtin isinf-function,
   *   <code>false</code> otherwise
   */
  public static boolean matchesIsInfinity(String pFunctionName) {
    return isBuiltinFloatFunctionWithPrefix(pFunctionName, IS_INFINITY);
  }

}
