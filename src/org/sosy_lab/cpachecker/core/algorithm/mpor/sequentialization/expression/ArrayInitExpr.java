// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression;

import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.data_entity.Value;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqOperator;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqSyntax;

/**
 * An Expression for initializing an array, e.g. int array[size] = { 0 }. Note that all init values
 * are the same in the sequentialization.
 */
public class ArrayInitExpr implements SeqExpression {

  public final String dataType;

  public final ArrayExpr arrayExpr;

  public final Value initValue;

  /**
   * Creates an expression for initializing an array.
   *
   * @param pDataType the data type of the array
   * @param pArrayExpr the expression of the array, e.g. array_name[array_size]
   * @param pInitValue the initial value assigned to ALL indexes in the array
   */
  public ArrayInitExpr(String pDataType, ArrayExpr pArrayExpr, Value pInitValue) {
    dataType = pDataType;
    arrayExpr = pArrayExpr;
    initValue = pInitValue;
  }

  @Override
  public String toString() {
    return dataType
        + SeqSyntax.SPACE
        + arrayExpr
        + SeqSyntax.SPACE
        + SeqOperator.ASSIGN
        + SeqSyntax.SPACE
        + SeqSyntax.CURLY_BRACKET_LEFT
        + SeqSyntax.SPACE
        + initValue
        + SeqSyntax.SPACE
        + SeqSyntax.CURLY_BRACKET_RIGHT
        + SeqSyntax.SEMICOLON;
  }
}
