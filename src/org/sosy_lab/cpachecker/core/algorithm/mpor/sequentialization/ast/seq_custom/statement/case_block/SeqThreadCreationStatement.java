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
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.Sequentialization;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqStatements.SeqExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost.pc.PcVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;

/**
 * Represents a statement that simulates calls to {@code pthread_create} of the form:
 *
 * <p>{@code pc[i] = 0; pc[j] = n; } where thread {@code j} creates thread {@code i}.
 */
public class SeqThreadCreationStatement implements SeqCaseBlockStatement {

  private final int createdThreadId;

  private final int threadId;

  private final Optional<Integer> targetPc;

  private final Optional<CExpression> targetPcExpression;

  private final PcVariables pcVariables;

  SeqThreadCreationStatement(
      int pCreatedThreadId, int pThreadId, int pTargetPc, PcVariables pPcVariables) {

    createdThreadId = pCreatedThreadId;
    threadId = pThreadId;
    targetPc = Optional.of(pTargetPc);
    targetPcExpression = Optional.empty();
    pcVariables = pPcVariables;
  }

  private SeqThreadCreationStatement(
      int pCreatedThreadId, int pThreadId, CExpression pTargetPc, PcVariables pPcVariables) {

    createdThreadId = pCreatedThreadId;
    threadId = pThreadId;
    targetPc = Optional.empty();
    targetPcExpression = Optional.of(pTargetPc);
    pcVariables = pPcVariables;
  }

  @Override
  public String toASTString() {
    CExpressionAssignmentStatement createdPcWrite =
        SeqExpressionAssignmentStatement.buildPcWrite(
            pcVariables.get(createdThreadId), Sequentialization.INIT_PC);
    CExpressionAssignmentStatement pcWrite =
        SeqExpressionAssignmentStatement.buildPcWriteByTargetPc(
            pcVariables.get(threadId), targetPc, targetPcExpression);
    return createdPcWrite.toASTString() + SeqSyntax.SPACE + pcWrite.toASTString();
  }

  @Override
  public Optional<Integer> getTargetPc() {
    return targetPc;
  }

  @Override
  public Optional<CExpression> getTargetPcExpression() {
    return targetPcExpression;
  }

  @NonNull
  @Override
  public SeqThreadCreationStatement cloneWithTargetPc(CExpression pTargetPc) {
    return new SeqThreadCreationStatement(createdThreadId, threadId, pTargetPc, pcVariables);
  }

  @Override
  public boolean alwaysWritesPc() {
    return true;
  }

  @Override
  public boolean onlyWritesPc() {
    return false;
  }
}
