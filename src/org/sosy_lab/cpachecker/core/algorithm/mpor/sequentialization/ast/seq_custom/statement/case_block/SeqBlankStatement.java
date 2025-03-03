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
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqStatements.SeqExpressionAssignmentStatement;

/**
 * Represents a blank case block which only has a {@code pc} update. All blank statements are later
 * pruned from the sequentialization.
 *
 * <p>E.g. {@code case m: pc[thread_id] = n; continue;}
 */
public class SeqBlankStatement implements SeqCaseBlockStatement {

  private final CLeftHandSide pcLeftHandSide;

  private final Optional<Integer> targetPc;

  private final Optional<CExpression> targetPcExpression;

  /** Use this if the target pc is an {@code int}. */
  SeqBlankStatement(CLeftHandSide pPcLeftHandSide, int pTargetPc) {
    pcLeftHandSide = pPcLeftHandSide;
    targetPc = Optional.of(pTargetPc);
    targetPcExpression = Optional.empty();
  }

  private SeqBlankStatement(CLeftHandSide pPcLeftHandSide, CExpression pTargetPcExpression) {
    pcLeftHandSide = pPcLeftHandSide;
    targetPc = Optional.empty();
    targetPcExpression = Optional.of(pTargetPcExpression);
  }

  @Override
  public String toASTString() {
    CExpressionAssignmentStatement pcWrite =
        SeqExpressionAssignmentStatement.buildPcWriteByTargetPc(
            pcLeftHandSide, targetPc, targetPcExpression);
    return pcWrite.toASTString();
  }

  @Override
  public Optional<Integer> getTargetPc() {
    return targetPc;
  }

  @Override
  public Optional<CExpression> getTargetPcExpression() {
    return targetPcExpression;
  }

  @Override
  public Optional<ImmutableList<SeqCaseBlockStatement>> getConcatenatedStatements() {
    // this should never be called because we concatenate after pruning (no blanks left)
    throw new UnsupportedOperationException(
        this.getClass().getName() + " do not have concatenated statements");
  }

  @Override
  public SeqBlankStatement cloneWithTargetPc(CExpression pTargetPc) {
    return new SeqBlankStatement(pcLeftHandSide, pTargetPc);
  }

  @Override
  public SeqCaseBlockStatement cloneWithConcatenatedStatements(
      ImmutableList<SeqCaseBlockStatement> pConcatenatedStatements) {
    // this should never be called because we concatenate after pruning (no blanks left)
    throw new UnsupportedOperationException(
        this.getClass().getName() + " do not have concatenated statements");
  }

  @Override
  public boolean alwaysWritesPc() {
    return true;
  }

  @Override
  public boolean onlyWritesPc() {
    return true;
  }
}
