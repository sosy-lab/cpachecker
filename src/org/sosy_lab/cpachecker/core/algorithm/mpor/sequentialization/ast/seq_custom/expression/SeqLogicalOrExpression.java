// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression;

import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqSyntax;

public class SeqLogicalOrExpression implements SeqExpression {

  private final Optional<CExpression> operand1;

  private final Optional<CExpression> operand2;

  private final Optional<SeqExpression> logicalOperand1;

  private final Optional<SeqExpression> logicalOperand2;

  public SeqLogicalOrExpression(CExpression pOperand1, CExpression pOperand2) {
    operand1 = Optional.of(pOperand1);
    operand2 = Optional.of(pOperand2);
    logicalOperand1 = Optional.empty();
    logicalOperand2 = Optional.empty();
  }

  /** Use this constructor if the expressions are logical AND, OR, NOT themselves. */
  public SeqLogicalOrExpression(SeqExpression pLogicalOperand1, SeqExpression pLogicalOperand2) {
    operand1 = Optional.empty();
    operand2 = Optional.empty();
    logicalOperand1 = Optional.of(pLogicalOperand1);
    logicalOperand2 = Optional.of(pLogicalOperand2);
  }

  @Override
  public String toASTString() {
    String expression1;
    String expression2;
    if (operand1.isPresent() && operand2.isPresent()) {
      expression1 = operand1.orElseThrow().toASTString();
      expression2 = operand2.orElseThrow().toASTString();
    } else if (logicalOperand1.isPresent() && logicalOperand2.isPresent()) {
      expression1 = logicalOperand1.orElseThrow().toASTString();
      expression2 = logicalOperand2.orElseThrow().toASTString();
    } else {
      throw new IllegalArgumentException(
          "either both CExpression or SeqExpression operands must be present");
    }
    return SeqSyntax.BRACKET_LEFT
        + expression1
        + SeqSyntax.SPACE
        + SeqSyntax.LOGICAL_OR
        + SeqSyntax.SPACE
        + expression2
        + SeqSyntax.BRACKET_RIGHT;
  }
}
