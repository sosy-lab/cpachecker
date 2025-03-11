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
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.injected.SeqCaseBlockInjectedStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqStringUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;

/**
 * Represents a statement that simulates calls to {@code pthread_mutex_unlock} of the form:
 *
 * <p>{@code __MPOR_SEQ__GLOBAL_14_m_LOCKED = 0; }
 */
public class SeqMutexUnlockStatement implements SeqCaseBlockStatement {

  private final CExpressionAssignmentStatement lockedFalse;

  private final CLeftHandSide pcLeftHandSide;

  private final Optional<Integer> targetPc;

  private final ImmutableList<SeqCaseBlockInjectedStatement> injectedStatements;

  private final ImmutableList<SeqCaseBlockStatement> concatenatedStatements;

  SeqMutexUnlockStatement(
      CExpressionAssignmentStatement pLockedFalse, CLeftHandSide pPcLeftHandSide, int pTargetPc) {

    lockedFalse = pLockedFalse;
    pcLeftHandSide = pPcLeftHandSide;
    targetPc = Optional.of(pTargetPc);
    injectedStatements = ImmutableList.of();
    concatenatedStatements = ImmutableList.of();
  }

  private SeqMutexUnlockStatement(
      CExpressionAssignmentStatement pLockedFalse,
      CLeftHandSide pPcLeftHandSide,
      Optional<Integer> pTargetPc,
      ImmutableList<SeqCaseBlockInjectedStatement> pInjectedStatements,
      ImmutableList<SeqCaseBlockStatement> pConcatenatedStatements) {

    lockedFalse = pLockedFalse;
    pcLeftHandSide = pPcLeftHandSide;
    targetPc = pTargetPc;
    injectedStatements = pInjectedStatements;
    concatenatedStatements = pConcatenatedStatements;
  }

  @Override
  public String toASTString() {
    String targetStatements =
        SeqStringUtil.buildTargetStatements(
            pcLeftHandSide, targetPc, injectedStatements, concatenatedStatements);
    return lockedFalse.toASTString() + SeqSyntax.SPACE + targetStatements;
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
  public SeqMutexUnlockStatement cloneWithTargetPc(int pTargetPc) {
    return new SeqMutexUnlockStatement(
        lockedFalse,
        pcLeftHandSide,
        Optional.of(pTargetPc),
        injectedStatements,
        concatenatedStatements);
  }

  @Override
  public SeqCaseBlockStatement cloneWithInjectedStatements(
      ImmutableList<SeqCaseBlockInjectedStatement> pInjectedStatements) {

    return new SeqMutexUnlockStatement(
        lockedFalse, pcLeftHandSide, targetPc, pInjectedStatements, concatenatedStatements);
  }

  @Override
  public SeqCaseBlockStatement cloneWithConcatenatedStatements(
      ImmutableList<SeqCaseBlockStatement> pConcatenatedStatements) {

    return new SeqMutexUnlockStatement(
        lockedFalse, pcLeftHandSide, Optional.empty(), injectedStatements, pConcatenatedStatements);
  }

  @Override
  public boolean isConcatenable() {
    return true;
  }

  @Override
  public boolean isCriticalSectionStart() {
    return true;
  }

  @Override
  public boolean onlyWritesPc() {
    return false;
  }
}
