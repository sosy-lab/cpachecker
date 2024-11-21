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

public class SeqThreadJoinStatement implements SeqCaseBlockStatement {

  private final SeqControlFlowStatement ifActive;

  private final CExpressionAssignmentStatement joinsTrue;

  private static final SeqControlFlowStatement elseNotActive = new SeqControlFlowStatement();

  private final CExpressionAssignmentStatement joinsFalse;

  private final CExpressionAssignmentStatement pcUpdate;

  private final int targetPc;

  public SeqThreadJoinStatement(
      CIdExpression pThreadActive,
      CIdExpression pThreadJoins,
      CExpressionAssignmentStatement pPcUpdate,
      int pTargetPc) {

    ifActive = new SeqControlFlowStatement(pThreadActive, SeqControlFlowStatementType.IF);
    joinsTrue =
        new CExpressionAssignmentStatement(
            FileLocation.DUMMY, pThreadJoins, SeqIntegerLiteralExpression.INT_1);
    joinsFalse =
        new CExpressionAssignmentStatement(
            FileLocation.DUMMY, pThreadJoins, SeqIntegerLiteralExpression.INT_0);
    pcUpdate = pPcUpdate;
    targetPc = pTargetPc;
  }

  @Override
  public String toASTString() {
    String elseStmts =
        SeqUtil.wrapInCurlyInwards(
            joinsFalse.toASTString() + SeqSyntax.SPACE + pcUpdate.toASTString());
    return ifActive.toASTString()
        + SeqSyntax.SPACE
        + SeqUtil.wrapInCurlyInwards(joinsTrue.toASTString())
        + SeqSyntax.SPACE
        + elseNotActive.toASTString()
        + SeqSyntax.SPACE
        + elseStmts;
  }

  @Override
  public Optional<Integer> getTargetPc() {
    return Optional.of(targetPc);
  }
}
