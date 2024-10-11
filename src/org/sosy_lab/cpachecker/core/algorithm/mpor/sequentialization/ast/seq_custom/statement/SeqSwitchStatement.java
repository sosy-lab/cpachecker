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
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SeqUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqControlFlowStatement.SeqControlFlowStatementType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqSyntax;

public class SeqSwitchStatement implements SeqStatement {

  private final SeqControlFlowStatement switchExpression;

  private final ImmutableList<SeqCaseClause> caseClauses;

  private final int tabs;

  // TODO create default case
  //  __assert_fail("0", "{seq_file_name}", 0, "__SEQUENTIALIZATION_ERROR__");
  //  i.e. if a (return_) pc can not be matched, the sequentialization is faulty
  //  the third parameter means that the assertion error occured in the
  //  "__SEQUENTIALIZTION_ERROR__" function (which is non-existent, but thats fine)
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
      casesString.append(SeqUtil.prependTabsWithoutNewline(tabs + 1, caseClause.toASTString()));
    }
    return SeqUtil.repeat(SeqSyntax.TAB, tabs)
        + SeqUtil.appendOpeningCurly(switchExpression.toASTString())
        + SeqSyntax.NEWLINE
        + casesString
        + SeqUtil.repeat(SeqSyntax.TAB, tabs)
        + SeqSyntax.CURLY_BRACKET_RIGHT;
  }
}
