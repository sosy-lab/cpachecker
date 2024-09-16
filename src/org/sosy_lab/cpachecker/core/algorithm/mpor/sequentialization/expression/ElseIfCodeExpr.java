// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression;

import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqToken;

public class ElseIfCodeExpr implements SeqExpression {

  public final IfExpr ifExpr;

  // TODO optional list?
  public final SeqExpression code;

  public ElseIfCodeExpr(IfExpr pIfExpr, SeqExpression pCode) {
    ifExpr = pIfExpr;
    code = pCode;
  }

  @Override
  public String createString() {
    return SeqToken.ELSE
        + SeqSyntax.SPACE
        + ifExpr.createString()
        + SeqSyntax.SPACE
        + SeqSyntax.CURLY_BRACKET_LEFT
        + SeqSyntax.SPACE
        + code.createString()
        + SeqSyntax.SPACE
        + SeqSyntax.CURLY_BRACKET_RIGHT;
  }
}
