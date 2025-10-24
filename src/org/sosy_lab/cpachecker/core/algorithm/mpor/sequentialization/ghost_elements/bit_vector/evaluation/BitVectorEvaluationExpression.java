// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.evaluation;

import com.google.common.base.Joiner;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqAstNode;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqIntegerLiteralExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;

public class BitVectorEvaluationExpression implements SeqAstNode {

  public final Optional<CBinaryExpression> binaryExpression;

  public final Optional<ExpressionTree<AExpression>> logicalExpression;

  public BitVectorEvaluationExpression(CBinaryExpression pBinaryExpression) {
    binaryExpression = Optional.of(pBinaryExpression);
    logicalExpression = Optional.empty();
  }

  public BitVectorEvaluationExpression(ExpressionTree<AExpression> pLogicalExpression) {
    binaryExpression = Optional.empty();
    logicalExpression = Optional.of(pLogicalExpression);
  }

  private BitVectorEvaluationExpression() {
    binaryExpression = Optional.empty();
    logicalExpression = Optional.empty();
  }

  public static BitVectorEvaluationExpression empty() {
    return new BitVectorEvaluationExpression();
  }

  @Override
  public String toASTString() throws UnrecognizedCodeException {
    if (binaryExpression.isPresent()) {
      return binaryExpression.orElseThrow().toASTString();
    }
    if (logicalExpression.isPresent()) {
      return logicalExpression.orElseThrow().toString();
    }
    throw new IllegalStateException("both binaryExpression and logicalExpression are empty");
  }

  /**
   * Negates the evaluation expression via {@code 0 == expression}. Note that if a negated
   * evaluation expression evaluates to {@code true}, then there is no conflict.
   */
  public String negate() throws UnrecognizedCodeException {
    return Joiner.on(SeqSyntax.SPACE)
        .join(
            SeqIntegerLiteralExpressions.INT_0.toASTString(),
            BinaryOperator.EQUALS.getOperator(),
            toASTString());
  }

  public boolean isEmpty() {
    return binaryExpression.isEmpty() && logicalExpression.isEmpty();
  }
}
