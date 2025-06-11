// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.bit_vector.evaluation;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqExpressions.SeqIntegerLiteralExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.CToSeqExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.SeqExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.logical.SeqLogicalExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.logical.SeqLogicalOperator;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class BitVectorEvaluationUtil {

  static CIdExpression extractActiveVariable(
      MPORThread pActiveThread, ImmutableMap<MPORThread, CIdExpression> pAllVariables) {
    assert pAllVariables.containsKey(pActiveThread) : "no variable found for active thread";
    CIdExpression rActiveVariable = pAllVariables.get(pActiveThread);
    assert rActiveVariable != null;
    return rActiveVariable;
  }

  static ImmutableList<SeqExpression> convertOtherVariablesToSeqExpression(
      CIdExpression pActiveVariable, ImmutableMap<MPORThread, CIdExpression> pAllVariables) {

    return pAllVariables.values().stream()
        .filter(v -> !v.equals(pActiveVariable))
        .map(CToSeqExpression::new)
        .collect(ImmutableList.toImmutableList());
  }

  static SeqExpression buildScalarDirectBitVector(
      CVariableDeclaration pGlobalVariable,
      ImmutableSet<CVariableDeclaration> pDirectAccessVariables) {

    CIntegerLiteralExpression integerLiteralExpression =
        pDirectAccessVariables.contains(pGlobalVariable)
            ? SeqIntegerLiteralExpression.INT_1
            : SeqIntegerLiteralExpression.INT_0;
    return new CToSeqExpression(integerLiteralExpression);
  }

  // Conjunction and Disjunction ===================================================================

  /** Creates a logical conjunction of the given terms: {@code A && B && C ...}. */
  static BitVectorEvaluationExpression buildScalarLogicalConjunction(
      ImmutableList<SeqExpression> pScalarExpressions) {

    SeqExpression logicalConjunction =
        BitVectorEvaluationUtil.logicalConjunction(pScalarExpressions);
    return new BitVectorEvaluationExpression(Optional.empty(), Optional.of(logicalConjunction));
  }

  /** Creates a conjunction of the given terms i.e. {@code (A && B && C && ...)}. */
  static SeqExpression logicalConjunction(ImmutableCollection<SeqExpression> pTerms) {
    return nestLogicalExpressions(pTerms, SeqLogicalOperator.AND);
  }

  /** Creates a disjunction of the given terms i.e. {@code (A || B || C || ...)}. */
  static SeqExpression logicalDisjunction(ImmutableCollection<SeqExpression> pTerms) {
    return nestLogicalExpressions(pTerms, SeqLogicalOperator.OR);
  }

  /** Creates a disjunction of the given terms i.e. {@code (A | B | C | ...)}. */
  static CExpression binaryDisjunction(
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
