// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.evaluation;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SequentializationUtils;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.SeqBitVectorEncoding;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.SeqBitVectorUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.SeqBitVectorVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryAccessType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryModel;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.ReachType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.SeqMemoryLocation;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.cwriter.export.CExportExpression;
import org.sosy_lab.cpachecker.util.cwriter.export.CExpressionWrapper;

class BitVectorAccessEvaluationBuilder {

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
      case SPARSE ->
          buildFullSparseVariableOnlyEvaluation(pActiveThread, pOtherThreads, pBitVectorVariables);
    };
  }

  static Optional<CExportExpression> buildDenseEvaluation(
      MPOROptions pOptions,
      ImmutableSet<CExpression> pOtherBitVectors,
      ImmutableSet<SeqMemoryLocation> pDirectMemoryLocations,
      MachineModel pMachineModel,
      MemoryModel pMemoryModel,
      SequentializationUtils pUtils)
      throws UnrecognizedCodeException {

    if (pOptions.pruneBitVectorEvaluations()) {
      return buildPrunedDenseEvaluation(
          pOptions.bitVectorEncoding(),
          pOtherBitVectors,
          pDirectMemoryLocations,
          pMachineModel,
          pMemoryModel,
          pUtils);
    } else {
      return Optional.of(
          buildFullDenseEvaluation(
              pOptions.bitVectorEncoding(),
              pOtherBitVectors,
              pDirectMemoryLocations,
              pMachineModel,
              pMemoryModel,
              pUtils));
    }
  }

  static Optional<CExportExpression> buildSparseEvaluation(
      MPOROptions pOptions,
      ImmutableListMultimap<SeqMemoryLocation, CExpression> pRightHandSides,
      ImmutableSet<SeqMemoryLocation> pAccessedMemoryLocations,
      SeqBitVectorVariables pBitVectorVariables) {

    ImmutableMap<SeqMemoryLocation, CExpression> leftHandSides =
        pBitVectorVariables.getSparseAccessBitVectors().keySet().stream()
            .collect(
                ImmutableMap.toImmutableMap(
                    memoryLocation -> memoryLocation,
                    memoryLocation ->
                        BitVectorEvaluationUtil.buildSparseDirectBitVector(
                            memoryLocation, pAccessedMemoryLocations)));
    return buildSparseEvaluation(
        pOptions, leftHandSides, pRightHandSides, pAccessedMemoryLocations, pBitVectorVariables);
  }

  static Optional<CExportExpression> buildPrevSparseEvaluation(
      MPOROptions pOptions,
      MPORThread pCurrentThread,
      ImmutableListMultimap<SeqMemoryLocation, CExpression> pRightHandSides,
      ImmutableSet<SeqMemoryLocation> pAccessedMemoryLocations,
      SeqBitVectorVariables pBitVectorVariables) {

    ImmutableMap<SeqMemoryLocation, CExpression> leftHandSides =
        pBitVectorVariables.getSparseAccessBitVectors().entrySet().stream()
            .collect(
                ImmutableMap.toImmutableMap(
                    Entry::getKey,
                    entry ->
                        Objects.requireNonNull(
                            entry.getValue().directVariables().get(pCurrentThread))));
    return buildSparseEvaluation(
        pOptions, leftHandSides, pRightHandSides, pAccessedMemoryLocations, pBitVectorVariables);
  }

  private static Optional<CExportExpression> buildSparseEvaluation(
      MPOROptions pOptions,
      ImmutableMap<SeqMemoryLocation, CExpression> pLeftHandSides,
      ImmutableListMultimap<SeqMemoryLocation, CExpression> pRightHandSides,
      ImmutableSet<SeqMemoryLocation> pAccessedMemoryLocations,
      SeqBitVectorVariables pBitVectorVariables) {

    if (pOptions.pruneBitVectorEvaluations()) {
      return BitVectorEvaluationUtil.buildPrunedSparseEvaluation(
          pLeftHandSides,
          pRightHandSides,
          pAccessedMemoryLocations,
          MemoryAccessType.ACCESS,
          pBitVectorVariables);
    } else {
      return BitVectorEvaluationUtil.buildFullSparseEvaluation(
          pLeftHandSides, pRightHandSides, MemoryAccessType.ACCESS, pBitVectorVariables);
    }
  }

  // Dense Access Bit Vectors ======================================================================

  private static Optional<CExportExpression> buildPrunedDenseEvaluation(
      SeqBitVectorEncoding pEncoding,
      ImmutableSet<CExpression> pOtherBitVectors,
      ImmutableSet<SeqMemoryLocation> pDirectAccessMemoryLocations,
      MachineModel pMachineModel,
      MemoryModel pMemoryModel,
      SequentializationUtils pUtils)
      throws UnrecognizedCodeException {

    // no direct global variable accesses -> prune (either full or entirely pruned evaluation)
    if (pDirectAccessMemoryLocations.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(
        buildFullDenseEvaluation(
            pEncoding,
            pOtherBitVectors,
            pDirectAccessMemoryLocations,
            pMachineModel,
            pMemoryModel,
            pUtils));
  }

  private static CExpressionWrapper buildFullDenseEvaluation(
      SeqBitVectorEncoding pEncoding,
      ImmutableSet<CExpression> pOtherBitVectors,
      ImmutableSet<SeqMemoryLocation> pDirectMemoryLocations,
      MachineModel pMachineModel,
      MemoryModel pMemoryModel,
      SequentializationUtils pUtils)
      throws UnrecognizedCodeException {

    CIntegerLiteralExpression directBitVector =
        SeqBitVectorUtil.buildBitVectorExpression(
            pEncoding, pMachineModel, pMemoryModel, pDirectMemoryLocations);
    return buildFullDenseBinaryAnd(directBitVector, pOtherBitVectors, pUtils);
  }

  private static CExpressionWrapper buildFullDenseVariableOnlyEvaluation(
      MPORThread pActiveThread,
      ImmutableSet<MPORThread> pOtherThreads,
      SeqBitVectorVariables pBitVectorVariables,
      SequentializationUtils pUtils)
      throws UnrecognizedCodeException {

    CExpression directBitVector =
        pBitVectorVariables.getDenseBitVector(
            pActiveThread, MemoryAccessType.ACCESS, ReachType.DIRECT);
    ImmutableSet<CExpression> otherReachableBitVectors =
        pBitVectorVariables.getOtherDenseReachableBitVectorsByAccessType(
            MemoryAccessType.ACCESS, pOtherThreads);
    return buildFullDenseBinaryAnd(directBitVector, otherReachableBitVectors, pUtils);
  }

  static CExpressionWrapper buildFullDenseBinaryAnd(
      CExpression pDirectBitVector,
      ImmutableSet<CExpression> pOtherBitVectors,
      SequentializationUtils pUtils)
      throws UnrecognizedCodeException {

    CBinaryExpressionBuilder binaryExpressionBuilder = pUtils.binaryExpressionBuilder();
    CExpression rightHandSide =
        BitVectorEvaluationUtil.binaryDisjunction(pOtherBitVectors, binaryExpressionBuilder);
    CBinaryExpression binaryExpression =
        binaryExpressionBuilder.buildBinaryExpression(
            pDirectBitVector, rightHandSide, BinaryOperator.BITWISE_AND);
    return new CExpressionWrapper(binaryExpression);
  }

  // Sparse Access Bit Vectors =====================================================================

  /**
   * Note that the 'full' evaluation can still be pruned entirely if {@link
   * MPOROptions#pruneSparseBitVectors()} is enabled.
   */
  private static Optional<CExportExpression> buildFullSparseVariableOnlyEvaluation(
      MPORThread pActiveThread,
      ImmutableSet<MPORThread> pOtherThreads,
      SeqBitVectorVariables pBitVectorVariables) {

    ImmutableListMultimap<SeqMemoryLocation, CExpression> sparseBitVectorMap =
        BitVectorEvaluationUtil.mapMemoryLocationsToSparseBitVectorsByAccessType(
            pOtherThreads, pBitVectorVariables, MemoryAccessType.ACCESS);
    Builder<CExportExpression> sparseExpressions = ImmutableList.builder();
    for (var entry : pBitVectorVariables.getSparseAccessBitVectors().entrySet()) {
      CIdExpression directBitVector =
          entry.getValue().getVariablesByReachType(ReachType.DIRECT).get(pActiveThread);
      CExportExpression sparseExpression =
          BitVectorEvaluationUtil.buildSingleSparseLogicalAndExpression(
              sparseBitVectorMap, Objects.requireNonNull(directBitVector), entry.getKey());
      sparseExpressions.add(sparseExpression);
    }
    // create disjunction of logical not: (A && (B || C)) || (A' && (B' || C'))
    return BitVectorEvaluationUtil.tryBuildLogicalOrExpression(sparseExpressions.build());
  }
}
