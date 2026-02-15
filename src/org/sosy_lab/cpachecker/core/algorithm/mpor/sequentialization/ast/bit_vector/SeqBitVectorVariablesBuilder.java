// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.bit_vector;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.bit_vector.SeqBitVectorVariables.DenseBitVector;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.bit_vector.SeqBitVectorVariables.LastDenseBitVector;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.bit_vector.SeqBitVectorVariables.LastSparseBitVector;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.bit_vector.SeqBitVectorVariables.SparseBitVector;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqDeclarationBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqInitializers;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryAccessType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryModel;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.ReachType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.SeqMemoryLocation;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqNameUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.CFAEdgeForThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;

public record SeqBitVectorVariablesBuilder(
    MPOROptions options,
    ImmutableList<MPORThread> threads,
    ImmutableMap<CFAEdgeForThread, SubstituteEdge> substituteEdges,
    MemoryModel memoryModel) {

  public Optional<SeqBitVectorVariables> buildBitVectorVariables() {
    return switch (options.reductionMode()) {
      case NONE ->
          throw new IllegalArgumentException(
              "reductionMode is not set, cannot build bit vector variables");
      case ACCESS_ONLY -> buildAccessOnlyBitVectorVariables();
      case READ_AND_WRITE -> buildReadWriteBitVectorVariables();
    };
  }

  private Optional<SeqBitVectorVariables> buildAccessOnlyBitVectorVariables() {
    // create bit vector access variables for all threads, e.g. __uint8_t ba0
    Optional<ImmutableSet<DenseBitVector>> denseAccessBitVectors =
        buildDenseBitVectorsByAccessType(MemoryAccessType.ACCESS);
    // create access variables for all global variables for all threads (for sparse bit vectors)
    Optional<ImmutableMap<SeqMemoryLocation, SparseBitVector>> sparseAccessBitVectors =
        buildSparseBitVectors(MemoryAccessType.ACCESS);
    // last bit vector used to store the bit vector of a thread before context switch
    Optional<LastDenseBitVector> lastDenseAccessBitVector =
        tryBuildLastDenseBitVectorByAccessType(MemoryAccessType.ACCESS);
    Optional<ImmutableMap<SeqMemoryLocation, LastSparseBitVector>> lastSparseAccessBitVectors =
        tryBuildLastSparseBitVectorsByAccessType(MemoryAccessType.ACCESS);
    return Optional.of(
        new SeqBitVectorVariables(
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

  private Optional<SeqBitVectorVariables> buildReadWriteBitVectorVariables() {

    // create bit vector read + write variables for all threads, e.g. __uint8_t br0, bw0
    Optional<ImmutableSet<DenseBitVector>> denseAccessBitVectors =
        buildDenseBitVectorsByAccessType(MemoryAccessType.ACCESS);
    Optional<ImmutableSet<DenseBitVector>> denseReadBitVectors =
        buildDenseBitVectorsByAccessType(MemoryAccessType.READ);
    Optional<ImmutableSet<DenseBitVector>> denseWriteBitVectors =
        buildDenseBitVectorsByAccessType(MemoryAccessType.WRITE);

    // create read + write variables (for sparse bit vectors)
    Optional<ImmutableMap<SeqMemoryLocation, SparseBitVector>> sparseAccessBitVectors =
        buildSparseBitVectors(MemoryAccessType.ACCESS);
    Optional<ImmutableMap<SeqMemoryLocation, SparseBitVector>> sparseReadBitVectors =
        buildSparseBitVectors(MemoryAccessType.READ);
    Optional<ImmutableMap<SeqMemoryLocation, SparseBitVector>> sparseWriteBitVectors =
        buildSparseBitVectors(MemoryAccessType.WRITE);

    // last bit vector used to store the bit vector of a thread before context switch
    Optional<LastDenseBitVector> lastDenseAccessBitVector =
        tryBuildLastDenseBitVectorByAccessType(MemoryAccessType.ACCESS);
    Optional<LastDenseBitVector> lastDenseWriteBitVector =
        tryBuildLastDenseBitVectorByAccessType(MemoryAccessType.WRITE);
    Optional<ImmutableMap<SeqMemoryLocation, LastSparseBitVector>> lastSparseAccessBitVectors =
        tryBuildLastSparseBitVectorsByAccessType(MemoryAccessType.ACCESS);
    Optional<ImmutableMap<SeqMemoryLocation, LastSparseBitVector>> lastSparseWriteBitVectors =
        tryBuildLastSparseBitVectorsByAccessType(MemoryAccessType.WRITE);

    return Optional.of(
        new SeqBitVectorVariables(
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

  private Optional<ImmutableSet<DenseBitVector>> buildDenseBitVectorsByAccessType(
      MemoryAccessType pAccessType) {

    if (!options.bitVectorEncoding().isDense) {
      return Optional.empty();
    }
    ImmutableSet.Builder<DenseBitVector> rBitVectors = ImmutableSet.builder();
    for (MPORThread thread : threads) {
      Optional<CIdExpression> directVariable =
          buildDenseBitVector(thread, pAccessType, ReachType.DIRECT);
      Optional<CIdExpression> reachableVariable =
          buildDenseBitVector(thread, pAccessType, ReachType.REACHABLE);
      rBitVectors.add(new DenseBitVector(thread, directVariable, reachableVariable));
    }
    return Optional.of(rBitVectors.build());
  }

  private Optional<CIdExpression> buildDenseBitVector(
      MPORThread pThread, MemoryAccessType pAccessType, ReachType pReachType) {

    if (!SeqBitVectorUtil.isAccessReachPairNeeded(
        options.reduceIgnoreSleep(), options.reductionMode(), pAccessType, pReachType)) {
      return Optional.empty();
    }
    return switch (pReachType) {
      case DIRECT ->
          Optional.of(
              buildBitVectorIdExpression(
                  Optional.of(pThread),
                  Optional.empty(),
                  pAccessType,
                  ReachType.DIRECT,
                  SeqBitVectorDirection.CURRENT));
      case REACHABLE ->
          Optional.of(
              buildBitVectorIdExpression(
                  Optional.of(pThread),
                  Optional.empty(),
                  pAccessType,
                  ReachType.REACHABLE,
                  SeqBitVectorDirection.CURRENT));
    };
  }

  private Optional<ImmutableMap<SeqMemoryLocation, SparseBitVector>> buildSparseBitVectors(
      MemoryAccessType pAccessType) {

    if (options.bitVectorEncoding().isDense) {
      return Optional.empty();
    }
    ImmutableMap.Builder<SeqMemoryLocation, SparseBitVector> rAccessVariables =
        ImmutableMap.builder();
    for (SeqMemoryLocation memoryLocation : memoryModel.getRelevantMemoryLocations().keySet()) {
      ImmutableMap<MPORThread, CIdExpression> directVariables =
          buildSparseBitVectors(memoryLocation, pAccessType, ReachType.DIRECT);
      ImmutableMap<MPORThread, CIdExpression> reachableVariables =
          buildSparseBitVectors(memoryLocation, pAccessType, ReachType.REACHABLE);
      rAccessVariables.put(
          memoryLocation, new SparseBitVector(directVariables, reachableVariables));
    }
    return Optional.of(rAccessVariables.buildOrThrow());
  }

  private ImmutableMap<MPORThread, CIdExpression> buildSparseBitVectors(
      SeqMemoryLocation pMemoryLocation, MemoryAccessType pAccessType, ReachType pReachType) {

    if (!SeqBitVectorUtil.isAccessReachPairNeeded(
        options.reduceIgnoreSleep(), options.reductionMode(), pAccessType, pReachType)) {
      return ImmutableMap.of();
    }
    ImmutableMap.Builder<MPORThread, CIdExpression> rAccessVariables = ImmutableMap.builder();
    for (MPORThread thread : threads) {
      CIdExpression idExpression =
          buildBitVectorIdExpression(
              Optional.of(thread),
              Optional.of(pMemoryLocation),
              pAccessType,
              pReachType,
              SeqBitVectorDirection.CURRENT);
      if (options.pruneSparseBitVectors()) {
        boolean isReachable =
            memoryModel.isMemoryLocationReachableByThread(
                pMemoryLocation, thread, substituteEdges, pAccessType);
        if (isReachable) {
          rAccessVariables.put(thread, idExpression);
        }
      } else {
        rAccessVariables.put(thread, idExpression);
      }
    }
    return rAccessVariables.buildOrThrow();
  }

  // Last Bit Vectors ==============================================================================

  private Optional<LastDenseBitVector> tryBuildLastDenseBitVectorByAccessType(
      MemoryAccessType pAccessType) {

    if (!options.reduceLastThreadOrder() || options.bitVectorEncoding().isSparse) {
      return Optional.empty();
    }
    CIdExpression lastIdExpression =
        buildBitVectorIdExpression(
            Optional.empty(),
            Optional.empty(),
            pAccessType,
            ReachType.REACHABLE,
            SeqBitVectorDirection.LAST);
    return Optional.of(new LastDenseBitVector(lastIdExpression));
  }

  private Optional<ImmutableMap<SeqMemoryLocation, LastSparseBitVector>>
      tryBuildLastSparseBitVectorsByAccessType(MemoryAccessType pAccessType) {

    if (!options.reduceLastThreadOrder() || options.bitVectorEncoding().isDense) {
      return Optional.empty();
    }
    ImmutableMap.Builder<SeqMemoryLocation, LastSparseBitVector> rMap = ImmutableMap.builder();
    for (SeqMemoryLocation memoryLocation : memoryModel.getRelevantMemoryLocations().keySet()) {
      CIdExpression lastIdExpression =
          buildBitVectorIdExpression(
              Optional.empty(),
              Optional.of(memoryLocation),
              pAccessType,
              ReachType.REACHABLE,
              SeqBitVectorDirection.LAST);
      rMap.put(memoryLocation, new LastSparseBitVector(lastIdExpression));
    }
    return Optional.of(rMap.buildOrThrow());
  }

  // Helper ========================================================================================

  public CIdExpression buildBitVectorIdExpression(
      Optional<MPORThread> pThread,
      Optional<SeqMemoryLocation> pMemoryLocation,
      MemoryAccessType pAccessType,
      ReachType pReachType,
      SeqBitVectorDirection pDirection) {

    checkArgument(
        !options.bitVectorEncoding().isSparse || pMemoryLocation.isPresent(),
        "if the bitVectorEncoding is sparse, then pMemoryLocation must be present");

    String name =
        SeqNameUtil.buildBitVectorName(
            options, pThread, pMemoryLocation, pAccessType, pReachType, pDirection);
    // this declaration is not actually used, we only need it for the CIdExpression
    CSimpleDeclaration declaration =
        SeqDeclarationBuilder.buildVariableDeclaration(
            true, CNumericTypes.UNSIGNED_CHAR, name, SeqInitializers.INT_0);
    return SeqExpressionBuilder.buildIdExpression(declaration);
  }
}
