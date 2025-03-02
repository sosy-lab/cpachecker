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

public class SeqAtomicEndStatement implements SeqCaseBlockStatement {

  private final CExpressionAssignmentStatement atomicLockedFalse;

  private final CLeftHandSide pcLeftHandSide;

  private final Optional<Integer> targetPc;

  private final Optional<CExpression> targetPcExpression;

  protected SeqAtomicEndStatement(
      CExpressionAssignmentStatement pAtomicLockedFalse,
      CLeftHandSide pPcLeftHandSide,
      int pTargetPc) {

    atomicLockedFalse = pAtomicLockedFalse;
    pcLeftHandSide = pPcLeftHandSide;
    targetPc = Optional.of(pTargetPc);
    targetPcExpression = Optional.empty();
  }

  protected SeqAtomicEndStatement(
      CExpressionAssignmentStatement pAtomicLockedFalse,
      CLeftHandSide pPcLeftHandSide,
      CExpression pTargetPcExpression) {

    atomicLockedFalse = pAtomicLockedFalse;
    pcLeftHandSide = pPcLeftHandSide;
    targetPc = Optional.empty();
    targetPcExpression = Optional.of(pTargetPcExpression);
  }

  @Override
  public String toASTString() {
    CExpressionAssignmentStatement pcWrite =
        SeqExpressionAssignmentStatement.buildPcWriteByTargetPc(
            pcLeftHandSide, targetPc, targetPcExpression);
    return atomicLockedFalse.toASTString() + SeqSyntax.SPACE + pcWrite.toASTString();
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
  public @NonNull SeqCaseBlockStatement cloneWithTargetPc(CExpression pTargetPc) {
    return new SeqAtomicEndStatement(atomicLockedFalse, pcLeftHandSide, pTargetPc);
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
