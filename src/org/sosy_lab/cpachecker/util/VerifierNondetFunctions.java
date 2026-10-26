// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util;

import java.util.Optional;
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
   * __VERIFIER_nondet_uint for unsigned int, or nothing if SV-COMP defines no such function.
   */
  public static Optional<String> getFunctionName(CSimpleType pType) {
    return getSuffix(pType).map(suffix -> PREFIX + suffix);
  }

  /**
   * Returns the declaration of the function that returns nondeterministic values of the given type,
   * e.g., {@code unsigned int __VERIFIER_nondet_uint(void);}, see {@link #getFunctionName}.
   */
  public static Optional<String> getFunctionDeclaration(CSimpleType pType) {
    return getFunctionName(pType)
        .map(name -> pType.withoutQualifiers().toASTString(name + "(void)") + ";");
  }

  private static Optional<String> getSuffix(CSimpleType pType) {
    boolean unsigned = pType.hasUnsignedSpecifier();
    return switch (pType.getType()) {
      case BOOL -> Optional.of("bool");
      case CHAR -> Optional.of(unsigned ? "uchar" : "char");
      case INT, UNSPECIFIED -> {
        if (pType.hasShortSpecifier()) {
          yield Optional.of(unsigned ? "ushort" : "short");
        } else if (pType.hasLongLongSpecifier()) {
          yield Optional.of(unsigned ? "ulonglong" : "longlong");
        } else if (pType.hasLongSpecifier()) {
          yield Optional.of(unsigned ? "ulong" : "long");
        } else {
          yield Optional.of(unsigned ? "uint" : "int");
        }
      }
      case INT128 -> Optional.of(unsigned ? "uint128" : "int128");
      case FLOAT -> Optional.of("float");
      case DOUBLE -> pType.hasLongSpecifier() ? Optional.empty() : Optional.of("double");
      case FLOAT128 -> Optional.empty();
    };
  }
}
