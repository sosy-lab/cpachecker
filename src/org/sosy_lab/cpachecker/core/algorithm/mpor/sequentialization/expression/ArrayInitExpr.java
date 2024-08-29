// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression;

import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SeqDataType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.data_entity.Value;

/**
 * An Expression for initializing an array, e.g. int array[size] = { 0 }. Note that all init values
 * are the same in the sequentialization.
 */
public class ArrayInitExpr implements SeqExpression {

  private final SeqDataType dataType;

  private final ArrayExpr array;

  private final Value initValue;

  /**
   * Creates an expression for initializing an array.
   *
   * @param pDataType the data type of the array
   * @param pArray the expression of the array, e.g. array_name[array_size]
   * @param pInitValue the initial value assigned to ALL indexes in the array
   */
  public ArrayInitExpr(SeqDataType pDataType, ArrayExpr pArray, Value pInitValue) {
    dataType = pDataType;
    array = pArray;
    initValue = pInitValue;
  }

  @Override
  public String generateString() {
    return dataType.string
        + SeqSyntax.SPACE.getString()
        + array.generateString()
        + SeqSyntax.SPACE.getString()
        + Operator.ASSIGN.string
        + SeqSyntax.SPACE.getString()
        + SeqSyntax.CURLY_BRACKET_LEFT
        + initValue.generateString()
        + SeqSyntax.CURLY_BRACKET_RIGHT;
  }
}
