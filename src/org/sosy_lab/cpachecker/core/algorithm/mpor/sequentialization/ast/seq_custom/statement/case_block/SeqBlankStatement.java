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
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.injected.SeqCaseBlockInjectedStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqStringUtil;

/**
 * Represents a blank case block which only has a {@code pc} update.
 *
 * <p>E.g. {@code case m: pc[thread_id] = n; continue;}
 */
public class SeqBlankStatement implements SeqCaseBlockStatement {

  private final CLeftHandSide pcLeftHandSide;

  private final Optional<Integer> targetPc;

  private final ImmutableList<SeqCaseBlockInjectedStatement> injectedStatements;

  /** Use this if the target pc is an {@code int}. */
  SeqBlankStatement(CLeftHandSide pPcLeftHandSide, int pTargetPc) {
    pcLeftHandSide = pPcLeftHandSide;
    targetPc = Optional.of(pTargetPc);
    injectedStatements = ImmutableList.of();
  }

  private SeqBlankStatement(
      CLeftHandSide pPcLeftHandSide,
      Optional<Integer> pTargetPc,
      ImmutableList<SeqCaseBlockInjectedStatement> pInjectedStatements) {

    pcLeftHandSide = pPcLeftHandSide;
    targetPc = pTargetPc;
    injectedStatements = pInjectedStatements;
  }

  @Override
  public String toASTString() {
    return SeqStringUtil.buildTargetStatements(
        pcLeftHandSide, targetPc, injectedStatements, ImmutableList.of());
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
    // this should never be called because we concatenate after pruning (no blanks left)
    throw new UnsupportedOperationException(
        this.getClass().getName() + " do not have concatenated statements");
  }

  @Override
  public SeqBlankStatement cloneWithTargetPc(int pTargetPc) {
    return new SeqBlankStatement(pcLeftHandSide, Optional.of(pTargetPc), injectedStatements);
  }

  @Override
  public SeqCaseBlockStatement cloneWithInjectedStatements(
      ImmutableList<SeqCaseBlockInjectedStatement> pInjectedStatements) {

    return new SeqBlankStatement(pcLeftHandSide, targetPc, pInjectedStatements);
  }

  @Override
  public SeqCaseBlockStatement cloneWithConcatenatedStatements(
      ImmutableList<SeqCaseBlockStatement> pConcatenatedStatements) {
    // this should never be called because we concatenate after pruning (no blanks left)
    throw new UnsupportedOperationException(
        this.getClass().getName() + " do not have concatenated statements");
  }

  @Override
  public boolean isConcatenable() {
    return false;
  }

  @Override
  public boolean alwaysWritesPc() {
    return true;
  }

  @Override
  public boolean onlyWritesPc() {
    return injectedStatements.isEmpty();
  }
}
