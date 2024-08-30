// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression;

import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqSyntax;

public class VariableExpr implements SeqExpression {

  private final String dataType;

  // TODO restrict to ArrayExpr, Variable
  private final SeqExpression variable;

  public VariableExpr(String pDataType, SeqExpression pVariable) {
    dataType = pDataType;
    variable = pVariable;
  }

  @Override
  public String createString() {
    return dataType + SeqSyntax.SPACE + variable.createString();
  }
}
