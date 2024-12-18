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
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SeqUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqExpressions.SeqIntegerLiteralExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqStatements;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqControlFlowStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqControlFlowStatement.SeqControlFlowStatementType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqSyntax;

public class SeqAtomicBeginStatement implements SeqCaseBlockStatement {

  private static final SeqControlFlowStatement elseNotLocked = new SeqControlFlowStatement();

  private final CIdExpression atomicInUse;

  private final CIdExpression threadBeginsAtomic;

  private final int threadId;

  private final int targetPc;

  public SeqAtomicBeginStatement(
      CIdExpression pAtomicInUse, CIdExpression pThreadBeginsAtomic, int pThreadId, int pTargetPc) {
    atomicInUse = pAtomicInUse;
    threadBeginsAtomic = pThreadBeginsAtomic;
    threadId = pThreadId;
    targetPc = pTargetPc;
  }

  @Override
  public String toASTString() {
    SeqControlFlowStatement ifAtomicInUse =
        new SeqControlFlowStatement(atomicInUse, SeqControlFlowStatementType.IF);
    CExpressionAssignmentStatement setBeginsTrue =
        new CExpressionAssignmentStatement(
            FileLocation.DUMMY, threadBeginsAtomic, SeqIntegerLiteralExpression.INT_1);
    CExpressionAssignmentStatement setAtomicInUseTrue =
        new CExpressionAssignmentStatement(
            FileLocation.DUMMY, atomicInUse, SeqIntegerLiteralExpression.INT_1);
    CExpressionAssignmentStatement setBeginsFalse =
        new CExpressionAssignmentStatement(
            FileLocation.DUMMY, threadBeginsAtomic, SeqIntegerLiteralExpression.INT_0);
    CExpressionAssignmentStatement pcUpdate = SeqStatements.buildPcUpdate(threadId, targetPc);

    String elseStmts =
        SeqUtil.wrapInCurlyInwards(
            setAtomicInUseTrue.toASTString()
                + SeqSyntax.SPACE
                + setBeginsFalse.toASTString()
                + SeqSyntax.SPACE
                + pcUpdate.toASTString());

    return ifAtomicInUse.toASTString()
        + SeqSyntax.SPACE
        + SeqUtil.wrapInCurlyInwards(setBeginsTrue.toASTString())
        + SeqSyntax.SPACE
        + elseNotLocked.toASTString()
        + SeqSyntax.SPACE
        + elseStmts;
  }

  @Override
  public Optional<Integer> getTargetPc() {
    return Optional.of(targetPc);
  }

  @Override
  public @NonNull SeqAtomicBeginStatement cloneWithTargetPc(int pTargetPc) {
    return new SeqAtomicBeginStatement(atomicInUse, threadBeginsAtomic, threadId, pTargetPc);
  }

  @Override
  public boolean alwaysUpdatesPc() {
    return false;
  }
}
