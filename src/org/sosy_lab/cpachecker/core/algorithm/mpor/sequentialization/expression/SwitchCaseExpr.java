// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SeqUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqToken;

public class SwitchCaseExpr implements SeqExpression {

  // TODO restrict to Variable, ArrayExpr
  public final SeqExpression expression;

  public final ImmutableList<String> cases;

  private final int tabs;

  public SwitchCaseExpr(SeqExpression pExpression, ImmutableList<String> pCases, int pTabs) {
    expression = pExpression;
    cases = pCases;
    tabs = pTabs;
  }

  @Override
  public String toString() {
    StringBuilder casesString = new StringBuilder(SeqSyntax.EMPTY_STRING);
    for (String caseString : cases) {
      casesString.append(SeqUtil.prependTabsWithoutNewline(tabs + 1, caseString));
    }
    return SeqUtil.repeat(SeqSyntax.TAB, tabs)
        + SeqToken.SWITCH
        + SeqSyntax.SPACE
        + SeqSyntax.BRACKET_LEFT
        + expression
        + SeqSyntax.BRACKET_RIGHT
        + SeqSyntax.SPACE
        + SeqSyntax.CURLY_BRACKET_LEFT
        + SeqSyntax.NEWLINE
        + casesString
        + SeqUtil.repeat(SeqSyntax.TAB, tabs)
        + SeqSyntax.CURLY_BRACKET_RIGHT;
  }
}
