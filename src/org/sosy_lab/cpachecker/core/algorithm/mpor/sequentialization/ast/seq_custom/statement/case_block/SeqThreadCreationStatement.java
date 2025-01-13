// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block;

import java.util.Optional;
import javax.annotation.Nonnull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqStatements;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqSyntax;

/**
 * Represents a statement that simulates calls to {@code pthread_create} of the form:
 *
 * <p>{@code __MPOR_SEQ__THREAD1_ACTIVE = 1; }
 */
public class SeqThreadCreationStatement implements SeqCaseBlockStatement {

  private final CExpressionAssignmentStatement assign;

  private final int threadId;

  private final int targetPc;

  public SeqThreadCreationStatement(
      CExpressionAssignmentStatement pAssign, int pThreadId, int pTargetPc) {

    assign = pAssign;
    threadId = pThreadId;
    targetPc = pTargetPc;
  }

  @Override
  public String toASTString() {
    CExpressionAssignmentStatement pcUpdate = SeqStatements.buildPcUpdate(threadId, targetPc);
    return assign.toASTString() + SeqSyntax.SPACE + pcUpdate.toASTString();
  }

  @Override
  public Optional<Integer> getTargetPc() {
    return Optional.of(targetPc);
  }

  @Nonnull
  @Override
  public @NonNull SeqThreadCreationStatement cloneWithTargetPc(int pTargetPc) {
    return new SeqThreadCreationStatement(assign, threadId, pTargetPc);
  }

  @Override
  public boolean alwaysUpdatesPc() {
    return true;
  }
}
