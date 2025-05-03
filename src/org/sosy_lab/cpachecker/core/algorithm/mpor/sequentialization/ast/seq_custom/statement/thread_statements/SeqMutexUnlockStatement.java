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
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqThreadStatementClauseUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.goto_labels.SeqLoopHeadLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.SeqInjectedStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqStringUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

/**
 * Represents a statement that simulates calls to {@code pthread_mutex_unlock} of the form:
 *
 * <p>{@code __MPOR_SEQ__GLOBAL_14_m_LOCKED = 0; }
 */
public class SeqMutexUnlockStatement implements SeqThreadStatement {

  private final Optional<SeqLoopHeadLabelStatement> loopHeadLabel;

  private final CExpressionAssignmentStatement lockedFalse;

  private final CLeftHandSide pcLeftHandSide;

  private final ImmutableSet<SubstituteEdge> substituteEdges;

  private final Optional<Integer> targetPc;

  private final Optional<String> targetGoto;

  private final ImmutableList<SeqInjectedStatement> injectedStatements;

  private final ImmutableList<SeqThreadStatement> concatenatedStatements;

  SeqMutexUnlockStatement(
      CExpressionAssignmentStatement pLockedFalse,
      CLeftHandSide pPcLeftHandSide,
      ImmutableSet<SubstituteEdge> pSubstituteEdges,
      int pTargetPc) {

    loopHeadLabel = Optional.empty();
    lockedFalse = pLockedFalse;
    pcLeftHandSide = pPcLeftHandSide;
    substituteEdges = pSubstituteEdges;
    targetPc = Optional.of(pTargetPc);
    targetGoto = Optional.empty();
    injectedStatements = ImmutableList.of();
    concatenatedStatements = ImmutableList.of();
  }

  private SeqMutexUnlockStatement(
      Optional<SeqLoopHeadLabelStatement> pLoopHeadLabel,
      CExpressionAssignmentStatement pLockedFalse,
      CLeftHandSide pPcLeftHandSide,
      ImmutableSet<SubstituteEdge> pSubstituteEdges,
      Optional<Integer> pTargetPc,
      Optional<String> pTargetGoto,
      ImmutableList<SeqInjectedStatement> pInjectedStatements,
      ImmutableList<SeqThreadStatement> pConcatenatedStatements) {

    loopHeadLabel = pLoopHeadLabel;
    lockedFalse = pLockedFalse;
    pcLeftHandSide = pPcLeftHandSide;
    substituteEdges = pSubstituteEdges;
    targetPc = pTargetPc;
    targetGoto = pTargetGoto;
    injectedStatements = pInjectedStatements;
    concatenatedStatements = pConcatenatedStatements;
  }

  @Override
  public String toASTString() throws UnrecognizedCodeException {
    String targetStatements =
        SeqStringUtil.buildTargetStatements(
            pcLeftHandSide, targetPc, targetGoto, injectedStatements, concatenatedStatements);
    return SeqStringUtil.buildLoopHeadLabel(loopHeadLabel)
        + lockedFalse.toASTString()
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
  public SeqMutexUnlockStatement cloneWithTargetPc(int pTargetPc) {
    return new SeqMutexUnlockStatement(
        loopHeadLabel,
        lockedFalse,
        pcLeftHandSide,
        substituteEdges,
        Optional.of(pTargetPc),
        Optional.empty(),
        SeqThreadStatementClauseUtil.replaceTargetGotoLabel(injectedStatements, pTargetPc),
        concatenatedStatements);
  }

  @Override
  public SeqThreadStatement cloneWithTargetGoto(String pLabel) {
    return new SeqMutexUnlockStatement(
        loopHeadLabel,
        lockedFalse,
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

    return new SeqMutexUnlockStatement(
        loopHeadLabel,
        lockedFalse,
        pcLeftHandSide,
        substituteEdges,
        targetPc,
        targetGoto,
        pInjectedStatements,
        concatenatedStatements);
  }

  @Override
  public SeqThreadStatement cloneWithLoopHeadLabel(SeqLoopHeadLabelStatement pLoopHeadLabel) {
    return new SeqMutexUnlockStatement(
        Optional.of(pLoopHeadLabel),
        lockedFalse,
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

    return new SeqMutexUnlockStatement(
        loopHeadLabel,
        lockedFalse,
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
    return true;
  }

  @Override
  public boolean onlyWritesPc() {
    return false;
  }
}
