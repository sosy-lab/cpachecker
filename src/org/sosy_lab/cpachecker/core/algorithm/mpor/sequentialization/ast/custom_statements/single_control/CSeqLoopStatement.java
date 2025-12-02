// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.single_control;

public abstract sealed class CSeqLoopStatement implements SeqSingleControlStatement
    permits SeqForLoopStatement, SeqWhileLoopStatement {

  /**
   * The compound statement i.e. the loop block. Can be empty to model infinite loops that do
   * nothing.
   */
  final SeqCompoundStatement compoundStatement;

  CSeqLoopStatement(SeqCompoundStatement pCompoundStatement) {
    compoundStatement = pCompoundStatement;
  }
}
