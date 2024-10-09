// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block;

import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;

public class SeqBlankStatement implements SeqCaseBlockStatement {

  private final CExpressionAssignmentStatement pcUpdate;

  public final int targetPc;

  public SeqBlankStatement(CExpressionAssignmentStatement pPcUpdate, int pTargetPc) {
    pcUpdate = pPcUpdate;
    targetPc = pTargetPc;
  }

  @Override
  public String toASTString() {
    return pcUpdate.toASTString();
  }
}
