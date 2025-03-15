// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.injected;

import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqExpressions.SeqIntegerLiteralExpression;

public class SeqThreadJoinsThreadStatement implements SeqInjectedStatement {

  public final CIdExpression threadJoinsThread;

  public SeqThreadJoinsThreadStatement(CIdExpression pThreadJoinsThread) {
    threadJoinsThread = pThreadJoinsThread;
  }

  @Override
  public String toASTString() {
    CExpressionAssignmentStatement setJoinsTrue =
        new CExpressionAssignmentStatement(
            FileLocation.DUMMY, threadJoinsThread, SeqIntegerLiteralExpression.INT_1);
    return setJoinsTrue.toASTString();
  }

  @Override
  public boolean marksCriticalSection() {
    return true;
  }

  @Override
  public Optional<CIdExpression> getIdExpression() {
    return Optional.of(threadJoinsThread);
  }
}
