// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.logical;

import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.SeqExpression;

public class SeqLogicalExpressionBuilder {
  public static SeqLogicalExpression buildBinaryLogicalExpressionByOperator(
      SeqLogicalOperator pOperator, SeqExpression pOperand1, SeqExpression pOperand2) {

    return switch (pOperator) {
      case AND -> new SeqLogicalAndExpression(pOperand1, pOperand2);
      case OR -> new SeqLogicalOrExpression(pOperand1, pOperand2);
      case NOT -> throw new IllegalArgumentException("NOT is not a binary logical operator");
    };
  }
}
