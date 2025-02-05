// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.Sequentialization;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqControlFlowStatement.SeqControlFlowStatementType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqStringUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.hard_coded.SeqToken;

/**
 * Represents the entirety of a switch statement.
 *
 * <p>Example: {@code switch(a) { case b: ...; break; case c: ...; break; } }
 *
 * <p>with the default sequentialization error {@code default: reach_error(...); }
 */
public class SeqSwitchStatement implements SeqStatement {

  private final SeqControlFlowStatement switchExpression;

  private final ImmutableList<SeqCaseClause> caseClauses;

  private final int tabs;

  public SeqSwitchStatement(
      CExpression pExpression, ImmutableList<SeqCaseClause> pCaseClauses, int pTabs) {

    switchExpression = new SeqControlFlowStatement(pExpression, SeqControlFlowStatementType.SWITCH);
    caseClauses = pCaseClauses;
    tabs = pTabs;
  }

  @Override
  public String toASTString() {
    StringBuilder casesString = new StringBuilder(SeqSyntax.EMPTY_STRING);
    for (SeqCaseClause caseClause : caseClauses) {
      casesString.append(
          SeqStringUtil.prependTabsWithoutNewline(tabs + 1, caseClause.toASTString()));
    }
    String defaultCaseClause =
        SeqToken._default
            + SeqSyntax.COLON
            + SeqSyntax.SPACE
            + Sequentialization.outputReachErrorDummy;
    return SeqStringUtil.buildTab(tabs)
        + SeqStringUtil.appendOpeningCurly(switchExpression.toASTString())
        + SeqSyntax.NEWLINE
        + casesString
        + SeqStringUtil.prependTabsWithNewline(tabs + 1, defaultCaseClause)
        + SeqStringUtil.buildTab(tabs)
        + SeqSyntax.CURLY_BRACKET_RIGHT;
  }
}
