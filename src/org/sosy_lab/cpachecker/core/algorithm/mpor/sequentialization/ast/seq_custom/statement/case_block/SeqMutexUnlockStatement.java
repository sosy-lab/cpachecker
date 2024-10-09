// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block;

import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqSyntax;

public class SeqMutexUnlockStatement implements SeqCaseBlockStatement {

  private final CExpressionAssignmentStatement lockedFalse;

  private final CExpressionAssignmentStatement pcUpdate;

  public SeqMutexUnlockStatement(
      CExpressionAssignmentStatement pLockedFalse, CExpressionAssignmentStatement pPcUpdate) {

    lockedFalse = pLockedFalse;
    pcUpdate = pPcUpdate;
  }

  @Override
  public String toASTString() {
    return lockedFalse.toASTString() + SeqSyntax.SPACE + pcUpdate;
  }
}
