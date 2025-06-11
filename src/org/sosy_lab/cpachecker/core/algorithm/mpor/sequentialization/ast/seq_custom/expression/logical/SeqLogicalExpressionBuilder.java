// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.logical;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableCollection;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.SeqExpression;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class SeqLogicalExpressionBuilder {

  public static SeqLogicalExpression buildBinaryLogicalExpressionByOperator(
      SeqLogicalOperator pOperator, SeqExpression pOperand1, SeqExpression pOperand2) {

    return switch (pOperator) {
      case AND -> new SeqLogicalAndExpression(pOperand1, pOperand2);
      case OR -> new SeqLogicalOrExpression(pOperand1, pOperand2);
      case NOT -> throw new IllegalArgumentException("NOT is not a binary logical operator");
    };
  }

  // Conjunction and Disjunction ===================================================================

  /** Creates a conjunction of the given terms i.e. {@code (A && B && C && ...)}. */
  public static SeqExpression logicalConjunction(ImmutableCollection<SeqExpression> pTerms) {
    return nestLogicalExpressions(pTerms, SeqLogicalOperator.AND);
  }

  /** Creates a disjunction of the given terms i.e. {@code (A || B || C || ...)}. */
  public static SeqExpression logicalDisjunction(ImmutableCollection<SeqExpression> pTerms) {
    return nestLogicalExpressions(pTerms, SeqLogicalOperator.OR);
  }

  /** Creates a disjunction of the given terms i.e. {@code (A | B | C | ...)}. */
  public static CExpression binaryDisjunction(
      ImmutableCollection<CExpression> pDisjunctionTerms,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    return nestBinaryExpressions(
        pDisjunctionTerms, BinaryOperator.BINARY_OR, pBinaryExpressionBuilder);
  }

  // Nest Expressions ==============================================================================

  private static SeqExpression nestLogicalExpressions(
      ImmutableCollection<SeqExpression> pExpressions, SeqLogicalOperator pLogicalOperator) {

    checkArgument(!pExpressions.isEmpty(), "pExpressions must not be empty");

    SeqExpression rNested = pExpressions.iterator().next();
    for (SeqExpression next : pExpressions) {
      if (!next.equals(rNested)) {
        rNested =
            SeqLogicalExpressionBuilder.buildBinaryLogicalExpressionByOperator(
                pLogicalOperator, rNested, next);
      }
    }
    return rNested;
  }

  private static CExpression nestBinaryExpressions(
      ImmutableCollection<CExpression> pAllExpressions,
      BinaryOperator pBinaryOperator,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    checkArgument(!pAllExpressions.isEmpty(), "pAllExpressions must not be empty");

    CExpression rNested = pAllExpressions.iterator().next();
    for (CExpression next : pAllExpressions) {
      if (!next.equals(rNested)) {
        rNested = pBinaryExpressionBuilder.buildBinaryExpression(rNested, next, pBinaryOperator);
      }
    }
    return rNested;
  }
}
