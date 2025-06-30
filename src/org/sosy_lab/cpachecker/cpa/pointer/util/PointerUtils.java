// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.pointer.util;

import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;

import java.util.Optional;

public final class PointerUtils {

  private PointerUtils() {}

  public static boolean isFreeFunction(CExpression pExpression) {
    return getFunctionName(pExpression).map("free"::equals).orElse(false);
  }

  public static boolean isMallocFunction(CExpression pExpression) {
    return getFunctionName(pExpression).map("malloc"::equals).orElse(false);
  }

  public static boolean isNondetPointerReturn(CExpression pExpression) {
    return getFunctionName(pExpression).map("__VERIFIER_nondet_pointer"::equals).orElse(false);
  }

  private static Optional<String> getFunctionName(CExpression pExpression) {
    if (pExpression instanceof CIdExpression idExpr) {
      return Optional.of(idExpr.getName());
    }
    return Optional.empty();
  }

  public static boolean isValidFunctionReturn(PointerTarget pTarget) {
    if (pTarget instanceof HeapLocation) {
      return true;
    }
    if (pTarget instanceof MemoryLocationPointer ptr) {
      return ptr.isNotLocalVariable();
    }
    return true;
  }
}
