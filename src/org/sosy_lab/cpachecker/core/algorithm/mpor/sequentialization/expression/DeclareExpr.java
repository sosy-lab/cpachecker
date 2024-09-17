// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression;

import java.util.Optional;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqOperator;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqSyntax;

public class DeclareExpr implements SeqExpression {

  public final VariableExpr variableExpr;

  public final Optional<SeqExpression> value;

  public DeclareExpr(VariableExpr pVariableExpr, Optional<SeqExpression> pValue) {
    variableExpr = pVariableExpr;
    value = pValue;
  }

  @Override
  public String createString() {
    String assignment =
        value.isEmpty()
            ? SeqSyntax.EMPTY_STRING
            : SeqSyntax.SPACE
                + SeqOperator.ASSIGN
                + SeqSyntax.SPACE
                + value.orElseThrow().createString();
    return variableExpr.createString() + assignment + SeqSyntax.SEMICOLON + SeqSyntax.NEWLINE;
  }
}
