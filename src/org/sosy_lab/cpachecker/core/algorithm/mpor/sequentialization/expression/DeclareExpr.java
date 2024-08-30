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

public class DeclareExpr implements SeqExpression {

  private final VariableExpr variableExpr;

  private final SeqExpression value;

  public DeclareExpr(VariableExpr pVariableExpr, SeqExpression pValue) {
    variableExpr = pVariableExpr;
    value = pValue;
  }

  @Override
  public String createString() {
    return variableExpr.createString()
        + SeqSyntax.SPACE
        + SeqOperator.ASSIGN
        + SeqSyntax.SPACE
        + value.createString()
        + SeqSyntax.SEMICOLON;
  }
}
