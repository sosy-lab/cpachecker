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
      case SPARSE -> {
        ImmutableListMultimap<SeqMemoryLocation, CExpression> sparseBitVectors =
            BitVectorEvaluationUtil.mapMemoryLocationsToSparseBitVectorsByAccessType(
                pOtherThreads, pBitVectorVariables, SeqMemoryAccessType.ACCESS);
        yield BitVectorEvaluationUtil.buildFullSparseVariableOnlyEvaluationByAccessType(
            pActiveThread, SeqMemoryAccessType.ACCESS, sparseBitVectors, pBitVectorVariables);
      }
    };
  }

  static Optional<CExportExpression> buildDenseEvaluation(
      MPOROptions pOptions,
      ImmutableSet<CExpression> pOtherBitVectors,
      ImmutableSet<SeqMemoryLocation> pDirectMemoryLocations,
      MachineModel pMachineModel,
      SeqPointerAliasingMap pPointerAliasingMap,
      SequentializationUtils pUtils)
      throws UnrecognizedCodeException {

    if (pOptions.pruneBitVectorEvaluations()) {
      return buildPrunedDenseEvaluation(
          pOptions.bitVectorEncoding(),
          pOtherBitVectors,
          pDirectMemoryLocations,
          pMachineModel,
          pPointerAliasingMap,
          pUtils);
    } else {
      return Optional.of(
          buildFullDenseEvaluation(
              pOptions.bitVectorEncoding(),
              pOtherBitVectors,
              pDirectMemoryLocations,
              pMachineModel,
              pPointerAliasingMap,
              pUtils));
    }
  }

  static Optional<CExportExpression> buildSparseEvaluation(
      MPOROptions pOptions,
      ImmutableMap<SeqMemoryLocation, CExpression> pLeftHandSides,
      ImmutableListMultimap<SeqMemoryLocation, CExpression> pRightHandSides,
      ImmutableSet<SeqMemoryLocation> pAccessedMemoryLocations,
      SeqBitVectorVariables pBitVectorVariables) {

    if (pOptions.pruneBitVectorEvaluations()) {
      return BitVectorEvaluationUtil.buildPrunedSparseEvaluationByAccessType(
          pLeftHandSides,
          pRightHandSides,
          pAccessedMemoryLocations,
          SeqMemoryAccessType.ACCESS,
          pBitVectorVariables);
    } else {
      return BitVectorEvaluationUtil.buildFullSparseEvaluationByAccessType(
          pLeftHandSides, pRightHandSides, SeqMemoryAccessType.ACCESS, pBitVectorVariables);
    }
  }

  // Dense Access Bit Vectors ======================================================================

  private static Optional<CExportExpression> buildPrunedDenseEvaluation(
      SeqBitVectorEncoding pEncoding,
      ImmutableSet<CExpression> pOtherBitVectors,
      ImmutableSet<SeqMemoryLocation> pDirectAccessMemoryLocations,
      MachineModel pMachineModel,
      SeqPointerAliasingMap pPointerAliasingMap,
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
            pPointerAliasingMap,
            pUtils));
  }

  private static CExpressionWrapper buildFullDenseEvaluation(
      SeqBitVectorEncoding pEncoding,
      ImmutableSet<CExpression> pOtherBitVectors,
      ImmutableSet<SeqMemoryLocation> pDirectMemoryLocations,
      MachineModel pMachineModel,
      SeqPointerAliasingMap pPointerAliasingMap,
      SequentializationUtils pUtils)
      throws UnrecognizedCodeException {

    CIntegerLiteralExpression directBitVector =
        SeqBitVectorUtil.buildBitVectorExpression(
            pEncoding, pMachineModel, pPointerAliasingMap, pDirectMemoryLocations);
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
            pActiveThread, SeqMemoryAccessType.ACCESS, SeqMemoryReachType.DIRECT);
    ImmutableSet<CExpression> otherReachableBitVectors =
        pBitVectorVariables.getOtherDenseReachableBitVectorsByAccessType(
            SeqMemoryAccessType.ACCESS, pOtherThreads);
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
}
