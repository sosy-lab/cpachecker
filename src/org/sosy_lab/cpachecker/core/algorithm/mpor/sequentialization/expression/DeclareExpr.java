// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression;

import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.data_entity.Variable;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqOperator;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqSyntax;

public class DeclareExpr implements SeqExpression {

  private final String dataType;

  private final Variable variable;

  private final SeqExpression value;

  public DeclareExpr(String pDataType, Variable pVariable, SeqExpression pValue) {
    dataType = pDataType;
    variable = pVariable;
    value = pValue;
  }

  @Override
  public String createString() {
    return dataType
        + SeqSyntax.SPACE
        + variable.createString()
        + SeqSyntax.SPACE
        + SeqOperator.ASSIGN
        + SeqSyntax.SPACE
        + value.createString()
        + SeqSyntax.SEMICOLON;
  }
}
