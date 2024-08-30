// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression;

import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.data_entity.SeqDataEntity;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.data_entity.Variable;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqSyntax;

public class ArrayExpr implements SeqExpression {

  private final Variable array;

  private final SeqDataEntity index;

  public ArrayExpr(Variable pArray, Variable pIndex) {
    array = pArray;
    index = pIndex;
  }

  @Override
  public String createString() {
    return array.createString()
        + SeqSyntax.SQUARE_BRACKET_LEFT
        + index.createString()
        + SeqSyntax.SQUARE_BRACKET_RIGHT;
  }
}
