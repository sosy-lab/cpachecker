// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.evaluation;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.memory_model.MemoryAccessType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.memory_model.MemoryModel;
import org.sosy_lab.cpachecker.core.algorithm.mpor.memory_model.ReachType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.memory_model.SeqMemoryLocation;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SequentializationUtils;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.SeqBitVectorEncoding;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.SeqBitVectorUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.SeqBitVectorVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.cwriter.export.CExportExpression;
import org.sosy_lab.cpachecker.util.cwriter.export.CExpressionWrapper;
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
        ImmutableListMultimap<SeqMemoryLocation, CExpression> sparseWriteBitVectors =
            BitVectorEvaluationUtil.mapMemoryLocationsToSparseBitVectorsByAccessType(
                pOtherThreads, pBitVectorVariables, MemoryAccessType.WRITE);
        ImmutableListMultimap<SeqMemoryLocation, CExpression> sparseAccessBitVectors =
            BitVectorEvaluationUtil.mapMemoryLocationsToSparseBitVectorsByAccessType(
                pOtherThreads, pBitVectorVariables, MemoryAccessType.ACCESS);

        // direct reads and reachable writes: dR && (rW' || rW'' || ...)
        Optional<CExportExpression> readWriteEvaluation =
            BitVectorEvaluationUtil.buildFullSparseVariableOnlyEvaluationByAccessType(
                pActiveThread, MemoryAccessType.READ, sparseWriteBitVectors, pBitVectorVariables);
        // direct writes and reachable accesses: dW && (rA' || rA'' || ...)
        Optional<CExportExpression> writeAccessEvaluation =
            BitVectorEvaluationUtil.buildFullSparseVariableOnlyEvaluationByAccessType(
                pActiveThread, MemoryAccessType.WRITE, sparseAccessBitVectors, pBitVectorVariables);

        yield tryBuildLogicalOr(readWriteEvaluation, writeAccessEvaluation);
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
      ImmutableListMultimap<SeqMemoryLocation, CExpression> pSparseWriteMap,
      ImmutableListMultimap<SeqMemoryLocation, CExpression> pSparseAccessMap,
      ImmutableSet<SeqMemoryLocation> pReadMemoryLocations,
      ImmutableSet<SeqMemoryLocation> pWriteMemoryLocations,
      SeqBitVectorVariables pBitVectorVariables) {

    ImmutableMap<SeqMemoryLocation, CExpression> readLeftHandSides =
        BitVectorEvaluationUtil.buildPrevSparseLeftHandSidesByAccessType(
            MemoryAccessType.READ, pBitVectorVariables);
    ImmutableMap<SeqMemoryLocation, CExpression> writeLeftHandSides =
        BitVectorEvaluationUtil.buildPrevSparseLeftHandSidesByAccessType(
            MemoryAccessType.WRITE, pBitVectorVariables);
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
            ? BitVectorEvaluationUtil.buildPrunedSparseEvaluationByAccessType(
                pReadLeftHandSides,
                pSparseWriteMap,
                pDirectReadMemoryLocations,
                MemoryAccessType.WRITE,
                pBitVectorVariables)
            : BitVectorEvaluationUtil.buildFullSparseEvaluationByAccessType(
                pReadLeftHandSides, pSparseWriteMap, MemoryAccessType.WRITE, pBitVectorVariables);

    // W && (A' || A'' || ...)
    Optional<CExportExpression> accessEvaluation =
        pOptions.pruneBitVectorEvaluations()
            ? BitVectorEvaluationUtil.buildPrunedSparseEvaluationByAccessType(
                pWriteLeftHandSides,
                pSparseAccessMap,
                pDirectWriteMemoryLocations,
                MemoryAccessType.ACCESS,
                pBitVectorVariables)
            : BitVectorEvaluationUtil.buildFullSparseEvaluationByAccessType(
                pWriteLeftHandSides,
                pSparseAccessMap,
                MemoryAccessType.ACCESS,
                pBitVectorVariables);

    return tryBuildLogicalOr(accessEvaluation, writeEvaluation);
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

    Optional<CExportExpression> leftHandSide =
        buildPrunedDenseLeftHandSide(
            pEncoding,
            pOtherWriteBitVectors,
            pDirectReadMemoryLocations,
            pMachineModel,
            pMemoryModel,
            pUtils.binaryExpressionBuilder());
    Optional<CExportExpression> rightHandSide =
        buildPrunedDenseRightHandSide(
            pEncoding,
            pOtherAccessBitVectors,
            pDirectWriteMemoryLocations,
            pMachineModel,
            pMemoryModel,
            pUtils.binaryExpressionBuilder());

    return tryBuildLogicalOr(leftHandSide, rightHandSide);
  }

  private static Optional<CExportExpression> buildPrunedDenseLeftHandSide(
      SeqBitVectorEncoding pEncoding,
      ImmutableSet<CExpression> pOtherWriteBitVectors,
      ImmutableSet<SeqMemoryLocation> pDirectReadMemoryLocations,
      MachineModel pMachineModel,
      MemoryModel pMemoryModel,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    if (pDirectReadMemoryLocations.isEmpty()) {
      return Optional.empty();
    }
    CIntegerLiteralExpression directReadBitVector =
        SeqBitVectorUtil.buildBitVectorExpression(
            pEncoding, pMachineModel, pMemoryModel, pDirectReadMemoryLocations);
    CBinaryExpression leftHandSide =
        buildGeneralDenseLeftHandSide(
            directReadBitVector, pOtherWriteBitVectors, pBinaryExpressionBuilder);
    return Optional.of(new CExpressionWrapper(leftHandSide));
  }

  private static Optional<CExportExpression> buildPrunedDenseRightHandSide(
      SeqBitVectorEncoding pEncoding,
      ImmutableSet<CExpression> pOtherAccessBitVectors,
      ImmutableSet<SeqMemoryLocation> pDirectWriteMemoryLocations,
      MachineModel pMachineModel,
      MemoryModel pMemoryModel,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    if (pDirectWriteMemoryLocations.isEmpty()) {
      return Optional.empty();
    }
    CIntegerLiteralExpression directWriteBitVector =
        SeqBitVectorUtil.buildBitVectorExpression(
            pEncoding, pMachineModel, pMemoryModel, pDirectWriteMemoryLocations);
    CBinaryExpression rRightHandSide =
        buildGeneralDenseRightHandSide(
            directWriteBitVector, pOtherAccessBitVectors, pBinaryExpressionBuilder);
    return Optional.of(new CExpressionWrapper(rRightHandSide));
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

  // Helper

  private static Optional<CExportExpression> tryBuildLogicalOr(
      Optional<CExportExpression> pLeftHandSide, Optional<CExportExpression> pRightHandSide) {

    if (pLeftHandSide.isPresent() && pRightHandSide.isPresent()) {
      // return (LHS || RHS)
      return Optional.of(
          CLogicalOrExpression.of(pLeftHandSide.orElseThrow(), pRightHandSide.orElseThrow()));
    } else if (pLeftHandSide.isPresent()) {
      // return (LHS)
      return pLeftHandSide;
    }
    // return (RHS) if present, or empty if not
    return pRightHandSide;
  }
}
