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

public class FunctionCallExpr implements SeqExpression {

  private final SeqToken functionName;

  private final SeqExpression parameter;

  public FunctionCallExpr(SeqToken pFunctionName, SeqExpression pParameter) {
    functionName = pFunctionName;
    parameter = pParameter;
  }

  @Override
  public String generateString() {
    return functionName.getString()
        + SeqSyntax.BRACKET_LEFT
        + parameter.generateString()
        + SeqSyntax.BRACKET_RIGHT
        + SeqSyntax.SEMICOLON;
  }
}
