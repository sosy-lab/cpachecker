// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.evaluation;

import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SequentializationUtils;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqAstNode;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.expressions.And;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.LeafExpression;
import org.sosy_lab.cpachecker.util.expressions.Or;
import org.sosy_lab.cpachecker.util.expressions.ToCExpressionVisitor;

public class BitVectorEvaluationExpression implements SeqAstNode {

  public final Optional<CBinaryExpression> binaryExpression;

  public final Optional<ExpressionTree<AExpression>> logicalExpression;

  private final Optional<SequentializationUtils> utils;

  public BitVectorEvaluationExpression(
      CBinaryExpression pBinaryExpression, SequentializationUtils pUtils) {

    binaryExpression = Optional.of(pBinaryExpression);
    logicalExpression = Optional.empty();
    utils = Optional.of(pUtils);
  }

  public BitVectorEvaluationExpression(
      ExpressionTree<AExpression> pLogicalExpression, SequentializationUtils pUtils) {

    binaryExpression = Optional.empty();
    logicalExpression = Optional.of(pLogicalExpression);
    utils = Optional.of(pUtils);
  }

  private BitVectorEvaluationExpression() {
    binaryExpression = Optional.empty();
    logicalExpression = Optional.empty();
    utils = Optional.empty();
  }

  public static BitVectorEvaluationExpression empty() {
    return new BitVectorEvaluationExpression();
  }

  @Override
  public String toASTString() throws UnrecognizedCodeException {
    return toCExpression().toASTString();
  }

  public CExpression toCExpression() throws UnrecognizedCodeException {
    if (binaryExpression.isPresent()) {
      return binaryExpression.orElseThrow();
    }
    ExpressionTree<AExpression> expressionTree = logicalExpression.orElseThrow();
    ToCExpressionVisitor toCExpressionVisitor = utils.orElseThrow().getToCExpressionVisitor();
    return switch (expressionTree) {
      case And<AExpression> andExpression -> toCExpressionVisitor.visit(andExpression);
      case Or<AExpression> orExpression -> toCExpressionVisitor.visit(orExpression);
      // the logical expression can be a single leaf if all else is pruned
      case LeafExpression<AExpression> leafExpression -> toCExpressionVisitor.visit(leafExpression);
      default ->
          throw new IllegalStateException(
              "logicalExpression must be either And, Or, LeafExpression");
    };
  }

  /**
   * Negates the evaluation expression via {@code expression == 0}. Note that if a negated
   * evaluation expression evaluates to {@code true}, then there is no conflict.
   */
  public CExpression negate() throws UnrecognizedCodeException {
    CBinaryExpressionBuilder binaryExpressionBuilder =
        utils.orElseThrow().getBinaryExpressionBuilder();
    return binaryExpressionBuilder.negateExpressionAndSimplify(toCExpression());
  }

  public boolean isEmpty() {
    return binaryExpression.isEmpty() && logicalExpression.isEmpty();
  }
}
