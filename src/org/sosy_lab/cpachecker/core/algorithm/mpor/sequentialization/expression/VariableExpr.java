// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression;

import java.util.Optional;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqSyntax;

public class VariableExpr implements SeqExpression {

  public final Optional<String> dataType;

  // TODO restrict to ArrayExpr, Variable
  public final SeqExpression variable;

  public VariableExpr(Optional<String> pDataType, SeqExpression pVariable) {
    dataType = pDataType;
    variable = pVariable;
  }

  @Override
  public String toString() {
    return dataType.map(dType -> dType + SeqSyntax.SPACE).orElse(SeqSyntax.EMPTY_STRING) + variable;
  }
}