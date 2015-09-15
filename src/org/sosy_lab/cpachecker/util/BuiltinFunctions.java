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

import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;

/**
 * This class provides constants for builtin function's names and methods for checking whether
 * a function is a specific builtin one.
 * The builtin functions of gcc are used as a reference for the provided function names.
 */
public class BuiltinFunctions {

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
  private static final String FLOAT_CLASSIFY_FLOAT = "__fpclassifyf";
  private static final String FLOAT_CLASSIFY_DOUBLE = "__fpclassifyd";
  private static final String FLOAT_CLASSIFY_LONG_DOUBLE = "__fpclassifyl";

  private static final String FREE = "free";

  private static final CType UNSPECIFIED_TYPE = new CSimpleType(false, false, CBasicType.UNSPECIFIED,
      false, false, false, false, false, false, false);

  private static String[] possiblePrefixes = { INFINITY, HUGE_VAL, NOT_A_NUMBER, ABSOLUTE_VAL, FLOAT_CLASSIFY };

  public static boolean isBuiltinFunction(String pFunctionName) {
    return pFunctionName.startsWith("__builtin_") || pFunctionName.equals(FREE);
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
    return isInfinityDouble(pFunctionName) || isInfinityFloat(pFunctionName)
        || isInfinityLongDouble(pFunctionName);
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
    return isHugeValFloat(pFunctionName) || isHugeValDouble(pFunctionName)
        || isHugeValLongDouble(pFunctionName);
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
    return isNaNDouble(pFunctionName) || isNaNFloat(pFunctionName) || isNaNLongDouble(pFunctionName);
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
    return isAbsoluteDouble(pFunctionName) || isAbsoluteFloat(pFunctionName)
        || isAbsoluteLongDouble(pFunctionName);
  }

  public static boolean isFloatClassify(String pFunctionName) {
    return FLOAT_CLASSIFY.equals(pFunctionName) || FLOAT_CLASSIFY_DOUBLE.equals(pFunctionName)
        || FLOAT_CLASSIFY_FLOAT.equals(pFunctionName)
        || FLOAT_CLASSIFY_LONG_DOUBLE.equals(pFunctionName);
  }

  /**
   * Returns the function type of the specified function, if known.
   * Returns the type <code>UNSPECIFIED</code> otherwise.
   *
   * @param pFunctionName function name to get the return type for
   * @return the return type of the specified function, if known
   */
  public static CType getFunctionType(String pFunctionName) {

    for (String p : possiblePrefixes) {
      if (pFunctionName.startsWith(p)) {
        String suffix = pFunctionName.substring(p.length());

        if (suffix.isEmpty()) {
          return CNumericTypes.DOUBLE;

        } else {
          assert suffix.length() == 1;
          switch (suffix.charAt(0)) {
            case 'f':
              return CNumericTypes.FLOAT;
            case 'd':
              return CNumericTypes.DOUBLE;
            case 'l':
              return CNumericTypes.LONG_DOUBLE;
            default:
              throw new AssertionError();
          }
        }
      }
    }

    if (pFunctionName.equals(FREE)) {
      return CVoidType.VOID;
    }

    return UNSPECIFIED_TYPE;
  }
}
