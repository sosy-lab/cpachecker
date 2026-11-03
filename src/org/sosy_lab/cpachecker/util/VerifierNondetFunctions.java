// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util;

import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;

/**
 * The functions __VERIFIER_nondet_X that return nondeterministic values, as defined by the <a
 * href="https://sv-comp.sosy-lab.org/rules.php">rules of SV-COMP</a>.
 */
public final class VerifierNondetFunctions {

  public static final String PREFIX = "__VERIFIER_nondet_";

  private VerifierNondetFunctions() {}

  /**
   * Returns the name of the function that returns nondeterministic values of the given type, e.g.,
   * __VERIFIER_nondet_uint for unsigned int. SV-COMP defines no function for long double and
   * __float128, so their functions are named like the others, e.g., __VERIFIER_nondet_longdouble.
   * Verifiers treat them like any undefined function with a nondeterministic result.
   */
  public static String getFunctionName(CSimpleType pType) {
    return PREFIX + getSuffix(pType);
  }

  /**
   * Returns the declaration of the function that returns nondeterministic values of the given type,
   * e.g., {@code unsigned int __VERIFIER_nondet_uint(void);}, see {@link #getFunctionName}.
   */
  public static String getFunctionDeclaration(CSimpleType pType) {
    return pType.withoutQualifiers().toASTString(getFunctionName(pType) + "(void)") + ";";
  }

  private static String getSuffix(CSimpleType pType) {
    boolean unsigned = pType.hasUnsignedSpecifier();
    return switch (pType.getType()) {
      case BOOL -> "bool";
      case CHAR -> unsigned ? "uchar" : "char";
      case INT, UNSPECIFIED -> {
        if (pType.hasShortSpecifier()) {
          yield unsigned ? "ushort" : "short";
        } else if (pType.hasLongLongSpecifier()) {
          yield unsigned ? "ulonglong" : "longlong";
        } else if (pType.hasLongSpecifier()) {
          yield unsigned ? "ulong" : "long";
        } else {
          yield unsigned ? "uint" : "int";
        }
      }
      case INT128 -> unsigned ? "uint128" : "int128";
      case FLOAT -> "float";
      case DOUBLE -> pType.hasLongSpecifier() ? "longdouble" : "double";
      case FLOAT128 -> "float128";
    };
  }
}
