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
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.memory_model.MemoryAccessType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.memory_model.MemoryModel;
import org.sosy_lab.cpachecker.core.algorithm.mpor.memory_model.ReachType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.memory_model.SeqMemoryLocation;
import org.sosy_lab.cpachecker.core.algorithm.mpor.memory_model.SeqMemoryLocationFinder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SequentializationUtils;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqThreadStatementBlock;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.SeqBitVectorVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.SeqBitVectorVariables.PrevDenseBitVector;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.cwriter.export.CExportExpression;

public class BitVectorEvaluationBuilder {

  // variable only i.e. no literal expressions =====================================================

  public static Optional<CExportExpression> buildVariableOnlyEvaluation(
      MPOROptions pOptions,
      MPORThread pActiveThread,
      ImmutableSet<MPORThread> pOtherThreads,
      SeqBitVectorVariables pBitVectorVariables,
      SequentializationUtils pUtils)
      throws UnrecognizedCodeException {

    return switch (pOptions.partialOrderReductionMode()) {
      case NONE ->
          throw new IllegalArgumentException(
              String.format(
                  "cannot build evaluation for partialOrderReductionMode %s",
                  pOptions.partialOrderReductionMode()));
      case ACCESS_ONLY ->
          BitVectorAccessEvaluationBuilder.buildVariableOnlyEvaluationByEncoding(
              pOptions, pActiveThread, pOtherThreads, pBitVectorVariables, pUtils);
      case READ_AND_WRITE ->
          BitVectorReadWriteEvaluationBuilder.buildVariableOnlyEvaluationByEncoding(
              pOptions, pActiveThread, pOtherThreads, pBitVectorVariables, pUtils);
    };
  }

  // prev bit vector evaluations ===================================================================

  public static Optional<CExportExpression> buildPrevBitVectorEvaluation(
      MPOROptions pOptions,
      MPORThread pThread,
      ImmutableMap<Integer, SeqThreadStatementClause> pLabelClauseMap,
      ImmutableMap<Integer, SeqThreadStatementBlock> pLabelBlockMap,
      SeqThreadStatementBlock pFirstBlock,
      SeqBitVectorVariables pBitVectorVariables,
      MemoryModel pMemoryModel,
      SequentializationUtils pUtils)
      throws UnrecognizedCodeException {

    checkArgument(
        pOptions.abortCommutingContextSwitches(), "abortCommutingContextSwitches must be enabled");

    return switch (pOptions.partialOrderReductionMode()) {
      case NONE ->
          throw new IllegalArgumentException(
              String.format(
                  "cannot build evaluation for partialOrderReductionMode %s",
                  pOptions.partialOrderReductionMode()));
      case ACCESS_ONLY -> {
        ImmutableSet<SeqMemoryLocation> reachableAccessMemoryLocations =
            SeqMemoryLocationFinder.findReachableMemoryLocationsByAccessType(
                pLabelClauseMap,
                pLabelBlockMap,
                pFirstBlock,
                pMemoryModel,
                MemoryAccessType.ACCESS);
        yield buildPrevAccessBitVectorEvaluationByEncoding(
            pOptions, pThread, reachableAccessMemoryLocations, pBitVectorVariables, pUtils);
      }
      case READ_AND_WRITE -> {
        ImmutableSet<SeqMemoryLocation> reachableReadMemoryLocations =
            SeqMemoryLocationFinder.findReachableMemoryLocationsByAccessType(
                pLabelClauseMap, pLabelBlockMap, pFirstBlock, pMemoryModel, MemoryAccessType.READ);
        ImmutableSet<SeqMemoryLocation> reachableWriteMemoryLocations =
            SeqMemoryLocationFinder.findReachableMemoryLocationsByAccessType(
                pLabelClauseMap, pLabelBlockMap, pFirstBlock, pMemoryModel, MemoryAccessType.WRITE);
        yield buildPrevReadWriteBitVectorEvaluationByEncoding(
            pOptions,
            pThread,
            reachableReadMemoryLocations,
            reachableWriteMemoryLocations,
            pBitVectorVariables,
            pUtils);
      }
    };
  }

  private static Optional<CExportExpression> buildPrevAccessBitVectorEvaluationByEncoding(
      MPOROptions pOptions,
      MPORThread pCurrentThread,
      ImmutableSet<SeqMemoryLocation> pReachableAccessMemoryLocations,
      SeqBitVectorVariables pBitVectorVariables,
      SequentializationUtils pUtils)
      throws UnrecognizedCodeException {

    return switch (pOptions.bitVectorEncoding()) {
      case NONE ->
          throw new IllegalArgumentException(
              String.format(
                  "cannot build evaluation for encoding %s", pOptions.bitVectorEncoding()));
      case BINARY, OCTAL, DECIMAL, HEXADECIMAL -> {
        // get the bit vector that stores the direct accesses of the previous thread
        PrevDenseBitVector prevDirectAccessBitVector =
            pBitVectorVariables.getPrevDenseBitVectorByAccessType(MemoryAccessType.ACCESS);

        // get the reachable memory location accesses of the current thread
        CIdExpression currentReachableAccessBitVector =
            pBitVectorVariables.getDenseBitVector(
                pCurrentThread, MemoryAccessType.ACCESS, ReachType.REACHABLE);

        yield Optional.of(
            BitVectorAccessEvaluationBuilder.buildFullDenseBinaryAnd(
                prevDirectAccessBitVector.directVariable(),
                ImmutableSet.of(currentReachableAccessBitVector),
                pUtils));
      }
      case SPARSE -> {
        // get the bit vector that stores the direct accesses of the previous thread
        ImmutableMap<SeqMemoryLocation, CExpression> prevDirectAccessBitVector =
            BitVectorEvaluationUtil.buildPrevSparseLeftHandSidesByAccessType(
                MemoryAccessType.ACCESS, pBitVectorVariables);

        // get the reachable memory location accesses of the current thread
        ImmutableListMultimap<SeqMemoryLocation, CExpression> currentReachableAccessBitVector =
            BitVectorEvaluationUtil.mapMemoryLocationsToSparseBitVectorsByAccessType(
                ImmutableSet.of(pCurrentThread), pBitVectorVariables, MemoryAccessType.ACCESS);

        yield BitVectorAccessEvaluationBuilder.buildSparseEvaluation(
            pOptions,
            prevDirectAccessBitVector,
            currentReachableAccessBitVector,
            pReachableAccessMemoryLocations,
            pBitVectorVariables);
      }
    };
  }

  private static Optional<CExportExpression> buildPrevReadWriteBitVectorEvaluationByEncoding(
      MPOROptions pOptions,
      MPORThread pCurrentThread,
      ImmutableSet<SeqMemoryLocation> pDirectReadMemoryLocations,
      ImmutableSet<SeqMemoryLocation> pDirectWriteMemoryLocations,
      SeqBitVectorVariables pBitVectorVariables,
      SequentializationUtils pUtils)
      throws UnrecognizedCodeException {

    return switch (pOptions.bitVectorEncoding()) {
      case NONE ->
          throw new IllegalArgumentException(
              String.format(
                  "cannot build evaluation for encoding %s", pOptions.bitVectorEncoding()));
      case BINARY, OCTAL, DECIMAL, HEXADECIMAL -> {
        // get the direct memory location accesses of the current thread
        CIdExpression currentReachableWriteBitVector =
            pBitVectorVariables.getDenseBitVector(
                pCurrentThread, MemoryAccessType.WRITE, ReachType.REACHABLE);
        CIdExpression currentReachableAccessBitVector =
            pBitVectorVariables.getDenseBitVector(
                pCurrentThread, MemoryAccessType.ACCESS, ReachType.REACHABLE);

        // get the bit vectors that store the reachable writes / accesses of the previous thread
        PrevDenseBitVector prevDirectReadBitVector =
            pBitVectorVariables.getPrevDenseBitVectorByAccessType(MemoryAccessType.READ);
        PrevDenseBitVector prevDirectWriteBitVector =
            pBitVectorVariables.getPrevDenseBitVectorByAccessType(MemoryAccessType.WRITE);

        yield Optional.of(
            BitVectorReadWriteEvaluationBuilder.buildFullDenseLogicalOr(
                prevDirectReadBitVector.directVariable(),
                prevDirectWriteBitVector.directVariable(),
                ImmutableSet.of(currentReachableWriteBitVector),
                ImmutableSet.of(currentReachableAccessBitVector),
                pUtils));
      }
      case SPARSE -> {
        ImmutableListMultimap<SeqMemoryLocation, CExpression> sparseWriteMap =
            BitVectorEvaluationUtil.mapMemoryLocationsToSparseBitVectorsByAccessType(
                ImmutableSet.of(pCurrentThread), pBitVectorVariables, MemoryAccessType.WRITE);
        ImmutableListMultimap<SeqMemoryLocation, CExpression> sparseAccessMap =
            BitVectorEvaluationUtil.mapMemoryLocationsToSparseBitVectorsByAccessType(
                ImmutableSet.of(pCurrentThread), pBitVectorVariables, MemoryAccessType.ACCESS);

        yield BitVectorReadWriteEvaluationBuilder.buildPrevSparseEvaluation(
            pOptions,
            sparseWriteMap,
            sparseAccessMap,
            pDirectReadMemoryLocations,
            pDirectWriteMemoryLocations,
            pBitVectorVariables);
      }
    };
  }

  // bit vector evaluations by accessed global variables (bit vector reduction) ====================

  public static Optional<CExportExpression> buildEvaluationByDirectVariableAccesses(
      MPOROptions pOptions,
      ImmutableSet<MPORThread> pOtherThreads,
      ImmutableMap<Integer, SeqThreadStatementBlock> pLabelBlockMap,
      SeqThreadStatementBlock pTargetBlock,
      SeqBitVectorVariables pBitVectorVariables,
      MachineModel pMachineModel,
      MemoryModel pMemoryModel,
      SequentializationUtils pUtils)
      throws UnrecognizedCodeException {

    checkArgument(
        pOptions.executeThreadsUntilConflict(),
        "executeThreadsUntilConflict must be enabled to build evaluation expression");

    return switch (pOptions.partialOrderReductionMode()) {
      case NONE ->
          throw new IllegalArgumentException(
              String.format(
                  "cannot build evaluation for partialOrderReductionMode %s",
                  pOptions.partialOrderReductionMode()));
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
            pMachineModel,
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
            pMachineModel,
            pMemoryModel,
            pUtils);
      }
    };
  }

  private static Optional<CExportExpression> buildEvaluationByReduction(
      MPOROptions pOptions,
      ImmutableSet<MPORThread> pOtherThreads,
      ImmutableSet<SeqMemoryLocation> pDirectAccessMemoryLocations,
      ImmutableSet<SeqMemoryLocation> pDirectReadMemoryLocations,
      ImmutableSet<SeqMemoryLocation> pDirectWriteMemoryLocations,
      SeqBitVectorVariables pBitVectorVariables,
      MachineModel pMachineModel,
      MemoryModel pMemoryModel,
      SequentializationUtils pUtils)
      throws UnrecognizedCodeException {

    return switch (pOptions.partialOrderReductionMode()) {
      case NONE ->
          throw new IllegalArgumentException(
              String.format(
                  "cannot build evaluation for partialOrderReductionMode %s",
                  pOptions.partialOrderReductionMode()));
      case ACCESS_ONLY ->
          buildAccessEvaluationByEncoding(
              pOptions,
              pOtherThreads,
              pDirectAccessMemoryLocations,
              pBitVectorVariables,
              pMachineModel,
              pMemoryModel,
              pUtils);
      case READ_AND_WRITE ->
          buildReadWriteEvaluationByEncoding(
              pOptions,
              pOtherThreads,
              pDirectReadMemoryLocations,
              pDirectWriteMemoryLocations,
              pBitVectorVariables,
              pMachineModel,
              pMemoryModel,
              pUtils);
    };
  }

  private static Optional<CExportExpression> buildAccessEvaluationByEncoding(
      MPOROptions pOptions,
      ImmutableSet<MPORThread> pOtherThreads,
      ImmutableSet<SeqMemoryLocation> pDirectAccessMemoryLocations,
      SeqBitVectorVariables pBitVectorVariables,
      MachineModel pMachineModel,
      MemoryModel pMemoryModel,
      SequentializationUtils pUtils)
      throws UnrecognizedCodeException {

    return switch (pOptions.bitVectorEncoding()) {
      case NONE ->
          throw new IllegalArgumentException(
              String.format(
                  "cannot build evaluation for encoding %s", pOptions.bitVectorEncoding()));
      case BINARY, OCTAL, DECIMAL, HEXADECIMAL -> {
        ImmutableSet<CExpression> otherBitVectors =
            pBitVectorVariables.getOtherDenseReachableBitVectorsByAccessType(
                MemoryAccessType.ACCESS, pOtherThreads);
        yield BitVectorAccessEvaluationBuilder.buildDenseEvaluation(
            pOptions,
            otherBitVectors,
            pDirectAccessMemoryLocations,
            pMachineModel,
            pMemoryModel,
            pUtils);
      }
      case SPARSE -> {
        ImmutableListMultimap<SeqMemoryLocation, CExpression> rightHandSides =
            BitVectorEvaluationUtil.mapMemoryLocationsToSparseBitVectorsByAccessType(
                pOtherThreads, pBitVectorVariables, MemoryAccessType.ACCESS);
        ImmutableMap<SeqMemoryLocation, CExpression> leftHandSides =
            BitVectorEvaluationUtil.buildSparseLeftHandSidesByAccessType(
                pDirectAccessMemoryLocations, MemoryAccessType.ACCESS, pBitVectorVariables);
        yield BitVectorAccessEvaluationBuilder.buildSparseEvaluation(
            pOptions,
            leftHandSides,
            rightHandSides,
            pDirectAccessMemoryLocations,
            pBitVectorVariables);
      }
    };
  }

  private static Optional<CExportExpression> buildReadWriteEvaluationByEncoding(
      MPOROptions pOptions,
      ImmutableSet<MPORThread> pOtherThreads,
      ImmutableSet<SeqMemoryLocation> pDirectReadMemoryLocations,
      ImmutableSet<SeqMemoryLocation> pDirectWriteMemoryLocations,
      SeqBitVectorVariables pBitVectorVariables,
      MachineModel pMachineModel,
      MemoryModel pMemoryModel,
      SequentializationUtils pUtils)
      throws UnrecognizedCodeException {

    return switch (pOptions.bitVectorEncoding()) {
      case NONE ->
          throw new IllegalArgumentException(
              String.format(
                  "cannot build evaluation for encoding %s", pOptions.bitVectorEncoding()));
      case BINARY, OCTAL, DECIMAL, HEXADECIMAL -> {
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
            pMachineModel,
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
