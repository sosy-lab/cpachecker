// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.evaluation;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.SeqExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.logical.SeqLogicalAndExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.logical.SeqLogicalOrExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryAccessType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryLocation;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

class BitVectorReadWriteEvaluationBuilder {

  static BitVectorEvaluationExpression buildVariableOnlyEvaluationByEncoding(
      MPOROptions pOptions,
      MPORThread pActiveThread,
      ImmutableSet<MPORThread> pOtherThreads,
      BitVectorVariables pBitVectorVariables,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    return switch (pOptions.bitVectorEncoding) {
      case NONE ->
          throw new IllegalArgumentException(
              "cannot build evaluation for encoding " + pOptions.bitVectorEncoding);
      case BINARY, DECIMAL, HEXADECIMAL ->
          buildFullDenseVariableOnlyEvaluation(
              pActiveThread, pOtherThreads, pBitVectorVariables, pBinaryExpressionBuilder);
      case SPARSE ->
          // TODO add support
          throw new IllegalArgumentException(
              "cannot build evaluation for encoding " + pOptions.bitVectorEncoding);
    };
  }

  static BitVectorEvaluationExpression buildDenseEvaluation(
      MPOROptions pOptions,
      ImmutableSet<CExpression> pOtherWriteBitVectors,
      ImmutableSet<CExpression> pOtherAccessBitVectors,
      ImmutableSet<MemoryLocation> pDirectReadMemoryLocations,
      ImmutableSet<MemoryLocation> pDirectWriteMemoryLocations,
      BitVectorVariables pBitVectorVariables,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    if (pOptions.bitVectorEvaluationPrune) {
      return buildPrunedDenseEvaluation(
          pOtherWriteBitVectors,
          pOtherAccessBitVectors,
          pDirectReadMemoryLocations,
          pDirectWriteMemoryLocations,
          pBitVectorVariables,
          pBinaryExpressionBuilder);
    } else {
      return buildFullDenseEvaluation(
          pOtherWriteBitVectors,
          pOtherAccessBitVectors,
          pDirectReadMemoryLocations,
          pDirectWriteMemoryLocations,
          pBitVectorVariables,
          pBinaryExpressionBuilder);
    }
  }

  static BitVectorEvaluationExpression buildSparseEvaluation(
      MPOROptions pOptions,
      ImmutableListMultimap<MemoryLocation, SeqExpression> pSparseWriteMap,
      ImmutableListMultimap<MemoryLocation, SeqExpression> pSparseAccessMap,
      ImmutableSet<MemoryLocation> pDirectReadMemoryLocations,
      ImmutableSet<MemoryLocation> pDirectWriteMemoryLocations,
      BitVectorVariables pBitVectorVariables) {

    if (pOptions.bitVectorEvaluationPrune) {
      return buildPrunedSparseEvaluation(
          pSparseWriteMap,
          pSparseAccessMap,
          pDirectReadMemoryLocations,
          pDirectWriteMemoryLocations,
          pBitVectorVariables);
    } else {
      return buildFullSparseEvaluation(
          pSparseWriteMap,
          pSparseAccessMap,
          pDirectReadMemoryLocations,
          pDirectWriteMemoryLocations,
          pBitVectorVariables);
    }
  }

  // Pruned Dense Evaluation =======================================================================

  private static BitVectorEvaluationExpression buildPrunedDenseEvaluation(
      ImmutableSet<CExpression> pOtherWriteBitVectors,
      ImmutableSet<CExpression> pOtherAccessBitVectors,
      ImmutableSet<MemoryLocation> pDirectReadMemoryLocations,
      ImmutableSet<MemoryLocation> pDirectWriteMemoryLocations,
      BitVectorVariables pBitVectorVariables,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    Optional<CBinaryExpression> leftHandSide =
        buildPrunedDenseLeftHandSide(
            pOtherWriteBitVectors,
            pDirectReadMemoryLocations,
            pBitVectorVariables,
            pBinaryExpressionBuilder);
    Optional<CBinaryExpression> rightHandSide =
        buildPrunedDenseRightHandSide(
            pOtherAccessBitVectors,
            pDirectWriteMemoryLocations,
            pBitVectorVariables,
            pBinaryExpressionBuilder);

    if (leftHandSide.isPresent() && rightHandSide.isPresent()) {
      // both LHS and RHS present: create or expression: ||
      SeqLogicalOrExpression logicalOr =
          new SeqLogicalOrExpression(leftHandSide.orElseThrow(), rightHandSide.orElseThrow());
      return new BitVectorEvaluationExpression(Optional.empty(), Optional.of(logicalOr));
    } else if (leftHandSide.isPresent()) {
      return new BitVectorEvaluationExpression(leftHandSide, Optional.empty());
    } else if (rightHandSide.isPresent()) {
      return new BitVectorEvaluationExpression(rightHandSide, Optional.empty());
    }
    return new BitVectorEvaluationExpression(Optional.empty(), Optional.empty());
  }

  private static Optional<CBinaryExpression> buildPrunedDenseLeftHandSide(
      ImmutableSet<CExpression> pOtherWriteBitVectors,
      ImmutableSet<MemoryLocation> pDirectReadMemoryLocations,
      BitVectorVariables pBitVectorVariables,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    if (pDirectReadMemoryLocations.isEmpty()) {
      return Optional.empty();
    } else {
      CIntegerLiteralExpression directReadBitVector =
          BitVectorUtil.buildDirectBitVectorExpression(
              pBitVectorVariables.getMemoryLocationIds(), pDirectReadMemoryLocations);
      CBinaryExpression leftHandSide =
          buildGeneralDenseLeftHandSide(
              directReadBitVector, pOtherWriteBitVectors, pBinaryExpressionBuilder);
      return Optional.of(leftHandSide);
    }
  }

  private static Optional<CBinaryExpression> buildPrunedDenseRightHandSide(
      ImmutableSet<CExpression> pOtherAccessBitVectors,
      ImmutableSet<MemoryLocation> pDirectWriteMemoryLocations,
      BitVectorVariables pBitVectorVariables,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    if (pDirectWriteMemoryLocations.isEmpty()) {
      return Optional.empty();
    } else {
      CIntegerLiteralExpression directWriteBitVector =
          BitVectorUtil.buildDirectBitVectorExpression(
              pBitVectorVariables.getMemoryLocationIds(), pDirectWriteMemoryLocations);
      CBinaryExpression rRightHandSide =
          buildGeneralDenseRightHandSide(
              directWriteBitVector, pOtherAccessBitVectors, pBinaryExpressionBuilder);
      return Optional.of(rRightHandSide);
    }
  }

  // Full Dense Evaluation =========================================================================

  private static BitVectorEvaluationExpression buildFullDenseEvaluation(
      ImmutableSet<CExpression> pOtherWriteBitVectors,
      ImmutableSet<CExpression> pOtherAccessBitVectors,
      ImmutableSet<MemoryLocation> pDirectReadMemoryLocations,
      ImmutableSet<MemoryLocation> pDirectWriteMemoryLocations,
      BitVectorVariables pBitVectorVariables,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    CIntegerLiteralExpression directReadBitVector =
        BitVectorUtil.buildDirectBitVectorExpression(
            pBitVectorVariables.getMemoryLocationIds(), pDirectReadMemoryLocations);
    CIntegerLiteralExpression directWriteBitVector =
        BitVectorUtil.buildDirectBitVectorExpression(
            pBitVectorVariables.getMemoryLocationIds(), pDirectWriteMemoryLocations);
    return buildFullDenseLogicalOr(
        directReadBitVector,
        directWriteBitVector,
        pOtherWriteBitVectors,
        pOtherAccessBitVectors,
        pBinaryExpressionBuilder);
  }

  private static BitVectorEvaluationExpression buildFullDenseVariableOnlyEvaluation(
      MPORThread pActiveThread,
      ImmutableSet<MPORThread> pOtherThreads,
      BitVectorVariables pBitVectorVariables,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    CExpression directReadBitVector =
        pBitVectorVariables.getDenseDirectBitVectorByAccessType(
            MemoryAccessType.READ, pActiveThread);
    CExpression directWriteBitVector =
        pBitVectorVariables.getDenseDirectBitVectorByAccessType(
            MemoryAccessType.WRITE, pActiveThread);
    ImmutableSet<CExpression> otherWriteBitVectors =
        pBitVectorVariables.getOtherDenseReachableBitVectorsByAccessType(
            MemoryAccessType.WRITE, pOtherThreads);
    ImmutableSet<CExpression> otherAccessBitVectors =
        pBitVectorVariables.getOtherDenseReachableBitVectorsByAccessType(
            MemoryAccessType.ACCESS, pOtherThreads);
    return buildFullDenseLogicalOr(
        directReadBitVector,
        directWriteBitVector,
        otherWriteBitVectors,
        otherAccessBitVectors,
        pBinaryExpressionBuilder);
  }

  private static BitVectorEvaluationExpression buildFullDenseLogicalOr(
      CExpression pDirectReadBitVector,
      CExpression pDirectWriteBitVector,
      ImmutableSet<CExpression> pOtherWriteBitVectors,
      ImmutableSet<CExpression> pOtherAccessBitVectors,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    // (R & (W' | W'' | ...))
    CExpression leftHandSide =
        buildGeneralDenseLeftHandSide(
            pDirectReadBitVector, pOtherWriteBitVectors, pBinaryExpressionBuilder);
    // (W & (A' | A'' | ...))
    CExpression rightHandSide =
        buildGeneralDenseRightHandSide(
            pDirectWriteBitVector, pOtherAccessBitVectors, pBinaryExpressionBuilder);
    // (R & (W' | W'' | ...)) || (W & (A' | A'' | ...))
    SeqLogicalOrExpression logicalOr = new SeqLogicalOrExpression(leftHandSide, rightHandSide);
    return new BitVectorEvaluationExpression(Optional.empty(), Optional.of(logicalOr));
  }

  // General Dense Evaluation ======================================================================

  /** General = used for both pruned and full evaluations. */
  private static CBinaryExpression buildGeneralDenseLeftHandSide(
      CExpression pDirectReadBitVector,
      ImmutableSet<CExpression> pOtherWriteBitVectors,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    CExpression otherWrites =
        BitVectorEvaluationUtil.binaryDisjunction(pOtherWriteBitVectors, pBinaryExpressionBuilder);
    return pBinaryExpressionBuilder.buildBinaryExpression(
        pDirectReadBitVector, otherWrites, BinaryOperator.BINARY_AND);
  }

  /** General = used for both pruned and full evaluations. */
  private static CBinaryExpression buildGeneralDenseRightHandSide(
      CExpression pDirectWriteBitVector,
      ImmutableSet<CExpression> pOtherAccesses,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    CExpression otherReadsAndWrites =
        BitVectorEvaluationUtil.binaryDisjunction(pOtherAccesses, pBinaryExpressionBuilder);
    return pBinaryExpressionBuilder.buildBinaryExpression(
        pDirectWriteBitVector, otherReadsAndWrites, BinaryOperator.BINARY_AND);
  }

  // Pruned Sparse Evaluation ======================================================================

  private static BitVectorEvaluationExpression buildPrunedSparseEvaluation(
      ImmutableListMultimap<MemoryLocation, SeqExpression> pSparseWriteMap,
      ImmutableListMultimap<MemoryLocation, SeqExpression> pSparseAccessMap,
      ImmutableSet<MemoryLocation> pDirectReadMemoryLocations,
      ImmutableSet<MemoryLocation> pDirectWriteMemoryLocations,
      BitVectorVariables pBitVectorVariables) {

    if (pBitVectorVariables.areSparseBitVectorsEmpty()) {
      // no bit vectors (e.g. no global variables) -> no evaluation
      return BitVectorEvaluationExpression.empty();
    }
    ImmutableList.Builder<SeqExpression> sparseExpressions = ImmutableList.builder();
    for (var entry : pBitVectorVariables.getSparseAccessBitVectors().entrySet()) {
      MemoryLocation memoryLocation = entry.getKey();

      // handle write variables
      ImmutableList<SeqExpression> otherWriteVariables = pSparseWriteMap.get(memoryLocation);
      // handle access variables
      ImmutableList<SeqExpression> otherAccessVariables = pSparseAccessMap.get(memoryLocation);

      Optional<SeqExpression> leftHandSide =
          buildPrunedSparseLeftHandSide(
              pDirectReadMemoryLocations, memoryLocation, otherWriteVariables);
      Optional<SeqExpression> rightHandSide =
          buildPrunedSparseRightHandSide(
              pDirectWriteMemoryLocations, memoryLocation, otherAccessVariables);

      // only add expression if it was not pruned entirely (LHS or RHS present)
      if (leftHandSide.isPresent() || rightHandSide.isPresent()) {
        sparseExpressions.add(
            buildPrunedSparseSingleVariableEvaluation(leftHandSide, rightHandSide));
      }
    }
    if (sparseExpressions.build().isEmpty()) {
      return BitVectorEvaluationExpression.empty();
    }
    return BitVectorEvaluationUtil.buildSparseLogicalDisjunction(sparseExpressions.build());
  }

  /** Builds the logical LHS i.e. {@code (R && (W' || W'' || ...))}. */
  private static Optional<SeqExpression> buildPrunedSparseLeftHandSide(
      ImmutableSet<MemoryLocation> pDirectReadMemoryLocations,
      MemoryLocation pMemoryLocation,
      ImmutableList<SeqExpression> pOtherWriteVariables) {

    if (!pDirectReadMemoryLocations.contains(pMemoryLocation)) {
      // if the LHS is 0, then the entire && expression is 0 -> prune
      return Optional.empty();
    } else {
      // otherwise the LHS is 1, and we only need the right side of the && expression
      return Optional.of(BitVectorEvaluationUtil.logicalDisjunction(pOtherWriteVariables));
    }
  }

  /** Builds the logical RHS i.e. {@code (W && (A' || A'' || ...))}. */
  private static Optional<SeqExpression> buildPrunedSparseRightHandSide(
      ImmutableSet<MemoryLocation> pDirectWriteMemoryLocations,
      MemoryLocation pMemoryLocation,
      ImmutableList<SeqExpression> pOtherAccessVariables) {

    if (!pDirectWriteMemoryLocations.contains(pMemoryLocation)) {
      // if the LHS (activeWriteVariable) is 0, then the entire && expression is 0 -> prune
      return Optional.empty();
    } else {
      // otherwise the LHS is 1, and we only need the right side of the && expression
      return Optional.of(BitVectorEvaluationUtil.logicalDisjunction(pOtherAccessVariables));
    }
  }

  // Full Sparse Evaluation ========================================================================

  private static BitVectorEvaluationExpression buildFullSparseEvaluation(
      ImmutableListMultimap<MemoryLocation, SeqExpression> pSparseWriteMap,
      ImmutableListMultimap<MemoryLocation, SeqExpression> pSparseAccessMap,
      ImmutableSet<MemoryLocation> pDirectReadMemoryLocations,
      ImmutableSet<MemoryLocation> pDirectWriteMemoryLocations,
      BitVectorVariables pBitVectorVariables) {

    if (pBitVectorVariables.areSparseBitVectorsEmpty()) {
      return BitVectorEvaluationExpression.empty();
    }
    ImmutableList.Builder<SeqExpression> sparseExpressions = ImmutableList.builder();
    ImmutableSet<MemoryLocation> memoryLocations =
        pBitVectorVariables.getSparseAccessBitVectors().keySet();
    for (MemoryLocation memoryLocation : memoryLocations) {
      ImmutableList<SeqExpression> otherWriteVariables = pSparseWriteMap.get(memoryLocation);
      ImmutableList<SeqExpression> otherAccessVariables = pSparseAccessMap.get(memoryLocation);
      sparseExpressions.add(
          buildFullSparseSingleVariableEvaluation(
              memoryLocation,
              pDirectReadMemoryLocations,
              pDirectWriteMemoryLocations,
              otherWriteVariables,
              otherAccessVariables));
    }
    return BitVectorEvaluationUtil.buildSparseLogicalDisjunction(sparseExpressions.build());
  }

  // Pruned Sparse Single Variable Evaluation ======================================================

  private static SeqExpression buildPrunedSparseSingleVariableEvaluation(
      Optional<SeqExpression> pLeftHandSide, Optional<SeqExpression> pRightHandSide) {

    if (pLeftHandSide.isPresent() && pRightHandSide.isEmpty()) {
      return pLeftHandSide.orElseThrow(); // only LHS
    }
    if (pLeftHandSide.isEmpty() && pRightHandSide.isPresent()) {
      return pRightHandSide.orElseThrow(); // only RHS
    }
    return new SeqLogicalOrExpression(pLeftHandSide.orElseThrow(), pRightHandSide.orElseThrow());
  }

  // Full Sparse Single Variable Evaluation ========================================================

  private static SeqLogicalAndExpression buildFullSparseSingleVariableLeftHandSide(
      MemoryLocation pMemoryLocation,
      ImmutableSet<MemoryLocation> pDirectReadMemoryLocations,
      ImmutableList<SeqExpression> pOtherWriteVariables) {

    SeqExpression activeReadValue =
        BitVectorEvaluationUtil.buildSparseDirectBitVector(
            pMemoryLocation, pDirectReadMemoryLocations);
    return new SeqLogicalAndExpression(
        activeReadValue, BitVectorEvaluationUtil.logicalDisjunction(pOtherWriteVariables));
  }

  private static SeqLogicalAndExpression buildFullSparseSingleVariableRightHandSide(
      MemoryLocation pMemoryLocation,
      ImmutableSet<MemoryLocation> pDirectWriteMemoryLocations,
      ImmutableList<SeqExpression> pOtherReadAndWriteVariables) {

    SeqExpression activeWriteValue =
        BitVectorEvaluationUtil.buildSparseDirectBitVector(
            pMemoryLocation, pDirectWriteMemoryLocations);
    return new SeqLogicalAndExpression(
        activeWriteValue, BitVectorEvaluationUtil.logicalDisjunction(pOtherReadAndWriteVariables));
  }

  private static SeqLogicalOrExpression buildFullSparseSingleVariableEvaluation(
      MemoryLocation pMemoryLocation,
      ImmutableSet<MemoryLocation> pDirectReadMemoryLocations,
      ImmutableSet<MemoryLocation> pDirectWriteMemoryLocations,
      ImmutableList<SeqExpression> pOtherWriteVariables,
      ImmutableList<SeqExpression> pOtherAccessVariables) {

    SeqLogicalAndExpression leftHandSide =
        buildFullSparseSingleVariableLeftHandSide(
            pMemoryLocation, pDirectReadMemoryLocations, pOtherWriteVariables);
    SeqLogicalAndExpression rightHandSide =
        buildFullSparseSingleVariableRightHandSide(
            pMemoryLocation, pDirectWriteMemoryLocations, pOtherAccessVariables);
    return new SeqLogicalOrExpression(leftHandSide, rightHandSide);
  }
}
