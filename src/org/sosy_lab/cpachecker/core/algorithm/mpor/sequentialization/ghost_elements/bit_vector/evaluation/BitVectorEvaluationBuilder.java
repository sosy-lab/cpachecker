// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.evaluation;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionTree;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SequentializationUtils;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.block.SeqThreadStatementBlock;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.clause.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.BitVectorVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.BitVectorVariables.LastDenseBitVector;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.BitVectorVariables.LastSparseBitVector;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryAccessType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryModel;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.SeqMemoryLocation;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.SeqMemoryLocationFinder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class BitVectorEvaluationBuilder {

  // variable only i.e. no literal expressions =====================================================

  public static Optional<CExpressionTree> buildVariableOnlyEvaluation(
      MPOROptions pOptions,
      MPORThread pActiveThread,
      ImmutableSet<MPORThread> pOtherThreads,
      BitVectorVariables pBitVectorVariables,
      SequentializationUtils pUtils)
      throws UnrecognizedCodeException {

    return switch (pOptions.reductionMode()) {
      case NONE ->
          throw new IllegalArgumentException(
              String.format(
                  "cannot build evaluation for reductionMode %s", pOptions.reductionMode()));
      case ACCESS_ONLY ->
          BitVectorAccessEvaluationBuilder.buildVariableOnlyEvaluationByEncoding(
              pOptions, pActiveThread, pOtherThreads, pBitVectorVariables, pUtils);
      case READ_AND_WRITE ->
          BitVectorReadWriteEvaluationBuilder.buildVariableOnlyEvaluationByEncoding(
              pOptions, pActiveThread, pOtherThreads, pBitVectorVariables, pUtils);
    };
  }

  // last bit vector evaluations (conflict reduction) ==============================================

  public static Optional<CExpressionTree> buildLastBitVectorEvaluation(
      MPOROptions pOptions,
      ImmutableMap<Integer, SeqThreadStatementClause> pLabelClauseMap,
      ImmutableMap<Integer, SeqThreadStatementBlock> pLabelBlockMap,
      SeqThreadStatementBlock pFirstBlock,
      BitVectorVariables pBitVectorVariables,
      MemoryModel pMemoryModel,
      SequentializationUtils pUtils)
      throws UnrecognizedCodeException {

    checkArgument(pOptions.reduceLastThreadOrder(), "reduceLastThreadOrder must be enabled");

    return switch (pOptions.reductionMode()) {
      case NONE ->
          throw new IllegalArgumentException(
              String.format(
                  "cannot build evaluation for reductionMode %s", pOptions.reductionMode()));
      case ACCESS_ONLY -> {
        ImmutableSet<SeqMemoryLocation> reachableAccessMemoryLocations =
            SeqMemoryLocationFinder.findReachableMemoryLocationsByAccessType(
                pLabelClauseMap,
                pLabelBlockMap,
                pFirstBlock,
                pMemoryModel,
                MemoryAccessType.ACCESS);
        yield buildLastAccessBitVectorEvaluationByEncoding(
            pOptions, reachableAccessMemoryLocations, pBitVectorVariables, pMemoryModel, pUtils);
      }
      case READ_AND_WRITE -> {
        ImmutableSet<SeqMemoryLocation> reachableReadMemoryLocations =
            SeqMemoryLocationFinder.findReachableMemoryLocationsByAccessType(
                pLabelClauseMap, pLabelBlockMap, pFirstBlock, pMemoryModel, MemoryAccessType.READ);
        ImmutableSet<SeqMemoryLocation> reachableWriteMemoryLocations =
            SeqMemoryLocationFinder.findReachableMemoryLocationsByAccessType(
                pLabelClauseMap, pLabelBlockMap, pFirstBlock, pMemoryModel, MemoryAccessType.WRITE);
        yield buildLastReadWriteBitVectorEvaluationByEncoding(
            pOptions,
            reachableReadMemoryLocations,
            reachableWriteMemoryLocations,
            pBitVectorVariables,
            pMemoryModel,
            pUtils);
      }
    };
  }

  private static Optional<CExpressionTree> buildLastAccessBitVectorEvaluationByEncoding(
      MPOROptions pOptions,
      ImmutableSet<SeqMemoryLocation> pDirectAccessMemoryLocations,
      BitVectorVariables pBitVectorVariables,
      MemoryModel pMemoryModel,
      SequentializationUtils pUtils)
      throws UnrecognizedCodeException {

    return switch (pOptions.bitVectorEncoding()) {
      case NONE ->
          throw new IllegalArgumentException(
              String.format(
                  "cannot build evaluation for encoding %s", pOptions.bitVectorEncoding()));
      case BINARY, DECIMAL, HEXADECIMAL -> {
        LastDenseBitVector lastAccessBitVector =
            pBitVectorVariables.getLastDenseBitVectorByAccessType(MemoryAccessType.ACCESS);
        ImmutableSet<CExpression> otherAccessBitVectors =
            ImmutableSet.of(lastAccessBitVector.reachableVariable());
        yield BitVectorAccessEvaluationBuilder.buildDenseEvaluation(
            pOptions, otherAccessBitVectors, pDirectAccessMemoryLocations, pMemoryModel, pUtils);
      }
      case SPARSE -> {
        ImmutableListMultimap<SeqMemoryLocation, CExpression> sparseAccessMap =
            mapMemoryLocationsToLastSparseBitVectorsByAccessType(
                pBitVectorVariables, MemoryAccessType.ACCESS);
        yield BitVectorAccessEvaluationBuilder.buildSparseEvaluation(
            pOptions, sparseAccessMap, pDirectAccessMemoryLocations, pBitVectorVariables);
      }
    };
  }

  private static Optional<CExpressionTree> buildLastReadWriteBitVectorEvaluationByEncoding(
      MPOROptions pOptions,
      ImmutableSet<SeqMemoryLocation> pDirectReadMemoryLocations,
      ImmutableSet<SeqMemoryLocation> pDirectWriteMemoryLocations,
      BitVectorVariables pBitVectorVariables,
      MemoryModel pMemoryModel,
      SequentializationUtils pUtils)
      throws UnrecognizedCodeException {

    return switch (pOptions.bitVectorEncoding()) {
      case NONE ->
          throw new IllegalArgumentException(
              String.format(
                  "cannot build evaluation for encoding %s", pOptions.bitVectorEncoding()));
      case BINARY, DECIMAL, HEXADECIMAL -> {
        LastDenseBitVector lastWriteBitVector =
            pBitVectorVariables.getLastDenseBitVectorByAccessType(MemoryAccessType.WRITE);
        LastDenseBitVector lastAccessBitVector =
            pBitVectorVariables.getLastDenseBitVectorByAccessType(MemoryAccessType.ACCESS);
        yield BitVectorReadWriteEvaluationBuilder.buildDenseEvaluation(
            pOptions,
            ImmutableSet.of(lastWriteBitVector.reachableVariable()),
            ImmutableSet.of(lastAccessBitVector.reachableVariable()),
            pDirectReadMemoryLocations,
            pDirectWriteMemoryLocations,
            pMemoryModel,
            pUtils);
      }
      case SPARSE -> {
        ImmutableListMultimap<SeqMemoryLocation, CExpression> sparseWriteMap =
            mapMemoryLocationsToLastSparseBitVectorsByAccessType(
                pBitVectorVariables, MemoryAccessType.WRITE);
        ImmutableListMultimap<SeqMemoryLocation, CExpression> sparseAccessMap =
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

  private static ImmutableListMultimap<SeqMemoryLocation, CExpression>
      mapMemoryLocationsToLastSparseBitVectorsByAccessType(
          BitVectorVariables pBitVectorVariables, MemoryAccessType pAccessType) {

    ImmutableListMultimap.Builder<SeqMemoryLocation, CExpression> rMap =
        ImmutableListMultimap.builder();
    ImmutableMap<SeqMemoryLocation, LastSparseBitVector> lastSparseBitVectors =
        pBitVectorVariables.getLastSparseBitVectorByAccessType(pAccessType);
    for (var entry : lastSparseBitVectors.entrySet()) {
      SeqMemoryLocation memoryLocation = entry.getKey();
      rMap.put(memoryLocation, entry.getValue().reachableVariable());
    }
    return rMap.build();
  }

  // bit vector evaluations by accessed global variables (bit vector reduction) ====================

  public static Optional<CExpressionTree> buildEvaluationByDirectVariableAccesses(
      MPOROptions pOptions,
      ImmutableSet<MPORThread> pOtherThreads,
      ImmutableMap<Integer, SeqThreadStatementBlock> pLabelBlockMap,
      SeqThreadStatementBlock pTargetBlock,
      BitVectorVariables pBitVectorVariables,
      MemoryModel pMemoryModel,
      SequentializationUtils pUtils)
      throws UnrecognizedCodeException {

    checkArgument(
        pOptions.reduceUntilConflict(),
        "reduceUntilConflict must be enabled to build evaluation expression");

    return switch (pOptions.reductionMode()) {
      case NONE ->
          throw new IllegalArgumentException(
              String.format(
                  "cannot build evaluation for reductionMode %s", pOptions.reductionMode()));
      case ACCESS_ONLY -> {
        ImmutableSet<SeqMemoryLocation> directAccessMemoryLocations =
            SeqMemoryLocationFinder.findDirectMemoryLocationsByAccessType(
                pLabelBlockMap, pTargetBlock, pMemoryModel, MemoryAccessType.ACCESS);
        yield buildEvaluationByReduction(
            pOptions,
            pOtherThreads,
            directAccessMemoryLocations,
            ImmutableSet.of(),
            ImmutableSet.of(),
            pBitVectorVariables,
            pMemoryModel,
            pUtils);
      }
      case READ_AND_WRITE -> {
        ImmutableSet<SeqMemoryLocation> directReadMemoryLocations =
            SeqMemoryLocationFinder.findDirectMemoryLocationsByAccessType(
                pLabelBlockMap, pTargetBlock, pMemoryModel, MemoryAccessType.READ);
        ImmutableSet<SeqMemoryLocation> directWriteMemoryLocations =
            SeqMemoryLocationFinder.findDirectMemoryLocationsByAccessType(
                pLabelBlockMap, pTargetBlock, pMemoryModel, MemoryAccessType.WRITE);
        yield buildEvaluationByReduction(
            pOptions,
            pOtherThreads,
            ImmutableSet.of(),
            directReadMemoryLocations,
            directWriteMemoryLocations,
            pBitVectorVariables,
            pMemoryModel,
            pUtils);
      }
    };
  }

  private static Optional<CExpressionTree> buildEvaluationByReduction(
      MPOROptions pOptions,
      ImmutableSet<MPORThread> pOtherThreads,
      ImmutableSet<SeqMemoryLocation> pDirectAccessMemoryLocations,
      ImmutableSet<SeqMemoryLocation> pDirectReadMemoryLocations,
      ImmutableSet<SeqMemoryLocation> pDirectWriteMemoryLocations,
      BitVectorVariables pBitVectorVariables,
      MemoryModel pMemoryModel,
      SequentializationUtils pUtils)
      throws UnrecognizedCodeException {

    return switch (pOptions.reductionMode()) {
      case NONE ->
          throw new IllegalArgumentException(
              String.format(
                  "cannot build evaluation for reductionMode %s", pOptions.reductionMode()));
      case ACCESS_ONLY ->
          buildAccessEvaluationByEncoding(
              pOptions,
              pOtherThreads,
              pDirectAccessMemoryLocations,
              pBitVectorVariables,
              pMemoryModel,
              pUtils);
      case READ_AND_WRITE ->
          buildReadWriteEvaluationByEncoding(
              pOptions,
              pOtherThreads,
              pDirectReadMemoryLocations,
              pDirectWriteMemoryLocations,
              pBitVectorVariables,
              pMemoryModel,
              pUtils);
    };
  }

  private static Optional<CExpressionTree> buildAccessEvaluationByEncoding(
      MPOROptions pOptions,
      ImmutableSet<MPORThread> pOtherThreads,
      ImmutableSet<SeqMemoryLocation> pDirectAccessMemoryLocations,
      BitVectorVariables pBitVectorVariables,
      MemoryModel pMemoryModel,
      SequentializationUtils pUtils)
      throws UnrecognizedCodeException {

    return switch (pOptions.bitVectorEncoding()) {
      case NONE ->
          throw new IllegalArgumentException(
              String.format(
                  "cannot build evaluation for encoding %s", pOptions.bitVectorEncoding()));
      case BINARY, DECIMAL, HEXADECIMAL -> {
        ImmutableSet<CExpression> otherBitVectors =
            pBitVectorVariables.getOtherDenseReachableBitVectorsByAccessType(
                MemoryAccessType.ACCESS, pOtherThreads);
        yield BitVectorAccessEvaluationBuilder.buildDenseEvaluation(
            pOptions, otherBitVectors, pDirectAccessMemoryLocations, pMemoryModel, pUtils);
      }
      case SPARSE -> {
        ImmutableListMultimap<SeqMemoryLocation, CExpression> sparseAccessMap =
            BitVectorEvaluationUtil.mapMemoryLocationsToSparseBitVectorsByAccessType(
                pOtherThreads, pBitVectorVariables, MemoryAccessType.ACCESS);
        yield BitVectorAccessEvaluationBuilder.buildSparseEvaluation(
            pOptions, sparseAccessMap, pDirectAccessMemoryLocations, pBitVectorVariables);
      }
    };
  }

  private static Optional<CExpressionTree> buildReadWriteEvaluationByEncoding(
      MPOROptions pOptions,
      ImmutableSet<MPORThread> pOtherThreads,
      ImmutableSet<SeqMemoryLocation> pDirectReadMemoryLocations,
      ImmutableSet<SeqMemoryLocation> pDirectWriteMemoryLocations,
      BitVectorVariables pBitVectorVariables,
      MemoryModel pMemoryModel,
      SequentializationUtils pUtils)
      throws UnrecognizedCodeException {

    return switch (pOptions.bitVectorEncoding()) {
      case NONE ->
          throw new IllegalArgumentException(
              String.format(
                  "cannot build evaluation for encoding %s", pOptions.bitVectorEncoding()));
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
            pUtils);
      }
      case SPARSE -> {
        ImmutableListMultimap<SeqMemoryLocation, CExpression> sparseWriteMap =
            BitVectorEvaluationUtil.mapMemoryLocationsToSparseBitVectorsByAccessType(
                pOtherThreads, pBitVectorVariables, MemoryAccessType.WRITE);
        ImmutableListMultimap<SeqMemoryLocation, CExpression> sparseAccessMap =
            BitVectorEvaluationUtil.mapMemoryLocationsToSparseBitVectorsByAccessType(
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
}
