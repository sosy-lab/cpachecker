// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression;

import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.data_entity.DataType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.data_entity.Value;

/**
 * An Expression for initializing an array, e.g. int array[size] = { 0 }. Note that all init values
 * are the same in the sequentialization.
 */
public class ArrayInitExpr implements SeqExpression {

  private final DataType dataType;

  private final ArrayExpr array;

  private final Value initValue;

  /**
   * Creates an expression for initializing an array.
   *
   * @param pDataType the data type of the array
   * @param pArray the expression of the array, e.g. array_name[array_size]
   * @param pInitValue the initial value assigned to ALL indexes in the array
   */
  public ArrayInitExpr(DataType pDataType, ArrayExpr pArray, Value pInitValue) {
    dataType = pDataType;
    array = pArray;
    initValue = pInitValue;
  }

  @Override
  public String string() {
    return dataType.string
        + " "
        + array.string()
        + " "
        + Operator.ASSIGN.string
        + " { "
        + initValue.string()
        + " };";
  }
}
