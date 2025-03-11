// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.injected;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqExpressions.SeqIntegerLiteralExpression;

public class SeqThreadLocksMutexStatement implements SeqCaseBlockInjectedStatement {

  // TODO we need to check when the very first statement in a thread is mutex_lock, then do we have
  //  to inject an entire case clause, not just a statement?

  private final CIdExpression threadLocksMutex;

  public SeqThreadLocksMutexStatement(CIdExpression pThreadLocksMutex) {
    threadLocksMutex = pThreadLocksMutex;
  }

  @Override
  public String toASTString() {
    CExpressionAssignmentStatement setLocksTrue =
        new CExpressionAssignmentStatement(
            FileLocation.DUMMY, threadLocksMutex, SeqIntegerLiteralExpression.INT_1);
    return setLocksTrue.toASTString();
  }
}
