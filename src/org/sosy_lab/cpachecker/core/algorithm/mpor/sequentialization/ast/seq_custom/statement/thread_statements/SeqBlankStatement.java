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
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqThreadStatementClauseUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.goto_labels.SeqLoopHeadLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.SeqInjectedStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqStringUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

/**
 * Represents a blank case block which only has a {@code pc} update.
 *
 * <p>E.g. {@code case m: pc[thread_id] = n; continue;}
 */
public class SeqBlankStatement implements SeqThreadStatement {

  private final Optional<SeqLoopHeadLabelStatement> loopHeadLabel;

  private final CLeftHandSide pcLeftHandSide;

  private final Optional<Integer> targetPc;

  private final ImmutableList<SeqInjectedStatement> injectedStatements;

  private final ImmutableList<SeqThreadStatement> concatenatedStatements;

  /** Use this if the target pc is an {@code int}. */
  SeqBlankStatement(CLeftHandSide pPcLeftHandSide, int pTargetPc) {
    loopHeadLabel = Optional.empty();
    pcLeftHandSide = pPcLeftHandSide;
    targetPc = Optional.of(pTargetPc);
    injectedStatements = ImmutableList.of();
    concatenatedStatements = ImmutableList.of();
  }

  private SeqBlankStatement(
      Optional<SeqLoopHeadLabelStatement> pLoopHeadLabel,
      CLeftHandSide pPcLeftHandSide,
      Optional<Integer> pTargetPc,
      ImmutableList<SeqInjectedStatement> pInjectedStatements,
      ImmutableList<SeqThreadStatement> pConcatenatedStatements) {

    loopHeadLabel = pLoopHeadLabel;
    pcLeftHandSide = pPcLeftHandSide;
    targetPc = pTargetPc;
    injectedStatements = pInjectedStatements;
    concatenatedStatements = pConcatenatedStatements;
  }

  @Override
  public String toASTString() throws UnrecognizedCodeException {
    String targetStatements =
        SeqStringUtil.buildTargetStatements(
            pcLeftHandSide, targetPc, Optional.empty(), injectedStatements, ImmutableList.of());
    return SeqStringUtil.buildLoopHeadLabel(loopHeadLabel) + targetStatements;
  }

  @Override
  public ImmutableSet<SubstituteEdge> getSubstituteEdges() {
    return ImmutableSet.of();
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
  public SeqBlankStatement cloneWithTargetPc(int pTargetPc) {
    return new SeqBlankStatement(
        loopHeadLabel,
        pcLeftHandSide,
        Optional.of(pTargetPc),
        SeqThreadStatementClauseUtil.replaceTargetGotoLabel(injectedStatements, pTargetPc),
        concatenatedStatements);
  }

  @Override
  public SeqThreadStatement cloneWithTargetGoto(String pLabel) {
    throw new UnsupportedOperationException(this.getClass().getName() + " do not have target goto");
  }

  @Override
  public SeqThreadStatement cloneWithInjectedStatements(
      ImmutableList<SeqInjectedStatement> pInjectedStatements) {

    return new SeqBlankStatement(
        loopHeadLabel, pcLeftHandSide, targetPc, pInjectedStatements, concatenatedStatements);
  }

  @Override
  public SeqThreadStatement cloneWithLoopHeadLabel(SeqLoopHeadLabelStatement pLoopHeadLabel) {
    return new SeqBlankStatement(
        Optional.of(pLoopHeadLabel),
        pcLeftHandSide,
        targetPc,
        injectedStatements,
        concatenatedStatements);
  }

  @Override
  public SeqThreadStatement cloneWithConcatenatedStatements(
      ImmutableList<SeqThreadStatement> pConcatenatedStatements) {

    return new SeqBlankStatement(
        loopHeadLabel, pcLeftHandSide, targetPc, injectedStatements, pConcatenatedStatements);
  }

  @Override
  public boolean isConcatenable() {
    return false;
  }

  @Override
  public boolean isCriticalSectionStart() {
    return false;
  }

  @Override
  public boolean onlyWritesPc() {
    return injectedStatements.isEmpty();
  }
}
