// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.multi_control;

import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqStatement;

public sealed interface SeqMultiControlStatement extends SeqStatement
    permits SeqBinarySearchTreeStatement, SeqIfElseChainStatement, SeqSwitchStatement {

  MultiControlStatementEncoding getEncoding();
}
