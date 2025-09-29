// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqDeclarationBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqInitializers.SeqInitializer;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryAccessType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryLocation;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryModel;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.ReachType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqNameUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;

public class BitVectorBuilder {

  // Bit Vectors ===================================================================================

  public static Optional<BitVectorVariables> buildBitVectorVariables(
      MPOROptions pOptions,
      ImmutableList<MPORThread> pThreads,
      Optional<MemoryModel> pMemoryModel) {

    if (!pOptions.areBitVectorsEnabled()) {
      // no bit vector reduction -> no bit vector variables
      return Optional.empty();
    }
    ImmutableMap<MemoryLocation, Integer> relevantMemoryLocations =
        pMemoryModel.orElseThrow().getRelevantMemoryLocations();
    return switch (pOptions.reductionMode) {
      case NONE ->
          throw new IllegalArgumentException(
              "reductionMode is not set, cannot build bit vector variables");
      case ACCESS_ONLY ->
          buildAccessOnlyBitVectorVariables(pOptions, pThreads, relevantMemoryLocations);
      case READ_AND_WRITE ->
          buildReadWriteBitVectorVariables(pOptions, pThreads, relevantMemoryLocations);
    };
  }

  private static Optional<BitVectorVariables> buildAccessOnlyBitVectorVariables(
      MPOROptions pOptions,
      ImmutableList<MPORThread> pThreads,
      ImmutableMap<MemoryLocation, Integer> pRelevantMemoryLocations) {

    // create bit vector access variables for all threads, e.g. __uint8_t ba0
    Optional<ImmutableSet<DenseBitVector>> denseAccessBitVectors =
        buildDenseBitVectorsByAccessType(pOptions, pThreads, MemoryAccessType.ACCESS);
    // create access variables for all global variables for all threads (for sparse bit vectors)
    Optional<ImmutableMap<MemoryLocation, SparseBitVector>> sparseAccessBitVectors =
        buildSparseBitVectors(
            pOptions, pThreads, pRelevantMemoryLocations, MemoryAccessType.ACCESS);
    // last bit vector used to store the bit vector of a thread before context switch
    Optional<LastDenseBitVector> lastDenseAccessBitVector =
        tryBuildLastDenseBitVectorByAccessType(pOptions, MemoryAccessType.ACCESS);
    Optional<ImmutableMap<MemoryLocation, LastSparseBitVector>> lastSparseAccessBitVectors =
        tryBuildLastSparseBitVectorsByAccessType(
            pOptions, pRelevantMemoryLocations, MemoryAccessType.ACCESS);
    return Optional.of(
        new BitVectorVariables(
            denseAccessBitVectors,
            Optional.empty(),
            Optional.empty(),
            sparseAccessBitVectors,
            Optional.empty(),
            Optional.empty(),
            lastDenseAccessBitVector,
            Optional.empty(),
            lastSparseAccessBitVectors,
            Optional.empty()));
  }

  private static Optional<BitVectorVariables> buildReadWriteBitVectorVariables(
      MPOROptions pOptions,
      ImmutableList<MPORThread> pThreads,
      ImmutableMap<MemoryLocation, Integer> pRelevantMemoryLocations) {

    // create bit vector read + write variables for all threads, e.g. __uint8_t br0, bw0
    Optional<ImmutableSet<DenseBitVector>> denseAccessBitVectors =
        buildDenseBitVectorsByAccessType(pOptions, pThreads, MemoryAccessType.ACCESS);
    Optional<ImmutableSet<DenseBitVector>> denseReadBitVectors =
        buildDenseBitVectorsByAccessType(pOptions, pThreads, MemoryAccessType.READ);
    Optional<ImmutableSet<DenseBitVector>> denseWriteBitVectors =
        buildDenseBitVectorsByAccessType(pOptions, pThreads, MemoryAccessType.WRITE);
    // create read + write variables (for sparse bit vectors)
    Optional<ImmutableMap<MemoryLocation, SparseBitVector>> sparseAccessBitVectors =
        buildSparseBitVectors(
            pOptions, pThreads, pRelevantMemoryLocations, MemoryAccessType.ACCESS);
    Optional<ImmutableMap<MemoryLocation, SparseBitVector>> sparseReadBitVectors =
        buildSparseBitVectors(pOptions, pThreads, pRelevantMemoryLocations, MemoryAccessType.READ);
    Optional<ImmutableMap<MemoryLocation, SparseBitVector>> sparseWriteBitVectors =
        buildSparseBitVectors(pOptions, pThreads, pRelevantMemoryLocations, MemoryAccessType.WRITE);
    // last bit vector used to store the bit vector of a thread before context switch
    Optional<LastDenseBitVector> lastDenseAccessBitVector =
        tryBuildLastDenseBitVectorByAccessType(pOptions, MemoryAccessType.ACCESS);
    Optional<LastDenseBitVector> lastDenseWriteBitVector =
        tryBuildLastDenseBitVectorByAccessType(pOptions, MemoryAccessType.WRITE);
    Optional<ImmutableMap<MemoryLocation, LastSparseBitVector>> lastSparseAccessBitVectors =
        tryBuildLastSparseBitVectorsByAccessType(
            pOptions, pRelevantMemoryLocations, MemoryAccessType.ACCESS);
    Optional<ImmutableMap<MemoryLocation, LastSparseBitVector>> lastSparseWriteBitVectors =
        tryBuildLastSparseBitVectorsByAccessType(
            pOptions, pRelevantMemoryLocations, MemoryAccessType.WRITE);
    return Optional.of(
        new BitVectorVariables(
            denseAccessBitVectors,
            denseReadBitVectors,
            denseWriteBitVectors,
            sparseAccessBitVectors,
            sparseReadBitVectors,
            sparseWriteBitVectors,
            lastDenseAccessBitVector,
            lastDenseWriteBitVector,
            lastSparseAccessBitVectors,
            lastSparseWriteBitVectors));
  }

  // Dense / Sparse Bit Vectors ====================================================================

  private static Optional<ImmutableSet<DenseBitVector>> buildDenseBitVectorsByAccessType(
      MPOROptions pOptions, ImmutableList<MPORThread> pThreads, MemoryAccessType pAccessType) {

    if (!pOptions.bitVectorEncoding.isDense) {
      return Optional.empty();
    }
    ImmutableSet.Builder<DenseBitVector> rBitVectors = ImmutableSet.builder();
    for (MPORThread thread : pThreads) {
      Optional<CIdExpression> directVariable =
          buildDenseBitVector(pOptions, thread, pAccessType, ReachType.DIRECT);
      Optional<CIdExpression> reachableVariable =
          buildDenseBitVector(pOptions, thread, pAccessType, ReachType.REACHABLE);
      rBitVectors.add(new DenseBitVector(thread, directVariable, reachableVariable));
    }
    return Optional.of(rBitVectors.build());
  }

  private static Optional<CIdExpression> buildDenseBitVector(
      MPOROptions pOptions,
      MPORThread pThread,
      MemoryAccessType pAccessType,
      ReachType pReachType) {

    return switch (pReachType) {
      case DIRECT -> {
        if (pOptions.kIgnoreZeroReduction) {
          yield Optional.of(
              buildBitVectorIdExpression(
                  pOptions,
                  Optional.of(pThread),
                  Optional.empty(),
                  pAccessType,
                  ReachType.DIRECT,
                  BitVectorDirection.CURRENT));
        }
        yield Optional.empty();
      }
      case REACHABLE -> {
        if (!pAccessType.equals(MemoryAccessType.READ)) {
          yield Optional.of(
              buildBitVectorIdExpression(
                  pOptions,
                  Optional.of(pThread),
                  Optional.empty(),
                  pAccessType,
                  ReachType.REACHABLE,
                  BitVectorDirection.CURRENT));
        }
        // we never need reachable read bit vectors
        yield Optional.empty();
      }
    };
  }

  private static Optional<ImmutableMap<MemoryLocation, SparseBitVector>> buildSparseBitVectors(
      MPOROptions pOptions,
      ImmutableList<MPORThread> pThreads,
      ImmutableMap<MemoryLocation, Integer> pRelevantMemoryLocations,
      MemoryAccessType pAccessType) {

    if (pOptions.bitVectorEncoding.isDense) {
      return Optional.empty();
    }
    ImmutableMap.Builder<MemoryLocation, SparseBitVector> rAccessVariables = ImmutableMap.builder();
    for (MemoryLocation memoryLocation : pRelevantMemoryLocations.keySet()) {
      ImmutableMap<MPORThread, CIdExpression> directVariables =
          buildSparseBitVectors(pOptions, pThreads, memoryLocation, pAccessType, ReachType.DIRECT);
      ImmutableMap<MPORThread, CIdExpression> reachableVariables =
          buildSparseBitVectors(
              pOptions, pThreads, memoryLocation, pAccessType, ReachType.REACHABLE);
      rAccessVariables.put(
          memoryLocation, new SparseBitVector(directVariables, reachableVariables, pAccessType));
    }
    return Optional.of(rAccessVariables.buildOrThrow());
  }

  private static ImmutableMap<MPORThread, CIdExpression> buildSparseBitVectors(
      MPOROptions pOptions,
      ImmutableList<MPORThread> pThreads,
      MemoryLocation pMemoryLocation,
      MemoryAccessType pAccessType,
      ReachType pReachType) {

    ImmutableMap.Builder<MPORThread, CIdExpression> rAccessVariables = ImmutableMap.builder();
    for (MPORThread thread : pThreads) {
      rAccessVariables.put(
          thread,
          buildBitVectorIdExpression(
              pOptions,
              Optional.of(thread),
              Optional.of(pMemoryLocation),
              pAccessType,
              pReachType,
              BitVectorDirection.CURRENT));
    }
    return rAccessVariables.buildOrThrow();
  }

  // Last Bit Vectors ==============================================================================

  private static Optional<LastDenseBitVector> tryBuildLastDenseBitVectorByAccessType(
      MPOROptions pOptions, MemoryAccessType pAccessType) {

    if (!pOptions.conflictReduction || pOptions.bitVectorEncoding.isSparse) {
      return Optional.empty();
    }
    CIdExpression lastIdExpression =
        buildBitVectorIdExpression(
            pOptions,
            Optional.empty(),
            Optional.empty(),
            pAccessType,
            ReachType.REACHABLE,
            BitVectorDirection.LAST);
    return Optional.of(new LastDenseBitVector(lastIdExpression));
  }

  private static Optional<ImmutableMap<MemoryLocation, LastSparseBitVector>>
      tryBuildLastSparseBitVectorsByAccessType(
          MPOROptions pOptions,
          ImmutableMap<MemoryLocation, Integer> pRelevantMemoryLocations,
          MemoryAccessType pAccessType) {

    if (!pOptions.conflictReduction || pOptions.bitVectorEncoding.isDense) {
      return Optional.empty();
    }
    ImmutableMap.Builder<MemoryLocation, LastSparseBitVector> rMap = ImmutableMap.builder();
    for (MemoryLocation memoryLocation : pRelevantMemoryLocations.keySet()) {
      CIdExpression lastIdExpression =
          buildBitVectorIdExpression(
              pOptions,
              Optional.empty(),
              Optional.of(memoryLocation),
              pAccessType,
              ReachType.REACHABLE,
              BitVectorDirection.LAST);
      rMap.put(memoryLocation, new LastSparseBitVector(lastIdExpression));
    }
    return Optional.of(rMap.buildOrThrow());
  }

  // Helper ========================================================================================

  public static CIdExpression buildBitVectorIdExpression(
      MPOROptions pOptions,
      Optional<MPORThread> pThread,
      Optional<MemoryLocation> pMemoryLocation,
      MemoryAccessType pAccessType,
      ReachType pReachType,
      BitVectorDirection pDirection) {

    checkArgument(
        !pOptions.bitVectorEncoding.isSparse || pMemoryLocation.isPresent(),
        "if the bitVectorEncoding is sparse, then pMemoryLocation must be present");

    String name =
        SeqNameUtil.buildBitVectorName(
            pOptions, pThread, pMemoryLocation, pAccessType, pReachType, pDirection);
    // this declaration is not actually used, we only need it for the CIdExpression
    CSimpleDeclaration declaration =
        SeqDeclarationBuilder.buildVariableDeclaration(
            true, CNumericTypes.UNSIGNED_CHAR, name, SeqInitializer.INT_0);
    return SeqExpressionBuilder.buildIdExpression(declaration);
  }
}
