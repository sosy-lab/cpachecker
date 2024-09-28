// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqSyntax;

public class FunctionCallExpr implements SeqExpression {

  public final String functionName;

  public final ImmutableList<SeqExpression> parameters;

  /**
   * Returns a new {@link FunctionCallExpr}. Use ImmutableList.of() for pParameters if there are no
   * parameters.
   */
  public FunctionCallExpr(String pFunctionName, ImmutableList<SeqExpression> pParameters) {
    functionName = pFunctionName;
    parameters = pParameters;
  }

  @Override
  public String toString() {
    StringBuilder parametersString = new StringBuilder(SeqSyntax.EMPTY_STRING);
    if (!parameters.isEmpty()) {
      String separator = SeqSyntax.COMMA + SeqSyntax.SPACE;
      for (int i = 0; i < parameters.size(); i++) {
        parametersString
            .append(parameters.get(i).toString())
            .append(i == parameters.size() - 1 ? SeqSyntax.EMPTY_STRING : separator);
      }
    }
    return functionName + SeqSyntax.BRACKET_LEFT + parametersString + SeqSyntax.BRACKET_RIGHT;
  }
}
