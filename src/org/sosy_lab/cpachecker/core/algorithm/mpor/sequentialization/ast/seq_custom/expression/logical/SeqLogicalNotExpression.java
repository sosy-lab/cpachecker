// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.logical;

import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.SeqExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;

public class SeqLogicalNotExpression implements SeqLogicalExpression {

  private final Optional<CExpression> operand;

  private final Optional<SeqExpression> logicalOperand;

  public SeqLogicalNotExpression(CExpression pOperand) {
    operand = Optional.of(pOperand);
    logicalOperand = Optional.empty();
  }

  /** Use this constructor if the expression is a logical AND, OR, NOT itself. */
  public SeqLogicalNotExpression(SeqExpression pLogicalOperand) {
    operand = Optional.empty();
    logicalOperand = Optional.of(pLogicalOperand);
  }

  @Override
  public String toASTString() {
    String expression;
    if (operand.isPresent()) {
      expression = operand.orElseThrow().toASTString();
    } else if (logicalOperand.isPresent()) {
      expression = logicalOperand.orElseThrow().toASTString();
    } else {
      throw new IllegalArgumentException(
          "either CExpression or SeqExpression operand must be present");
    }
    return getOperator() + SeqSyntax.BRACKET_LEFT + expression + SeqSyntax.BRACKET_RIGHT;
  }

  @Override
  public SeqLogicalOperator getOperator() {
    return SeqLogicalOperator.NOT;
  }
}
