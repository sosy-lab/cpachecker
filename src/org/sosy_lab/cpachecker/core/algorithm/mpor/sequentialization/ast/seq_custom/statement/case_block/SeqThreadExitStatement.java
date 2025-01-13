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
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SeqUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqStatements;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqSyntax;

/**
 * Represents a statement that simulates the termination of a thread of the form:
 *
 * <p>{@code __MPOR_SEQ__THREAD1_ACTIVE = 0; }
 *
 * <p>This statement is injected when encountering the {@link FunctionExitNode} of the respective
 * threads start routine / main function.
 */
public class SeqThreadExitStatement implements SeqCaseBlockStatement {

  private final CExpressionAssignmentStatement assign;

  private final int threadId;

  public SeqThreadExitStatement(CExpressionAssignmentStatement pAssign, int pThreadId) {
    assign = pAssign;
    threadId = pThreadId;
  }

  @Override
  public String toASTString() {
    CExpressionAssignmentStatement pcUpdate =
        SeqStatements.buildPcUpdate(threadId, SeqUtil.EXIT_PC);
    return assign.toASTString() + SeqSyntax.SPACE + pcUpdate.toASTString();
  }

  @Override
  public Optional<Integer> getTargetPc() {
    return Optional.of(SeqUtil.EXIT_PC);
  }

  @Nonnull
  @Override
  public @NonNull SeqThreadExitStatement cloneWithTargetPc(int pTargetPc) {
    throw new UnsupportedOperationException(
        "SeqThreadExitStatement only have targetPc " + SeqUtil.EXIT_PC);
  }

  @Override
  public boolean alwaysUpdatesPc() {
    return true;
  }
}
