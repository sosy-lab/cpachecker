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
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
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

  private final int targetPc;

  /** Use this if the target pc is an {@code int}. */
  protected SeqBlankStatement(CLeftHandSide pPcLeftHandSide, int pTargetPc) {
    pcLeftHandSide = pPcLeftHandSide;
    targetPc = pTargetPc;
  }

  @Override
  public String toASTString() {
    CExpressionAssignmentStatement pcWrite =
        SeqExpressionAssignmentStatement.buildPcWrite(pcLeftHandSide, targetPc);
    return pcWrite.toASTString();
  }

  @Override
  public Optional<Integer> getTargetPc() {
    return Optional.of(targetPc);
  }

  @Override
  public Optional<CIdExpression> getTargetPcExpression() {
    throw new UnsupportedOperationException(
        this.getClass().getSimpleName() + " does not have targetPcExpressions");
  }

  @NonNull
  @Override
  public SeqBlankStatement cloneWithTargetPc(int pTargetPc) {
    return new SeqBlankStatement(pcLeftHandSide, pTargetPc);
    // we never want to clone blank statements
    // throw new UnsupportedOperationException(this.getClass().getSimpleName() + " cannot be
    // cloned");
  }

  @NonNull
  @Override
  public SeqBlankStatement cloneWithTargetPc(CIdExpression pTargetPc) {
    // we never want to clone blank statements
    throw new UnsupportedOperationException(this.getClass().getSimpleName() + " cannot be cloned");
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
