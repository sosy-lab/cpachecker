// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression;

import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SeqSyntax;

public class NegationExpr implements SeqExpression {

  private final SeqExpression expression;

  public NegationExpr(SeqExpression pExpression) {
    expression = pExpression;
  }

  @Override
  public String generateString() {
    return SeqSyntax.EXCLAMATION_MARK.getString()
        + SeqSyntax.BRACKET_LEFT.getString()
        + expression.generateString()
        + SeqSyntax.BRACKET_RIGHT.getString();
  }
}
