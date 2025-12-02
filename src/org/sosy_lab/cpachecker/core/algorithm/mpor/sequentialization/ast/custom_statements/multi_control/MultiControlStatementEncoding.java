// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.multi_control;

public enum MultiControlStatementEncoding {
  /** Used when the next thread is chosen deterministically. */
  NONE,
  BINARY_SEARCH_TREE,
  IF_ELSE_CHAIN,
  SWITCH_CASE;

  // TODO CONDITIONAL_GOTO (cf. Lazy-CSeq)

  public boolean isEnabled() {
    return !this.equals(NONE);
  }
}
