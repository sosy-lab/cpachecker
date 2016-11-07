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
import java.util.Collection;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;

/**
 * This class provides methods for checking whether a function is a specific builtin
 * for handling floats.
 * The builtin functions of gcc are used as a reference for the provided function names.
 */
public class BuiltinFloatFunctions {

  private static final List<String> INFINITY_FLOAT = of("inff");
  private static final List<String> INFINITY = of("inf");
  private static final List<String> INFINITY_LONG_DOUBLE = of("infl");

  private static final List<String> HUGE_VAL_FLOAT = of("huge_valf");
  private static final List<String> HUGE_VAL = of("huge_val");
  private static final List<String> HUGE_VAL_LONG_DOUBLE = of("huge_vall");

  private static final List<String> NOT_A_NUMBER_FLOAT = of("nanf");
  private static final List<String> NOT_A_NUMBER = of("nan");
  private static final List<String> NOT_A_NUMBER_LONG_DOUBLE = of("nanl");

  private static final List<String> ABSOLUTE_VAL_FLOAT = of("fabsf");
  private static final List<String> ABSOLUTE_VAL = of("fabs");
  private static final List<String> ABSOLUTE_VAL_LONG_DOUBLE = of("fabsl");

  private static final List<String> FLOOR_FLOAT = of("floorf");
  private static final List<String> FLOOR = of("floor");
  private static final List<String> FLOOR_LONG_DOUBLE = of("floorl");

  private static final List<String> CEIL_FLOAT = of("ceilf");
  private static final List<String> CEIL = of("ceil");
  private static final List<String> CEIL_LONG_DOUBLE = of("ceill");

  private static final List<String> ROUND_FLOAT = of("roundf");
  private static final List<String> ROUND = of("round");
  private static final List<String> ROUND_LONG_DOUBLE = of("roundl");

  private static final List<String> TRUNC_FLOAT = of("truncf");
  private static final List<String> TRUNC = of("trunc");
  private static final List<String> TRUNC_LONG_DOUBLE = of("truncl");

  private static final List<String> FDIM_FLOAT = of("fdimf");
  private static final List<String> FDIM = of("fdim");
  private static final List<String> FDIM_LONG_DOUBLE = of("fdiml");

  private static final List<String> FMAX_FLOAT = of("fmaxf");
  private static final List<String> FMAX = of("fmax");
  private static final List<String> FMAX_LONG_DOUBLE = of("fmaxl");

  private static final List<String> FMIN_FLOAT = of("fminf");
  private static final List<String> FMIN = of("fmin");
  private static final List<String> FMIN_LONG_DOUBLE = of("fminl");

  private static final List<String> FMOD_FLOAT = of("fmodf");
  private static final List<String> FMOD = of("fmod");
  private static final List<String> FMOD_LONG_DOUBLE = of("fmodl");

  private static final List<String> MODF_FLOAT = of("modff");
  private static final List<String> MODF = of("modf");
  private static final List<String> MODF_LONG_DOUBLE = of("modfl");

  private static final List<String> FREMAINDER_FLOAT = of("remainderf");
  private static final List<String> FREMAINDER = of("remainder");
  private static final List<String> FREMAINDER_LONG_DOUBLE = of("remainderl");

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
      ImmutableList.<String>builder()
          .addAll(INFINITY)
          .addAll(HUGE_VAL)
          .addAll(ABSOLUTE_VAL)
          .addAll(CEIL)
          .addAll(FLOOR)
          .addAll(ROUND)
          .addAll(TRUNC)
          .addAll(FDIM)
          .addAll(FMAX)
          .addAll(FMIN)
          .addAll(FMOD)
          .addAll(MODF)
          .addAll(FREMAINDER)
          .add(FLOAT_CLASSIFY)
          .add(COPYSIGN)
          .add(SIGNBIT)
          .add(IS_FINITE)
          .add(IS_NAN)
          .add(IS_INFINITY)
          .addAll(NOT_A_NUMBER)
          .build();

  private static ImmutableList<String> of(String suffix) {
    return ImmutableList.of("__builtin_" + suffix, suffix);
  }

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
      Collection<String> pPrefix) {
    return pPrefix.stream()
        .anyMatch(nan -> isBuiltinFloatFunctionWithPrefix(pFunctionName, nan));
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
              "Builtin function '" + pFunctionName + "' with unknown suffix '" + suffix + "'");
        }
      }
    }

    throw new IllegalArgumentException("Invalid function name " + pFunctionName);
  }

  public static boolean matchesInfinityFloat(String pFunctionName) {
    return INFINITY_FLOAT.contains(pFunctionName);
  }

  public static boolean matchesInfinityDouble(String pFunctionName) {
    return INFINITY.contains(pFunctionName);
  }

  public static boolean matchesInfinityLongDouble(String pFunctionName) {
    return INFINITY_LONG_DOUBLE.contains(pFunctionName);
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
    return HUGE_VAL_FLOAT.contains(pFunctionName);
  }

  public static boolean matchesHugeValDouble(String pFunctionName) {
    return HUGE_VAL.contains(pFunctionName);
  }

  public static boolean matchesHugeValLongDouble(String pFunctionName) {
    return HUGE_VAL_LONG_DOUBLE.contains(pFunctionName);
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
    return NOT_A_NUMBER_FLOAT.contains(pFunctionName);
  }

  public static boolean matchesNaNDouble(String pFunctionName) {
    return NOT_A_NUMBER.contains(pFunctionName);
  }

  public static boolean matchesNaNLongDouble(String pFunctionName) {
    return NOT_A_NUMBER_LONG_DOUBLE.contains(pFunctionName);
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
    return ABSOLUTE_VAL_FLOAT.contains(pFunctionName);
  }

  public static boolean matchesAbsoluteDouble(String pFunctionName) {
    return ABSOLUTE_VAL.contains(pFunctionName);
  }

  public static boolean matchesAbsoluteLongDouble(String pFunctionName) {
    return ABSOLUTE_VAL_LONG_DOUBLE.contains(pFunctionName);
  }

  public static boolean matchesCeil(String pFunctionName) {
    return matchesCeilFloat(pFunctionName)
        || matchesCeilDouble(pFunctionName)
        || matchesCeilLongDouble(pFunctionName);
  }

  public static boolean matchesCeilFloat(String pFunctionName) {
    return CEIL_FLOAT.contains(pFunctionName);
  }

  public static boolean matchesCeilDouble(String pFunctionName) {
    return CEIL.contains(pFunctionName);
  }

  public static boolean matchesCeilLongDouble(String pFunctionName) {
    return CEIL_LONG_DOUBLE.contains(pFunctionName);
  }

  public static boolean matchesFloor(String pFunctionName) {
    return matchesFloorFloat(pFunctionName)
        || matchesFloorDouble(pFunctionName)
        || matchesFloorLongDouble(pFunctionName);
  }

  public static boolean matchesFloorFloat(String pFunctionName) {
    return FLOOR_FLOAT.contains(pFunctionName);
  }

  public static boolean matchesFloorDouble(String pFunctionName) {
    return FLOOR.contains(pFunctionName);
  }

  public static boolean matchesFloorLongDouble(String pFunctionName) {
    return FLOOR_LONG_DOUBLE.contains(pFunctionName);
  }

  public static boolean matchesRound(String pFunctionName) {
    return matchesRoundFloat(pFunctionName)
        || matchesRoundDouble(pFunctionName)
        || matchesRoundLongDouble(pFunctionName);
  }

  public static boolean matchesRoundFloat(String pFunctionName) {
    return ROUND_FLOAT.contains(pFunctionName);
  }

  public static boolean matchesRoundDouble(String pFunctionName) {
    return ROUND.contains(pFunctionName);
  }

  public static boolean matchesRoundLongDouble(String pFunctionName) {
    return ROUND_LONG_DOUBLE.contains(pFunctionName);
  }

  public static boolean matchesTrunc(String pFunctionName) {
    return matchesTruncFloat(pFunctionName)
        || matchesTruncDouble(pFunctionName)
        || matchesTruncLongDouble(pFunctionName);
  }

  public static boolean matchesTruncFloat(String pFunctionName) {
    return TRUNC_FLOAT.contains(pFunctionName);
  }

  public static boolean matchesTruncDouble(String pFunctionName) {
    return TRUNC.contains(pFunctionName);
  }

  public static boolean matchesTruncLongDouble(String pFunctionName) {
    return TRUNC_LONG_DOUBLE.contains(pFunctionName);
  }

  public static boolean matchesFdim(String pFunctionName) {
    return matchesFdimFloat(pFunctionName)
        || matchesFdimDouble(pFunctionName)
        || matchesFdimLongDouble(pFunctionName);
  }

  public static boolean matchesFdimFloat(String pFunctionName) {
    return FDIM_FLOAT.contains(pFunctionName);
  }

  public static boolean matchesFdimDouble(String pFunctionName) {
    return FDIM.contains(pFunctionName);
  }

  public static boolean matchesFdimLongDouble(String pFunctionName) {
    return FDIM_LONG_DOUBLE.contains(pFunctionName);
  }

  public static boolean matchesFmax(String pFunctionName) {
    return matchesFmaxFloat(pFunctionName)
        || matchesFmaxDouble(pFunctionName)
        || matchesFmaxLongDouble(pFunctionName);
  }

  public static boolean matchesFmaxFloat(String pFunctionName) {
    return FMAX_FLOAT.contains(pFunctionName);
  }

  public static boolean matchesFmaxDouble(String pFunctionName) {
    return FMAX.contains(pFunctionName);
  }

  public static boolean matchesFmaxLongDouble(String pFunctionName) {
    return FMAX_LONG_DOUBLE.contains(pFunctionName);
  }

  public static boolean matchesFmin(String pFunctionName) {
    return matchesFminFloat(pFunctionName)
        || matchesFminDouble(pFunctionName)
        || matchesFminLongDouble(pFunctionName);
  }

  public static boolean matchesFminFloat(String pFunctionName) {
    return FMIN_FLOAT.contains(pFunctionName);
  }

  public static boolean matchesFminDouble(String pFunctionName) {
    return FMIN.contains(pFunctionName);
  }

  public static boolean matchesFminLongDouble(String pFunctionName) {
    return FMIN_LONG_DOUBLE.contains(pFunctionName);
  }

  public static boolean matchesFmod(String pFunctionName) {
    return matchesFmodFloat(pFunctionName)
        || matchesFmodDouble(pFunctionName)
        || matchesFmodLongDouble(pFunctionName);
  }

  public static boolean matchesFmodFloat(String pFunctionName) {
    return FMOD_FLOAT.contains(pFunctionName);
  }

  public static boolean matchesFmodDouble(String pFunctionName) {
    return FMOD.contains(pFunctionName);
  }

  public static boolean matchesFmodLongDouble(String pFunctionName) {
    return FMOD_LONG_DOUBLE.contains(pFunctionName);
  }

  public static boolean matchesFmodf(String pFunctionName) {
    return matchesModfFloat(pFunctionName)
        || matchesModfDouble(pFunctionName)
        || matchesModfLongDouble(pFunctionName);
  }

  public static boolean matchesModfFloat(String pFunctionName) {
    return MODF_FLOAT.contains(pFunctionName);
  }

  public static boolean matchesModfDouble(String pFunctionName) {
    return MODF.contains(pFunctionName);
  }

  public static boolean matchesModfLongDouble(String pFunctionName) {
    return MODF_LONG_DOUBLE.contains(pFunctionName);
  }

  public static boolean matchesFremainder(String pFunctionName) {
    return matchesFremainderFloat(pFunctionName)
        || matchesFremainderDouble(pFunctionName)
        || matchesFremainderLongDouble(pFunctionName);
  }

  public static boolean matchesFremainderFloat(String pFunctionName) {
    return FREMAINDER_FLOAT.contains(pFunctionName);
  }

  public static boolean matchesFremainderDouble(String pFunctionName) {
    return FREMAINDER.contains(pFunctionName);
  }

  public static boolean matchesFremainderLongDouble(String pFunctionName) {
    return FREMAINDER_LONG_DOUBLE.contains(pFunctionName);
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
