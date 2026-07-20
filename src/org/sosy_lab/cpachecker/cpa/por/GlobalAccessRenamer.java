// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.por;

import java.util.function.UnaryOperator;
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
   * before its operand is cloned), given a sub-cloner that clones an rvalue sub-expression with the
   * same renaming — so a compound operand such as {@code &p->field} or {@code &a[i]} can have its
   * pointer/index sub-parts cloned while the object base and byte offset are resolved separately
   * and combined into a single address. Returning null keeps the default cloning.
   */
  default @Nullable CExpression replaceAddressOf(
      CUnaryExpression pOriginalAddressOf, UnaryOperator<CExpression> pSubCloner) {
    return null;
  }

  /**
   * Whether accesses to this <b>local</b> variable should be routed through {@link #freshName} (the
   * same path globals take) instead of the plain per-thread rename. A renamer that models an
   * aliasing regime returns {@code true} for an address-taken local, so that a pointer — or another
   * thread the local's address escaped to — reaches the same memory as the local's own accesses.
   * Globals always go through {@link #freshName}; this only concerns locals.
   */
  default boolean treatsLocalAsRegion(CVariableDeclaration pLocal) {
    return false;
  }

  /**
   * Offers to replace one aliased memory access (pointer dereference, array subscript, or field
   * reference), given the ORIGINAL access expression and a sub-cloner that clones an rvalue
   * sub-expression with the same renaming (so the renamer can decide, per sub-part, whether to
   * clone it or treat it as an object base/offset). Returning null keeps the default cloning.
   */
  default @Nullable CIdExpression replaceAliasedAccess(
      CExpression pOriginalAccess, boolean pIsWrite, UnaryOperator<CExpression> pSubCloner) {
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
