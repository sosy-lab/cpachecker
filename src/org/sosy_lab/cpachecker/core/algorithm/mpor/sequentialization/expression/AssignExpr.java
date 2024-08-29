// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression;

import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SeqSyntax;

public class AssignExpr implements SeqExpression {

  private final SeqExpression preceding;

  private final SeqExpression subsequent;

  public AssignExpr(SeqExpression pPreceding, SeqExpression pSubsequent) {
    preceding = pPreceding;
    subsequent = pSubsequent;
  }

  @Override
  public String generateString() {
    return preceding.generateString()
        + SeqSyntax.SPACE.getString()
        + Operator.ASSIGN.string
        + SeqSyntax.SPACE.getString()
        + subsequent.generateString()
        + SeqSyntax.SEMICOLON.getString();
  }
}
