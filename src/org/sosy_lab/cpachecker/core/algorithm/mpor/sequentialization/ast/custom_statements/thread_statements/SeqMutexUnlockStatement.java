// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.thread_statements;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqStatementBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqIntegerLiteralExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.goto_labels.SeqBlockLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.injected.SeqInjectedStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.thread_sync_flags.MutexLockedFlag;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

/**
 * Represents a statement that simulates calls to {@code pthread_mutex_unlock} of the form:
 *
 * <p>{@code m_LOCKED = 0; }
 */
public class SeqMutexUnlockStatement extends ASeqThreadStatement {

  private final MutexLockedFlag mutexLockedFlag;

  private final CLeftHandSide pcLeftHandSide;

  private final Optional<Integer> targetPc;

  private final Optional<SeqBlockLabelStatement> targetGoto;

  SeqMutexUnlockStatement(
      MPOROptions pOptions,
      MutexLockedFlag pMutexLockedFlag,
      CLeftHandSide pPcLeftHandSide,
      ImmutableSet<SubstituteEdge> pSubstituteEdges,
      int pTargetPc) {

    super(pOptions, pSubstituteEdges, ImmutableList.of());
    mutexLockedFlag = pMutexLockedFlag;
    pcLeftHandSide = pPcLeftHandSide;
    targetPc = Optional.of(pTargetPc);
    targetGoto = Optional.empty();
  }

  private SeqMutexUnlockStatement(
      MPOROptions pOptions,
      MutexLockedFlag pMutexLockedFlag,
      CLeftHandSide pPcLeftHandSide,
      ImmutableSet<SubstituteEdge> pSubstituteEdges,
      Optional<Integer> pTargetPc,
      Optional<SeqBlockLabelStatement> pTargetGoto,
      ImmutableList<SeqInjectedStatement> pInjectedStatements) {

    super(pOptions, pSubstituteEdges, pInjectedStatements);
    mutexLockedFlag = pMutexLockedFlag;
    pcLeftHandSide = pPcLeftHandSide;
    targetPc = pTargetPc;
    targetGoto = pTargetGoto;
  }

  @Override
  public String toASTString() throws UnrecognizedCodeException {
    CExpressionAssignmentStatement lockedFalseAssignment =
        SeqStatementBuilder.buildExpressionAssignmentStatement(
            mutexLockedFlag.idExpression, SeqIntegerLiteralExpressions.INT_0);
    String injected =
        SeqThreadStatementUtil.buildInjectedStatementsString(
            options, pcLeftHandSide, targetPc, targetGoto, injectedStatements);
    return lockedFalseAssignment.toASTString() + SeqSyntax.SPACE + injected;
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
  public Optional<SeqBlockLabelStatement> getTargetGoto() {
    return targetGoto;
  }

  @Override
  public ImmutableList<SeqInjectedStatement> getInjectedStatements() {
    return injectedStatements;
  }

  @Override
  public SeqMutexUnlockStatement withTargetPc(int pTargetPc) {
    return new SeqMutexUnlockStatement(
        options,
        mutexLockedFlag,
        pcLeftHandSide,
        substituteEdges,
        Optional.of(pTargetPc),
        Optional.empty(),
        injectedStatements);
  }

  @Override
  public ASeqThreadStatement withTargetGoto(SeqBlockLabelStatement pLabel) {
    return new SeqMutexUnlockStatement(
        options,
        mutexLockedFlag,
        pcLeftHandSide,
        substituteEdges,
        Optional.empty(),
        Optional.of(pLabel),
        injectedStatements);
  }

  @Override
  public ASeqThreadStatement withInjectedStatements(
      ImmutableList<SeqInjectedStatement> pInjectedStatements) {

    return new SeqMutexUnlockStatement(
        options,
        mutexLockedFlag,
        pcLeftHandSide,
        substituteEdges,
        targetPc,
        targetGoto,
        pInjectedStatements);
  }

  @Override
  public boolean isLinkable() {
    return true;
  }

  @Override
  public boolean synchronizesThreads() {
    return false;
  }

  @Override
  public boolean onlyWritesPc() {
    return false;
  }
}
