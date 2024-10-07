// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.data_entity;

import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqSyntax;

public class ArrayElement implements SeqDataEntity {

  public final Variable array;

  public final CExpression index;

  public ArrayElement(Variable pArray, CExpression pIndex) {
    array = pArray;
    index = pIndex;
  }

  @Override
  public String toASTString() {
    return array.toASTString()
        + SeqSyntax.SQUARE_BRACKET_LEFT
        + index.toASTString()
        + SeqSyntax.SQUARE_BRACKET_RIGHT;
  }
}
