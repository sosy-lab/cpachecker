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
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.Sequentialization;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqStatements.SeqExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;

/**
 * Represents an injected call to {@code reach_error} so that the sequentialization actually adopts
 * {@code reach_error}s from the input program for the property {@code unreach-call.prp} instead of
 * inlining the function.
 */
public class SeqReachErrorStatement implements SeqCaseBlockStatement {

  private final CLeftHandSide pcLeftHandSide;

  private final int targetPc;

  SeqReachErrorStatement(CLeftHandSide pPcLeftHandSide) {
    pcLeftHandSide = pPcLeftHandSide;
    // reach_error calls may not stop a thread, we enforce it
    targetPc = Sequentialization.EXIT_PC;
  }

  @Override
  public String toASTString() {
    CExpressionAssignmentStatement pcWrite =
        SeqExpressionAssignmentStatement.buildPcWrite(pcLeftHandSide, targetPc);
    return Sequentialization.inputReachErrorDummy + SeqSyntax.SPACE + pcWrite.toASTString();
  }

  @Override
  public Optional<Integer> getTargetPc() {
    return Optional.of(targetPc);
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
  public boolean isConcatenable() {
    return false;
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
