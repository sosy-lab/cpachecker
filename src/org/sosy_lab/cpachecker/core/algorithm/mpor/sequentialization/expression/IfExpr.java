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

public class IfExpr implements SeqExpression {

  // TODO create SeqControlStructure for if, else, else if, switch, while

  public final CExpression condition;

  public IfExpr(CExpression pCondition) {
    condition = pCondition;
  }

  @Override
  public String toASTString() {
    return SeqToken.IF
        + SeqSyntax.SPACE
        + SeqSyntax.BRACKET_LEFT
        + condition.toASTString()
        + SeqSyntax.BRACKET_RIGHT;
  }
}
