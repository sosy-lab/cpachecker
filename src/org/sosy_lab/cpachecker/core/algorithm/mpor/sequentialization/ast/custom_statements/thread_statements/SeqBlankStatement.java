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
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.goto_labels.SeqBlockLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.injected.SeqInjectedStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

/**
 * Represents a blank case block which only has a {@code pc} update.
 *
 * <p>E.g. {@code case m: pc[thread_id] = n; continue;}
 */
public class SeqBlankStatement implements SeqThreadStatement {

  private final MPOROptions options;

  private final CLeftHandSide pcLeftHandSide;

  private final Optional<Integer> targetPc;

  private final ImmutableList<SeqInjectedStatement> injectedStatements;

  /** Use this if the target pc is an {@code int}. */
  SeqBlankStatement(MPOROptions pOptions, CLeftHandSide pPcLeftHandSide, int pTargetPc) {
    options = pOptions;
    pcLeftHandSide = pPcLeftHandSide;
    targetPc = Optional.of(pTargetPc);
    injectedStatements = ImmutableList.of();
  }

  private SeqBlankStatement(
      MPOROptions pOptions,
      CLeftHandSide pPcLeftHandSide,
      Optional<Integer> pTargetPc,
      ImmutableList<SeqInjectedStatement> pInjectedStatements) {

    options = pOptions;
    pcLeftHandSide = pPcLeftHandSide;
    targetPc = pTargetPc;
    injectedStatements = pInjectedStatements;
  }

  @Override
  public String toASTString() throws UnrecognizedCodeException {
    return SeqThreadStatementUtil.buildInjectedStatementsString(
        options, pcLeftHandSide, targetPc, Optional.empty(), injectedStatements);
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
  public Optional<SeqBlockLabelStatement> getTargetGoto() {
    return Optional.empty();
  }

  @Override
  public ImmutableList<SeqInjectedStatement> getInjectedStatements() {
    return injectedStatements;
  }

  @Override
  public SeqBlankStatement cloneWithTargetPc(int pTargetPc) {
    return new SeqBlankStatement(
        options, pcLeftHandSide, Optional.of(pTargetPc), injectedStatements);
  }

  @Override
  public SeqThreadStatement cloneWithTargetGoto(SeqBlockLabelStatement pLabel) {
    throw new UnsupportedOperationException(this.getClass().getName() + " do not have target goto");
  }

  @Override
  public SeqThreadStatement cloneReplacingInjectedStatements(
      ImmutableList<SeqInjectedStatement> pReplacingInjectedStatements) {

    throw new UnsupportedOperationException(
        this.getClass().getName() + " do not have injected statements");
  }

  @Override
  public SeqThreadStatement cloneAppendingInjectedStatements(
      ImmutableList<SeqInjectedStatement> pAppendedInjectedStatements) {

    throw new UnsupportedOperationException(
        this.getClass().getName() + " do not have injected statements");
  }

  @Override
  public boolean isLinkable() {
    return false;
  }

  @Override
  public boolean synchronizesThreads() {
    return false;
  }

  @Override
  public boolean onlyWritesPc() {
    return injectedStatements.isEmpty();
  }
}
