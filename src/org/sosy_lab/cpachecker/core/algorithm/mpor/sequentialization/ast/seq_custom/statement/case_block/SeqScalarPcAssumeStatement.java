// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block;

import java.util.Optional;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqStatement;

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
  public Optional<Integer> getTargetPc() {
    throw new UnsupportedOperationException(
        this.getClass().getSimpleName() + " do not have a target pc");
  }

  @Override
  public Optional<CExpression> getTargetPcExpression() {
    throw new UnsupportedOperationException(
        this.getClass().getSimpleName() + " do not have a target pc");
  }

  @NonNull
  @Override
  public SeqScalarPcAssumeStatement cloneWithTargetPc(CExpression pTargetPc) {
    // we do not clone this as it is not used for pruning, but just for the loop head assumption
    throw new UnsupportedOperationException(this.getClass().getSimpleName() + " cannot be cloned");
  }

  @Override
  public boolean alwaysWritesPc() {
    throw new UnsupportedOperationException(
        this.getClass().getSimpleName() + " are not part of POR");
  }

  @Override
  public boolean onlyWritesPc() {
    return false;
  }
}
