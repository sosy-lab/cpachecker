// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.logical;

import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.SeqExpression;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

// TODO remove
public class SeqLogicalAndExpression implements SeqLogicalExpression {

  private final CExpression operand1;

  private final SeqExpression operand2;

  /** Use this constructor if the expressions are logical AND, OR, NOT themselves. */
  public SeqLogicalAndExpression(CExpression pOperand1, SeqExpression pOperand2) {
    operand1 = pOperand1;
    operand2 = pOperand2;
  }

  @Override
  public String toASTString() throws UnrecognizedCodeException {
    return "(" + operand1.toASTString() + " && " + operand2.toASTString() + ")";
  }

  @Override
  public SeqLogicalOperator getOperator() {
    return SeqLogicalOperator.AND;
  }
}
