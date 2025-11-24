// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.evaluation;

import com.google.common.base.Joiner;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqAstNode;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqIntegerLiteralExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;

public record BitVectorEvaluationExpression(ExpressionTree<CExpression> expression)
    implements SeqAstNode {

  @Override
  public String toASTString() throws UnrecognizedCodeException {
    return expression.toString();
  }

  /**
   * Negates the evaluation expression via {@code 0 == expression}. Note that if a negated
   * evaluation expression evaluates to {@code true}, then there is no conflict.
   */
  public String toNegatedASTString() throws UnrecognizedCodeException {
    return Joiner.on(SeqSyntax.SPACE)
        .join(
            SeqIntegerLiteralExpressions.INT_0.toASTString(),
            BinaryOperator.EQUALS.getOperator(),
            toASTString());
  }
}
