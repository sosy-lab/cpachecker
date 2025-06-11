// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.control_flow;

public enum MultiControlFlowEncoding {
  SWITCH_CASE,
  BINARY_IF_TREE,
  // TODO
  // CONDITIONAL_GOTO
  // IF_ELSE_IF ...
}
