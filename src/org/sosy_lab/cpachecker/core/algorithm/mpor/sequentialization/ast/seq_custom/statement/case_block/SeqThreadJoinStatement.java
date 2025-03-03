// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqExpressions.SeqBinaryExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqExpressions.SeqIntegerLiteralExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqStatements.SeqExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqControlFlowStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqControlFlowStatement.SeqControlFlowStatementType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost.pc.PcVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqStringUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
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

  private final CIdExpression threadJoins;

  private final int threadId;

  private final Optional<Integer> targetPc;

  private final Optional<CExpression> targetPcExpression;

  private final Optional<ImmutableList<SeqCaseBlockStatement>> concatenatedStatements;

  private final PcVariables pcVariables;

  private final CBinaryExpressionBuilder binaryExpressionBuilder;

  private final CBinaryExpression pcUnequalExitPc;

  SeqThreadJoinStatement(
      int pJoinedThreadId,
      CIdExpression pThreadJoins,
      int pThreadId,
      int pTargetPc,
      PcVariables pPcVariables,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    joinedThreadId = pJoinedThreadId;
    threadJoins = pThreadJoins;
    threadId = pThreadId;
    targetPc = Optional.of(pTargetPc);
    targetPcExpression = Optional.empty();
    concatenatedStatements = Optional.empty();
    pcVariables = pPcVariables;
    binaryExpressionBuilder = pBinaryExpressionBuilder;
    pcUnequalExitPc =
        SeqBinaryExpression.buildPcUnequalExitPc(
            pcVariables, joinedThreadId, binaryExpressionBuilder);
  }

  private SeqThreadJoinStatement(
      int pJoinedThreadId,
      CIdExpression pThreadJoins,
      int pThreadId,
      CExpression pTargetPc,
      PcVariables pPcVariables,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    joinedThreadId = pJoinedThreadId;
    threadJoins = pThreadJoins;
    threadId = pThreadId;
    targetPc = Optional.empty();
    targetPcExpression = Optional.of(pTargetPc);
    concatenatedStatements = Optional.empty();
    pcVariables = pPcVariables;
    binaryExpressionBuilder = pBinaryExpressionBuilder;
    pcUnequalExitPc =
        SeqBinaryExpression.buildPcUnequalExitPc(
            pcVariables, joinedThreadId, binaryExpressionBuilder);
  }

  private SeqThreadJoinStatement(
      int pJoinedThreadId,
      CIdExpression pThreadJoins,
      int pThreadId,
      ImmutableList<SeqCaseBlockStatement> pConcatenatedStatements,
      PcVariables pPcVariables,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    joinedThreadId = pJoinedThreadId;
    threadJoins = pThreadJoins;
    threadId = pThreadId;
    targetPc = Optional.empty();
    targetPcExpression = Optional.empty();
    concatenatedStatements = Optional.of(pConcatenatedStatements);
    pcVariables = pPcVariables;
    binaryExpressionBuilder = pBinaryExpressionBuilder;
    pcUnequalExitPc =
        SeqBinaryExpression.buildPcUnequalExitPc(
            pcVariables, joinedThreadId, binaryExpressionBuilder);
  }

  @Override
  public String toASTString() {
    SeqControlFlowStatement ifJoinedThreadActive =
        new SeqControlFlowStatement(pcUnequalExitPc, SeqControlFlowStatementType.IF);
    CExpressionAssignmentStatement joinsTrue =
        new CExpressionAssignmentStatement(
            FileLocation.DUMMY, threadJoins, SeqIntegerLiteralExpression.INT_1);
    CExpressionAssignmentStatement joinsFalse =
        new CExpressionAssignmentStatement(
            FileLocation.DUMMY, threadJoins, SeqIntegerLiteralExpression.INT_0);
    CExpressionAssignmentStatement pcWrite =
        SeqExpressionAssignmentStatement.buildPcWriteByTargetPc(
            pcVariables.get(threadId), targetPc, targetPcExpression);
    String elseStatements =
        SeqStringUtil.wrapInCurlyInwards(
            joinsFalse.toASTString() + SeqSyntax.SPACE + pcWrite.toASTString());
    return ifJoinedThreadActive.toASTString()
        + SeqSyntax.SPACE
        + SeqStringUtil.wrapInCurlyInwards(joinsTrue.toASTString())
        + SeqSyntax.SPACE
        + elseJoinedThreadNotActive.toASTString()
        + SeqSyntax.SPACE
        + elseStatements;
  }

  @Override
  public Optional<Integer> getTargetPc() {
    return targetPc;
  }

  @Override
  public Optional<CExpression> getTargetPcExpression() {
    return targetPcExpression;
  }

  @Override
  public Optional<ImmutableList<SeqCaseBlockStatement>> getConcatenatedStatements() {
    return concatenatedStatements;
  }

  @Override
  public SeqThreadJoinStatement cloneWithTargetPc(CExpression pTargetPc)
      throws UnrecognizedCodeException {

    return new SeqThreadJoinStatement(
        joinedThreadId, threadJoins, threadId, pTargetPc, pcVariables, binaryExpressionBuilder);
  }

  @Override
  public SeqCaseBlockStatement cloneWithConcatenatedStatements(
      ImmutableList<SeqCaseBlockStatement> pConcatenatedStatements)
      throws UnrecognizedCodeException {
    return new SeqThreadJoinStatement(
        joinedThreadId,
        threadJoins,
        threadId,
        pConcatenatedStatements,
        pcVariables,
        binaryExpressionBuilder);
  }

  @Override
  public boolean alwaysWritesPc() {
    return false;
  }

  @Override
  public boolean onlyWritesPc() {
    return false;
  }
}
