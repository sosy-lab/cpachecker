// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.por;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;

/**
 * Supplies a fresh qualified name for each single access to a global variable. When such a renamer
 * is passed to the POR cloners, every occurrence of a global variable in a cloned AST is renamed to
 * the value returned by {@link #freshName}, turning each access into a distinct symbol. Without a
 * renamer, global variables keep their original names.
 */
public interface GlobalAccessRenamer {

  /**
   * Returns the fresh qualified name to use for one access to the given global variable.
   *
   * @param pDeclaration the original declaration of the accessed global variable
   * @param pIsWrite whether the access writes the variable (otherwise it reads it)
   */
  String freshName(CVariableDeclaration pDeclaration, boolean pIsWrite);

  /**
   * Offers to replace a whole address-of expression (called with the ORIGINAL, un-cloned expression
   * before its operand is cloned). Returning null keeps the default cloning.
   */
  default @Nullable CExpression replaceAddressOf(CUnaryExpression pOriginalAddressOf) {
    return null;
  }

  /**
   * Offers to replace one dereferencing access (pointer dereference or array subscript, rebuilt
   * from already-cloned operands). Returning null keeps the rebuilt expression unchanged.
   */
  default @Nullable CIdExpression replaceAliasedAccess(
      CExpression pClonedAccess, boolean pIsWrite) {
    return null;
  }

  /** Thrown by renamer callbacks for accesses the caller cannot support. */
  final class UnsupportedAccessException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public UnsupportedAccessException(String pMessage) {
      super(pMessage);
    }
  }
}
