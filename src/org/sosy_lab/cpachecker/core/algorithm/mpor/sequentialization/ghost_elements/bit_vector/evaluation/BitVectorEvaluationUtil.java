// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.evaluation;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqExpressions.SeqIntegerLiteralExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.CToSeqExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.SeqExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.logical.SeqLogicalExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.logical.SeqLogicalOperator;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.BitVectorVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryAccessType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryLocation;
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
      ImmutableSet<MPORThread> pOtherThreads,
      ImmutableMap<MPORThread, CIdExpression> pAllVariables) {

    return pAllVariables.entrySet().stream()
        .filter(entry -> pOtherThreads.contains(entry.getKey()))
        .map(entry -> new CToSeqExpression(entry.getValue()))
        .collect(ImmutableList.toImmutableList());
  }

  static SeqExpression buildSparseDirectBitVector(
      MemoryLocation pMemoryLocation, ImmutableSet<MemoryLocation> pDirectAccessMemoryLocations) {

    CIntegerLiteralExpression integerLiteralExpression =
        pDirectAccessMemoryLocations.contains(pMemoryLocation)
            ? SeqIntegerLiteralExpression.INT_1
            : SeqIntegerLiteralExpression.INT_0;
    return new CToSeqExpression(integerLiteralExpression);
  }

  // Conjunction and Disjunction ===================================================================

  /** Creates a logical conjunction of the given terms: {@code A || B || C ...}. */
  static BitVectorEvaluationExpression buildSparseLogicalDisjunction(
      ImmutableList<SeqExpression> pExpressions) {

    if (pExpressions.isEmpty()) {
      return BitVectorEvaluationExpression.empty();
    }
    SeqExpression logicalDisjunction = BitVectorEvaluationUtil.logicalDisjunction(pExpressions);
    return new BitVectorEvaluationExpression(Optional.empty(), Optional.of(logicalDisjunction));
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

    return SeqExpressionBuilder.nestBinaryExpressions(
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

  static ImmutableListMultimap<MemoryLocation, SeqExpression>
      mapMemoryLocationsToSparseBitVectorsByAccessType(
          ImmutableSet<MPORThread> pOtherThreads,
          BitVectorVariables pBitVectorVariables,
          MemoryAccessType pAccessType) {

    ImmutableListMultimap.Builder<MemoryLocation, SeqExpression> rMap =
        ImmutableListMultimap.builder();
    for (var entry : pBitVectorVariables.getSparseBitVectorByAccessType(pAccessType).entrySet()) {
      MemoryLocation memoryLocation = entry.getKey();
      ImmutableMap<MPORThread, CIdExpression> variables = entry.getValue().reachableVariables;
      ImmutableList<SeqExpression> otherVariables =
          BitVectorEvaluationUtil.convertOtherVariablesToSeqExpression(pOtherThreads, variables);
      rMap.putAll(memoryLocation, otherVariables);
    }
    return rMap.build();
  }
}
