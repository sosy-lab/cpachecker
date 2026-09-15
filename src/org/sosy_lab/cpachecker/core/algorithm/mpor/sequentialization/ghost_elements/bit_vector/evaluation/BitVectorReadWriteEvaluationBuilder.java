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
import org.sosy_lab.cpachecker.core.algorithm.mpor.pointer_aliasing.SeqMemoryAccessType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pointer_aliasing.SeqMemoryLocation;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pointer_aliasing.SeqMemoryReachType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pointer_aliasing.SeqPointerAliasingMap;
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
                pOtherThreads, pBitVectorVariables, SeqMemoryAccessType.WRITE);
        ImmutableListMultimap<SeqMemoryLocation, CExpression> sparseAccessBitVectors =
            BitVectorEvaluationUtil.mapMemoryLocationsToSparseBitVectorsByAccessType(
                pOtherThreads, pBitVectorVariables, SeqMemoryAccessType.ACCESS);

        // direct reads and reachable writes: dR && (rW' || rW'' || ...)
        Optional<CExportExpression> readWriteEvaluation =
            BitVectorEvaluationUtil.buildFullSparseVariableOnlyEvaluationByAccessType(
                pActiveThread,
                SeqMemoryAccessType.READ,
                sparseWriteBitVectors,
                pBitVectorVariables);
        // direct writes and reachable accesses: dW && (rA' || rA'' || ...)
        Optional<CExportExpression> writeAccessEvaluation =
            BitVectorEvaluationUtil.buildFullSparseVariableOnlyEvaluationByAccessType(
                pActiveThread,
                SeqMemoryAccessType.WRITE,
                sparseAccessBitVectors,
                pBitVectorVariables);

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
      SeqPointerAliasingMap pPointerAliasingMap,
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
          pPointerAliasingMap,
          pUtils);
    } else {
      return buildFullDenseEvaluation(
          pOptions.bitVectorEncoding(),
          pOtherWriteBitVectors,
          pOtherAccessBitVectors,
          pDirectReadMemoryLocations,
          pDirectWriteMemoryLocations,
          pMachineModel,
          pPointerAliasingMap,
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
            pReadMemoryLocations, SeqMemoryAccessType.READ, pBitVectorVariables);
    ImmutableMap<SeqMemoryLocation, CExpression> writeLeftHandSides =
        BitVectorEvaluationUtil.buildSparseLeftHandSidesByAccessType(
            pWriteMemoryLocations, SeqMemoryAccessType.WRITE, pBitVectorVariables);
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
            SeqMemoryAccessType.READ, pBitVectorVariables);
    ImmutableMap<SeqMemoryLocation, CExpression> writeLeftHandSides =
        BitVectorEvaluationUtil.buildPrevSparseLeftHandSidesByAccessType(
            SeqMemoryAccessType.WRITE, pBitVectorVariables);
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
                SeqMemoryAccessType.WRITE,
                pBitVectorVariables)
            : BitVectorEvaluationUtil.buildFullSparseEvaluationByAccessType(
                pReadLeftHandSides,
                pSparseWriteMap,
                SeqMemoryAccessType.WRITE,
                pBitVectorVariables);

    // W && (A' || A'' || ...)
    Optional<CExportExpression> accessEvaluation =
        pOptions.pruneBitVectorEvaluations()
            ? BitVectorEvaluationUtil.buildPrunedSparseEvaluationByAccessType(
                pWriteLeftHandSides,
                pSparseAccessMap,
                pDirectWriteMemoryLocations,
                SeqMemoryAccessType.ACCESS,
                pBitVectorVariables)
            : BitVectorEvaluationUtil.buildFullSparseEvaluationByAccessType(
                pWriteLeftHandSides,
                pSparseAccessMap,
                SeqMemoryAccessType.ACCESS,
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
      SeqPointerAliasingMap pPointerAliasingMap,
      SequentializationUtils pUtils)
      throws UnrecognizedCodeException {

    Optional<CExportExpression> leftHandSide =
        buildPrunedDenseLeftHandSide(
            pEncoding,
            pOtherWriteBitVectors,
            pDirectReadMemoryLocations,
            pMachineModel,
            pPointerAliasingMap,
            pUtils.binaryExpressionBuilder());
    Optional<CExportExpression> rightHandSide =
        buildPrunedDenseRightHandSide(
            pEncoding,
            pOtherAccessBitVectors,
            pDirectWriteMemoryLocations,
            pMachineModel,
            pPointerAliasingMap,
            pUtils.binaryExpressionBuilder());

    return tryBuildLogicalOr(leftHandSide, rightHandSide);
  }

  private static Optional<CExportExpression> buildPrunedDenseLeftHandSide(
      SeqBitVectorEncoding pEncoding,
      ImmutableSet<CExpression> pOtherWriteBitVectors,
      ImmutableSet<SeqMemoryLocation> pDirectReadMemoryLocations,
      MachineModel pMachineModel,
      SeqPointerAliasingMap pPointerAliasingMap,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    if (pDirectReadMemoryLocations.isEmpty()) {
      return Optional.empty();
    }
    CIntegerLiteralExpression directReadBitVector =
        SeqBitVectorUtil.buildBitVectorExpression(
            pEncoding, pMachineModel, pPointerAliasingMap, pDirectReadMemoryLocations);
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
      SeqPointerAliasingMap pPointerAliasingMap,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    if (pDirectWriteMemoryLocations.isEmpty()) {
      return Optional.empty();
    }
    CIntegerLiteralExpression directWriteBitVector =
        SeqBitVectorUtil.buildBitVectorExpression(
            pEncoding, pMachineModel, pPointerAliasingMap, pDirectWriteMemoryLocations);
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
      SeqPointerAliasingMap pPointerAliasingMap,
      SequentializationUtils pUtils)
      throws UnrecognizedCodeException {

    CIntegerLiteralExpression directReadBitVector =
        SeqBitVectorUtil.buildBitVectorExpression(
            pEncoding, pMachineModel, pPointerAliasingMap, pDirectReadMemoryLocations);
    CIntegerLiteralExpression directWriteBitVector =
        SeqBitVectorUtil.buildBitVectorExpression(
            pEncoding, pMachineModel, pPointerAliasingMap, pDirectWriteMemoryLocations);
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
            pActiveThread, SeqMemoryAccessType.READ, SeqMemoryReachType.DIRECT);
    CExpression directWriteBitVector =
        pBitVectorVariables.getDenseBitVector(
            pActiveThread, SeqMemoryAccessType.WRITE, SeqMemoryReachType.DIRECT);
    ImmutableSet<CExpression> otherWriteBitVectors =
        pBitVectorVariables.getOtherDenseReachableBitVectorsByAccessType(
            SeqMemoryAccessType.WRITE, pOtherThreads);
    ImmutableSet<CExpression> otherAccessBitVectors =
        pBitVectorVariables.getOtherDenseReachableBitVectorsByAccessType(
            SeqMemoryAccessType.ACCESS, pOtherThreads);
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
