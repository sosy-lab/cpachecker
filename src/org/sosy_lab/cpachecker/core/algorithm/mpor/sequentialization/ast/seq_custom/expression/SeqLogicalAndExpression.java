// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression;

import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;

// TODO try and replace with ExpressionTree And.of etc.
public class SeqLogicalAndExpression implements SeqExpression {

  private final Optional<CExpression> operand1;

  private final Optional<CExpression> operand2;

  private final Optional<SeqExpression> logicalOperand1;

  private final Optional<SeqExpression> logicalOperand2;

  public SeqLogicalAndExpression(CExpression pOperand1, CExpression pOperand2) {
    operand1 = Optional.of(pOperand1);
    operand2 = Optional.of(pOperand2);
    logicalOperand1 = Optional.empty();
    logicalOperand2 = Optional.empty();
  }

  /** Use this constructor if the expressions are logical AND, OR, NOT themselves. */
  public SeqLogicalAndExpression(SeqExpression pLogicalOperand1, SeqExpression pLogicalOperand2) {
    operand1 = Optional.empty();
    operand2 = Optional.empty();
    logicalOperand1 = Optional.of(pLogicalOperand1);
    logicalOperand2 = Optional.of(pLogicalOperand2);
  }

  @Override
  public String toASTString() {
    String left;
    String right;
    if (operand1.isPresent() && operand2.isPresent()) {
      left = operand1.orElseThrow().toASTString();
      right = operand2.orElseThrow().toASTString();
    } else if (logicalOperand1.isPresent() && logicalOperand2.isPresent()) {
      left = logicalOperand1.orElseThrow().toASTString();
      right = logicalOperand2.orElseThrow().toASTString();
    } else {
      throw new IllegalArgumentException(
          "either both CExpression or SeqExpression operands must be present");
    }
    return SeqSyntax.BRACKET_LEFT
        + left
        + SeqSyntax.SPACE
        + SeqSyntax.LOGICAL_AND
        + SeqSyntax.SPACE
        + right
        + SeqSyntax.BRACKET_RIGHT;
  }
}
