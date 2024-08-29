// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression;

import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SeqToken;

public class LoopExpr implements SeqExpression {
  // TODO restrict to ArrayExpr, BoolenaExpr, FunctionCallExpr, NegationExpr
  private final SeqExpression condition;

  public LoopExpr(SeqExpression pCondition) {
    condition = pCondition;
  }

  @Override
  public String generateString() {
    return SeqToken.WHILE.getString()
        + SeqSyntax.SPACE
        + SeqSyntax.BRACKET_LEFT
        + condition.generateString()
        + SeqSyntax.BRACKET_RIGHT;
  }
}
