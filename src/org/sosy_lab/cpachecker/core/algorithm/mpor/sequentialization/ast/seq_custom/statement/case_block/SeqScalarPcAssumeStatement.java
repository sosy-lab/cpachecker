// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.goto_labels.SeqLoopHeadLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.SeqInjectedStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;

/**
 * Only used with {@code scalarPc} when assuming that the next thread is still active, e.g.
 *
 * <p>{@code switch(next_thread) { case 0: assume(pc0 != -1); break; ... }}
 */
public class SeqScalarPcAssumeStatement implements SeqCaseBlockStatement {

  private final SeqStatement statement;

  SeqScalarPcAssumeStatement(SeqStatement pStatement) {
    statement = pStatement;
  }

  @Override
  public String toASTString() {
    return statement.toASTString();
  }

  @Override
  public ImmutableSet<SubstituteEdge> getSubstituteEdges() {
    throw new UnsupportedOperationException(
        this.getClass().getSimpleName() + " do not have substitute edges");
  }

  @Override
  public Optional<Integer> getTargetPc() {
    throw new UnsupportedOperationException(
        this.getClass().getSimpleName() + " do not have a target pc");
  }

  @Override
  public Optional<SeqLoopHeadLabelStatement> getLoopHeadLabel() {
    throw new UnsupportedOperationException(
        this.getClass().getSimpleName() + " do not have a loop head label");
  }

  @Override
  public ImmutableList<SeqInjectedStatement> getInjectedStatements() {
    throw new UnsupportedOperationException(
        this.getClass().getSimpleName() + " do not have injected statements");
  }

  @Override
  public ImmutableList<SeqCaseBlockStatement> getConcatenatedStatements() {
    throw new UnsupportedOperationException(
        this.getClass().getSimpleName() + " do not have concatenated statements");
  }

  @Override
  public SeqCaseBlockStatement cloneWithTargetPc(int pTargetPc) {
    // we do not clone this as it is not used for pruning, but just for the loop head assumption
    throw new UnsupportedOperationException(this.getClass().getSimpleName() + " cannot be cloned");
  }

  @Override
  public SeqCaseBlockStatement cloneWithTargetGoto(String pLabel) {
    throw new UnsupportedOperationException(
        this.getClass().getSimpleName() + " do not have a target goto");
  }

  @Override
  public SeqCaseBlockStatement cloneWithInjectedStatements(
      ImmutableList<SeqInjectedStatement> pInjectedStatements) {

    // we do not clone this as it is not used for pruning, but just for the loop head assumption
    throw new UnsupportedOperationException(this.getClass().getSimpleName() + " cannot be cloned");
  }

  @Override
  public SeqCaseBlockStatement cloneWithLoopHeadLabel(SeqLoopHeadLabelStatement pLoopHeadLabel) {
    throw new UnsupportedOperationException(this.getClass().getSimpleName() + " cannot be cloned");
  }

  @Override
  public SeqCaseBlockStatement cloneWithConcatenatedStatements(
      ImmutableList<SeqCaseBlockStatement> pConcatenatedStatements) {

    // we do not clone this as it is not used for pruning, but just for the loop head assumption
    throw new UnsupportedOperationException(this.getClass().getSimpleName() + " cannot be cloned");
  }

  @Override
  public boolean isConcatenable() {
    return false;
  }

  @Override
  public boolean isCriticalSectionStart() {
    throw new UnsupportedOperationException(
        this.getClass().getSimpleName() + " are not part of POR");
  }

  @Override
  public boolean onlyWritesPc() {
    return false;
  }
}
