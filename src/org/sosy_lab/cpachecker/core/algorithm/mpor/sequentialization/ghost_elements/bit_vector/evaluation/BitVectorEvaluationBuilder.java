// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.evaluation;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.CToSeqExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.SeqExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.block.SeqThreadStatementBlock;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.BitVectorVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.LastDenseBitVector;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.LastSparseBitVector;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryAccessType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryLocation;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryLocationFinder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryModel;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class BitVectorEvaluationBuilder {

  // variable only i.e. no literal expressions =====================================================

  public static BitVectorEvaluationExpression buildVariableOnlyEvaluation(
      MPOROptions pOptions,
      MPORThread pActiveThread,
      ImmutableSet<MPORThread> pOtherThreads,
      BitVectorVariables pBitVectorVariables,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    checkArgument(
        pOptions.areBitVectorsEnabled(),
        "either conflictReduction or bitVectorReduction must be enabled");

    return switch (pOptions.reductionMode) {
      case NONE ->
          throw new IllegalArgumentException(
              "conflictReduction must be enabled to build evaluation expression");
      case ACCESS_ONLY ->
          BitVectorAccessEvaluationBuilder.buildVariableOnlyEvaluationByEncoding(
              pOptions,
              pActiveThread,
              pOtherThreads,
              pBitVectorVariables,
              pBinaryExpressionBuilder);
      case READ_AND_WRITE ->
          BitVectorReadWriteEvaluationBuilder.buildVariableOnlyEvaluationByEncoding(
              pOptions,
              pActiveThread,
              pOtherThreads,
              pBitVectorVariables,
              pBinaryExpressionBuilder);
    };
  }

  // last bit vector evaluations (conflict reduction) ==============================================

  public static BitVectorEvaluationExpression buildLastBitVectorEvaluation(
      MPOROptions pOptions,
      ImmutableMap<Integer, SeqThreadStatementBlock> pLabelBlockMap,
      SeqThreadStatementBlock pTargetBlock,
      BitVectorVariables pBitVectorVariables,
      MemoryModel pMemoryModel,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    checkArgument(pOptions.conflictReduction, "conflict reduction must be enabled");

    return switch (pOptions.reductionMode) {
      case NONE ->
          throw new IllegalArgumentException(
              "conflictReduction must be enabled to build evaluation expression");
      case ACCESS_ONLY -> {
        ImmutableSet<MemoryLocation> directAccessMemoryLocations =
            MemoryLocationFinder.findDirectMemoryLocationsByAccessType(
                pLabelBlockMap, pMemoryModel, pTargetBlock, MemoryAccessType.ACCESS);
        yield buildLastAccessBitVectorEvaluationByEncoding(
            pOptions,
            directAccessMemoryLocations,
            pBitVectorVariables,
            pMemoryModel,
            pBinaryExpressionBuilder);
      }
      case READ_AND_WRITE -> {
        ImmutableSet<MemoryLocation> directReadMemoryLocations =
            MemoryLocationFinder.findDirectMemoryLocationsByAccessType(
                pLabelBlockMap, pMemoryModel, pTargetBlock, MemoryAccessType.READ);
        ImmutableSet<MemoryLocation> directWriteMemoryLocations =
            MemoryLocationFinder.findDirectMemoryLocationsByAccessType(
                pLabelBlockMap, pMemoryModel, pTargetBlock, MemoryAccessType.WRITE);
        yield buildLastReadWriteBitVectorEvaluationByEncoding(
            pOptions,
            directReadMemoryLocations,
            directWriteMemoryLocations,
            pBitVectorVariables,
            pMemoryModel,
            pBinaryExpressionBuilder);
      }
    };
  }

  private static BitVectorEvaluationExpression buildLastAccessBitVectorEvaluationByEncoding(
      MPOROptions pOptions,
      ImmutableSet<MemoryLocation> pDirectAccessMemoryLocations,
      BitVectorVariables pBitVectorVariables,
      MemoryModel pMemoryModel,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    return switch (pOptions.bitVectorEncoding) {
      case NONE ->
          throw new IllegalArgumentException(
              "cannot build evaluation for encoding " + pOptions.bitVectorEncoding);
      case BINARY, DECIMAL, HEXADECIMAL -> {
        LastDenseBitVector lastAccessBitVector =
            pBitVectorVariables.getLastDenseBitVectorByAccessType(MemoryAccessType.ACCESS);
        ImmutableSet<CExpression> otherAccessBitVectors =
            ImmutableSet.of(lastAccessBitVector.reachableVariable);
        yield BitVectorAccessEvaluationBuilder.buildDenseEvaluation(
            pOptions,
            otherAccessBitVectors,
            pDirectAccessMemoryLocations,
            pMemoryModel,
            pBinaryExpressionBuilder);
      }
      case SPARSE -> {
        ImmutableListMultimap<MemoryLocation, SeqExpression> sparseAccessMap =
            mapMemoryLocationsToLastSparseBitVectorsByAccessType(
                pBitVectorVariables, MemoryAccessType.ACCESS);
        yield BitVectorAccessEvaluationBuilder.buildSparseEvaluation(
            pOptions, sparseAccessMap, pDirectAccessMemoryLocations, pBitVectorVariables);
      }
    };
  }

  private static BitVectorEvaluationExpression buildLastReadWriteBitVectorEvaluationByEncoding(
      MPOROptions pOptions,
      ImmutableSet<MemoryLocation> pDirectReadMemoryLocations,
      ImmutableSet<MemoryLocation> pDirectWriteMemoryLocations,
      BitVectorVariables pBitVectorVariables,
      MemoryModel pMemoryModel,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    return switch (pOptions.bitVectorEncoding) {
      case NONE ->
          throw new IllegalArgumentException(
              "cannot build evaluation for encoding " + pOptions.bitVectorEncoding);
      case BINARY, DECIMAL, HEXADECIMAL -> {
        LastDenseBitVector lastWriteBitVector =
            pBitVectorVariables.getLastDenseBitVectorByAccessType(MemoryAccessType.WRITE);
        LastDenseBitVector lastAccessBitVector =
            pBitVectorVariables.getLastDenseBitVectorByAccessType(MemoryAccessType.ACCESS);
        yield BitVectorReadWriteEvaluationBuilder.buildDenseEvaluation(
            pOptions,
            ImmutableSet.of(lastWriteBitVector.reachableVariable),
            ImmutableSet.of(lastAccessBitVector.reachableVariable),
            pDirectReadMemoryLocations,
            pDirectWriteMemoryLocations,
            pMemoryModel,
            pBinaryExpressionBuilder);
      }
      case SPARSE -> {
        ImmutableListMultimap<MemoryLocation, SeqExpression> sparseWriteMap =
            mapMemoryLocationsToLastSparseBitVectorsByAccessType(
                pBitVectorVariables, MemoryAccessType.WRITE);
        ImmutableListMultimap<MemoryLocation, SeqExpression> sparseAccessMap =
            mapMemoryLocationsToLastSparseBitVectorsByAccessType(
                pBitVectorVariables, MemoryAccessType.ACCESS);
        yield BitVectorReadWriteEvaluationBuilder.buildSparseEvaluation(
            pOptions,
            sparseWriteMap,
            sparseAccessMap,
            pDirectReadMemoryLocations,
            pDirectWriteMemoryLocations,
            pBitVectorVariables);
      }
    };
  }

  private static ImmutableListMultimap<MemoryLocation, SeqExpression>
      mapMemoryLocationsToLastSparseBitVectorsByAccessType(
          BitVectorVariables pBitVectorVariables, MemoryAccessType pAccessType) {

    ImmutableListMultimap.Builder<MemoryLocation, SeqExpression> rMap =
        ImmutableListMultimap.builder();
    ImmutableMap<MemoryLocation, LastSparseBitVector> lastSparseBitVectors =
        pBitVectorVariables.getLastSparseBitVectorByAccessType(pAccessType);
    for (var entry : lastSparseBitVectors.entrySet()) {
      MemoryLocation memoryLocation = entry.getKey();
      rMap.put(memoryLocation, new CToSeqExpression(entry.getValue().variable));
    }
    return rMap.build();
  }

  // bit vector evaluations by accessed global variables (bit vector reduction) ====================

  public static BitVectorEvaluationExpression buildEvaluationByDirectVariableAccesses(
      MPOROptions pOptions,
      ImmutableSet<MPORThread> pOtherThreads,
      ImmutableMap<Integer, SeqThreadStatementBlock> pLabelBlockMap,
      SeqThreadStatementBlock pTargetBlock,
      BitVectorVariables pBitVectorVariables,
      MemoryModel pMemoryModel,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    checkArgument(
        pOptions.bitVectorReduction,
        "bitVectorReduction must be enabled to build evaluation expression");

    return switch (pOptions.reductionMode) {
      case NONE ->
          throw new IllegalArgumentException(
              "bitVectorReduction must be enabled to build evaluation expression");
      case ACCESS_ONLY -> {
        ImmutableSet<MemoryLocation> directAccessMemoryLocations =
            MemoryLocationFinder.findDirectMemoryLocationsByAccessType(
                pLabelBlockMap, pMemoryModel, pTargetBlock, MemoryAccessType.ACCESS);
        yield buildEvaluationByReduction(
            pOptions,
            pOtherThreads,
            directAccessMemoryLocations,
            ImmutableSet.of(),
            ImmutableSet.of(),
            pBitVectorVariables,
            pMemoryModel,
            pBinaryExpressionBuilder);
      }
      case READ_AND_WRITE -> {
        ImmutableSet<MemoryLocation> directReadMemoryLocations =
            MemoryLocationFinder.findDirectMemoryLocationsByAccessType(
                pLabelBlockMap, pMemoryModel, pTargetBlock, MemoryAccessType.READ);
        ImmutableSet<MemoryLocation> directWriteMemoryLocations =
            MemoryLocationFinder.findDirectMemoryLocationsByAccessType(
                pLabelBlockMap, pMemoryModel, pTargetBlock, MemoryAccessType.WRITE);
        yield buildEvaluationByReduction(
            pOptions,
            pOtherThreads,
            ImmutableSet.of(),
            directReadMemoryLocations,
            directWriteMemoryLocations,
            pBitVectorVariables,
            pMemoryModel,
            pBinaryExpressionBuilder);
      }
    };
  }

  private static BitVectorEvaluationExpression buildEvaluationByReduction(
      MPOROptions pOptions,
      ImmutableSet<MPORThread> pOtherThreads,
      ImmutableSet<MemoryLocation> pDirectAccessMemoryLocations,
      ImmutableSet<MemoryLocation> pDirectReadMemoryLocations,
      ImmutableSet<MemoryLocation> pDirectWriteMemoryLocations,
      BitVectorVariables pBitVectorVariables,
      MemoryModel pMemoryModel,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    return switch (pOptions.reductionMode) {
      case NONE ->
          throw new IllegalArgumentException(
              "bitVectorReduction must be enabled to build evaluation expression");
      case ACCESS_ONLY ->
          buildAccessEvaluationByEncoding(
              pOptions,
              pOtherThreads,
              pDirectAccessMemoryLocations,
              pBitVectorVariables,
              pMemoryModel,
              pBinaryExpressionBuilder);
      case READ_AND_WRITE ->
          buildReadWriteEvaluationByEncoding(
              pOptions,
              pOtherThreads,
              pDirectReadMemoryLocations,
              pDirectWriteMemoryLocations,
              pBitVectorVariables,
              pMemoryModel,
              pBinaryExpressionBuilder);
    };
  }

  private static BitVectorEvaluationExpression buildAccessEvaluationByEncoding(
      MPOROptions pOptions,
      ImmutableSet<MPORThread> pOtherThreads,
      ImmutableSet<MemoryLocation> pDirectAccessMemoryLocations,
      BitVectorVariables pBitVectorVariables,
      MemoryModel pMemoryModel,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    return switch (pOptions.bitVectorEncoding) {
      case NONE ->
          throw new IllegalArgumentException(
              "cannot build evaluation for encoding " + pOptions.bitVectorEncoding);
      case BINARY, DECIMAL, HEXADECIMAL -> {
        ImmutableSet<CExpression> otherBitVectors =
            pBitVectorVariables.getOtherDenseReachableBitVectorsByAccessType(
                MemoryAccessType.ACCESS, pOtherThreads);
        yield BitVectorAccessEvaluationBuilder.buildDenseEvaluation(
            pOptions,
            otherBitVectors,
            pDirectAccessMemoryLocations,
            pMemoryModel,
            pBinaryExpressionBuilder);
      }
      case SPARSE -> {
        ImmutableListMultimap<MemoryLocation, SeqExpression> sparseAccessMap =
            mapMemoryLocationsToSparseBitVectorsByAccessType(
                pOtherThreads, pBitVectorVariables, MemoryAccessType.ACCESS);
        yield BitVectorAccessEvaluationBuilder.buildSparseEvaluation(
            pOptions, sparseAccessMap, pDirectAccessMemoryLocations, pBitVectorVariables);
      }
    };
  }

  private static BitVectorEvaluationExpression buildReadWriteEvaluationByEncoding(
      MPOROptions pOptions,
      ImmutableSet<MPORThread> pOtherThreads,
      ImmutableSet<MemoryLocation> pDirectReadMemoryLocations,
      ImmutableSet<MemoryLocation> pDirectWriteMemoryLocations,
      BitVectorVariables pBitVectorVariables,
      MemoryModel pMemoryModel,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    return switch (pOptions.bitVectorEncoding) {
      case NONE ->
          throw new IllegalArgumentException(
              "cannot build evaluation for encoding " + pOptions.bitVectorEncoding);
      case BINARY, DECIMAL, HEXADECIMAL -> {
        ImmutableSet<CExpression> otherWriteBitVectors =
            pBitVectorVariables.getOtherDenseReachableBitVectorsByAccessType(
                MemoryAccessType.WRITE, pOtherThreads);
        ImmutableSet<CExpression> otherAccessBitVectors =
            pBitVectorVariables.getOtherDenseReachableBitVectorsByAccessType(
                MemoryAccessType.ACCESS, pOtherThreads);
        yield BitVectorReadWriteEvaluationBuilder.buildDenseEvaluation(
            pOptions,
            otherWriteBitVectors,
            otherAccessBitVectors,
            pDirectReadMemoryLocations,
            pDirectWriteMemoryLocations,
            pMemoryModel,
            pBinaryExpressionBuilder);
      }
      case SPARSE -> {
        ImmutableListMultimap<MemoryLocation, SeqExpression> sparseWriteMap =
            mapMemoryLocationsToSparseBitVectorsByAccessType(
                pOtherThreads, pBitVectorVariables, MemoryAccessType.WRITE);
        ImmutableListMultimap<MemoryLocation, SeqExpression> sparseAccessMap =
            mapMemoryLocationsToSparseBitVectorsByAccessType(
                pOtherThreads, pBitVectorVariables, MemoryAccessType.ACCESS);
        yield BitVectorReadWriteEvaluationBuilder.buildSparseEvaluation(
            pOptions,
            sparseWriteMap,
            sparseAccessMap,
            pDirectReadMemoryLocations,
            pDirectWriteMemoryLocations,
            pBitVectorVariables);
      }
    };
  }

  private static ImmutableListMultimap<MemoryLocation, SeqExpression>
      mapMemoryLocationsToSparseBitVectorsByAccessType(
          ImmutableSet<MPORThread> pOtherThreads,
          BitVectorVariables pBitVectorVariables,
          MemoryAccessType pAccessType) {

    ImmutableListMultimap.Builder<MemoryLocation, SeqExpression> rMap =
        ImmutableListMultimap.builder();
    for (var entry : pBitVectorVariables.getSparseBitVectorByAccessType(pAccessType).entrySet()) {
      MemoryLocation memoryLocation = entry.getKey();
      ImmutableMap<MPORThread, CIdExpression> variables = entry.getValue().variables;
      ImmutableList<SeqExpression> otherVariables =
          BitVectorEvaluationUtil.convertOtherVariablesToSeqExpression(pOtherThreads, variables);
      rMap.putAll(memoryLocation, otherVariables);
    }
    return rMap.build();
  }
}
