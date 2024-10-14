// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SeqUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqCaseClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqCaseClause.CaseBlockTerminator;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqSwitchStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.function_vars.FunctionReturnValueAssignment;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqSyntax;

public class SeqReturnValueAssignStatements implements SeqCaseBlockStatement {

  private final ImmutableSet<FunctionReturnValueAssignment> assigns;

  private final CIdExpression returnPc;

  private final CExpressionAssignmentStatement pcUpdate;

  public SeqReturnValueAssignStatements(
      CIdExpression pReturnPc,
      ImmutableSet<FunctionReturnValueAssignment> pAssigns,
      CExpressionAssignmentStatement pPcUpdate) {

    checkArgument(!pAssigns.isEmpty(), "pAssigns must contain at least one entry");

    assigns = pAssigns;
    returnPc = pReturnPc;
    pcUpdate = pPcUpdate;
  }

  @Override
  public String toASTString() {
    ImmutableList.Builder<SeqCaseClause> caseClauses = ImmutableList.builder();
    for (FunctionReturnValueAssignment assignment : assigns) {
      int caseLabelValue = assignment.returnPcStorage.value;
      SeqReturnValueAssignCaseBlockStatement assignmentStatement =
          new SeqReturnValueAssignCaseBlockStatement(assignment.statement);
      caseClauses.add(
          new SeqCaseClause(
              caseLabelValue, ImmutableList.of(assignmentStatement), CaseBlockTerminator.BREAK));
    }
    SeqSwitchStatement switchStatement = new SeqSwitchStatement(returnPc, caseClauses.build(), 6);
    return SeqSyntax.NEWLINE
        + switchStatement.toASTString()
        + SeqSyntax.NEWLINE
        + SeqUtil.prependTabsWithoutNewline(6, pcUpdate.toASTString());
  }

  private static class SeqReturnValueAssignCaseBlockStatement implements SeqCaseBlockStatement {

    private final CExpressionAssignmentStatement assignment;

    private SeqReturnValueAssignCaseBlockStatement(CExpressionAssignmentStatement pAssignment) {
      assignment = pAssignment;
    }

    @Override
    public String toASTString() {
      return assignment.toASTString();
    }
  }
}
