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
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqExpressions.SeqIntegerLiteralExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqStatements.SeqExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqControlFlowStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqControlFlowStatement.SeqControlFlowStatementType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqStringUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.hard_coded.SeqSyntax;

public class SeqAtomicBeginStatement implements SeqCaseBlockStatement {

  private static final SeqControlFlowStatement elseNotLocked = new SeqControlFlowStatement();

  private final CIdExpression atomicLocked;

  private final CIdExpression threadBeginsAtomic;

  private final int threadId;

  private final int targetPc;

  public SeqAtomicBeginStatement(
      CIdExpression pAtomicLocked,
      CIdExpression pThreadBeginsAtomic,
      int pThreadId,
      int pTargetPc) {

    atomicLocked = pAtomicLocked;
    threadBeginsAtomic = pThreadBeginsAtomic;
    threadId = pThreadId;
    targetPc = pTargetPc;
  }

  @Override
  public String toASTString() {
    SeqControlFlowStatement ifAtomicLocked =
        new SeqControlFlowStatement(atomicLocked, SeqControlFlowStatementType.IF);
    CExpressionAssignmentStatement setBeginsTrue =
        new CExpressionAssignmentStatement(
            FileLocation.DUMMY, threadBeginsAtomic, SeqIntegerLiteralExpression.INT_1);
    CExpressionAssignmentStatement setAtomicLockedTrue =
        new CExpressionAssignmentStatement(
            FileLocation.DUMMY, atomicLocked, SeqIntegerLiteralExpression.INT_1);
    CExpressionAssignmentStatement setBeginsFalse =
        new CExpressionAssignmentStatement(
            FileLocation.DUMMY, threadBeginsAtomic, SeqIntegerLiteralExpression.INT_0);

    CExpressionAssignmentStatement pcWrite =
        SeqExpressionAssignmentStatement.buildPcWrite(threadId, targetPc);

    String elseStmts =
        SeqStringUtil.wrapInCurlyInwards(
            setAtomicLockedTrue.toASTString()
                + SeqSyntax.SPACE
                + setBeginsFalse.toASTString()
                + SeqSyntax.SPACE
                + pcWrite.toASTString());

    return ifAtomicLocked.toASTString()
        + SeqSyntax.SPACE
        + SeqStringUtil.wrapInCurlyInwards(setBeginsTrue.toASTString())
        + SeqSyntax.SPACE
        + elseNotLocked.toASTString()
        + SeqSyntax.SPACE
        + elseStmts;
  }

  @Override
  public Optional<Integer> getTargetPc() {
    return Optional.of(targetPc);
  }

  @NonNull
  @Override
  public SeqAtomicBeginStatement cloneWithTargetPc(int pTargetPc) {
    return new SeqAtomicBeginStatement(atomicLocked, threadBeginsAtomic, threadId, pTargetPc);
  }

  @Override
  public boolean alwaysWritesPc() {
    return false;
  }
}
