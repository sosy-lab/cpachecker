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

import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;

import com.google.common.collect.ImmutableList;

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

  private static final String FLOAT_CLASSIFY = "__fpclassify";

  private static final ImmutableList<String> possiblePrefixes = ImmutableList.of(
      INFINITY, HUGE_VAL, NOT_A_NUMBER, ABSOLUTE_VAL, FLOAT_CLASSIFY);

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
   * Get the return type of a builtin float function.
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

  public static boolean isInfinityFloat(String pFunctionName) {
    return INFINITY_FLOAT.equals(pFunctionName);
  }

  public static boolean isInfinityDouble(String pFunctionName) {
    return INFINITY.equals(pFunctionName);
  }

  public static boolean isInfinityLongDouble(String pFunctionName) {
    return INFINITY_LONG_DOUBLE.equals(pFunctionName);
  }

  /**
   * Returns whether the given function name is any builtin infinity-function.
   *
   * @param pFunctionName the function name to check
   * @return <code>true</code> if the given function name is any builtin infinity-function,
   *   <code>false</code> otherwise
   */
  public static boolean isInfinity(String pFunctionName) {
    return isBuiltinFloatFunctionWithPrefix(pFunctionName, INFINITY);
  }

  public static boolean isHugeValFloat(String pFunctionName) {
    return HUGE_VAL_FLOAT.equals(pFunctionName);
  }

  public static boolean isHugeValDouble(String pFunctionName) {
    return HUGE_VAL.equals(pFunctionName);
  }

  public static boolean isHugeValLongDouble(String pFunctionName) {
    return HUGE_VAL_LONG_DOUBLE.equals(pFunctionName);
  }

  /**
   * Returns whether the given function name is any builtin huge_val-function.
   *
   * @param pFunctionName the function name to check
   * @return <code>true</code> if the given function name is any builtin huge_val-function,
   *   <code>false</code> otherwise
   */
  public static boolean isHugeVal(String pFunctionName) {
    return isBuiltinFloatFunctionWithPrefix(pFunctionName, HUGE_VAL);
  }

  public static boolean isNaNFloat(String pFunctionName) {
    return NOT_A_NUMBER_FLOAT.equals(pFunctionName);
  }

  public static boolean isNaNDouble(String pFunctionName) {
    return NOT_A_NUMBER.equals(pFunctionName);
  }

  public static boolean isNaNLongDouble(String pFunctionName) {
    return NOT_A_NUMBER_LONG_DOUBLE.equals(pFunctionName);
  }

  /**
   * Returns whether the given function name is any builtin NaN-function.
   *
   * @param pFunctionName the function name to check
   * @return <code>true</code> if the given function name is any builtin NaN-function,
   *   <code>false</code> otherwise
   */
  public static boolean isNaN(String pFunctionName) {
    return isBuiltinFloatFunctionWithPrefix(pFunctionName, NOT_A_NUMBER);
  }

  public static boolean isAbsoluteFloat(String pFunctionName) {
    return ABSOLUTE_VAL_FLOAT.equals(pFunctionName);
  }

  public static boolean isAbsoluteDouble(String pFunctionName) {
    return ABSOLUTE_VAL.equals(pFunctionName);
  }

  public static boolean isAbsoluteLongDouble(String pFunctionName) {
    return ABSOLUTE_VAL_LONG_DOUBLE.equals(pFunctionName);
  }

  /**
   * Returns whether the given function name is any builtin absolute-function.
   *
   * @param pFunctionName the function name to check
   * @return <code>true</code> if the given function name is any builtin absolute-function,
   *   <code>false</code> otherwise
   */
  public static boolean isAbsolute(String pFunctionName) {
    return isBuiltinFloatFunctionWithPrefix(pFunctionName, ABSOLUTE_VAL);
  }

  /**
   * Returns whether the given function name is any builtin fpclassify-function.
   *
   * @param pFunctionName the function name to check
   * @return <code>true</code> if the given function name is any builtin fpclassify-function,
   *   <code>false</code> otherwise
   */
  public static boolean isFloatClassify(String pFunctionName) {
    return isBuiltinFloatFunctionWithPrefix(pFunctionName, FLOAT_CLASSIFY);
  }
}
