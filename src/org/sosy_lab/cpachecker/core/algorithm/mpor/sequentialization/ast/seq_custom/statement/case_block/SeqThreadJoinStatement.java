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
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SeqUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqExpressions.SeqBinaryExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqExpressions.SeqIntegerLiteralExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqStatements;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqControlFlowStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqControlFlowStatement.SeqControlFlowStatementType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqSyntax;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

/**
 * Represents a statement that simulates calls to {@code pthread_join} of the form:
 *
 * <p>{@code if (pc[1] != -1) { __MPOR_SEQ__THREAD0_JOINS_THREAD1 = 1; }}
 *
 * <p>{@code else { __MPOR_SEQ__THREAD0_JOINS_THREAD1 = 0; pc[0] = ...; }}
 */
public class SeqThreadJoinStatement implements SeqCaseBlockStatement {

  private static final SeqControlFlowStatement elseJoinedThreadNotActive =
      new SeqControlFlowStatement();

  private final int joinedThreadId;

  private final CBinaryExpression pcNotExitPc;

  private final CIdExpression threadJoins;

  private final int threadId;

  private final int targetPc;

  public SeqThreadJoinStatement(
      int pJoinedThreadId, CIdExpression pThreadJoins, int pThreadId, int pTargetPc)
      throws UnrecognizedCodeException {

    joinedThreadId = pJoinedThreadId;
    pcNotExitPc = buildPcNotExitPc(joinedThreadId);
    threadJoins = pThreadJoins;
    threadId = pThreadId;
    targetPc = pTargetPc;
  }

  private CBinaryExpression buildPcNotExitPc(int pJoinedThreadId) throws UnrecognizedCodeException {
    return SeqBinaryExpression.buildBinaryExpression(
        SeqExpressions.getPcExpression(pJoinedThreadId),
        SeqIntegerLiteralExpression.INT_EXIT_PC,
        BinaryOperator.NOT_EQUALS);
  }

  @Override
  public String toASTString() {
    SeqControlFlowStatement ifJoinedThreadActive =
        new SeqControlFlowStatement(pcNotExitPc, SeqControlFlowStatementType.IF);
    CExpressionAssignmentStatement joinsTrue =
        new CExpressionAssignmentStatement(
            FileLocation.DUMMY, threadJoins, SeqIntegerLiteralExpression.INT_1);
    CExpressionAssignmentStatement joinsFalse =
        new CExpressionAssignmentStatement(
            FileLocation.DUMMY, threadJoins, SeqIntegerLiteralExpression.INT_0);
    CExpressionAssignmentStatement pcUpdate = SeqStatements.buildPcUpdate(threadId, targetPc);
    String elseStmts =
        SeqUtil.wrapInCurlyInwards(
            joinsFalse.toASTString() + SeqSyntax.SPACE + pcUpdate.toASTString());
    return ifJoinedThreadActive.toASTString()
        + SeqSyntax.SPACE
        + SeqUtil.wrapInCurlyInwards(joinsTrue.toASTString())
        + SeqSyntax.SPACE
        + elseJoinedThreadNotActive.toASTString()
        + SeqSyntax.SPACE
        + elseStmts;
  }

  @Override
  public Optional<Integer> getTargetPc() {
    return Optional.of(targetPc);
  }

  @NonNull
  @Override
  public SeqThreadJoinStatement cloneWithTargetPc(int pTargetPc) throws UnrecognizedCodeException {
    return new SeqThreadJoinStatement(joinedThreadId, threadJoins, threadId, pTargetPc);
  }

  @Override
  public boolean alwaysUpdatesPc() {
    return false;
  }
}
