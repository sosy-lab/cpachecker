// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression;

import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqOperator;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqSyntax;

public class NegationExpr implements SeqExpression {

  public final SeqExpression expression;

  public NegationExpr(SeqExpression pExpression) {
    expression = pExpression;
  }

  @Override
  public String toString() {
    return SeqOperator.NOT
        + SeqSyntax.BRACKET_LEFT
        + expression.toString()
        + SeqSyntax.BRACKET_RIGHT;
  }
}
