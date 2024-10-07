// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.function_call;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.SeqExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqSyntax;

public class SeqFunctionCallExpression implements SeqExpression {

  public final CIdExpression functionName;

  public final ImmutableList<SeqExpression> parameters;

  /**
   * Returns a new {@link SeqFunctionCallExpression}. Use ImmutableList.of() for pParameters if
   * there are no parameters.
   */
  public SeqFunctionCallExpression(
      CIdExpression pFunctionName, ImmutableList<SeqExpression> pParameters) {
    functionName = pFunctionName;
    parameters = pParameters;
  }

  @Override
  public String toASTString() {
    StringBuilder parametersString = new StringBuilder(SeqSyntax.EMPTY_STRING);
    if (!parameters.isEmpty()) {
      String separator = SeqSyntax.COMMA + SeqSyntax.SPACE;
      for (int i = 0; i < parameters.size(); i++) {
        parametersString
            .append(parameters.get(i).toASTString())
            .append(i == parameters.size() - 1 ? SeqSyntax.EMPTY_STRING : separator);
      }
    }
    return functionName.getName()
        + SeqSyntax.BRACKET_LEFT
        + parametersString
        + SeqSyntax.BRACKET_RIGHT;
  }
}
