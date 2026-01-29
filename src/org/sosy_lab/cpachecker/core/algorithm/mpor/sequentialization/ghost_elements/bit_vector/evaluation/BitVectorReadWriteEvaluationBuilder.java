// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.evaluation;

import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableSet;
import java.util.Objects;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SequentializationUtils;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.BitVectorUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.BitVectorVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.BitVectorVariables.SparseBitVector;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryAccessType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryModel;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.ReachType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.SeqMemoryLocation;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.cwriter.export.CExportExpression;
import org.sosy_lab.cpachecker.util.cwriter.export.CExpressionTree;
import org.sosy_lab.cpachecker.util.cwriter.export.CExpressionWrapper;
import org.sosy_lab.cpachecker.util.expressions.And;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.LeafExpression;
import org.sosy_lab.cpachecker.util.expressions.Or;

class BitVectorReadWriteEvaluationBuilder {

  static Optional<CExpressionTree> buildVariableOnlyEvaluationByEncoding(
      MPOROptions pOptions,
      MPORThread pActiveThread,
      ImmutableSet<MPORThread> pOtherThreads,
      BitVectorVariables pBitVectorVariables,
      SequentializationUtils pUtils)
      throws UnrecognizedCodeException {

    return switch (pOptions.bitVectorEncoding()) {
      case NONE ->
          throw new IllegalArgumentException(
              "cannot build evaluation for encoding " + pOptions.bitVectorEncoding());
      case BINARY, DECIMAL, HEXADECIMAL ->
          Optional.of(
              buildFullDenseVariableOnlyEvaluation(
                  pActiveThread, pOtherThreads, pBitVectorVariables, pUtils));
      case SPARSE -> {
        ImmutableListMultimap<SeqMemoryLocation, CExpression> sparseWriteMap =
            BitVectorEvaluationUtil.mapMemoryLocationsToSparseBitVectorsByAccessType(
                pOtherThreads, pBitVectorVariables, MemoryAccessType.WRITE);
        ImmutableListMultimap<SeqMemoryLocation, CExpression> sparseAccessMap =
            BitVectorEvaluationUtil.mapMemoryLocationsToSparseBitVectorsByAccessType(
                pOtherThreads, pBitVectorVariables, MemoryAccessType.ACCESS);
        yield buildFullSparseVariableOnlyEvaluation(
            pActiveThread, sparseWriteMap, sparseAccessMap, pBitVectorVariables);
      }
    };
  }

  static Optional<CExpressionTree> buildDenseEvaluation(
      MPOROptions pOptions,
      ImmutableSet<CExpression> pOtherWriteBitVectors,
      ImmutableSet<CExpression> pOtherAccessBitVectors,
      ImmutableSet<SeqMemoryLocation> pDirectReadMemoryLocations,
      ImmutableSet<SeqMemoryLocation> pDirectWriteMemoryLocations,
      MemoryModel pMemoryModel,
      SequentializationUtils pUtils)
      throws UnrecognizedCodeException {

    if (pOptions.pruneBitVectorEvaluations()) {
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

  static Optional<CExpressionTree> buildSparseEvaluation(
      MPOROptions pOptions,
      ImmutableListMultimap<SeqMemoryLocation, CExpression> pSparseWriteMap,
      ImmutableListMultimap<SeqMemoryLocation, CExpression> pSparseAccessMap,
      ImmutableSet<SeqMemoryLocation> pDirectReadMemoryLocations,
      ImmutableSet<SeqMemoryLocation> pDirectWriteMemoryLocations,
      BitVectorVariables pBitVectorVariables) {

    if (pOptions.pruneBitVectorEvaluations()) {
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

  private static Optional<CExpressionTree> buildPrunedDenseEvaluation(
      ImmutableSet<CExpression> pOtherWriteBitVectors,
      ImmutableSet<CExpression> pOtherAccessBitVectors,
      ImmutableSet<SeqMemoryLocation> pDirectReadMemoryLocations,
      ImmutableSet<SeqMemoryLocation> pDirectWriteMemoryLocations,
      MemoryModel pMemoryModel,
      SequentializationUtils pUtils)
      throws UnrecognizedCodeException {

    Optional<CExpression> leftHandSide =
        buildPrunedDenseLeftHandSide(
            pOtherWriteBitVectors,
            pDirectReadMemoryLocations,
            pMemoryModel,
            pUtils.binaryExpressionBuilder());
    Optional<CExpression> rightHandSide =
        buildPrunedDenseRightHandSide(
            pOtherAccessBitVectors,
            pDirectWriteMemoryLocations,
            pMemoryModel,
            pUtils.binaryExpressionBuilder());

    if (leftHandSide.isPresent() && rightHandSide.isPresent()) {
      // both LHS and RHS present: create or expression: ||
      ImmutableList<CExportExpression> expressionList =
          ImmutableList.of(
              new CExpressionWrapper(leftHandSide.orElseThrow()),
              new CExpressionWrapper(rightHandSide.orElseThrow()));
      ExpressionTree<CExportExpression> logicalOr =
          Or.of(transformedImmutableListCopy(expressionList, LeafExpression::of));
      return Optional.of(new CExpressionTree(logicalOr));
    } else if (leftHandSide.isPresent()) {
      ExpressionTree<CExportExpression> binaryLhs =
          LeafExpression.of(new CExpressionWrapper(leftHandSide.orElseThrow()));
      return Optional.of(new CExpressionTree(binaryLhs));
    } else if (rightHandSide.isPresent()) {
      ExpressionTree<CExportExpression> binaryRhs =
          LeafExpression.of(new CExpressionWrapper(rightHandSide.orElseThrow()));
      return Optional.of(new CExpressionTree(binaryRhs));
    }
    return Optional.empty();
  }

  private static Optional<CExpression> buildPrunedDenseLeftHandSide(
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

  private static Optional<CExpression> buildPrunedDenseRightHandSide(
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

  private static Optional<CExpressionTree> buildFullDenseEvaluation(
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
    return Optional.of(
        buildFullDenseLogicalOr(
            directReadBitVector,
            directWriteBitVector,
            pOtherWriteBitVectors,
            pOtherAccessBitVectors,
            pUtils));
  }

  private static CExpressionTree buildFullDenseVariableOnlyEvaluation(
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

  private static CExpressionTree buildFullDenseLogicalOr(
      CExpression pDirectReadBitVector,
      CExpression pDirectWriteBitVector,
      ImmutableSet<CExpression> pOtherWriteBitVectors,
      ImmutableSet<CExpression> pOtherAccessBitVectors,
      SequentializationUtils pUtils)
      throws UnrecognizedCodeException {

    // (R & (W' | W'' | ...))
    CExpression leftHandSide =
        buildGeneralDenseLeftHandSide(
            pDirectReadBitVector, pOtherWriteBitVectors, pUtils.binaryExpressionBuilder());
    // (W & (A' | A'' | ...))
    CExpression rightHandSide =
        buildGeneralDenseRightHandSide(
            pDirectWriteBitVector, pOtherAccessBitVectors, pUtils.binaryExpressionBuilder());
    // (R & (W' | W'' | ...)) || (W & (A' | A'' | ...))
    ImmutableList<CExportExpression> expressionList =
        ImmutableList.of(
            new CExpressionWrapper(leftHandSide), new CExpressionWrapper(rightHandSide));
    ExpressionTree<CExportExpression> logicalOr =
        Or.of(transformedImmutableListCopy(expressionList, LeafExpression::of));
    return new CExpressionTree(logicalOr);
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
        pDirectReadBitVector, otherWrites, BinaryOperator.BITWISE_AND);
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
        pDirectWriteBitVector, otherReadsAndWrites, BinaryOperator.BITWISE_AND);
  }

  // Pruned Sparse Evaluation ======================================================================

  private static Optional<CExpressionTree> buildPrunedSparseEvaluation(
      ImmutableListMultimap<SeqMemoryLocation, CExpression> pSparseWriteMap,
      ImmutableListMultimap<SeqMemoryLocation, CExpression> pSparseAccessMap,
      ImmutableSet<SeqMemoryLocation> pDirectReadMemoryLocations,
      ImmutableSet<SeqMemoryLocation> pDirectWriteMemoryLocations,
      BitVectorVariables pBitVectorVariables) {

    if (pBitVectorVariables.areSparseBitVectorsEmpty()) {
      // no bit vectors (e.g. no global variables) -> no evaluation
      return Optional.empty();
    }
    ImmutableList.Builder<ExpressionTree<CExportExpression>> sparseExpressions =
        ImmutableList.builder();
    for (var entry : pBitVectorVariables.getSparseAccessBitVectors().entrySet()) {
      SeqMemoryLocation memoryLocation = entry.getKey();

      // handle write variables
      ImmutableList<CExpression> otherWriteVariables = pSparseWriteMap.get(memoryLocation);
      // handle access variables
      ImmutableList<CExpression> otherAccessVariables = pSparseAccessMap.get(memoryLocation);

      Optional<ExpressionTree<CExportExpression>> leftHandSide =
          buildPrunedSparseLeftHandSide(
              pDirectReadMemoryLocations, memoryLocation, otherWriteVariables);
      Optional<ExpressionTree<CExportExpression>> rightHandSide =
          buildPrunedSparseRightHandSide(
              pDirectWriteMemoryLocations, memoryLocation, otherAccessVariables);

      // only add expression if it was not pruned entirely (LHS or RHS present)
      if (leftHandSide.isPresent() || rightHandSide.isPresent()) {
        sparseExpressions.add(
            buildPrunedSparseSingleVariableEvaluation(leftHandSide, rightHandSide));
      }
    }
    return BitVectorEvaluationUtil.tryBuildSparseLogicalDisjunction(sparseExpressions.build());
  }

  /** Builds the logical LHS i.e. {@code (R && (W' || W'' || ...))}. */
  private static Optional<ExpressionTree<CExportExpression>> buildPrunedSparseLeftHandSide(
      ImmutableSet<SeqMemoryLocation> pDirectReadMemoryLocations,
      SeqMemoryLocation pMemoryLocation,
      ImmutableList<CExpression> pOtherWriteVariables) {

    // if the LHS is 0, then the entire && expression is 0 -> prune
    if (!pDirectReadMemoryLocations.contains(pMemoryLocation)) {
      return Optional.empty();
    }
    // otherwise the LHS is 1, and we only need the right side of the && expression
    if (pOtherWriteVariables.isEmpty()) {
      // RHS is empty too -> prune
      return Optional.empty();
    }
    return Optional.of(
        Or.of(
            transformedImmutableListCopy(
                pOtherWriteVariables, e -> LeafExpression.of(new CExpressionWrapper(e)))));
  }

  /** Builds the logical RHS i.e. {@code (W && (A' || A'' || ...))}. */
  private static Optional<ExpressionTree<CExportExpression>> buildPrunedSparseRightHandSide(
      ImmutableSet<SeqMemoryLocation> pDirectWriteMemoryLocations,
      SeqMemoryLocation pMemoryLocation,
      ImmutableList<CExpression> pOtherAccessVariables) {

    if (!pDirectWriteMemoryLocations.contains(pMemoryLocation)) {
      // if the LHS (activeWriteVariable) is 0, then the entire && expression is 0 -> prune
      return Optional.empty();
    }
    // otherwise the LHS is 1, and we only need the right side of the && expression
    if (pOtherAccessVariables.isEmpty()) {
      // RHS is empty too -> prune
      return Optional.empty();
    }
    return Optional.of(
        Or.of(
            transformedImmutableListCopy(
                pOtherAccessVariables, e -> LeafExpression.of(new CExpressionWrapper(e)))));
  }

  // Pruned Sparse Single Variable Evaluation ======================================================

  private static ExpressionTree<CExportExpression> buildPrunedSparseSingleVariableEvaluation(
      Optional<ExpressionTree<CExportExpression>> pLeftHandSide,
      Optional<ExpressionTree<CExportExpression>> pRightHandSide) {

    if (pLeftHandSide.isPresent() && pRightHandSide.isEmpty()) {
      return pLeftHandSide.orElseThrow(); // only LHS
    }
    if (pLeftHandSide.isEmpty() && pRightHandSide.isPresent()) {
      return pRightHandSide.orElseThrow(); // only RHS
    }
    return Or.of(pLeftHandSide.orElseThrow(), pLeftHandSide.orElseThrow());
  }

  // Full Sparse Evaluation ========================================================================

  private static Optional<CExpressionTree> buildFullSparseEvaluation(
      ImmutableListMultimap<SeqMemoryLocation, CExpression> pSparseWriteMap,
      ImmutableListMultimap<SeqMemoryLocation, CExpression> pSparseAccessMap,
      ImmutableSet<SeqMemoryLocation> pDirectReadMemoryLocations,
      ImmutableSet<SeqMemoryLocation> pDirectWriteMemoryLocations,
      BitVectorVariables pBitVectorVariables) {

    if (pBitVectorVariables.areSparseBitVectorsEmpty()) {
      return Optional.empty();
    }
    ImmutableList.Builder<ExpressionTree<CExportExpression>> sparseExpressions =
        ImmutableList.builder();
    ImmutableSet<SeqMemoryLocation> memoryLocations =
        pBitVectorVariables.getSparseAccessBitVectors().keySet();
    for (SeqMemoryLocation memoryLocation : memoryLocations) {
      ImmutableList<CExpression> otherWriteVariables = pSparseWriteMap.get(memoryLocation);
      ImmutableList<CExpression> otherAccessVariables = pSparseAccessMap.get(memoryLocation);
      sparseExpressions.add(
          buildFullSparseSingleVariableEvaluation(
              memoryLocation,
              pDirectReadMemoryLocations,
              pDirectWriteMemoryLocations,
              otherWriteVariables,
              otherAccessVariables));
    }
    return BitVectorEvaluationUtil.tryBuildSparseLogicalDisjunction(sparseExpressions.build());
  }

  private static Optional<CExpressionTree> buildFullSparseVariableOnlyEvaluation(
      MPORThread pActiveThread,
      ImmutableListMultimap<SeqMemoryLocation, CExpression> pSparseWriteMap,
      ImmutableListMultimap<SeqMemoryLocation, CExpression> pSparseAccessMap,
      BitVectorVariables pBitVectorVariables) {

    ImmutableList.Builder<ExpressionTree<CExportExpression>> sparseExpressions =
        ImmutableList.builder();
    ImmutableSet<SeqMemoryLocation> memoryLocations =
        pBitVectorVariables.getSparseAccessBitVectors().keySet();
    for (SeqMemoryLocation memoryLocation : memoryLocations) {
      ImmutableList<CExpression> otherWriteVariables = pSparseWriteMap.get(memoryLocation);
      ImmutableList<CExpression> otherAccessVariables = pSparseAccessMap.get(memoryLocation);
      sparseExpressions.add(
          buildSingleVariableFullSparseVariableOnlyEvaluation(
              pActiveThread,
              memoryLocation,
              otherWriteVariables,
              otherAccessVariables,
              pBitVectorVariables));
    }
    return BitVectorEvaluationUtil.tryBuildSparseLogicalDisjunction(sparseExpressions.build());
  }

  // Full Sparse Single Variable Evaluation ========================================================

  private static ExpressionTree<CExportExpression> buildFullSparseSingleVariableLeftHandSide(
      SeqMemoryLocation pMemoryLocation,
      ImmutableSet<SeqMemoryLocation> pDirectReadMemoryLocations,
      ImmutableList<CExpression> pOtherWriteVariables) {

    CExpression activeReadValue =
        BitVectorEvaluationUtil.buildSparseDirectBitVector(
            pMemoryLocation, pDirectReadMemoryLocations);
    return buildFullSparseSingleVariableLeftHandSide(activeReadValue, pOtherWriteVariables);
  }

  private static ExpressionTree<CExportExpression> buildFullSparseSingleVariableLeftHandSide(
      CExpression pActiveReadValue, ImmutableList<CExpression> pOtherWriteVariables) {

    return And.of(
        LeafExpression.of(new CExpressionWrapper(pActiveReadValue)),
        Or.of(
            transformedImmutableListCopy(
                pOtherWriteVariables, e -> LeafExpression.of(new CExpressionWrapper(e)))));
  }

  private static ExpressionTree<CExportExpression> buildFullSparseSingleVariableRightHandSide(
      SeqMemoryLocation pMemoryLocation,
      ImmutableSet<SeqMemoryLocation> pDirectWriteMemoryLocations,
      ImmutableList<CExpression> pOtherAccessVariables) {

    CExpression activeWriteValue =
        BitVectorEvaluationUtil.buildSparseDirectBitVector(
            pMemoryLocation, pDirectWriteMemoryLocations);
    return buildFullSparseSingleVariableRightHandSide(activeWriteValue, pOtherAccessVariables);
  }

  private static ExpressionTree<CExportExpression> buildFullSparseSingleVariableRightHandSide(
      CExpression pActiveWriteValue, ImmutableList<CExpression> pOtherAccessVariables) {

    return And.of(
        LeafExpression.of(new CExpressionWrapper(pActiveWriteValue)),
        Or.of(
            transformedImmutableListCopy(
                pOtherAccessVariables, e -> LeafExpression.of(new CExpressionWrapper(e)))));
  }

  private static ExpressionTree<CExportExpression> buildFullSparseSingleVariableEvaluation(
      SeqMemoryLocation pMemoryLocation,
      ImmutableSet<SeqMemoryLocation> pDirectReadMemoryLocations,
      ImmutableSet<SeqMemoryLocation> pDirectWriteMemoryLocations,
      ImmutableList<CExpression> pOtherWriteVariables,
      ImmutableList<CExpression> pOtherAccessVariables) {

    ExpressionTree<CExportExpression> leftHandSide =
        buildFullSparseSingleVariableLeftHandSide(
            pMemoryLocation, pDirectReadMemoryLocations, pOtherWriteVariables);
    ExpressionTree<CExportExpression> rightHandSide =
        buildFullSparseSingleVariableRightHandSide(
            pMemoryLocation, pDirectWriteMemoryLocations, pOtherAccessVariables);
    return Or.of(leftHandSide, rightHandSide);
  }

  private static ExpressionTree<CExportExpression>
      buildSingleVariableFullSparseVariableOnlyEvaluation(
          MPORThread pActiveThread,
          SeqMemoryLocation pMemoryLocation,
          ImmutableList<CExpression> pOtherWriteVariables,
          ImmutableList<CExpression> pOtherAccessVariables,
          BitVectorVariables pBitVectorVariables) {

    SparseBitVector sparseReadBitVector =
        Objects.requireNonNull(pBitVectorVariables.getSparseReadBitVectors().get(pMemoryLocation));
    CExpression activeReadVariable =
        sparseReadBitVector.getVariablesByReachType(ReachType.DIRECT).get(pActiveThread);
    ExpressionTree<CExportExpression> leftHandSide =
        buildFullSparseSingleVariableLeftHandSide(
            Objects.requireNonNull(activeReadVariable), pOtherWriteVariables);

    SparseBitVector sparseWriteBitVector =
        Objects.requireNonNull(pBitVectorVariables.getSparseWriteBitVectors().get(pMemoryLocation));
    CExpression activeWriteVariable =
        sparseWriteBitVector.getVariablesByReachType(ReachType.DIRECT).get(pActiveThread);
    ExpressionTree<CExportExpression> rightHandSide =
        buildFullSparseSingleVariableRightHandSide(
            Objects.requireNonNull(activeWriteVariable), pOtherAccessVariables);

    return Or.of(leftHandSide, rightHandSide);
  }
}
