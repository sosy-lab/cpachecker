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
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqToken;

public class DeclareExpr implements SeqExpression {

  // TODO we could also use CQualifierType here...
  public final boolean isConst;

  public final VariableExpr variableExpr;

  public final Optional<SeqExpression> value;

  public DeclareExpr(boolean pIsConst, VariableExpr pVariableExpr, Optional<SeqExpression> pValue) {
    isConst = pIsConst;
    variableExpr = pVariableExpr;
    value = pValue;
  }

  @Override
  public String toString() {
    String qualifier = isConst ? SeqToken.CONST + SeqSyntax.SPACE : SeqSyntax.EMPTY_STRING;
    String assignment =
        value.isEmpty()
            ? SeqSyntax.EMPTY_STRING
            : SeqSyntax.SPACE + SeqOperator.ASSIGN + SeqSyntax.SPACE + value.orElseThrow();
    return qualifier + variableExpr.toString() + assignment + SeqSyntax.SEMICOLON;
  }
}
