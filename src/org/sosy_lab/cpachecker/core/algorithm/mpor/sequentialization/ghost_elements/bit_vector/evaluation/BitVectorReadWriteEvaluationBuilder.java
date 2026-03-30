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
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Objects;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SequentializationUtils;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.SeqBitVectorEncoding;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.SeqBitVectorUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.SeqBitVectorVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.SeqBitVectorVariables.SparseBitVector;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryAccessType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryModel;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.ReachType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.SeqMemoryLocation;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.cwriter.export.CExportExpression;
import org.sosy_lab.cpachecker.util.cwriter.export.CExpressionWrapper;
import org.sosy_lab.cpachecker.util.cwriter.export.CLogicalAndExpression;
import org.sosy_lab.cpachecker.util.cwriter.export.CLogicalOrExpression;

class BitVectorReadWriteEvaluationBuilder {

  static Optional<CExportExpression> buildVariableOnlyEvaluationByEncoding(
      MPOROptions pOptions,
      MPORThread pActiveThread,
      ImmutableSet<MPORThread> pOtherThreads,
      SeqBitVectorVariables pBitVectorVariables,
      SequentializationUtils pUtils)
      throws UnrecognizedCodeException {

    return switch (pOptions.bitVectorEncoding()) {
      case NONE ->
          throw new IllegalArgumentException(
              "cannot build evaluation for encoding " + pOptions.bitVectorEncoding());
      case BINARY, OCTAL, DECIMAL, HEXADECIMAL ->
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

  static Optional<CExportExpression> buildDenseEvaluation(
      MPOROptions pOptions,
      ImmutableSet<CExpression> pOtherWriteBitVectors,
      ImmutableSet<CExpression> pOtherAccessBitVectors,
      ImmutableSet<SeqMemoryLocation> pDirectReadMemoryLocations,
      ImmutableSet<SeqMemoryLocation> pDirectWriteMemoryLocations,
      MachineModel pMachineModel,
      MemoryModel pMemoryModel,
      SequentializationUtils pUtils)
      throws UnrecognizedCodeException {

    if (pOptions.pruneBitVectorEvaluations()) {
      return buildPrunedDenseEvaluation(
          pOptions.bitVectorEncoding(),
          pOtherWriteBitVectors,
          pOtherAccessBitVectors,
          pDirectReadMemoryLocations,
          pDirectWriteMemoryLocations,
          pMachineModel,
          pMemoryModel,
          pUtils);
    } else {
      return buildFullDenseEvaluation(
          pOptions.bitVectorEncoding(),
          pOtherWriteBitVectors,
          pOtherAccessBitVectors,
          pDirectReadMemoryLocations,
          pDirectWriteMemoryLocations,
          pMachineModel,
          pMemoryModel,
          pUtils);
    }
  }

  static Optional<CExportExpression> buildSparseEvaluation(
      MPOROptions pOptions,
      ImmutableListMultimap<SeqMemoryLocation, CExpression> pSparseWriteMap,
      ImmutableListMultimap<SeqMemoryLocation, CExpression> pSparseAccessMap,
      ImmutableSet<SeqMemoryLocation> pReadMemoryLocations,
      ImmutableSet<SeqMemoryLocation> pWriteMemoryLocations,
      SeqBitVectorVariables pBitVectorVariables) {

    ImmutableMap<SeqMemoryLocation, CExpression> readLeftHandSides =
        BitVectorEvaluationUtil.buildSparseLeftHandSidesByAccessType(
            pReadMemoryLocations, MemoryAccessType.READ, pBitVectorVariables);
    ImmutableMap<SeqMemoryLocation, CExpression> writeLeftHandSides =
        BitVectorEvaluationUtil.buildSparseLeftHandSidesByAccessType(
            pWriteMemoryLocations, MemoryAccessType.WRITE, pBitVectorVariables);
    return buildSparseEvaluation(
        pOptions,
        readLeftHandSides,
        writeLeftHandSides,
        pSparseWriteMap,
        pSparseAccessMap,
        pReadMemoryLocations,
        pWriteMemoryLocations,
        pBitVectorVariables);
  }

  static Optional<CExportExpression> buildPrevSparseEvaluation(
      MPOROptions pOptions,
      MPORThread pCurrentThread,
      ImmutableListMultimap<SeqMemoryLocation, CExpression> pSparseWriteMap,
      ImmutableListMultimap<SeqMemoryLocation, CExpression> pSparseAccessMap,
      ImmutableSet<SeqMemoryLocation> pReadMemoryLocations,
      ImmutableSet<SeqMemoryLocation> pWriteMemoryLocations,
      SeqBitVectorVariables pBitVectorVariables) {

    ImmutableMap<SeqMemoryLocation, CExpression> readLeftHandSides =
        BitVectorEvaluationUtil.buildPrevSparseLeftHandSidesByAccessType(
            pCurrentThread, MemoryAccessType.READ, pBitVectorVariables);
    ImmutableMap<SeqMemoryLocation, CExpression> writeLeftHandSides =
        BitVectorEvaluationUtil.buildPrevSparseLeftHandSidesByAccessType(
            pCurrentThread, MemoryAccessType.WRITE, pBitVectorVariables);
    return buildSparseEvaluation(
        pOptions,
        readLeftHandSides,
        writeLeftHandSides,
        pSparseWriteMap,
        pSparseAccessMap,
        pReadMemoryLocations,
        pWriteMemoryLocations,
        pBitVectorVariables);
  }

  private static Optional<CExportExpression> buildSparseEvaluation(
      MPOROptions pOptions,
      ImmutableMap<SeqMemoryLocation, CExpression> pReadLeftHandSides,
      ImmutableMap<SeqMemoryLocation, CExpression> pWriteLeftHandSides,
      ImmutableListMultimap<SeqMemoryLocation, CExpression> pSparseWriteMap,
      ImmutableListMultimap<SeqMemoryLocation, CExpression> pSparseAccessMap,
      ImmutableSet<SeqMemoryLocation> pDirectReadMemoryLocations,
      ImmutableSet<SeqMemoryLocation> pDirectWriteMemoryLocations,
      SeqBitVectorVariables pBitVectorVariables) {

    // R && (W' || W'' || ...)
    Optional<CExportExpression> writeEvaluation =
        pOptions.pruneBitVectorEvaluations()
            ? BitVectorEvaluationUtil.buildPrunedSparseEvaluation(
                pReadLeftHandSides,
                pSparseWriteMap,
                pDirectReadMemoryLocations,
                MemoryAccessType.WRITE,
                pBitVectorVariables)
            : BitVectorEvaluationUtil.buildFullSparseEvaluation(
                pReadLeftHandSides, pSparseWriteMap, MemoryAccessType.WRITE, pBitVectorVariables);

    // W && (A' || A'' || ...)
    Optional<CExportExpression> accessEvaluation =
        pOptions.pruneBitVectorEvaluations()
            ? BitVectorEvaluationUtil.buildPrunedSparseEvaluation(
                pWriteLeftHandSides,
                pSparseAccessMap,
                pDirectWriteMemoryLocations,
                MemoryAccessType.ACCESS,
                pBitVectorVariables)
            : BitVectorEvaluationUtil.buildFullSparseEvaluation(
                pWriteLeftHandSides,
                pSparseAccessMap,
                MemoryAccessType.ACCESS,
                pBitVectorVariables);

    if (accessEvaluation.isPresent() && writeEvaluation.isPresent()) {
      // both LHS and RHS
      return Optional.of(
          CLogicalOrExpression.of(accessEvaluation.orElseThrow(), writeEvaluation.orElseThrow()));
    }
    if (accessEvaluation.isPresent()) {
      return accessEvaluation; // only LHS
    }
    // only RHS, if present, otherwise this is empty anyway
    return writeEvaluation;
  }

  // Pruned Dense Evaluation =======================================================================

  private static Optional<CExportExpression> buildPrunedDenseEvaluation(
      SeqBitVectorEncoding pEncoding,
      ImmutableSet<CExpression> pOtherWriteBitVectors,
      ImmutableSet<CExpression> pOtherAccessBitVectors,
      ImmutableSet<SeqMemoryLocation> pDirectReadMemoryLocations,
      ImmutableSet<SeqMemoryLocation> pDirectWriteMemoryLocations,
      MachineModel pMachineModel,
      MemoryModel pMemoryModel,
      SequentializationUtils pUtils)
      throws UnrecognizedCodeException {

    Optional<CExpression> leftHandSide =
        buildPrunedDenseLeftHandSide(
            pEncoding,
            pOtherWriteBitVectors,
            pDirectReadMemoryLocations,
            pMachineModel,
            pMemoryModel,
            pUtils.binaryExpressionBuilder());
    Optional<CExpression> rightHandSide =
        buildPrunedDenseRightHandSide(
            pEncoding,
            pOtherAccessBitVectors,
            pDirectWriteMemoryLocations,
            pMachineModel,
            pMemoryModel,
            pUtils.binaryExpressionBuilder());

    if (leftHandSide.isPresent() && rightHandSide.isPresent()) {
      // both LHS and RHS present: create or expression: ||
      return Optional.of(
          CLogicalOrExpression.of(leftHandSide.orElseThrow(), rightHandSide.orElseThrow()));

    } else if (leftHandSide.isPresent()) {
      return Optional.of(new CExpressionWrapper(leftHandSide.orElseThrow()));

    } else if (rightHandSide.isPresent()) {
      return Optional.of(new CExpressionWrapper(rightHandSide.orElseThrow()));
    }
    return Optional.empty();
  }

  private static Optional<CExpression> buildPrunedDenseLeftHandSide(
      SeqBitVectorEncoding pEncoding,
      ImmutableSet<CExpression> pOtherWriteBitVectors,
      ImmutableSet<SeqMemoryLocation> pDirectReadMemoryLocations,
      MachineModel pMachineModel,
      MemoryModel pMemoryModel,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    if (pDirectReadMemoryLocations.isEmpty()) {
      return Optional.empty();
    } else {
      CIntegerLiteralExpression directReadBitVector =
          SeqBitVectorUtil.buildBitVectorExpression(
              pEncoding, pMachineModel, pMemoryModel, pDirectReadMemoryLocations);
      CBinaryExpression leftHandSide =
          buildGeneralDenseLeftHandSide(
              directReadBitVector, pOtherWriteBitVectors, pBinaryExpressionBuilder);
      return Optional.of(leftHandSide);
    }
  }

  private static Optional<CExpression> buildPrunedDenseRightHandSide(
      SeqBitVectorEncoding pEncoding,
      ImmutableSet<CExpression> pOtherAccessBitVectors,
      ImmutableSet<SeqMemoryLocation> pDirectWriteMemoryLocations,
      MachineModel pMachineModel,
      MemoryModel pMemoryModel,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    if (pDirectWriteMemoryLocations.isEmpty()) {
      return Optional.empty();
    } else {
      CIntegerLiteralExpression directWriteBitVector =
          SeqBitVectorUtil.buildBitVectorExpression(
              pEncoding, pMachineModel, pMemoryModel, pDirectWriteMemoryLocations);
      CBinaryExpression rRightHandSide =
          buildGeneralDenseRightHandSide(
              directWriteBitVector, pOtherAccessBitVectors, pBinaryExpressionBuilder);
      return Optional.of(rRightHandSide);
    }
  }

  // Full Dense Evaluation =========================================================================

  private static Optional<CExportExpression> buildFullDenseEvaluation(
      SeqBitVectorEncoding pEncoding,
      ImmutableSet<CExpression> pOtherWriteBitVectors,
      ImmutableSet<CExpression> pOtherAccessBitVectors,
      ImmutableSet<SeqMemoryLocation> pDirectReadMemoryLocations,
      ImmutableSet<SeqMemoryLocation> pDirectWriteMemoryLocations,
      MachineModel pMachineModel,
      MemoryModel pMemoryModel,
      SequentializationUtils pUtils)
      throws UnrecognizedCodeException {

    CIntegerLiteralExpression directReadBitVector =
        SeqBitVectorUtil.buildBitVectorExpression(
            pEncoding, pMachineModel, pMemoryModel, pDirectReadMemoryLocations);
    CIntegerLiteralExpression directWriteBitVector =
        SeqBitVectorUtil.buildBitVectorExpression(
            pEncoding, pMachineModel, pMemoryModel, pDirectWriteMemoryLocations);
    return Optional.of(
        buildFullDenseLogicalOr(
            directReadBitVector,
            directWriteBitVector,
            pOtherWriteBitVectors,
            pOtherAccessBitVectors,
            pUtils));
  }

  private static CLogicalOrExpression buildFullDenseVariableOnlyEvaluation(
      MPORThread pActiveThread,
      ImmutableSet<MPORThread> pOtherThreads,
      SeqBitVectorVariables pBitVectorVariables,
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

  static CLogicalOrExpression buildFullDenseLogicalOr(
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
    return CLogicalOrExpression.of(leftHandSide, rightHandSide);
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

  // Full Sparse Evaluation ========================================================================

  private static Optional<CExportExpression> buildFullSparseVariableOnlyEvaluation(
      MPORThread pActiveThread,
      ImmutableListMultimap<SeqMemoryLocation, CExpression> pSparseWriteMap,
      ImmutableListMultimap<SeqMemoryLocation, CExpression> pSparseAccessMap,
      SeqBitVectorVariables pBitVectorVariables) {

    ImmutableList.Builder<CExportExpression> sparseExpressions = ImmutableList.builder();
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
    return BitVectorEvaluationUtil.tryBuildLogicalOrExpression(sparseExpressions.build());
  }

  // Full Sparse Single Variable Evaluation ========================================================

  private static CLogicalOrExpression buildSingleVariableFullSparseVariableOnlyEvaluation(
      MPORThread pActiveThread,
      SeqMemoryLocation pMemoryLocation,
      ImmutableList<CExpression> pOtherWriteVariables,
      ImmutableList<CExpression> pOtherAccessVariables,
      SeqBitVectorVariables pBitVectorVariables) {

    SparseBitVector sparseReadBitVector =
        Objects.requireNonNull(pBitVectorVariables.getSparseReadBitVectors().get(pMemoryLocation));
    CExpression activeReadVariable =
        sparseReadBitVector.getVariablesByReachType(ReachType.DIRECT).get(pActiveThread);
    CExportExpression leftHandSide =
        buildFullSparseSingleVariableExpression(
            Objects.requireNonNull(activeReadVariable), pOtherWriteVariables);

    SparseBitVector sparseWriteBitVector =
        Objects.requireNonNull(pBitVectorVariables.getSparseWriteBitVectors().get(pMemoryLocation));
    CExpression activeWriteVariable =
        sparseWriteBitVector.getVariablesByReachType(ReachType.DIRECT).get(pActiveThread);
    CExportExpression rightHandSide =
        buildFullSparseSingleVariableExpression(
            Objects.requireNonNull(activeWriteVariable), pOtherAccessVariables);

    return CLogicalOrExpression.of(leftHandSide, rightHandSide);
  }

  private static CExportExpression buildFullSparseSingleVariableExpression(
      CExpression pActiveReadValue, ImmutableList<CExpression> pOtherWriteVariables) {

    CExpressionWrapper leftHandSide = new CExpressionWrapper(pActiveReadValue);
    Optional<CExportExpression> rightHandSide =
        BitVectorEvaluationUtil.tryBuildLogicalOrExpressionFromCExpressions(pOtherWriteVariables);
    return rightHandSide.isEmpty()
        ? leftHandSide
        : CLogicalAndExpression.of(leftHandSide, rightHandSide.orElseThrow());
  }
}
