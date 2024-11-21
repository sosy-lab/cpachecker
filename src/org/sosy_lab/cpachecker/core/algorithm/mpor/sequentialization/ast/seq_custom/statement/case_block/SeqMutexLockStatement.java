// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block;

import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SeqUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqExpressions.SeqIntegerLiteralExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqControlFlowStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqControlFlowStatement.SeqControlFlowStatementType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqSyntax;

/**
 * Represents a statement of the form
 *
 * <p>{@code if (m_locked) { __t_awaits_m = 1; }}
 *
 * <p>{@code else { __t_awaits_m = 0; m_locked = 1; pc[...] = ...; }}
 *
 * <p>{@code continue;}
 */
public class SeqMutexLockStatement implements SeqCaseBlockStatement {

  private final SeqControlFlowStatement ifLocked;

  private final CExpressionAssignmentStatement awaitsTrue;

  private static final SeqControlFlowStatement elseNotLocked = new SeqControlFlowStatement();

  private final CExpressionAssignmentStatement awaitsFalse;

  private final CExpressionAssignmentStatement lockedTrue;

  private final CExpressionAssignmentStatement pcUpdate;

  private final int targetPc;

  public SeqMutexLockStatement(
      CIdExpression pMutexLocked,
      CIdExpression pMutexAwaits,
      CExpressionAssignmentStatement pPcUpdate,
      int pTargetPc) {

    ifLocked = new SeqControlFlowStatement(pMutexLocked, SeqControlFlowStatementType.IF);
    awaitsTrue =
        new CExpressionAssignmentStatement(
            FileLocation.DUMMY, pMutexAwaits, SeqIntegerLiteralExpression.INT_1);
    awaitsFalse =
        new CExpressionAssignmentStatement(
            FileLocation.DUMMY, pMutexAwaits, SeqIntegerLiteralExpression.INT_0);
    lockedTrue =
        new CExpressionAssignmentStatement(
            FileLocation.DUMMY, pMutexLocked, SeqIntegerLiteralExpression.INT_1);
    pcUpdate = pPcUpdate;
    targetPc = pTargetPc;
  }

  @Override
  public String toASTString() {
    String elseStmts =
        SeqUtil.wrapInCurlyInwards(
            awaitsFalse.toASTString()
                + SeqSyntax.SPACE
                + lockedTrue.toASTString()
                + SeqSyntax.SPACE
                + pcUpdate.toASTString());
    return ifLocked.toASTString()
        + SeqSyntax.SPACE
        + SeqUtil.wrapInCurlyInwards(awaitsTrue.toASTString())
        + SeqSyntax.SPACE
        + elseNotLocked.toASTString()
        + SeqSyntax.SPACE
        + elseStmts;
  }

  @Override
  public Optional<Integer> getTargetPc() {
    return Optional.of(targetPc);
  }
}
