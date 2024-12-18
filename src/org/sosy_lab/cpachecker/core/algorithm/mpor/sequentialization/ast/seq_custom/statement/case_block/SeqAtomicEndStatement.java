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
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqStatements;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqSyntax;

public class SeqAtomicEndStatement implements SeqCaseBlockStatement {

  private final CExpressionAssignmentStatement atomicInUseFalse;

  private final int threadId;

  private final int targetPc;

  public SeqAtomicEndStatement(
      CExpressionAssignmentStatement pAtomicInUseFalse, int pThreadId, int pTargetPc) {

    atomicInUseFalse = pAtomicInUseFalse;
    threadId = pThreadId;
    targetPc = pTargetPc;
  }

  @Override
  public String toASTString() {
    CExpressionAssignmentStatement pcUpdate = SeqStatements.buildPcUpdate(threadId, targetPc);
    return atomicInUseFalse.toASTString() + SeqSyntax.SPACE + pcUpdate;
  }

  @Override
  public Optional<Integer> getTargetPc() {
    return Optional.of(targetPc);
  }

  @Override
  public @NonNull SeqMutexUnlockStatement cloneWithTargetPc(int pTargetPc) {
    return new SeqMutexUnlockStatement(atomicInUseFalse, threadId, pTargetPc);
  }

  @Override
  public boolean alwaysUpdatesPc() {
    return true;
  }
}
