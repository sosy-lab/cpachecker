// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.evaluation;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqIntegerLiteralExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.SeqBitVectorVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryAccessType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.ReachType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.SeqMemoryLocation;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.cwriter.export.CExportExpression;
import org.sosy_lab.cpachecker.util.cwriter.export.CExpressionWrapper;
import org.sosy_lab.cpachecker.util.cwriter.export.CLogicalAndExpression;
import org.sosy_lab.cpachecker.util.cwriter.export.CLogicalOrExpression;

public class BitVectorEvaluationUtil {

  /**
   * Creates a logical conjunction of the given terms: {@code A || B || C ...} or returns {@link
   * Optional#empty()} if {@code pTerms} is empty.
   */
  static Optional<CExportExpression> tryBuildLogicalOrExpressionFromCExpressions(
      ImmutableList<CExpression> pTerms) {

    return tryBuildLogicalOrExpression(
        pTerms.stream().map(CExpressionWrapper::new).collect(ImmutableList.toImmutableList()));
  }

  /**
   * Creates a logical conjunction of the given terms: {@code A || B || C ...} or returns {@link
   * Optional#empty()} if {@code pTerms} is empty.
   */
  static Optional<CExportExpression> tryBuildLogicalOrExpression(
      ImmutableList<CExportExpression> pTerms) {

    if (pTerms.isEmpty()) {
      return Optional.empty();
    }
    // when there is only 1 term, use a normal CExportExpression
    if (pTerms.size() == 1) {
      return Optional.of(pTerms.getFirst());
    }
    // when there are at least 2 terms, use a CLogicalOrExpression (it needs at least 2)
    return Optional.of(new CLogicalOrExpression(pTerms));
  }

  /** Creates a disjunction of the given terms i.e. {@code (A | B | C | ...)}. */
  static CExpression binaryDisjunction(
      ImmutableCollection<CExpression> pDisjunctionTerms,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    checkArgument(!pDisjunctionTerms.isEmpty(), "pAllExpressions must not be empty");

    CExpression rNested = pDisjunctionTerms.iterator().next();
    for (CExpression next : pDisjunctionTerms) {
      if (!next.equals(rNested)) {
        rNested =
            pBinaryExpressionBuilder.buildBinaryExpression(
                rNested, next, BinaryOperator.BITWISE_OR);
      }
    }
    return rNested;
  }

  // Nest Expressions ==============================================================================

  static ImmutableListMultimap<SeqMemoryLocation, CExpression>
      mapMemoryLocationsToSparseBitVectorsByAccessType(
          ImmutableSet<MPORThread> pOtherThreads,
          SeqBitVectorVariables pBitVectorVariables,
          MemoryAccessType pAccessType) {

    ImmutableListMultimap.Builder<SeqMemoryLocation, CExpression> rMap =
        ImmutableListMultimap.builder();
    for (var entry : pBitVectorVariables.getSparseBitVectorByAccessType(pAccessType).entrySet()) {
      SeqMemoryLocation memoryLocation = entry.getKey();
      ImmutableMap<MPORThread, CIdExpression> variables =
          entry.getValue().getVariablesByReachType(ReachType.REACHABLE);
      ImmutableList<CIdExpression> otherVariables =
          variables.entrySet().stream()
              .filter(e -> pOtherThreads.contains(e.getKey()))
              .map(e -> e.getValue())
              .collect(ImmutableList.toImmutableList());
      rMap.putAll(memoryLocation, otherVariables);
    }
    return rMap.build();
  }

  // Sparse Bit Vectors ============================================================================

  static ImmutableMap<SeqMemoryLocation, CExpression> buildSparseLeftHandSidesByAccessType(
      ImmutableSet<SeqMemoryLocation> pAccessedMemoryLocations,
      MemoryAccessType pAccessType,
      SeqBitVectorVariables pBitVectorVariables) {

    return pBitVectorVariables.getSparseBitVectorByAccessType(pAccessType).keySet().stream()
        .collect(
            ImmutableMap.toImmutableMap(
                memoryLocation -> memoryLocation,
                memoryLocation ->
                    pAccessedMemoryLocations.contains(memoryLocation)
                        ? SeqIntegerLiteralExpressions.INT_1
                        : SeqIntegerLiteralExpressions.INT_0));
  }

  static ImmutableMap<SeqMemoryLocation, CExpression> buildPrevSparseLeftHandSidesByAccessType(
      MPORThread pCurrentThread,
      MemoryAccessType pAccessType,
      SeqBitVectorVariables pBitVectorVariables) {

    return pBitVectorVariables.getSparseBitVectorByAccessType(pAccessType).entrySet().stream()
        // filter the LHS that are accessed by the thread so that pruneSparseBitVectors is handled
        .filter(entry -> entry.getValue().directVariables().containsKey(pCurrentThread))
        .collect(
            ImmutableMap.toImmutableMap(
                Entry::getKey,
                entry ->
                    Objects.requireNonNull(
                        entry.getValue().directVariables().get(pCurrentThread))));
  }

  static Optional<CExportExpression> buildPrunedSparseEvaluation(
      ImmutableMap<SeqMemoryLocation, CExpression> pLeftHandSides,
      ImmutableListMultimap<SeqMemoryLocation, CExpression> pRightHandSides,
      ImmutableSet<SeqMemoryLocation> pMemoryLocations,
      MemoryAccessType pAccessType,
      SeqBitVectorVariables pBitVectorVariables) {

    if (!pBitVectorVariables.areSparseBitVectorsPresentByAccessType(pAccessType)) {
      // no sparse variables (i.e. no global variables) -> no evaluation
      return Optional.empty();
    }
    ImmutableList.Builder<CExportExpression> sparseExpressions = ImmutableList.builder();
    for (SeqMemoryLocation memoryLocation :
        pBitVectorVariables.getSparseAccessBitVectors().keySet()) {
      // if the memory location is not accessed, then the entire && expression can be pruned
      if (pMemoryLocations.contains(memoryLocation)) {
        ImmutableList<CExpression> sparseBitVectors = pRightHandSides.get(memoryLocation);
        // if the memory location is accessed, check if any expression exists for the RHS
        if (!sparseBitVectors.isEmpty()) {
          CExpression leftHandSide = Objects.requireNonNull(pLeftHandSides.get(memoryLocation));
          switch (leftHandSide) {
            case CIntegerLiteralExpression integerLiteralExpression -> {
              // if the LHS is an integer, it must be 1 and can be pruned
              checkState(integerLiteralExpression.equals(CIntegerLiteralExpression.ONE));
              Optional<CExportExpression> disjunction =
                  BitVectorEvaluationUtil.tryBuildLogicalOrExpressionFromCExpressions(
                      sparseBitVectors);
              if (disjunction.isPresent()) {
                // simplify A && (B || C || ...) to just (B || C || ...)
                sparseExpressions.add(disjunction.orElseThrow());
              }
            }
            case CIdExpression idExpression -> {
              // if the LHS is a CIdExpression, it cannot be pruned
              CExportExpression logicalAnd =
                  buildSingleSparseLogicalAndExpression(
                      pRightHandSides, idExpression, memoryLocation);
              sparseExpressions.add(logicalAnd);
            }
            default ->
                throw new IllegalStateException(
                    "Unexpected type for leftHandSide: " + leftHandSide);
          }
        }
      }
    }
    return BitVectorEvaluationUtil.tryBuildLogicalOrExpression(sparseExpressions.build());
  }

  static Optional<CExportExpression> buildFullSparseEvaluation(
      ImmutableMap<SeqMemoryLocation, CExpression> pLeftHandSides,
      ImmutableListMultimap<SeqMemoryLocation, CExpression> pRightHandSides,
      MemoryAccessType pAccessType,
      SeqBitVectorVariables pBitVectorVariables) {

    if (!pBitVectorVariables.areSparseBitVectorsPresentByAccessType(pAccessType)) {
      // no sparse variables (i.e. no global variables) -> no evaluation
      return Optional.empty();
    }
    ImmutableList.Builder<CExportExpression> sparseExpressions = ImmutableList.builder();
    for (var entry : pLeftHandSides.entrySet()) {
      CExportExpression logicalAnd =
          BitVectorEvaluationUtil.buildSingleSparseLogicalAndExpression(
              pRightHandSides, entry.getValue(), entry.getKey());
      sparseExpressions.add(logicalAnd);
    }
    // create disjunction of logical not: (A && (B || C)) || (A' && (B' || C'))
    return tryBuildLogicalOrExpression(sparseExpressions.build());
  }

  static CExportExpression buildSingleSparseLogicalAndExpression(
      ImmutableListMultimap<SeqMemoryLocation, CExpression> pSparseBitVectorMap,
      CExpression pDirectBitVector,
      SeqMemoryLocation pMemoryLocation) {

    // create logical disjunction -> (B || C || ...)
    Optional<CExportExpression> disjunction =
        BitVectorEvaluationUtil.tryBuildLogicalOrExpressionFromCExpressions(
            pSparseBitVectorMap.get(pMemoryLocation));
    CExportExpression directBitVector = new CExpressionWrapper(pDirectBitVector);

    // if the logical disjunction is empty, return (A), otherwise return (A && (B || ...))
    return disjunction.isEmpty()
        ? directBitVector
        : CLogicalAndExpression.of(directBitVector, disjunction.orElseThrow());
  }
}
