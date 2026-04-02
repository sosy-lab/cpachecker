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
import java.util.Objects;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
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

  static ImmutableListMultimap<SeqMemoryLocation, CExpression>
      mapMemoryLocationsToSparseBitVectorsByAccessType(
          ImmutableSet<MPORThread> pThreads,
          SeqBitVectorVariables pBitVectorVariables,
          MemoryAccessType pAccessType) {

    ImmutableListMultimap.Builder<SeqMemoryLocation, CExpression> rMap =
        ImmutableListMultimap.builder();
    for (var entry : pBitVectorVariables.getSparseBitVectorByAccessType(pAccessType).entrySet()) {
      for (MPORThread thread : pThreads) {
        Optional<CIdExpression> reachableVariable =
            entry.getValue().tryGetVariableByReachTypeAndThread(ReachType.REACHABLE, thread);
        if (reachableVariable.isPresent()) {
          rMap.put(entry.getKey(), reachableVariable.orElseThrow());
        }
      }
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
      MemoryAccessType pAccessType, SeqBitVectorVariables pBitVectorVariables) {

    return pBitVectorVariables.getPrevSparseBitVectorByAccessType(pAccessType).entrySet().stream()
        .collect(
            ImmutableMap.toImmutableMap(
                entry -> entry.getKey(),
                entry -> Objects.requireNonNull(entry).getValue().directVariable()));
  }

  static Optional<CExportExpression> buildPrunedSparseEvaluationByAccessType(
      ImmutableMap<SeqMemoryLocation, CExpression> pLeftHandSides,
      ImmutableListMultimap<SeqMemoryLocation, CExpression> pRightHandSides,
      ImmutableSet<SeqMemoryLocation> pAccessedMemoryLocations,
      MemoryAccessType pAccessType,
      SeqBitVectorVariables pBitVectorVariables) {

    if (!pBitVectorVariables.areSparseBitVectorsPresentByAccessType(pAccessType)) {
      // no sparse variables (i.e. no global variables) -> no evaluation
      return Optional.empty();
    }
    ImmutableList.Builder<CExportExpression> sparseExpressions = ImmutableList.builder();
    for (SeqMemoryLocation memoryLocation :
        pBitVectorVariables.getSparseBitVectorByAccessType(pAccessType).keySet()) {
      ImmutableList<CExpression> rightHandSide = pRightHandSides.get(memoryLocation);
      // if the LHS is not present, then the current thread never accesses the memory location.
      // if the RHS is empty, then no other thread accesses the memory location.
      if (pLeftHandSides.containsKey(memoryLocation) && !rightHandSide.isEmpty()) {
        CExpression leftHandSide = Objects.requireNonNull(pLeftHandSides.get(memoryLocation));
        switch (leftHandSide) {
          case CIntegerLiteralExpression integerLiteralExpression -> {
            // if the memory location is not accessed, then the entire && expression can be pruned
            if (pAccessedMemoryLocations.contains(memoryLocation)) {
              checkState(integerLiteralExpression.equals(CIntegerLiteralExpression.ONE));
              // simplify A && (B || C || ...) to just (B || C || ...) and use OR directly
              Optional<CExportExpression> disjunction =
                  BitVectorEvaluationUtil.tryBuildLogicalOrExpressionFromCExpressions(
                      rightHandSide);
              if (disjunction.isPresent()) {
                sparseExpressions.add(disjunction.orElseThrow());
              }
            } else {
              checkState(integerLiteralExpression.equals(CIntegerLiteralExpression.ZERO));
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
              throw new IllegalStateException("Unexpected type for leftHandSide: " + leftHandSide);
        }
      }
    }
    return BitVectorEvaluationUtil.tryBuildLogicalOrExpression(sparseExpressions.build());
  }

  static Optional<CExportExpression> buildFullSparseEvaluationByAccessType(
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

  /**
   * Note that the 'full' evaluation can still be pruned entirely if {@link
   * MPOROptions#pruneSparseBitVectors()} is enabled, which is why this returns an {@link Optional}.
   */
  static Optional<CExportExpression> buildFullSparseVariableOnlyEvaluationByAccessType(
      MPORThread pActiveThread,
      MemoryAccessType pAccessType,
      ImmutableListMultimap<SeqMemoryLocation, CExpression> pSparseBitVectors,
      SeqBitVectorVariables pBitVectorVariables) {

    ImmutableList.Builder<CExportExpression> sparseExpressions = ImmutableList.builder();
    for (var entry : pBitVectorVariables.getSparseBitVectorByAccessType(pAccessType).entrySet()) {
      Optional<CIdExpression> directVariable =
          entry.getValue().tryGetVariableByReachTypeAndThread(ReachType.DIRECT, pActiveThread);
      // if there is no direct variable for pActiveThread, then the thread does not access the
      // memory location at all, and it can be pruned from the evaluation
      if (directVariable.isPresent()) {
        // if the list of CExpression is empty, then there is no RHS, and pActiveThread is the only
        // thread that accesses the memory location, and it can be pruned from the evaluation.
        if (!pSparseBitVectors.get(entry.getKey()).isEmpty()) {
          CExportExpression sparseExpression =
              BitVectorEvaluationUtil.buildSingleSparseLogicalAndExpression(
                  pSparseBitVectors, directVariable.orElseThrow(), entry.getKey());
          sparseExpressions.add(sparseExpression);
        }
      }
    }
    // create disjunction of logical not: (A && (B || C)) || (A' && (B' || C'))
    return BitVectorEvaluationUtil.tryBuildLogicalOrExpression(sparseExpressions.build());
  }

  // Private Helpers

  private static CExportExpression buildSingleSparseLogicalAndExpression(
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

  /**
   * Creates a logical conjunction of the given terms: {@code A || B || C ...} or returns {@link
   * Optional#empty()} if {@code pTerms} is empty.
   */
  private static Optional<CExportExpression> tryBuildLogicalOrExpressionFromCExpressions(
      ImmutableList<CExpression> pTerms) {

    return tryBuildLogicalOrExpression(
        pTerms.stream().map(CExpressionWrapper::new).collect(ImmutableList.toImmutableList()));
  }

  /**
   * Creates a logical conjunction of the given terms: {@code A || B || C ...} or returns {@link
   * Optional#empty()} if {@code pTerms} is empty.
   */
  private static Optional<CExportExpression> tryBuildLogicalOrExpression(
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
}
