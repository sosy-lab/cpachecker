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

public class IfExpr implements SeqExpression {

  public final SeqExpression condition;

  public IfExpr(SeqExpression pCondition) {
    condition = pCondition;
  }

  @Override
  public String toString() {
    return SeqToken.IF
        + SeqSyntax.SPACE
        + SeqSyntax.BRACKET_LEFT
        + condition
        + SeqSyntax.BRACKET_RIGHT;
  }
}