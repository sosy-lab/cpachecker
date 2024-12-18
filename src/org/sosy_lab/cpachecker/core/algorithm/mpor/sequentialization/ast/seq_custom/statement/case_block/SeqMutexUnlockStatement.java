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

/**
 * Represents a statement that simulates calls to {@code pthread_mutex_unlock} of the form:
 *
 * <p>{@code __MPOR_SEQ__GLOBAL_14_m_LOCKED = 0; }
 */
public class SeqMutexUnlockStatement implements SeqCaseBlockStatement {

  private final CExpressionAssignmentStatement lockedFalse;

  private final int threadId;

  private final int targetPc;

  public SeqMutexUnlockStatement(
      CExpressionAssignmentStatement pLockedFalse, int pThreadId, int pTargetPc) {

    lockedFalse = pLockedFalse;
    threadId = pThreadId;
    targetPc = pTargetPc;
  }

  @Override
  public String toASTString() {
    CExpressionAssignmentStatement pcUpdate = SeqStatements.buildPcUpdate(threadId, targetPc);
    return lockedFalse.toASTString() + SeqSyntax.SPACE + pcUpdate;
  }

  @Override
  public Optional<Integer> getTargetPc() {
    return Optional.of(targetPc);
  }

  @Override
  public @NonNull SeqMutexUnlockStatement cloneWithTargetPc(int pTargetPc) {
    return new SeqMutexUnlockStatement(lockedFalse, threadId, pTargetPc);
  }

  @Override
  public boolean alwaysUpdatesPc() {
    return true;
  }
}
