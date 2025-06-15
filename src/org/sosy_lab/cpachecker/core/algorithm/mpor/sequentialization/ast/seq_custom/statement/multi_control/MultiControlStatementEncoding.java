// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.multi_control;

public enum MultiControlStatementEncoding {
  /** Used when the next thread is chosen deterministically. */
  NONE,
  BINARY_IF_TREE,
  IF_ELSE_CHAIN,
  SWITCH_CASE,
  // TODO
  // CONDITIONAL_GOTO
  // IF_ELSE_IF ...
}
