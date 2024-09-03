// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression;

import com.google.common.collect.ImmutableSet;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqToken;

public class SwitchCaseExpr implements SeqExpression {

  // TODO restrict to Variable, ArrayExpr
  private final SeqExpression expression;

  private final ImmutableSet<String> cases;

  public SwitchCaseExpr(SeqExpression pExpression, ImmutableSet<String> pCases) {
    expression = pExpression;
    cases = pCases;
  }

  @Override
  public String createString() {
    StringBuilder casesString = new StringBuilder(SeqSyntax.EMPTY_STRING);
    for (String caseString : cases) {
      casesString.append(caseString);
    }
    return SeqToken.SWITCH
        + SeqSyntax.SPACE
        + SeqSyntax.BRACKET_LEFT
        + expression.createString()
        + SeqSyntax.BRACKET_RIGHT
        + SeqSyntax.SPACE
        + SeqSyntax.CURLY_BRACKET_LEFT
        + SeqSyntax.NEWLINE
        + cases
        + SeqSyntax.TAB
        + SeqSyntax.CURLY_BRACKET_RIGHT;
  }
}
