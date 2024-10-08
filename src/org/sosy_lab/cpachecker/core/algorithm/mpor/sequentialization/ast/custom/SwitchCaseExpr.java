// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SeqUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom.control_flow.SeqControlFlowStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom.control_flow.SeqControlFlowStatement.SeqControlFlowStatementType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqSyntax;

public class SwitchCaseExpr implements SeqExpression {

  private final SeqControlFlowStatement switchExpression;

  private final ImmutableList<String> cases;

  private final int tabs;

  public SwitchCaseExpr(CExpression pExpression, ImmutableList<String> pCases, int pTabs) {
    switchExpression = new SeqControlFlowStatement(pExpression, SeqControlFlowStatementType.SWITCH);
    cases = pCases;
    tabs = pTabs;
  }

  @Override
  public String toASTString() {
    StringBuilder casesString = new StringBuilder(SeqSyntax.EMPTY_STRING);
    for (String caseString : cases) {
      casesString.append(SeqUtil.prependTabsWithoutNewline(tabs + 1, caseString));
    }
    return SeqUtil.repeat(SeqSyntax.TAB, tabs)
        + SeqUtil.appendOpeningCurly(switchExpression.toASTString())
        + SeqSyntax.NEWLINE
        + casesString
        + SeqUtil.repeat(SeqSyntax.TAB, tabs)
        + SeqSyntax.CURLY_BRACKET_RIGHT;
  }
}
