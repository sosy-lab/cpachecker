// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.evaluation;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqIntegerLiteralExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.BitVectorVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryAccessType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.ReachType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.SeqMemoryLocation;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.Or;

public class BitVectorEvaluationUtil {

  static ImmutableList<CIdExpression> getOtherVariables(
      ImmutableSet<MPORThread> pOtherThreads,
      ImmutableMap<MPORThread, CIdExpression> pAllVariables) {

    return pAllVariables.entrySet().stream()
        .filter(entry -> pOtherThreads.contains(entry.getKey()))
        .map(entry -> entry.getValue())
        .collect(ImmutableList.toImmutableList());
  }

  static CIntegerLiteralExpression buildSparseDirectBitVector(
      SeqMemoryLocation pMemoryLocation,
      ImmutableSet<SeqMemoryLocation> pDirectAccessMemoryLocations) {

    return pDirectAccessMemoryLocations.contains(pMemoryLocation)
        ? SeqIntegerLiteralExpressions.INT_1
        : SeqIntegerLiteralExpressions.INT_0;
  }

  // Conjunction and Disjunction ===================================================================

  /** Creates a logical conjunction of the given terms: {@code A || B || C ...}. */
  static BitVectorEvaluationExpression buildSparseLogicalDisjunction(
      ImmutableList<ExpressionTree<AExpression>> pTerms) {

    if (pTerms.isEmpty()) {
      return BitVectorEvaluationExpression.empty();
    }
    return new BitVectorEvaluationExpression(logicalDisjunction(pTerms));
  }

  static <LeafType> Optional<ExpressionTree<LeafType>> tryLogicalDisjunction(
      ImmutableList<ExpressionTree<LeafType>> pTerms) {

    return pTerms.isEmpty() ? Optional.empty() : Optional.of(Or.of(pTerms));
  }

  /** Creates a disjunction of the given terms i.e. {@code (A || B || C || ...)}. */
  static <LeafType> ExpressionTree<LeafType> logicalDisjunction(
      ImmutableList<ExpressionTree<LeafType>> pTerms) {

    return Or.of(pTerms);
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

  static ImmutableListMultimap<SeqMemoryLocation, AExpression>
      mapMemoryLocationsToSparseBitVectorsByAccessType(
          ImmutableSet<MPORThread> pOtherThreads,
          BitVectorVariables pBitVectorVariables,
          MemoryAccessType pAccessType) {

    ImmutableListMultimap.Builder<SeqMemoryLocation, AExpression> rMap =
        ImmutableListMultimap.builder();
    for (var entry : pBitVectorVariables.getSparseBitVectorByAccessType(pAccessType).entrySet()) {
      SeqMemoryLocation memoryLocation = entry.getKey();
      ImmutableMap<MPORThread, CIdExpression> variables =
          entry.getValue().getVariablesByReachType(ReachType.REACHABLE);
      ImmutableList<CIdExpression> otherVariables =
          BitVectorEvaluationUtil.getOtherVariables(pOtherThreads, variables);
      rMap.putAll(memoryLocation, otherVariables);
    }
    return rMap.build();
  }
}
