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
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SeqUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqStatements;

/**
 * Represents a statement that simulates the termination of a thread of the form:
 *
 * <p>{@code pc[i] = -1; }
 *
 * <p>This statement is injected when encountering the {@link FunctionExitNode} of the respective
 * threads start routine / main function.
 */
public class SeqThreadExitStatement implements SeqCaseBlockStatement {

  private final int threadId;

  public SeqThreadExitStatement(int pThreadId) {
    threadId = pThreadId;
  }

  @Override
  public String toASTString() {
    CExpressionAssignmentStatement pcUpdate =
        SeqStatements.buildPcUpdate(threadId, SeqUtil.EXIT_PC);
    return pcUpdate.toASTString();
  }

  @Override
  public Optional<Integer> getTargetPc() {
    return Optional.of(SeqUtil.EXIT_PC);
  }

  @NonNull
  @Override
  public SeqThreadExitStatement cloneWithTargetPc(int pTargetPc) {
    throw new UnsupportedOperationException(
        "SeqThreadExitStatement only have targetPc " + SeqUtil.EXIT_PC);
  }

  @Override
  public boolean alwaysUpdatesPc() {
    return true;
  }
}
