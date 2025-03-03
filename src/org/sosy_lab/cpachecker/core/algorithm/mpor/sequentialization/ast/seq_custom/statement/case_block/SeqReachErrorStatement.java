// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.Sequentialization;

/**
 * Represents an injected call to {@code reach_error} so that the sequentialization actually adopts
 * {@code reach_error}s from the input program for the property {@code unreach-call.prp} instead of
 * inlining the function.
 */
public class SeqReachErrorStatement implements SeqCaseBlockStatement {

  SeqReachErrorStatement() {}

  @Override
  public String toASTString() {
    return Sequentialization.inputReachErrorDummy;
  }

  @Override
  public Optional<Integer> getTargetPc() {
    // TODO test if we can also throw an exception here?
    return Optional.empty();
  }

  @Override
  public Optional<CExpression> getTargetPcExpression() {
    // TODO test if we can also throw an exception here?
    return Optional.empty();
  }

  @Override
  public Optional<ImmutableList<SeqCaseBlockStatement>> getConcatenatedStatements() {
    throw new UnsupportedOperationException(
        this.getClass().getSimpleName() + " do not have concatenated statements");
  }

  @Override
  public SeqReachErrorStatement cloneWithTargetPc(CExpression pTargetPc) {
    throw new UnsupportedOperationException(
        this.getClass().getSimpleName() + " do not have targetPcs and cannot be cloned");
  }

  @Override
  public SeqCaseBlockStatement cloneWithConcatenatedStatements(
      ImmutableList<SeqCaseBlockStatement> pConcatenatedStatements) {
    throw new UnsupportedOperationException(
        this.getClass().getSimpleName()
            + " do not have concatenated statements and cannot be cloned");
  }

  @Override
  // TODO this should technically return false?
  public boolean alwaysWritesPc() {
    return true;
  }

  @Override
  public boolean onlyWritesPc() {
    return false;
  }
}
