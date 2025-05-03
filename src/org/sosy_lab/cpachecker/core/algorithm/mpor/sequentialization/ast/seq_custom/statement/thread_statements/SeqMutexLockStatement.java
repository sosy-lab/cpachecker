// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqExpressions.SeqIntegerLiteralExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqThreadStatementClauseUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.goto_labels.SeqLoopHeadLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.SeqInjectedStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqStringUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

/**
 * Represents a statement that simulates calls to {@code pthread_mutex_lock} of the form:
 *
 * <p>{@code m_LOCKED = 1; THREADi_LOCKS_m = 0;}
 */
public class SeqMutexLockStatement implements SeqThreadStatement {

  private final Optional<SeqLoopHeadLabelStatement> loopHeadLabel;

  private final CIdExpression mutexLocked;

  public final CIdExpression threadLocksMutex;

  private final CLeftHandSide pcLeftHandSide;

  private final ImmutableSet<SubstituteEdge> substituteEdges;

  private final Optional<Integer> targetPc;

  private final Optional<String> targetGoto;

  private final ImmutableList<SeqInjectedStatement> injectedStatements;

  private final ImmutableList<SeqThreadStatement> concatenatedStatements;

  SeqMutexLockStatement(
      CIdExpression pMutexLocked,
      CIdExpression pThreadLocksMutex,
      CLeftHandSide pPcLeftHandSide,
      ImmutableSet<SubstituteEdge> pSubstituteEdges,
      int pTargetPc) {

    loopHeadLabel = Optional.empty();
    mutexLocked = pMutexLocked;
    threadLocksMutex = pThreadLocksMutex;
    pcLeftHandSide = pPcLeftHandSide;
    substituteEdges = pSubstituteEdges;
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
      ImmutableSet<SubstituteEdge> pSubstituteEdges,
      Optional<Integer> pTargetPc,
      Optional<String> pTargetGoto,
      ImmutableList<SeqInjectedStatement> pInjectedStatements,
      ImmutableList<SeqThreadStatement> pConcatenatedStatements) {

    loopHeadLabel = pLoopHeadLabel;
    mutexLocked = pMutexLocked;
    threadLocksMutex = pThreadLocksMutex;
    pcLeftHandSide = pPcLeftHandSide;
    substituteEdges = pSubstituteEdges;
    targetPc = pTargetPc;
    targetGoto = pTargetGoto;
    injectedStatements = pInjectedStatements;
    concatenatedStatements = pConcatenatedStatements;
  }

  @Override
  public String toASTString() throws UnrecognizedCodeException {
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
  public ImmutableSet<SubstituteEdge> getSubstituteEdges() {
    return substituteEdges;
  }

  @Override
  public Optional<Integer> getTargetPc() {
    return targetPc;
  }

  @Override
  public Optional<SeqLoopHeadLabelStatement> getLoopHeadLabel() {
    return loopHeadLabel;
  }

  @Override
  public ImmutableList<SeqInjectedStatement> getInjectedStatements() {
    return injectedStatements;
  }

  @Override
  public ImmutableList<SeqThreadStatement> getConcatenatedStatements() {
    return concatenatedStatements;
  }

  @Override
  public SeqMutexLockStatement cloneWithTargetPc(int pTargetPc) {
    return new SeqMutexLockStatement(
        loopHeadLabel,
        mutexLocked,
        threadLocksMutex,
        pcLeftHandSide,
        substituteEdges,
        Optional.of(pTargetPc),
        Optional.empty(),
        SeqThreadStatementClauseUtil.replaceTargetGotoLabel(injectedStatements, pTargetPc),
        concatenatedStatements);
  }

  @Override
  public SeqThreadStatement cloneWithTargetGoto(String pLabel) {
    return new SeqMutexLockStatement(
        loopHeadLabel,
        mutexLocked,
        threadLocksMutex,
        pcLeftHandSide,
        substituteEdges,
        Optional.empty(),
        Optional.of(pLabel),
        injectedStatements,
        concatenatedStatements);
  }

  @Override
  public SeqThreadStatement cloneWithInjectedStatements(
      ImmutableList<SeqInjectedStatement> pInjectedStatements) {

    return new SeqMutexLockStatement(
        loopHeadLabel,
        mutexLocked,
        threadLocksMutex,
        pcLeftHandSide,
        substituteEdges,
        targetPc,
        targetGoto,
        pInjectedStatements,
        concatenatedStatements);
  }

  @Override
  public SeqThreadStatement cloneWithLoopHeadLabel(SeqLoopHeadLabelStatement pLoopHeadLabel) {
    return new SeqMutexLockStatement(
        Optional.of(pLoopHeadLabel),
        mutexLocked,
        threadLocksMutex,
        pcLeftHandSide,
        substituteEdges,
        targetPc,
        targetGoto,
        injectedStatements,
        concatenatedStatements);
  }

  @Override
  public SeqThreadStatement cloneWithConcatenatedStatements(
      ImmutableList<SeqThreadStatement> pConcatenatedStatements) {

    return new SeqMutexLockStatement(
        loopHeadLabel,
        mutexLocked,
        threadLocksMutex,
        pcLeftHandSide,
        substituteEdges,
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
