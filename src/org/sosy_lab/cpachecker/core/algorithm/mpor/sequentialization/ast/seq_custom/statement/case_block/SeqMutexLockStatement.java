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
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqExpressions.SeqIntegerLiteralExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.goto_labels.SeqLoopHeadLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.injected.SeqInjectedStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqStringUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;

/**
 * Represents a statement that simulates calls to {@code pthread_mutex_lock} of the form:
 *
 * <p>{@code m_LOCKED = 1; THREADi_LOCKS_m = 0;}
 */
public class SeqMutexLockStatement implements SeqCaseBlockStatement {

  private final Optional<SeqLoopHeadLabelStatement> loopHeadLabel;

  private final CIdExpression mutexLocked;

  public final CIdExpression threadLocksMutex;

  private final CLeftHandSide pcLeftHandSide;

  private final Optional<Integer> targetPc;

  private final Optional<String> targetGoto;

  private final ImmutableList<SeqInjectedStatement> injectedStatements;

  private final ImmutableList<SeqCaseBlockStatement> concatenatedStatements;

  SeqMutexLockStatement(
      CIdExpression pMutexLocked,
      CIdExpression pThreadLocksMutex,
      CLeftHandSide pPcLeftHandSide,
      int pTargetPc) {

    loopHeadLabel = Optional.empty();
    mutexLocked = pMutexLocked;
    threadLocksMutex = pThreadLocksMutex;
    pcLeftHandSide = pPcLeftHandSide;
    targetPc = Optional.of(pTargetPc);
    targetGoto = Optional.empty();
    injectedStatements = ImmutableList.of();
    concatenatedStatements = ImmutableList.of();
  }

  private SeqMutexLockStatement(
      Optional<SeqLoopHeadLabelStatement> pLoopHeadLabel,
      CIdExpression pMutexLocked,
      CIdExpression pThreadLocksMutex,
      CLeftHandSide pPcLeftHandSide,
      Optional<Integer> pTargetPc,
      Optional<String> pTargetGoto,
      ImmutableList<SeqInjectedStatement> pInjectedStatements,
      ImmutableList<SeqCaseBlockStatement> pConcatenatedStatements) {

    loopHeadLabel = pLoopHeadLabel;
    mutexLocked = pMutexLocked;
    threadLocksMutex = pThreadLocksMutex;
    pcLeftHandSide = pPcLeftHandSide;
    targetPc = pTargetPc;
    targetGoto = pTargetGoto;
    injectedStatements = pInjectedStatements;
    concatenatedStatements = pConcatenatedStatements;
  }

  @Override
  public String toASTString() {
    CExpressionAssignmentStatement setLockedTrue =
        new CExpressionAssignmentStatement(
            FileLocation.DUMMY, mutexLocked, SeqIntegerLiteralExpression.INT_1);
    CExpressionAssignmentStatement setLocksFalse =
        new CExpressionAssignmentStatement(
            FileLocation.DUMMY, threadLocksMutex, SeqIntegerLiteralExpression.INT_0);

    String targetStatements =
        SeqStringUtil.buildTargetStatements(
            pcLeftHandSide, targetPc, targetGoto, injectedStatements, concatenatedStatements);

    return SeqStringUtil.buildLoopHeadLabel(loopHeadLabel)
        + setLockedTrue.toASTString()
        + SeqSyntax.SPACE
        + setLocksFalse.toASTString()
        + SeqSyntax.SPACE
        + targetStatements;
  }

  @Override
  public Optional<Integer> getTargetPc() {
    return targetPc;
  }

  @Override
  public ImmutableList<SeqInjectedStatement> getInjectedStatements() {
    return injectedStatements;
  }

  @Override
  public ImmutableList<SeqCaseBlockStatement> getConcatenatedStatements() {
    return concatenatedStatements;
  }

  @Override
  public SeqMutexLockStatement cloneWithTargetPc(int pTargetPc) {
    return new SeqMutexLockStatement(
        loopHeadLabel,
        mutexLocked,
        threadLocksMutex,
        pcLeftHandSide,
        Optional.of(pTargetPc),
        Optional.empty(),
        injectedStatements,
        concatenatedStatements);
  }

  @Override
  public SeqCaseBlockStatement cloneWithTargetGoto(String pLabel) {
    return new SeqMutexLockStatement(
        loopHeadLabel,
        mutexLocked,
        threadLocksMutex,
        pcLeftHandSide,
        Optional.empty(),
        Optional.of(pLabel),
        injectedStatements,
        concatenatedStatements);
  }

  @Override
  public SeqCaseBlockStatement cloneWithInjectedStatements(
      ImmutableList<SeqInjectedStatement> pInjectedStatements) {

    return new SeqMutexLockStatement(
        loopHeadLabel,
        mutexLocked,
        threadLocksMutex,
        pcLeftHandSide,
        targetPc,
        targetGoto,
        pInjectedStatements,
        concatenatedStatements);
  }

  @Override
  public SeqCaseBlockStatement cloneWithLoopHeadLabel(SeqLoopHeadLabelStatement pLoopHeadLabel) {
    return new SeqMutexLockStatement(
        Optional.of(pLoopHeadLabel),
        mutexLocked,
        threadLocksMutex,
        pcLeftHandSide,
        targetPc,
        targetGoto,
        injectedStatements,
        concatenatedStatements);
  }

  @Override
  public SeqCaseBlockStatement cloneWithConcatenatedStatements(
      ImmutableList<SeqCaseBlockStatement> pConcatenatedStatements) {

    return new SeqMutexLockStatement(
        loopHeadLabel,
        mutexLocked,
        threadLocksMutex,
        pcLeftHandSide,
        Optional.empty(),
        Optional.empty(),
        injectedStatements,
        pConcatenatedStatements);
  }

  @Override
  public boolean isConcatenable() {
    return true;
  }

  @Override
  public boolean isCriticalSectionStart() {
    return false;
  }

  @Override
  public boolean onlyWritesPc() {
    return false;
  }
}
