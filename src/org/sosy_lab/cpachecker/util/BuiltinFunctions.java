// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util;

import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;

/**
 * This class provides methods for checking whether a function is a specific builtin one.
 * The builtin functions of gcc are used as a reference for the provided function names.
 *
 * Float-specific builtin functions are implemented in {@link BuiltinFloatFunctions}.
 */
public class BuiltinFunctions {

  private static final String FREE = "free";
  private static final String STRLEN = "strlen";

  private static final CType UNSPECIFIED_TYPE = new CSimpleType(false, false, CBasicType.UNSPECIFIED,
      false, false, false, false, false, false, false);

  public static boolean isBuiltinFunction(String pFunctionName) {
    return pFunctionName.startsWith("__builtin_")
        // https://gcc.gnu.org/onlinedocs/gcc/_005f_005fatomic-Builtins.html
        || pFunctionName.startsWith("__atomic_")
        || pFunctionName.equals(FREE)
        || matchesStrlen(pFunctionName)
        || BuiltinFloatFunctions.isBuiltinFloatFunction(pFunctionName);
  }

  /**
   * Returns the function type of the specified function, if known.
   * This could be the return type or a parameter type.
   * Returns the type <code>UNSPECIFIED</code> otherwise.
   *
   * @param pFunctionName function name to get the return type for
   * @return the type of the specified function, if known
   */
  public static CType getFunctionType(String pFunctionName) {
    if (pFunctionName.equals(FREE)) {
      return CVoidType.VOID;
    }

    if (BuiltinFloatFunctions.isBuiltinFloatFunction(pFunctionName)) {
      return BuiltinFloatFunctions.getTypeOfBuiltinFloatFunction(pFunctionName);
    }

    return UNSPECIFIED_TYPE;
  }

  public static boolean matchesStrlen(String pFunctionName) {
    return pFunctionName.equals(STRLEN);
  }
}
