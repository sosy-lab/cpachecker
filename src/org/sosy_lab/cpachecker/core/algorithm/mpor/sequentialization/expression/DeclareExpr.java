// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression;

import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.data_entity.DataType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.data_entity.SeqDataEntity;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.data_entity.Variable;

public class DeclareExpr implements SeqExpression {

  private final DataType dataType;

  private final Variable variable;

  private final SeqDataEntity value;

  protected DeclareExpr(DataType pDataType, Variable pVariable, Variable pValue) {
    dataType = pDataType;
    variable = pVariable;
    value = pValue;
  }

  @Override
  public String string() {
    return dataType.string
        + " "
        + variable.string()
        + " "
        + Operator.ASSIGN.string
        + " "
        + value.string()
        + ";\n";
  }
}
