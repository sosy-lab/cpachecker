// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.evaluation;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqIntegerLiteralExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.BitVectorVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryAccessType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.ReachType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.SeqMemoryLocation;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.cwriter.export.CExportExpression;
import org.sosy_lab.cpachecker.util.cwriter.export.CLogicalOrExpression;

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

  /**
   * Creates a logical conjunction of the given terms: {@code A || B || C ...} or returns {@link
   * Optional#empty()} if {@code pTerms} is empty.
   */
  static Optional<CExportExpression> tryBuildSparseLogicalDisjunction(
      ImmutableList<CExportExpression> pTerms) {

    if (pTerms.isEmpty()) {
      return Optional.empty();
    }
    // when there is only 1 term, use a normal CExportExpression
    if (pTerms.size() == 1) {
      return Optional.of(checkNotNull(Iterables.getOnlyElement(pTerms)));
    }
    // when there are at least 2 terms, use a CLogicalOrExpression (it needs at least 2)
    return Optional.of(new CLogicalOrExpression(pTerms));
  }

  /** Creates a disjunction of the given terms i.e. {@code (A | B | C | ...)}. */
  static CExpression binaryDisjunction(
      ImmutableCollection<CExpression> pDisjunctionTerms,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    return nestBinaryExpressions(
        pDisjunctionTerms, BinaryOperator.BITWISE_OR, pBinaryExpressionBuilder);
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

  // Nest Expressions ==============================================================================

  static ImmutableListMultimap<SeqMemoryLocation, CExpression>
      mapMemoryLocationsToSparseBitVectorsByAccessType(
          ImmutableSet<MPORThread> pOtherThreads,
          BitVectorVariables pBitVectorVariables,
          MemoryAccessType pAccessType) {

    ImmutableListMultimap.Builder<SeqMemoryLocation, CExpression> rMap =
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
