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
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqStatements.SeqExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;

/**
 * Represents a statement that simulates calls to {@code pthread_mutex_unlock} of the form:
 *
 * <p>{@code __MPOR_SEQ__GLOBAL_14_m_LOCKED = 0; }
 */
public class SeqMutexUnlockStatement implements SeqCaseBlockStatement {

  private final CExpressionAssignmentStatement lockedFalse;

  private final CLeftHandSide pcLeftHandSide;

  private final Optional<Integer> targetPc;

  private final Optional<CExpression> targetPcExpression;

  protected SeqMutexUnlockStatement(
      CExpressionAssignmentStatement pLockedFalse, CLeftHandSide pPcLeftHandSide, int pTargetPc) {

    lockedFalse = pLockedFalse;
    pcLeftHandSide = pPcLeftHandSide;
    targetPc = Optional.of(pTargetPc);
    targetPcExpression = Optional.empty();
  }

  protected SeqMutexUnlockStatement(
      CExpressionAssignmentStatement pLockedFalse,
      CLeftHandSide pPcLeftHandSide,
      CExpression pTargetPc) {

    lockedFalse = pLockedFalse;
    pcLeftHandSide = pPcLeftHandSide;
    targetPc = Optional.empty();
    targetPcExpression = Optional.of(pTargetPc);
  }

  @Override
  public String toASTString() {
    CExpressionAssignmentStatement pcWrite =
        SeqExpressionAssignmentStatement.buildPcWriteByTargetPc(
            pcLeftHandSide, targetPc, targetPcExpression);
    return lockedFalse.toASTString() + SeqSyntax.SPACE + pcWrite.toASTString();
  }

  @Override
  public Optional<Integer> getTargetPc() {
    return targetPc;
  }

  @Override
  public Optional<CExpression> getTargetPcExpression() {
    return targetPcExpression;
  }

  @NonNull
  @Override
  public SeqMutexUnlockStatement cloneWithTargetPc(CExpression pTargetPc) {
    return new SeqMutexUnlockStatement(lockedFalse, pcLeftHandSide, pTargetPc);
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
