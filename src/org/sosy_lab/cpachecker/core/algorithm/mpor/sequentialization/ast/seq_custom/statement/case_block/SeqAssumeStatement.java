// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block;

import java.util.Optional;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SeqUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqStatements;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqControlFlowStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqSyntax;

/** Represents a conditional case block statement with {@code if} and {@code else if} statements. */
public class SeqAssumeStatement implements SeqCaseBlockStatement {

  private final SeqControlFlowStatement controlFlowStatement;

  private final int threadId;

  private final int targetPc;

  public SeqAssumeStatement(
      SeqControlFlowStatement pControlFlowStatement, int pThreadId, int pTargetPc) {

    controlFlowStatement = pControlFlowStatement;
    threadId = pThreadId;
    targetPc = pTargetPc;
  }

  @Override
  public String toASTString() {
    CExpressionAssignmentStatement pcUpdate = SeqStatements.buildPcUpdate(threadId, targetPc);
    return controlFlowStatement.toASTString()
        + SeqSyntax.SPACE
        + SeqUtil.wrapInCurlyInwards(pcUpdate.toASTString());
  }

  @Override
  public Optional<Integer> getTargetPc() {
    return Optional.of(targetPc);
  }

  @Override
  public @NonNull SeqAssumeStatement cloneWithTargetPc(int pTargetPc) {
    return new SeqAssumeStatement(controlFlowStatement, threadId, pTargetPc);
  }
}
