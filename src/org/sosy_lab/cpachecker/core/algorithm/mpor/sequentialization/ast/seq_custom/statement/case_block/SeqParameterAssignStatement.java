// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Optional;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqStatements;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqSyntax;

/**
 * Represents the assignment of a parameter given to a function to an injected parameter variable in
 * the sequentialization.
 *
 * <p>E.g. {@code __MPOR_SEQ__THREAD0_PARAM_q = GLOBAL_queue; }
 */
public class SeqParameterAssignStatement implements SeqCaseBlockStatement {

  private final CExpressionAssignmentStatement assign;

  private final Optional<Integer> threadId;

  private final Optional<Integer> targetPc;

  public SeqParameterAssignStatement(
      CExpressionAssignmentStatement pAssign,
      Optional<Integer> pThreadId,
      Optional<Integer> pTargetPc) {

    // the presence of pThreadId and pTargetPc must be equivalent
    checkArgument(pThreadId.isEmpty() || pTargetPc.isPresent());
    checkArgument(pTargetPc.isEmpty() || pThreadId.isPresent());

    assign = pAssign;
    threadId = pThreadId;
    targetPc = pTargetPc;
  }

  @Override
  public String toASTString() {
    if (threadId.isPresent() && targetPc.isPresent()) {
      CExpressionAssignmentStatement pcUpdate =
          SeqStatements.buildPcUpdate(threadId.orElseThrow(), targetPc.orElseThrow());
      return assign.toASTString() + SeqSyntax.SPACE + pcUpdate.toASTString();
    } else if (threadId.isEmpty() && targetPc.isEmpty()) {
      return assign.toASTString();
    } else {
      throw new IllegalArgumentException("presence of threadId and targetPc must be equivalent");
    }
  }

  @Override
  public Optional<Integer> getTargetPc() {
    return targetPc;
  }

  @Override
  public @NonNull SeqParameterAssignStatement cloneWithTargetPc(int pTargetPc) {
    checkArgument(targetPc.isPresent(), "cannot replace empty targetPc");
    return new SeqParameterAssignStatement(assign, threadId, Optional.of(pTargetPc));
  }

  @Override
  public boolean alwaysUpdatesPc() {
    return true;
  }
}
