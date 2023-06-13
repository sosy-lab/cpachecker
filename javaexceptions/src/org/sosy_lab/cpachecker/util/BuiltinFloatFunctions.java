// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

/**
 * This class provides methods for checking whether a function is a specific builtin for handling
 * floats. The builtin functions of gcc are used as a reference for the provided function names.
 */
public class BuiltinFloatFunctions {

  private static final ImmutableList<String> INFINITY_FLOAT = of("inff");
  private static final ImmutableList<String> INFINITY = of("inf");
  private static final ImmutableList<String> INFINITY_LONG_DOUBLE = of("infl");

  private static final ImmutableList<String> HUGE_VAL_FLOAT = of("huge_valf");
  private static final ImmutableList<String> HUGE_VAL = of("huge_val");
  private static final ImmutableList<String> HUGE_VAL_LONG_DOUBLE = of("huge_vall");

  private static final ImmutableList<String> NOT_A_NUMBER_FLOAT = of("nanf");
  private static final ImmutableList<String> NOT_A_NUMBER = of("nan");
  private static final ImmutableList<String> NOT_A_NUMBER_LONG_DOUBLE = of("nanl");

  private static final ImmutableList<String> ABSOLUTE_VAL_FLOAT = of("fabsf");
  private static final ImmutableList<String> ABSOLUTE_VAL = of("fabs");
  private static final ImmutableList<String> ABSOLUTE_VAL_LONG_DOUBLE = of("fabsl");

  private static final ImmutableList<String> FLOOR_FLOAT = of("floorf");
  private static final ImmutableList<String> FLOOR = of("floor");
  private static final ImmutableList<String> FLOOR_LONG_DOUBLE = of("floorl");

  private static final ImmutableList<String> CEIL_FLOAT = of("ceilf");
  private static final ImmutableList<String> CEIL = of("ceil");
  private static final ImmutableList<String> CEIL_LONG_DOUBLE = of("ceill");

  private static final ImmutableList<String> ROUND_FLOAT = of("roundf");
  private static final ImmutableList<String> ROUND = of("round");
  private static final ImmutableList<String> ROUND_LONG_DOUBLE = of("roundl");

  private static final ImmutableList<String> LROUND_FLOAT = of("lroundf");
  private static final ImmutableList<String> LROUND = of("lround");
  private static final ImmutableList<String> LROUND_LONG_DOUBLE = of("lroundl");

  private static final ImmutableList<String> LLROUND_FLOAT = of("llroundf");
  private static final ImmutableList<String> LLROUND = of("llround");
  private static final ImmutableList<String> LLROUND_LONG_DOUBLE = of("llroundl");

  private static final ImmutableList<String> TRUNC_FLOAT = of("truncf");
  private static final ImmutableList<String> TRUNC = of("trunc");
  private static final ImmutableList<String> TRUNC_LONG_DOUBLE = of("truncl");

  private static final ImmutableList<String> FDIM_FLOAT = of("fdimf");
  private static final ImmutableList<String> FDIM = of("fdim");
  private static final ImmutableList<String> FDIM_LONG_DOUBLE = of("fdiml");

  private static final ImmutableList<String> FMAX_FLOAT = of("fmaxf");
  private static final ImmutableList<String> FMAX = of("fmax");
  private static final ImmutableList<String> FMAX_LONG_DOUBLE = of("fmaxl");

  private static final ImmutableList<String> FMIN_FLOAT = of("fminf");
  private static final ImmutableList<String> FMIN = of("fmin");
  private static final ImmutableList<String> FMIN_LONG_DOUBLE = of("fminl");

  private static final ImmutableList<String> FMOD_FLOAT = of("fmodf");
  private static final ImmutableList<String> FMOD = of("fmod");
  private static final ImmutableList<String> FMOD_LONG_DOUBLE = of("fmodl");

  private static final ImmutableList<String> MODF_FLOAT = of("modff");
  private static final ImmutableList<String> MODF = of("modf");
  private static final ImmutableList<String> MODF_LONG_DOUBLE = of("modfl");

  private static final ImmutableList<String> FREMAINDER_FLOAT = of("remainderf");
  private static final ImmutableList<String> FREMAINDER = of("remainder");
  private static final ImmutableList<String> FREMAINDER_LONG_DOUBLE = of("remainderl");

  private static final ImmutableList<String> ISGREATER = of("isgreater");
  private static final ImmutableList<String> ISGREATEREQUAL = of("isgreaterequal");
  private static final ImmutableList<String> ISLESS = of("isless");
  private static final ImmutableList<String> ISLESSEQUAL = of("islessequal");
  private static final ImmutableList<String> ISLESSGREATER = of("islessgreater");
  private static final ImmutableList<String> ISUNORDERED = of("isunordered");

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
          .addAll(LROUND)
          .addAll(LLROUND)
          .addAll(TRUNC)
          .addAll(FDIM)
          .addAll(FMAX)
          .addAll(FMIN)
          .addAll(FMOD)
          .addAll(MODF)
          .addAll(FREMAINDER)

          // compare-functions are ordered backwards, such that the prefix-search works.
          .addAll(ISGREATEREQUAL)
          .addAll(ISGREATER)
          .addAll(ISLESSGREATER)
          .addAll(ISLESSEQUAL)
          .addAll(ISLESS)
          .addAll(ISUNORDERED)
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
   * Check whether a given function is a builtin function specific to floats that can be further
   * analyzed with this class.
   */
  public static boolean isBuiltinFloatFunction(String pFunctionName) {
    for (String prefix : possiblePrefixes) {
      if (isBuiltinFloatFunctionWithPrefix(pFunctionName, prefix)) {
        return true;
      }
    }

    return false;
  }

  private static boolean isBuiltinFloatFunctionWithPrefix(
      String pFunctionName, Collection<String> pPrefix) {
    return pPrefix.stream().anyMatch(nan -> isBuiltinFloatFunctionWithPrefix(pFunctionName, nan));
  }

  private static boolean isBuiltinFloatFunctionWithPrefix(String pFunctionName, String pPrefix) {
    int length = pFunctionName.length();
    int prefixLength = pPrefix.length();
    if ((length != prefixLength) && (length != prefixLength + 1)) {
      return false;
    }
    if (!pFunctionName.startsWith(pPrefix)) {
      return false;
    }
    String suffix = pFunctionName.substring(prefixLength);
    return suffix.isEmpty() || suffix.equals("f") || suffix.equals("l");
  }

  /**
   * Get the type of a builtin float function. This could be the return type or a parameter type.
   *
   * @param pFunctionName A function name for which {@link #isBuiltinFloatFunction(String)} returns
   *     true.
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
   * @return <code>true</code> if the given function name is any builtin infinity-function, <code>
   *     false</code> otherwise
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
   * @return <code>true</code> if the given function name is any builtin huge_val-function, <code>
   *     false</code> otherwise
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
   * @return <code>true</code> if the given function name is any builtin NaN-function, <code>false
   *     </code> otherwise
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
    return CEIL_FLOAT.contains(pFunctionName)
        || CEIL.contains(pFunctionName)
        || CEIL_LONG_DOUBLE.contains(pFunctionName);
  }

  public static boolean matchesFloor(String pFunctionName) {
    return FLOOR_FLOAT.contains(pFunctionName)
        || FLOOR.contains(pFunctionName)
        || FLOOR_LONG_DOUBLE.contains(pFunctionName);
  }

  public static boolean matchesRound(String pFunctionName) {
    return ROUND_FLOAT.contains(pFunctionName)
        || ROUND.contains(pFunctionName)
        || ROUND_LONG_DOUBLE.contains(pFunctionName);
  }

  public static boolean matchesLround(String pFunctionName) {
    return LROUND_FLOAT.contains(pFunctionName)
        || LROUND.contains(pFunctionName)
        || LROUND_LONG_DOUBLE.contains(pFunctionName);
  }

  public static boolean matchesLlround(String pFunctionName) {
    return LLROUND_FLOAT.contains(pFunctionName)
        || LLROUND.contains(pFunctionName)
        || LLROUND_LONG_DOUBLE.contains(pFunctionName);
  }

  public static boolean matchesTrunc(String pFunctionName) {
    return TRUNC_FLOAT.contains(pFunctionName)
        || TRUNC.contains(pFunctionName)
        || TRUNC_LONG_DOUBLE.contains(pFunctionName);
  }

  public static boolean matchesFdim(String pFunctionName) {
    return FDIM_FLOAT.contains(pFunctionName)
        || FDIM.contains(pFunctionName)
        || FDIM_LONG_DOUBLE.contains(pFunctionName);
  }

  public static boolean matchesFmax(String pFunctionName) {
    return FMAX_FLOAT.contains(pFunctionName)
        || FMAX.contains(pFunctionName)
        || FMAX_LONG_DOUBLE.contains(pFunctionName);
  }

  public static boolean matchesFmin(String pFunctionName) {
    return FMIN_FLOAT.contains(pFunctionName)
        || FMIN.contains(pFunctionName)
        || FMIN_LONG_DOUBLE.contains(pFunctionName);
  }

  public static boolean matchesFmod(String pFunctionName) {
    return FMOD_FLOAT.contains(pFunctionName)
        || FMOD.contains(pFunctionName)
        || FMOD_LONG_DOUBLE.contains(pFunctionName);
  }

  public static boolean matchesModf(String pFunctionName) {
    return MODF_FLOAT.contains(pFunctionName)
        || MODF.contains(pFunctionName)
        || MODF_LONG_DOUBLE.contains(pFunctionName);
  }

  public static boolean matchesFremainder(String pFunctionName) {
    return FREMAINDER_FLOAT.contains(pFunctionName)
        || FREMAINDER.contains(pFunctionName)
        || FREMAINDER_LONG_DOUBLE.contains(pFunctionName);
  }

  public static boolean matchesSignbit(String pFunctionName) {
    return SIGNBIT_FLOAT.equals(pFunctionName)
        || SIGNBIT.equals(pFunctionName)
        || SIGNBIT_LONG_DOUBLE.equals(pFunctionName);
  }

  public static boolean matchesCopysign(String pFunctionName) {
    return COPYSIGN_FLOAT.equals(pFunctionName)
        || COPYSIGN.equals(pFunctionName)
        || COPYSIGN_LONG_DOUBLE.equals(pFunctionName);
  }

  public static boolean matchesIsgreater(String pFunctionName) {
    return ISGREATER.contains(pFunctionName);
  }

  public static boolean matchesIsgreaterequal(String pFunctionName) {
    return ISGREATEREQUAL.contains(pFunctionName);
  }

  public static boolean matchesIsless(String pFunctionName) {
    return ISLESS.contains(pFunctionName);
  }

  public static boolean matchesIslessequal(String pFunctionName) {
    return ISLESSEQUAL.contains(pFunctionName);
  }

  public static boolean matchesIslessgreater(String pFunctionName) {
    return ISLESSGREATER.contains(pFunctionName);
  }

  public static boolean matchesIsunordered(String pFunctionName) {
    return ISUNORDERED.contains(pFunctionName);
  }

  /**
   * Returns whether the given function name is any builtin absolute-function.
   *
   * @param pFunctionName the function name to check
   * @return <code>true</code> if the given function name is any builtin absolute-function, <code>
   *     false</code> otherwise
   */
  public static boolean matchesAbsolute(String pFunctionName) {
    return isBuiltinFloatFunctionWithPrefix(pFunctionName, ABSOLUTE_VAL);
  }

  /**
   * Returns whether the given function name is any builtin fpclassify-function.
   *
   * @param pFunctionName the function name to check
   * @return <code>true</code> if the given function name is any builtin fpclassify-function, <code>
   *     false</code> otherwise
   */
  public static boolean matchesFloatClassify(String pFunctionName) {
    return isBuiltinFloatFunctionWithPrefix(pFunctionName, FLOAT_CLASSIFY);
  }

  /**
   * Returns whether the given function name is any builtin function that checks whether a float is
   * finite.
   *
   * @param pFunctionName the function name to check
   * @return <code>true</code> if the given function name is any builtin finite-function, <code>
   *     false</code> otherwise
   */
  public static boolean matchesFinite(String pFunctionName) {
    return isBuiltinFloatFunctionWithPrefix(pFunctionName, IS_FINITE);
  }

  /**
   * Returns whether the given function name is any builtin function that checks whether a float is
   * NaN.
   *
   * @param pFunctionName the function name to check
   * @return <code>true</code> if the given function name is any builtin isnan-function, <code>false
   *     </code> otherwise
   */
  public static boolean matchesIsNaN(String pFunctionName) {
    return isBuiltinFloatFunctionWithPrefix(pFunctionName, IS_NAN);
  }

  /**
   * Returns whether the given function name is any builtin function that checks whether a float is
   * infinite.
   *
   * @param pFunctionName the function name to check
   * @return <code>true</code> if the given function name is any builtin isinf-function, <code>false
   *     </code> otherwise
   */
  public static boolean matchesIsInfinity(String pFunctionName) {
    return isBuiltinFloatFunctionWithPrefix(pFunctionName, IS_INFINITY);
  }

  /**
   * In some case we have a type and need to invoke the appropriate trunc-function for it.<br>
   * This method is used to derive the name of that trunc-function and throws an <code>
   * AssertionError</code> if an unsupported type is asked for.
   *
   * @param pReturnType the wanted return type for the used trunc-function
   * @return the name of the appropriately typed trunc-function
   */
  public static String getAppropriateTruncName(CType pReturnType) {
    if (pReturnType == CNumericTypes.DOUBLE) {
      return TRUNC.get(1);
    }
    if (pReturnType == CNumericTypes.FLOAT) {
      return TRUNC_FLOAT.get(1);
    }
    if (pReturnType == CNumericTypes.LONG_DOUBLE) {
      return TRUNC_LONG_DOUBLE.get(1);
    }
    throw new AssertionError(
        "There is no built-in function 'trunc' with the parameter- or return-type " + pReturnType);
  }
}
