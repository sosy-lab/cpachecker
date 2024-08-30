// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqSyntax;

public class FunctionCallExpr implements SeqExpression {

  private final String functionName;

  private final Optional<ImmutableList<SeqExpression>> parameters;

  public FunctionCallExpr(
      String pFunctionName, Optional<ImmutableList<SeqExpression>> pParameters) {
    functionName = pFunctionName;
    parameters = pParameters;
  }

  @Override
  public String createString() {
    StringBuilder parametersString = new StringBuilder(SeqSyntax.EMPTY_STRING);
    if (parameters.isPresent()) {
      String separator = SeqSyntax.COMMA + SeqSyntax.SPACE;
      for (int i = 0; i < parameters.get().size(); i++) {
        parametersString
            .append(parameters.get().get(i).createString())
            .append(i == parameters.get().size() - 1 ? SeqSyntax.EMPTY_STRING : separator);
      }
    }
    return functionName + SeqSyntax.BRACKET_LEFT + parametersString + SeqSyntax.BRACKET_RIGHT;
  }
}
