// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block;

import java.util.Optional;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.Sequentialization;

/**
 * Represents an injected call to {@code reach_error} so that the sequentialization actually adopts
 * {@code reach_error}s from the input program for the property {@code unreach-call.prp} instead of
 * inlining the function.
 */
public class SeqReachErrorStatement implements SeqCaseBlockStatement {

  protected SeqReachErrorStatement() {}

  @Override
  public String toASTString() {
    return Sequentialization.inputReachErrorDummy;
  }

  @Override
  public Optional<Integer> getTargetPc() {
    return Optional.empty();
  }

  @Override
  public Optional<CExpression> getTargetPcExpression() {
    return Optional.empty();
  }

  @NonNull
  @Override
  public SeqReachErrorStatement cloneWithTargetPc(CExpression pTargetPc) {
    throw new UnsupportedOperationException(
        this.getClass().getSimpleName() + " do not have targetPcs");
  }

  @Override
  public boolean alwaysWritesPc() {
    return true;
  }

  @Override
  public boolean onlyWritesPc() {
    return false;
  }
}
