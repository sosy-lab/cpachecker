// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqExpressions.SeqIntegerLiteralExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqControlFlowStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqControlFlowStatement.SeqControlFlowStatementType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.injected.SeqCaseBlockInjectedStatement;
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

  private final ImmutableList<SeqCaseBlockInjectedStatement> injectedStatements;

  private final ImmutableList<SeqCaseBlockStatement> concatenatedStatements;

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
    injectedStatements = ImmutableList.of();
    concatenatedStatements = ImmutableList.of();
    pcVariables = pPcVariables;
    binaryExpressionBuilder = pBinaryExpressionBuilder;
    pcUnequalExitPc =
        SeqExpressionBuilder.buildPcUnequalExitPc(
            pcVariables, joinedThreadId, binaryExpressionBuilder);
  }

  private SeqThreadJoinStatement(
      int pJoinedThreadId,
      CIdExpression pThreadJoins,
      int pThreadId,
      Optional<Integer> pTargetPc,
      ImmutableList<SeqCaseBlockInjectedStatement> pInjectedStatements,
      ImmutableList<SeqCaseBlockStatement> pConcatenatedStatements,
      PcVariables pPcVariables,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    joinedThreadId = pJoinedThreadId;
    threadJoins = pThreadJoins;
    threadId = pThreadId;
    targetPc = pTargetPc;
    injectedStatements = pInjectedStatements;
    concatenatedStatements = pConcatenatedStatements;
    pcVariables = pPcVariables;
    binaryExpressionBuilder = pBinaryExpressionBuilder;
    pcUnequalExitPc =
        SeqExpressionBuilder.buildPcUnequalExitPc(
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
    String targetStatements =
        SeqStringUtil.buildTargetStatements(
            pcVariables.get(threadId), targetPc, injectedStatements, concatenatedStatements);
    String elseStatements =
        SeqStringUtil.wrapInCurlyInwards(
            joinsFalse.toASTString() + SeqSyntax.SPACE + targetStatements);
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
  public ImmutableList<SeqCaseBlockInjectedStatement> getInjectedStatements() {
    return injectedStatements;
  }

  @Override
  public ImmutableList<SeqCaseBlockStatement> getConcatenatedStatements() {
    return concatenatedStatements;
  }

  @Override
  public SeqThreadJoinStatement cloneWithTargetPc(int pTargetPc) throws UnrecognizedCodeException {

    return new SeqThreadJoinStatement(
        joinedThreadId, threadJoins, threadId, pTargetPc, pcVariables, binaryExpressionBuilder);
  }

  @Override
  public SeqCaseBlockStatement cloneWithInjectedStatements(
      ImmutableList<SeqCaseBlockInjectedStatement> pInjectedStatements)
      throws UnrecognizedCodeException {

    return new SeqThreadJoinStatement(
        joinedThreadId,
        threadJoins,
        threadId,
        targetPc,
        pInjectedStatements,
        concatenatedStatements,
        pcVariables,
        binaryExpressionBuilder);
  }

  @Override
  public SeqCaseBlockStatement cloneWithConcatenatedStatements(
      ImmutableList<SeqCaseBlockStatement> pConcatenatedStatements)
      throws UnrecognizedCodeException {

    return new SeqThreadJoinStatement(
        joinedThreadId,
        threadJoins,
        threadId,
        Optional.empty(),
        injectedStatements,
        pConcatenatedStatements,
        pcVariables,
        binaryExpressionBuilder);
  }

  @Override
  public boolean isConcatenable() {
    return true;
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
