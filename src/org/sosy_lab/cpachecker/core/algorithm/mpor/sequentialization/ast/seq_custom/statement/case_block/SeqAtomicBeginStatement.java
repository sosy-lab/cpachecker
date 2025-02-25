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
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqExpressions.SeqIntegerLiteralExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqStatements.SeqExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqControlFlowStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqControlFlowStatement.SeqControlFlowStatementType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqStringUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;

public class SeqAtomicBeginStatement implements SeqCaseBlockStatement {

  private static final SeqControlFlowStatement elseNotLocked = new SeqControlFlowStatement();

  private final CIdExpression atomicLocked;

  private final CIdExpression threadBeginsAtomic;

  private final CLeftHandSide pcLeftHandSide;

  private final int targetPc;

  protected SeqAtomicBeginStatement(
      CIdExpression pAtomicLocked,
      CIdExpression pThreadBeginsAtomic,
      CLeftHandSide pPcLeftHandSide,
      int pTargetPc) {

    atomicLocked = pAtomicLocked;
    threadBeginsAtomic = pThreadBeginsAtomic;
    pcLeftHandSide = pPcLeftHandSide;
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
        SeqExpressionAssignmentStatement.buildPcWrite(pcLeftHandSide, targetPc);

    String elseStatements =
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
        + elseStatements;
  }

  @Override
  public Optional<Integer> getTargetPc() {
    return Optional.of(targetPc);
  }

  @NonNull
  @Override
  public SeqAtomicBeginStatement cloneWithTargetPc(int pTargetPc) {
    return new SeqAtomicBeginStatement(atomicLocked, threadBeginsAtomic, pcLeftHandSide, pTargetPc);
  }

  @Override
  public boolean alwaysWritesPc() {
    return false;
  }
}
