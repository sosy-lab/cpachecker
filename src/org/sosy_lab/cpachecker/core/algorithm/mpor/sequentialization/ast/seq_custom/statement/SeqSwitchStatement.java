// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode.AAstNodeRepresentation;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SeqUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.Sequentialization;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqDeclarations.SeqFunctionDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqExpressions.SeqIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqExpressions.SeqIntegerLiteralExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqExpressions.SeqStringLiteralExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqTypes.SeqVoidType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqControlFlowStatement.SeqControlFlowStatementType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqToken;

public class SeqSwitchStatement implements SeqStatement {

  private final SeqControlFlowStatement switchExpression;

  private final ImmutableList<SeqCaseClause> caseClauses;

  private final CFunctionCallExpression assertFailCall;

  private final int tabs;

  public SeqSwitchStatement(
      int pThreadId,
      CExpression pExpression,
      ImmutableList<SeqCaseClause> pCaseClauses,
      int pTabs) {

    switchExpression = new SeqControlFlowStatement(pExpression, SeqControlFlowStatementType.SWITCH);
    caseClauses = pCaseClauses;

    // TODO how do we manage the line parameter?
    //  at the moment we just use the threadId, but a thread simulation may contain multiple
    //  switch cases where assert_fail is called
    CStringLiteralExpression seqFileName =
        SeqStringLiteralExpression.buildStringLiteralExpr(
            SeqUtil.wrapInQuotationMarks(Sequentialization.getFileName()));
    CIntegerLiteralExpression threadId = SeqIntegerLiteralExpression.buildIntLiteralExpr(pThreadId);
    assertFailCall =
        new CFunctionCallExpression(
            FileLocation.DUMMY,
            SeqVoidType.VOID,
            SeqIdExpression.ASSERT_FAIL,
            ImmutableList.of(
                SeqStringLiteralExpression.STRING_0,
                seqFileName,
                threadId,
                SeqStringLiteralExpression.SEQUENTIALIZATION_ERROR),
            SeqFunctionDeclaration.ASSERT_FAIL);

    tabs = pTabs;
  }

  @Override
  public String toASTString() {
    StringBuilder casesString = new StringBuilder(SeqSyntax.EMPTY_STRING);
    for (SeqCaseClause caseClause : caseClauses) {
      casesString.append(SeqUtil.prependTabsWithoutNewline(tabs + 1, caseClause.toASTString()));
    }
    String defaultCaseClause =
        SeqToken.DEFAULT
            + SeqSyntax.COLON
            + SeqSyntax.SPACE
            + assertFailCall.toASTString(AAstNodeRepresentation.QUALIFIED)
            + SeqSyntax.SEMICOLON;
    return SeqUtil.repeat(SeqSyntax.TAB, tabs)
        + SeqUtil.appendOpeningCurly(switchExpression.toASTString())
        + SeqSyntax.NEWLINE
        + casesString
        + SeqUtil.prependTabsWithNewline(tabs + 1, defaultCaseClause)
        + SeqUtil.repeat(SeqSyntax.TAB, tabs)
        + SeqSyntax.CURLY_BRACKET_RIGHT;
  }
}
