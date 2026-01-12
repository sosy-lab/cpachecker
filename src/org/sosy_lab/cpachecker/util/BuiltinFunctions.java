// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util;

import com.google.common.collect.ImmutableSet;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypeQualifiers;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;

/**
 * This class provides methods for checking whether a function is a specific builtin one. The
 * builtin functions of gcc are used as a reference for the provided function names. A more complete
 * list of all builtin functions can be found in {@link StandardFunctions}.
 *
 * <p>Float-specific builtin functions are implemented in {@link BuiltinFloatFunctions}.
 */
public class BuiltinFunctions {

  private static final String FREE = "free";
  private static final ImmutableSet<String> SETJMP =
      ImmutableSet.of("_setjmp", "setjmp", "sigsetjmp");
  private static final String STRLEN = "strlen";
  private static final String POPCOUNT = "popcount";

  private static final CType UNSPECIFIED_TYPE =
      new CSimpleType(
          CTypeQualifiers.NONE,
          CBasicType.UNSPECIFIED,
          false,
          false,
          false,
          false,
          false,
          false,
          false);

  public static boolean isBuiltinFunction(String pFunctionName) {
    return pFunctionName.startsWith("__builtin_")
        // https://gcc.gnu.org/onlinedocs/gcc/_005f_005fatomic-Builtins.html
        || pFunctionName.startsWith("__atomic_")
        || pFunctionName.equals(FREE)
        || matchesStrlen(pFunctionName)
        || BuiltinIoFunctions.matchesFscanf(pFunctionName)
        || isSetjmpFunction(pFunctionName)
        || BuiltinFloatFunctions.isBuiltinFloatFunction(pFunctionName);
  }

  /**
   * Returns the function type of the specified function, if known. This could be the return type or
   * a parameter type. Returns the type <code>UNSPECIFIED</code> otherwise.
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

    if (BuiltinOverflowFunctions.isBuiltinOverflowFunction(pFunctionName)) {
      return Objects.requireNonNullElse(
          BuiltinOverflowFunctions.getType(pFunctionName).orElse(null), UNSPECIFIED_TYPE);
    }

    if (isPopcountFunction(pFunctionName)) {
      return new CSimpleType(
          CTypeQualifiers.NONE, CBasicType.INT, false, false, false, false, false, false, false);
    }

    return UNSPECIFIED_TYPE;
  }

  public static boolean matchesStrlen(String pFunctionName) {
    return pFunctionName.equals(STRLEN);
  }

  public static boolean isSetjmpFunction(String pFunctionName) {
    return SETJMP.contains(pFunctionName);
  }

  public static boolean isPopcountFunction(String pFunctionName) {
    return pFunctionName.contains(POPCOUNT);
  }

  /**
   * Get the parameter type of builtin C functions popcount(), popcountl(), or popcountll(). The
   * parameters types are either type unsigned int for popcount(), unsigned long for popcountl(), or
   * unsigned long long for popcountll().
   *
   * @param pFunctionName A function name for which {@link #isPopcountFunction(String)} returns
   *     true.
   * @throws IllegalArgumentException For unhandled functions.
   */
  public static CSimpleType getParameterTypeOfBuiltinPopcountFunction(String pFunctionName) {
    if (isPopcountFunction(pFunctionName)) {
      if (pFunctionName.endsWith(POPCOUNT + "ll")) {
        return CNumericTypes.UNSIGNED_LONG_LONG_INT;
      } else if (pFunctionName.endsWith(POPCOUNT + "l")) {
        return CNumericTypes.UNSIGNED_LONG_INT;
      } else if (pFunctionName.endsWith(POPCOUNT)) {
        return CNumericTypes.UNSIGNED_INT;
      } else {
        throw new IllegalArgumentException(
            "Builtin function '"
                + pFunctionName
                + "' with unknown suffix '"
                + pFunctionName.substring(pFunctionName.length())
                + "'");
      }
    }
    throw new IllegalArgumentException(
        "Builtin function '" + pFunctionName + "' is not a popcount function'");
  }

  /**
   * Returns true for pointers towards FILE types, i.e. 'FILE *'. The FILE object type checked is
   * according to the definition used by GNU (GCC) compiler, and is capable of recording all the
   * information needed to control a stream, e.g. after opening a file. More information can be
   * found in the C11 standard sections 7.21.1 and 7.21.3 and the GCC header stdio.h. This might not
   * work for definitions of FILE that are distinct to the one used in GCC!
   */
  public static boolean isFilePointer(CType pType) {
    if (pType instanceof CPointerType pointerType) {
      if (pointerType.getType().getCanonicalType() instanceof CComplexType actualType) {
        // We use CComplexType here instead of CStructType, because _IO_FILE may be defined
        // externally i.e. `extern struct _IO_FILE *stdin;` or fully as a
        // `struct _IO_FILE { ... }`.
        return actualType.getKind() == ComplexTypeKind.STRUCT
            && actualType.getName().equals("_IO_FILE");
      }
    }
    return false;
  }
}
