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

  private static final CType UNSPECIFIED_TYPE = new CSimpleType(false, false, CBasicType.UNSPECIFIED,
      false, false, false, false, false, false, false);

  public static boolean isBuiltinFunction(String pFunctionName) {
    return pFunctionName.startsWith("__builtin_")
        || pFunctionName.equals(FREE)
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
}
