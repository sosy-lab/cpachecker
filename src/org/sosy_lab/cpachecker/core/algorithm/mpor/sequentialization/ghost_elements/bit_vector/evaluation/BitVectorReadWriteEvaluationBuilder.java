// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.evaluation;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableSet;
import java.util.Objects;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SequentializationUtils;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.BitVectorUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.BitVectorVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.SparseBitVector;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryAccessType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryModel;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.ReachType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.SeqMemoryLocation;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.expressions.And;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTreeUtil;
import org.sosy_lab.cpachecker.util.expressions.LeafExpression;
import org.sosy_lab.cpachecker.util.expressions.Or;

class BitVectorReadWriteEvaluationBuilder {

  static BitVectorEvaluationExpression buildVariableOnlyEvaluationByEncoding(
      MPOROptions pOptions,
      MPORThread pActiveThread,
      ImmutableSet<MPORThread> pOtherThreads,
      BitVectorVariables pBitVectorVariables,
      SequentializationUtils pUtils)
      throws UnrecognizedCodeException {

    return switch (pOptions.bitVectorEncoding) {
      case NONE ->
          throw new IllegalArgumentException(
              "cannot build evaluation for encoding " + pOptions.bitVectorEncoding);
      case BINARY, DECIMAL, HEXADECIMAL ->
          buildFullDenseVariableOnlyEvaluation(
              pActiveThread, pOtherThreads, pBitVectorVariables, pUtils);
      case SPARSE -> {
        ImmutableListMultimap<SeqMemoryLocation, AExpression> sparseWriteMap =
            BitVectorEvaluationUtil.mapMemoryLocationsToSparseBitVectorsByAccessType(
                pOtherThreads, pBitVectorVariables, MemoryAccessType.WRITE);
        ImmutableListMultimap<SeqMemoryLocation, AExpression> sparseAccessMap =
            BitVectorEvaluationUtil.mapMemoryLocationsToSparseBitVectorsByAccessType(
                pOtherThreads, pBitVectorVariables, MemoryAccessType.ACCESS);
        yield buildFullSparseVariableOnlyEvaluation(
            pActiveThread, sparseWriteMap, sparseAccessMap, pBitVectorVariables, pUtils);
      }
    };
  }

  static BitVectorEvaluationExpression buildDenseEvaluation(
      MPOROptions pOptions,
      ImmutableSet<CExpression> pOtherWriteBitVectors,
      ImmutableSet<CExpression> pOtherAccessBitVectors,
      ImmutableSet<SeqMemoryLocation> pDirectReadMemoryLocations,
      ImmutableSet<SeqMemoryLocation> pDirectWriteMemoryLocations,
      MemoryModel pMemoryModel,
      SequentializationUtils pUtils)
      throws UnrecognizedCodeException {

    if (pOptions.pruneBitVectorEvaluations) {
      return buildPrunedDenseEvaluation(
          pOtherWriteBitVectors,
          pOtherAccessBitVectors,
          pDirectReadMemoryLocations,
          pDirectWriteMemoryLocations,
          pMemoryModel,
          pUtils);
    } else {
      return buildFullDenseEvaluation(
          pOtherWriteBitVectors,
          pOtherAccessBitVectors,
          pDirectReadMemoryLocations,
          pDirectWriteMemoryLocations,
          pMemoryModel,
          pUtils);
    }
  }

  static BitVectorEvaluationExpression buildSparseEvaluation(
      MPOROptions pOptions,
      ImmutableListMultimap<SeqMemoryLocation, AExpression> pSparseWriteMap,
      ImmutableListMultimap<SeqMemoryLocation, AExpression> pSparseAccessMap,
      ImmutableSet<SeqMemoryLocation> pDirectReadMemoryLocations,
      ImmutableSet<SeqMemoryLocation> pDirectWriteMemoryLocations,
      BitVectorVariables pBitVectorVariables,
      SequentializationUtils pUtils) {

    if (pOptions.pruneBitVectorEvaluations) {
      return buildPrunedSparseEvaluation(
          pSparseWriteMap,
          pSparseAccessMap,
          pDirectReadMemoryLocations,
          pDirectWriteMemoryLocations,
          pBitVectorVariables,
          pUtils);
    } else {
      return buildFullSparseEvaluation(
          pSparseWriteMap,
          pSparseAccessMap,
          pDirectReadMemoryLocations,
          pDirectWriteMemoryLocations,
          pBitVectorVariables,
          pUtils);
    }
  }

  // Pruned Dense Evaluation =======================================================================

  private static BitVectorEvaluationExpression buildPrunedDenseEvaluation(
      ImmutableSet<CExpression> pOtherWriteBitVectors,
      ImmutableSet<CExpression> pOtherAccessBitVectors,
      ImmutableSet<SeqMemoryLocation> pDirectReadMemoryLocations,
      ImmutableSet<SeqMemoryLocation> pDirectWriteMemoryLocations,
      MemoryModel pMemoryModel,
      SequentializationUtils pUtils)
      throws UnrecognizedCodeException {

    Optional<CBinaryExpression> leftHandSide =
        buildPrunedDenseLeftHandSide(
            pOtherWriteBitVectors,
            pDirectReadMemoryLocations,
            pMemoryModel,
            pUtils.getBinaryExpressionBuilder());
    Optional<CBinaryExpression> rightHandSide =
        buildPrunedDenseRightHandSide(
            pOtherAccessBitVectors,
            pDirectWriteMemoryLocations,
            pMemoryModel,
            pUtils.getBinaryExpressionBuilder());

    if (leftHandSide.isPresent() && rightHandSide.isPresent()) {
      // both LHS and RHS present: create or expression: ||
      ExpressionTree<AExpression> logicalOr =
          Or.of(
              ExpressionTreeUtil.toExpressionTree(
                  leftHandSide.orElseThrow(), rightHandSide.orElseThrow()));
      return new BitVectorEvaluationExpression(logicalOr, pUtils);
    } else if (leftHandSide.isPresent()) {
      return new BitVectorEvaluationExpression(leftHandSide.orElseThrow(), pUtils);
    } else if (rightHandSide.isPresent()) {
      return new BitVectorEvaluationExpression(rightHandSide.orElseThrow(), pUtils);
    }
    return BitVectorEvaluationExpression.empty();
  }

  private static Optional<CBinaryExpression> buildPrunedDenseLeftHandSide(
      ImmutableSet<CExpression> pOtherWriteBitVectors,
      ImmutableSet<SeqMemoryLocation> pDirectReadMemoryLocations,
      MemoryModel pMemoryModel,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    if (pDirectReadMemoryLocations.isEmpty()) {
      return Optional.empty();
    } else {
      CIntegerLiteralExpression directReadBitVector =
          BitVectorUtil.buildDirectBitVectorExpression(pMemoryModel, pDirectReadMemoryLocations);
      CBinaryExpression leftHandSide =
          buildGeneralDenseLeftHandSide(
              directReadBitVector, pOtherWriteBitVectors, pBinaryExpressionBuilder);
      return Optional.of(leftHandSide);
    }
  }

  private static Optional<CBinaryExpression> buildPrunedDenseRightHandSide(
      ImmutableSet<CExpression> pOtherAccessBitVectors,
      ImmutableSet<SeqMemoryLocation> pDirectWriteMemoryLocations,
      MemoryModel pMemoryModel,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    if (pDirectWriteMemoryLocations.isEmpty()) {
      return Optional.empty();
    } else {
      CIntegerLiteralExpression directWriteBitVector =
          BitVectorUtil.buildDirectBitVectorExpression(pMemoryModel, pDirectWriteMemoryLocations);
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
      ImmutableSet<SeqMemoryLocation> pDirectReadMemoryLocations,
      ImmutableSet<SeqMemoryLocation> pDirectWriteMemoryLocations,
      MemoryModel pMemoryModel,
      SequentializationUtils pUtils)
      throws UnrecognizedCodeException {

    CIntegerLiteralExpression directReadBitVector =
        BitVectorUtil.buildDirectBitVectorExpression(pMemoryModel, pDirectReadMemoryLocations);
    CIntegerLiteralExpression directWriteBitVector =
        BitVectorUtil.buildDirectBitVectorExpression(pMemoryModel, pDirectWriteMemoryLocations);
    return buildFullDenseLogicalOr(
        directReadBitVector,
        directWriteBitVector,
        pOtherWriteBitVectors,
        pOtherAccessBitVectors,
        pUtils);
  }

  private static BitVectorEvaluationExpression buildFullDenseVariableOnlyEvaluation(
      MPORThread pActiveThread,
      ImmutableSet<MPORThread> pOtherThreads,
      BitVectorVariables pBitVectorVariables,
      SequentializationUtils pUtils)
      throws UnrecognizedCodeException {

    CExpression directReadBitVector =
        pBitVectorVariables.getDenseBitVector(
            pActiveThread, MemoryAccessType.READ, ReachType.DIRECT);
    CExpression directWriteBitVector =
        pBitVectorVariables.getDenseBitVector(
            pActiveThread, MemoryAccessType.WRITE, ReachType.DIRECT);
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
        pUtils);
  }

  private static BitVectorEvaluationExpression buildFullDenseLogicalOr(
      CExpression pDirectReadBitVector,
      CExpression pDirectWriteBitVector,
      ImmutableSet<CExpression> pOtherWriteBitVectors,
      ImmutableSet<CExpression> pOtherAccessBitVectors,
      SequentializationUtils pUtils)
      throws UnrecognizedCodeException {

    // (R & (W' | W'' | ...))
    CExpression leftHandSide =
        buildGeneralDenseLeftHandSide(
            pDirectReadBitVector, pOtherWriteBitVectors, pUtils.getBinaryExpressionBuilder());
    // (W & (A' | A'' | ...))
    CExpression rightHandSide =
        buildGeneralDenseRightHandSide(
            pDirectWriteBitVector, pOtherAccessBitVectors, pUtils.getBinaryExpressionBuilder());
    // (R & (W' | W'' | ...)) || (W & (A' | A'' | ...))
    ExpressionTree<AExpression> logicalOr =
        Or.of(ExpressionTreeUtil.toExpressionTree(leftHandSide, rightHandSide));
    return new BitVectorEvaluationExpression(logicalOr, pUtils);
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
      ImmutableListMultimap<SeqMemoryLocation, AExpression> pSparseWriteMap,
      ImmutableListMultimap<SeqMemoryLocation, AExpression> pSparseAccessMap,
      ImmutableSet<SeqMemoryLocation> pDirectReadMemoryLocations,
      ImmutableSet<SeqMemoryLocation> pDirectWriteMemoryLocations,
      BitVectorVariables pBitVectorVariables,
      SequentializationUtils pUtils) {

    if (pBitVectorVariables.areSparseBitVectorsEmpty()) {
      // no bit vectors (e.g. no global variables) -> no evaluation
      return BitVectorEvaluationExpression.empty();
    }
    ImmutableList.Builder<ExpressionTree<AExpression>> sparseExpressions = ImmutableList.builder();
    for (var entry : pBitVectorVariables.getSparseAccessBitVectors().entrySet()) {
      SeqMemoryLocation memoryLocation = entry.getKey();

      // handle write variables
      ImmutableList<AExpression> otherWriteVariables = pSparseWriteMap.get(memoryLocation);
      // handle access variables
      ImmutableList<AExpression> otherAccessVariables = pSparseAccessMap.get(memoryLocation);

      Optional<ExpressionTree<AExpression>> leftHandSide =
          buildPrunedSparseLeftHandSide(
              pDirectReadMemoryLocations, memoryLocation, otherWriteVariables);
      Optional<ExpressionTree<AExpression>> rightHandSide =
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
    return BitVectorEvaluationUtil.buildSparseLogicalDisjunction(sparseExpressions.build(), pUtils);
  }

  /** Builds the logical LHS i.e. {@code (R && (W' || W'' || ...))}. */
  private static Optional<ExpressionTree<AExpression>> buildPrunedSparseLeftHandSide(
      ImmutableSet<SeqMemoryLocation> pDirectReadMemoryLocations,
      SeqMemoryLocation pMemoryLocation,
      ImmutableList<AExpression> pOtherWriteVariables) {

    if (!pDirectReadMemoryLocations.contains(pMemoryLocation)) {
      // if the LHS is 0, then the entire && expression is 0 -> prune
      return Optional.empty();
    }
    // otherwise the LHS is 1, and we only need the right side of the && expression
    return BitVectorEvaluationUtil.tryLogicalDisjunction(
        ExpressionTreeUtil.toExpressionTree(pOtherWriteVariables));
  }

  /** Builds the logical RHS i.e. {@code (W && (A' || A'' || ...))}. */
  private static Optional<ExpressionTree<AExpression>> buildPrunedSparseRightHandSide(
      ImmutableSet<SeqMemoryLocation> pDirectWriteMemoryLocations,
      SeqMemoryLocation pMemoryLocation,
      ImmutableList<AExpression> pOtherAccessVariables) {

    if (!pDirectWriteMemoryLocations.contains(pMemoryLocation)) {
      // if the LHS (activeWriteVariable) is 0, then the entire && expression is 0 -> prune
      return Optional.empty();
    }
    // otherwise the LHS is 1, and we only need the right side of the && expression
    return BitVectorEvaluationUtil.tryLogicalDisjunction(
        ExpressionTreeUtil.toExpressionTree(pOtherAccessVariables));
  }

  // Pruned Sparse Single Variable Evaluation ======================================================

  private static ExpressionTree<AExpression> buildPrunedSparseSingleVariableEvaluation(
      Optional<ExpressionTree<AExpression>> pLeftHandSide,
      Optional<ExpressionTree<AExpression>> pRightHandSide) {

    if (pLeftHandSide.isPresent() && pRightHandSide.isEmpty()) {
      return pLeftHandSide.orElseThrow(); // only LHS
    }
    if (pLeftHandSide.isEmpty() && pRightHandSide.isPresent()) {
      return pRightHandSide.orElseThrow(); // only RHS
    }
    return Or.of(pLeftHandSide.orElseThrow(), pLeftHandSide.orElseThrow());
  }

  // Full Sparse Evaluation ========================================================================

  private static BitVectorEvaluationExpression buildFullSparseEvaluation(
      ImmutableListMultimap<SeqMemoryLocation, AExpression> pSparseWriteMap,
      ImmutableListMultimap<SeqMemoryLocation, AExpression> pSparseAccessMap,
      ImmutableSet<SeqMemoryLocation> pDirectReadMemoryLocations,
      ImmutableSet<SeqMemoryLocation> pDirectWriteMemoryLocations,
      BitVectorVariables pBitVectorVariables,
      SequentializationUtils pUtils) {

    if (pBitVectorVariables.areSparseBitVectorsEmpty()) {
      return BitVectorEvaluationExpression.empty();
    }
    ImmutableList.Builder<ExpressionTree<AExpression>> sparseExpressions = ImmutableList.builder();
    ImmutableSet<SeqMemoryLocation> memoryLocations =
        pBitVectorVariables.getSparseAccessBitVectors().keySet();
    for (SeqMemoryLocation memoryLocation : memoryLocations) {
      ImmutableList<AExpression> otherWriteVariables = pSparseWriteMap.get(memoryLocation);
      ImmutableList<AExpression> otherAccessVariables = pSparseAccessMap.get(memoryLocation);
      sparseExpressions.add(
          buildFullSparseSingleVariableEvaluation(
              memoryLocation,
              pDirectReadMemoryLocations,
              pDirectWriteMemoryLocations,
              otherWriteVariables,
              otherAccessVariables));
    }
    return BitVectorEvaluationUtil.buildSparseLogicalDisjunction(sparseExpressions.build(), pUtils);
  }

  private static BitVectorEvaluationExpression buildFullSparseVariableOnlyEvaluation(
      MPORThread pActiveThread,
      ImmutableListMultimap<SeqMemoryLocation, AExpression> pSparseWriteMap,
      ImmutableListMultimap<SeqMemoryLocation, AExpression> pSparseAccessMap,
      BitVectorVariables pBitVectorVariables,
      SequentializationUtils pUtils) {

    ImmutableList.Builder<ExpressionTree<AExpression>> sparseExpressions = ImmutableList.builder();
    ImmutableSet<SeqMemoryLocation> memoryLocations =
        pBitVectorVariables.getSparseAccessBitVectors().keySet();
    for (SeqMemoryLocation memoryLocation : memoryLocations) {
      ImmutableList<AExpression> otherWriteVariables = pSparseWriteMap.get(memoryLocation);
      ImmutableList<AExpression> otherAccessVariables = pSparseAccessMap.get(memoryLocation);
      sparseExpressions.add(
          buildSingleVariableFullSparseVariableOnlyEvaluation(
              pActiveThread,
              memoryLocation,
              otherWriteVariables,
              otherAccessVariables,
              pBitVectorVariables));
    }
    return BitVectorEvaluationUtil.buildSparseLogicalDisjunction(sparseExpressions.build(), pUtils);
  }

  // Full Sparse Single Variable Evaluation ========================================================

  private static ExpressionTree<AExpression> buildFullSparseSingleVariableLeftHandSide(
      SeqMemoryLocation pMemoryLocation,
      ImmutableSet<SeqMemoryLocation> pDirectReadMemoryLocations,
      ImmutableList<AExpression> pOtherWriteVariables) {

    AExpression activeReadValue =
        BitVectorEvaluationUtil.buildSparseDirectBitVector(
            pMemoryLocation, pDirectReadMemoryLocations);
    return buildFullSparseSingleVariableLeftHandSide(activeReadValue, pOtherWriteVariables);
  }

  private static ExpressionTree<AExpression> buildFullSparseSingleVariableLeftHandSide(
      AExpression pActiveReadValue, ImmutableList<AExpression> pOtherWriteVariables) {

    return And.of(
        LeafExpression.of(pActiveReadValue),
        BitVectorEvaluationUtil.logicalDisjunction(
            ExpressionTreeUtil.toExpressionTree(pOtherWriteVariables)));
  }

  private static ExpressionTree<AExpression> buildFullSparseSingleVariableRightHandSide(
      SeqMemoryLocation pMemoryLocation,
      ImmutableSet<SeqMemoryLocation> pDirectWriteMemoryLocations,
      ImmutableList<AExpression> pOtherAccessVariables) {

    AExpression activeWriteValue =
        BitVectorEvaluationUtil.buildSparseDirectBitVector(
            pMemoryLocation, pDirectWriteMemoryLocations);
    return buildFullSparseSingleVariableRightHandSide(activeWriteValue, pOtherAccessVariables);
  }

  private static ExpressionTree<AExpression> buildFullSparseSingleVariableRightHandSide(
      AExpression pActiveWriteValue, ImmutableList<AExpression> pOtherAccessVariables) {

    return And.of(
        LeafExpression.of(pActiveWriteValue),
        BitVectorEvaluationUtil.logicalDisjunction(
            ExpressionTreeUtil.toExpressionTree(pOtherAccessVariables)));
  }

  private static ExpressionTree<AExpression> buildFullSparseSingleVariableEvaluation(
      SeqMemoryLocation pMemoryLocation,
      ImmutableSet<SeqMemoryLocation> pDirectReadMemoryLocations,
      ImmutableSet<SeqMemoryLocation> pDirectWriteMemoryLocations,
      ImmutableList<AExpression> pOtherWriteVariables,
      ImmutableList<AExpression> pOtherAccessVariables) {

    ExpressionTree<AExpression> leftHandSide =
        buildFullSparseSingleVariableLeftHandSide(
            pMemoryLocation, pDirectReadMemoryLocations, pOtherWriteVariables);
    ExpressionTree<AExpression> rightHandSide =
        buildFullSparseSingleVariableRightHandSide(
            pMemoryLocation, pDirectWriteMemoryLocations, pOtherAccessVariables);
    return Or.of(leftHandSide, rightHandSide);
  }

  private static ExpressionTree<AExpression> buildSingleVariableFullSparseVariableOnlyEvaluation(
      MPORThread pActiveThread,
      SeqMemoryLocation pMemoryLocation,
      ImmutableList<AExpression> pOtherWriteVariables,
      ImmutableList<AExpression> pOtherAccessVariables,
      BitVectorVariables pBitVectorVariables) {

    SparseBitVector sparseReadBitVector =
        Objects.requireNonNull(pBitVectorVariables.getSparseReadBitVectors().get(pMemoryLocation));
    AExpression activeReadVariable =
        sparseReadBitVector.getVariablesByReachType(ReachType.DIRECT).get(pActiveThread);
    ExpressionTree<AExpression> leftHandSide =
        buildFullSparseSingleVariableLeftHandSide(activeReadVariable, pOtherWriteVariables);

    SparseBitVector sparseWriteBitVector =
        Objects.requireNonNull(pBitVectorVariables.getSparseWriteBitVectors().get(pMemoryLocation));
    AExpression activeWriteVariable =
        sparseWriteBitVector.getVariablesByReachType(ReachType.DIRECT).get(pActiveThread);
    ExpressionTree<AExpression> rightHandSide =
        buildFullSparseSingleVariableRightHandSide(activeWriteVariable, pOtherAccessVariables);

    return Or.of(leftHandSide, rightHandSide);
  }
}
