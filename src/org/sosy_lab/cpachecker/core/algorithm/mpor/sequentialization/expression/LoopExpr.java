// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression;

import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqToken;

public class LoopExpr implements SeqExpression {

  public final CExpression condition;

  public LoopExpr(CExpression pCondition) {
    condition = pCondition;
  }

  @Override
  public String toASTString() {
    return SeqToken.WHILE
        + SeqSyntax.SPACE
        + SeqSyntax.BRACKET_LEFT
        + condition.toASTString()
        + SeqSyntax.BRACKET_RIGHT;
  }
}
