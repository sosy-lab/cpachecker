// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression;

import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqSyntax;

public class FunctionCallExpr implements SeqExpression {

  private final String functionName;

  private final SeqExpression parameter;

  public FunctionCallExpr(String pFunctionName) {
    functionName = pFunctionName;
    parameter = null;
  }

  public FunctionCallExpr(String pFunctionName, SeqExpression pParameter) {
    functionName = pFunctionName;
    parameter = pParameter;
  }

  @Override
  public String createString() {
    String parameters = SeqSyntax.EMPTY_STRING;
    if (parameter != null) {
      parameters = parameter.createString();
    }
    return functionName + SeqSyntax.BRACKET_LEFT + parameters + SeqSyntax.BRACKET_RIGHT;
  }
}
