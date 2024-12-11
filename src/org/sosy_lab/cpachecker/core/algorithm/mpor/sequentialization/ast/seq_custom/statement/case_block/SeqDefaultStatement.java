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
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqStatements;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqSyntax;

/**
 * Represents the default case block statement where the original {@link CFAEdge}s require no
 * specific handling and their (substituted) code is placed directly into the case block.
 */
public class SeqDefaultStatement implements SeqCaseBlockStatement {

  private final CStatementEdge edge;

  private final int threadId;

  private final int targetPc;

  public SeqDefaultStatement(CStatementEdge pEdge, int pThreadId, int pTargetPc) {
    edge = pEdge;
    threadId = pThreadId;
    targetPc = pTargetPc;
  }

  @Override
  public String toASTString() {
    CExpressionAssignmentStatement pcUpdate = SeqStatements.buildPcUpdate(threadId, targetPc);
    return edge.getCode() + SeqSyntax.SPACE + pcUpdate.toASTString();
  }

  @Override
  public Optional<Integer> getTargetPc() {
    return Optional.of(targetPc);
  }

  @Override
  public @NonNull SeqDefaultStatement cloneWithTargetPc(int pTargetPc) {
    return new SeqDefaultStatement(edge, threadId, pTargetPc);
  }
}
