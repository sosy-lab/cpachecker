// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
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
  private static final String FSCANF = "fscanf";

  public static final Set<String> STANDARD_BYTE_INPUT_FUNCTIONS =
      ImmutableSet.of(
          "fgetc", "fgets", "fread", "fscanf", "getc", "getchar", "scanf", "vfscanf", "vscanf");

  public static final Set<String> STANDARD_BYTE_OUTPUT_FUNCTIONS =
      ImmutableSet.of(
          "fprintf",
          "fputc",
          "fputs",
          "fwrite",
          "printf",
          "vfprintf",
          "vprintf",
          "putc",
          "putchar",
          "puts",
          "ungetc");

  private static final Set<String> BUILTIN_STANDARD_WIDE_CHARACTER_INPUT_FUNCTIONS =
      ImmutableSet.of(
          "fgetwc", "fgetws", "getwc", "getwchar", "fwscanf", "wscanf", "vfwscanf", "vwscanf");

  private static final String UNGETWC_FUNCTION = "ungetwc";

  private static final Set<String> BUILTIN_STANDARD_WIDE_CHARACTER_OUTPUT_FUNCTIONS =
      ImmutableSet.of(
          "fputwc", "fputws", "putwc", "putwchar", "fwprintf", "wprintf", "vfwprintf", "vwprintf");

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

  private static final ImmutableMap<String, CType> supportedScanfFormatSpecifiers =
      ImmutableMap.<String, CType>builder()
          .put("%d", CNumericTypes.INT) // decimal integer
          .put("%i", CNumericTypes.INT) // decimal, octal, or hexadecimal integer
          .put("%o", CNumericTypes.UNSIGNED_INT) // octal integer
          .put("%u", CNumericTypes.UNSIGNED_INT) // unsigned decimal integer
          .put("%x", CNumericTypes.UNSIGNED_INT) // hexadecimal integer
          .put("%ld", CNumericTypes.LONG_INT) // long decimal integer
          .put("%li", CNumericTypes.LONG_INT) // long decimal, octal, or hexadecimal integer
          .put("%lo", CNumericTypes.UNSIGNED_LONG_INT) // long octal integer
          .put("%lu", CNumericTypes.UNSIGNED_LONG_INT) // long unsigned decimal integer
          .put("%lx", CNumericTypes.UNSIGNED_LONG_INT) // long hexadecimal integer
          .put("%hd", CNumericTypes.SHORT_INT) // short decimal integer
          .put("%hi", CNumericTypes.SHORT_INT) // short decimal, octal, or hexadecimal integer
          .put("%ho", CNumericTypes.UNSIGNED_SHORT_INT) // short octal integer
          .put("%hu", CNumericTypes.UNSIGNED_SHORT_INT) // short unsigned decimal integer
          .put("%hx", CNumericTypes.UNSIGNED_SHORT_INT) // short hexadecimal integer
          .put("%lld", CNumericTypes.LONG_LONG_INT) // long long decimal integer
          .put(
              "%lli",
              CNumericTypes.LONG_LONG_INT) // long long decimal, octal, or hexadecimal integer
          .put("%llo", CNumericTypes.UNSIGNED_LONG_LONG_INT) // long long octal integer
          .put("%llu", CNumericTypes.UNSIGNED_LONG_LONG_INT) // long long unsigned decimal integer
          .put("%llx", CNumericTypes.UNSIGNED_LONG_LONG_INT) // long long hexadecimal integer
          .put("%hhd", CNumericTypes.SIGNED_CHAR)
          .put("%hhi", CNumericTypes.SIGNED_CHAR)
          .put("%hhx", CNumericTypes.UNSIGNED_CHAR)
          .put("%hho", CNumericTypes.UNSIGNED_CHAR)
          .put("%hhu", CNumericTypes.UNSIGNED_CHAR)
          .put("%f", CNumericTypes.FLOAT)
          .put("%e", CNumericTypes.FLOAT)
          .put("%g", CNumericTypes.FLOAT)
          .put("%a", CNumericTypes.FLOAT)
          .put("%lf", CNumericTypes.DOUBLE)
          .put("%le", CNumericTypes.DOUBLE)
          .put("%lg", CNumericTypes.DOUBLE)
          .put("%la", CNumericTypes.DOUBLE)
          .put("%Lf", CNumericTypes.LONG_DOUBLE)
          .put("%Le", CNumericTypes.LONG_DOUBLE)
          .put("%Lg", CNumericTypes.LONG_DOUBLE)
          .put("%La", CNumericTypes.LONG_DOUBLE)
          .put("%c", CNumericTypes.CHAR)
          .buildOrThrow();

  public static boolean isBuiltinFunction(String pFunctionName) {
    return pFunctionName.startsWith("__builtin_")
        // https://gcc.gnu.org/onlinedocs/gcc/_005f_005fatomic-Builtins.html
        || pFunctionName.startsWith("__atomic_")
        || pFunctionName.equals(FREE)
        || matchesStrlen(pFunctionName)
        || matchesFscanf(pFunctionName)
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

  public static boolean matchesFscanf(String pFunctionName) {
    return pFunctionName.equals(FSCANF);
  }

  public static Optional<CType> getTypeFromScanfFormatSpecifier(String specifier) {
    return Optional.ofNullable(supportedScanfFormatSpecifiers.get(specifier));
  }

  public static ImmutableSet<String> getAllowedScanfFormatSpecifiers() {
    return supportedScanfFormatSpecifiers.keySet();
  }

  public static boolean isSetjmpFunction(String pFunctionName) {
    return SETJMP.contains(pFunctionName);
  }

  public static boolean isPopcountFunction(String pFunctionName) {
    return pFunctionName.contains(POPCOUNT);
  }

  /**
   * True for built-in input or output functions defined by the C11 standard (in §7.21.1.5). Checks
   * for wide character and byte input and output functions.
   */
  @SuppressWarnings("unused")
  public static boolean isStandardInputOrOutputFunction(String functionName) {
    return isStandardWideCharInputOrOutputFunction(functionName)
        || isStandardByteInputOrOutputFunction(functionName);
  }

  /**
   * True for built-in wide character input or output functions defined by the C11 standard (in
   * §7.21.1.5). Checks for wide character input and output functions, as well as the ungetwc()
   * function, which is not included in the split up sets and checks for wide character input or
   * output functions!
   */
  public static boolean isStandardWideCharInputOrOutputFunction(String functionName) {
    return UNGETWC_FUNCTION.contains(functionName)
        || isStandardWideCharInputFunction(functionName)
        || isStandardWideCharOutputFunction(functionName);
  }

  /**
   * True for built-in wide character input functions defined by the C11 standard (in §7.21.1.5).
   */
  public static boolean isStandardWideCharInputFunction(String functionName) {
    return BUILTIN_STANDARD_WIDE_CHARACTER_INPUT_FUNCTIONS.contains(functionName);
  }

  /**
   * True for built-in wide character output functions defined by the C11 standard (in §7.21.1.5).
   */
  public static boolean isStandardWideCharOutputFunction(String functionName) {
    return BUILTIN_STANDARD_WIDE_CHARACTER_OUTPUT_FUNCTIONS.contains(functionName);
  }

  /**
   * True for built-in functions defined by the C11 standard as byte input or output functions (in
   * §7.21.1.5).
   */
  public static boolean isStandardByteInputOrOutputFunction(String functionName) {
    return isStandardByteOutputFunction(functionName) || isStandardByteInputFunction(functionName);
  }

  /**
   * True for built-in functions defined by the C11 standard as byte input functions (in §7.21.1.5).
   */
  public static boolean isStandardByteInputFunction(String functionName) {
    return STANDARD_BYTE_INPUT_FUNCTIONS.contains(functionName);
  }

  /**
   * True for built-in functions defined by the C11 standard as byte output functions (in
   * §7.21.1.5).
   */
  public static boolean isStandardByteOutputFunction(String functionName) {
    return STANDARD_BYTE_OUTPUT_FUNCTIONS.contains(functionName);
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
}
