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
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqExpressions.SeqIntegerLiteralExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqControlFlowStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqControlFlowStatement.SeqControlFlowStatementType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqStringUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;

/**
 * Represents a statement that simulates calls to {@code pthread_mutex_lock} of the form:
 *
 * <p>{@code if (__MPOR_SEQ__GLOBAL_14_m_LOCKED) { __MPOR_SEQ__THREAD1_AWAITS_GLOBAL_14_m = 1; }}
 *
 * <p>{@code else { __MPOR_SEQ__THREAD1_AWAITS_GLOBAL_14_m = 0; __MPOR_SEQ__GLOBAL_14_m_LOCKED = 1;
 * pc[...] = ...; }}
 */
public class SeqMutexLockStatement implements SeqCaseBlockStatement {

  private static final SeqControlFlowStatement elseNotLocked = new SeqControlFlowStatement();

  private final CIdExpression mutexLocked;

  private final CIdExpression threadLocksMutex;

  private final CLeftHandSide pcLeftHandSide;

  private final Optional<Integer> targetPc;

  private final Optional<CExpression> targetPcExpression;

  private final Optional<ImmutableList<SeqCaseBlockStatement>> concatenatedStatements;

  SeqMutexLockStatement(
      CIdExpression pMutexLocked,
      CIdExpression pThreadLocksMutex,
      CLeftHandSide pPcLeftHandSide,
      int pTargetPc) {

    mutexLocked = pMutexLocked;
    threadLocksMutex = pThreadLocksMutex;
    pcLeftHandSide = pPcLeftHandSide;
    targetPc = Optional.of(pTargetPc);
    targetPcExpression = Optional.empty();
    concatenatedStatements = Optional.empty();
  }

  private SeqMutexLockStatement(
      CIdExpression pMutexLocked,
      CIdExpression pThreadLocksMutex,
      CLeftHandSide pPcLeftHandSide,
      CExpression pTargetPc) {

    mutexLocked = pMutexLocked;
    threadLocksMutex = pThreadLocksMutex;
    pcLeftHandSide = pPcLeftHandSide;
    targetPc = Optional.empty();
    targetPcExpression = Optional.of(pTargetPc);
    concatenatedStatements = Optional.empty();
  }

  private SeqMutexLockStatement(
      CIdExpression pMutexLocked,
      CIdExpression pThreadLocksMutex,
      CLeftHandSide pPcLeftHandSide,
      ImmutableList<SeqCaseBlockStatement> pConcatenatedStatements) {

    mutexLocked = pMutexLocked;
    threadLocksMutex = pThreadLocksMutex;
    pcLeftHandSide = pPcLeftHandSide;
    targetPc = Optional.empty();
    targetPcExpression = Optional.empty();
    concatenatedStatements = Optional.of(pConcatenatedStatements);
  }

  @Override
  public String toASTString() {
    SeqControlFlowStatement ifLocked =
        new SeqControlFlowStatement(mutexLocked, SeqControlFlowStatementType.IF);
    CExpressionAssignmentStatement setLocksTrue =
        new CExpressionAssignmentStatement(
            FileLocation.DUMMY, threadLocksMutex, SeqIntegerLiteralExpression.INT_1);
    CExpressionAssignmentStatement setLockedTrue =
        new CExpressionAssignmentStatement(
            FileLocation.DUMMY, mutexLocked, SeqIntegerLiteralExpression.INT_1);
    CExpressionAssignmentStatement setLocksFalse =
        new CExpressionAssignmentStatement(
            FileLocation.DUMMY, threadLocksMutex, SeqIntegerLiteralExpression.INT_0);

    String targetStatements =
        SeqStringUtil.buildTargetStatements(
            pcLeftHandSide, targetPc, targetPcExpression, concatenatedStatements);

    String elseStatements =
        SeqStringUtil.wrapInCurlyInwards(
            setLockedTrue.toASTString()
                + SeqSyntax.SPACE
                + setLocksFalse.toASTString()
                + SeqSyntax.SPACE
                + targetStatements);

    return ifLocked.toASTString()
        + SeqSyntax.SPACE
        + SeqStringUtil.wrapInCurlyInwards(setLocksTrue.toASTString())
        + SeqSyntax.SPACE
        + elseNotLocked.toASTString()
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
  public SeqMutexLockStatement cloneWithTargetPc(CExpression pTargetPc) {
    return new SeqMutexLockStatement(mutexLocked, threadLocksMutex, pcLeftHandSide, pTargetPc);
  }

  @Override
  public SeqCaseBlockStatement cloneWithConcatenatedStatements(
      ImmutableList<SeqCaseBlockStatement> pConcatenatedStatements) {
    return new SeqMutexLockStatement(
        mutexLocked, threadLocksMutex, pcLeftHandSide, pConcatenatedStatements);
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
