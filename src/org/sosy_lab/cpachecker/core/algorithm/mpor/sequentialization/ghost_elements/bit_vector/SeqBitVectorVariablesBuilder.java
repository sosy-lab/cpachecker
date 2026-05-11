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
import org.sosy_lab.cpachecker.core.algorithm.mpor.pointer_aliasing.SeqMemoryAccessType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pointer_aliasing.SeqMemoryLocation;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pointer_aliasing.SeqMemoryReachType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pointer_aliasing.SeqPointerAliasingMap;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqDeclarationBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqInitializers;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.SeqBitVectorVariables.PrevDenseBitVector;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.SeqBitVectorVariables.PrevSparseBitVector;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.SeqBitVectorVariables.SeqDenseBitVector;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.SeqBitVectorVariables.SeqSparseBitVector;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqNameUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.CFAEdgeForThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;

public record SeqBitVectorVariablesBuilder(
    MPOROptions options,
    ImmutableList<MPORThread> threads,
    ImmutableMap<CFAEdgeForThread, SubstituteEdge> substituteEdges,
    SeqPointerAliasingMap pointerAliasingMap) {

  public Optional<SeqBitVectorVariables> buildBitVectorVariables() {
    return switch (options.partialOrderReductionMode()) {
      case NONE ->
          throw new IllegalArgumentException(
              "partialOrderReductionMode is not set, cannot build bit vector variables");
      case ACCESS_ONLY -> buildAccessOnlyBitVectorVariables();
      case READ_AND_WRITE -> buildReadWriteBitVectorVariables();
    };
  }

  private Optional<SeqBitVectorVariables> buildAccessOnlyBitVectorVariables() {
    // create bit vector access variables for all threads, e.g. __uint8_t ba0
    Optional<ImmutableSet<SeqDenseBitVector>> denseAccessBitVectors =
        buildDenseBitVectorsByAccessType(SeqMemoryAccessType.ACCESS);
    // create access variables for all global variables for all threads (for sparse bit vectors)
    Optional<ImmutableMap<SeqMemoryLocation, SeqSparseBitVector>> sparseAccessBitVectors =
        buildSparseBitVectors(SeqMemoryAccessType.ACCESS);
    // prev bit vector used to store the bit vector of a thread before context switch
    Optional<PrevDenseBitVector> prevDenseAccessBitVectors =
        tryBuildPrevDenseBitVectorByAccessType(SeqMemoryAccessType.ACCESS);
    Optional<ImmutableMap<SeqMemoryLocation, PrevSparseBitVector>> prevSparseAccessBitVectors =
        tryBuildPrevSparseBitVectorsByAccessType(SeqMemoryAccessType.ACCESS);
    return Optional.of(
        new SeqBitVectorVariables(
            denseAccessBitVectors,
            Optional.empty(),
            Optional.empty(),
            sparseAccessBitVectors,
            Optional.empty(),
            Optional.empty(),
            prevDenseAccessBitVectors,
            Optional.empty(),
            Optional.empty(),
            prevSparseAccessBitVectors,
            Optional.empty(),
            Optional.empty()));
  }

  private Optional<SeqBitVectorVariables> buildReadWriteBitVectorVariables() {

    // create bit vector read + write variables for all threads, e.g. __uint8_t br0, bw0
    Optional<ImmutableSet<SeqDenseBitVector>> denseAccessBitVectors =
        buildDenseBitVectorsByAccessType(SeqMemoryAccessType.ACCESS);
    Optional<ImmutableSet<SeqDenseBitVector>> denseReadBitVectors =
        buildDenseBitVectorsByAccessType(SeqMemoryAccessType.READ);
    Optional<ImmutableSet<SeqDenseBitVector>> denseWriteBitVectors =
        buildDenseBitVectorsByAccessType(SeqMemoryAccessType.WRITE);

    // create read + write variables (for sparse bit vectors)
    Optional<ImmutableMap<SeqMemoryLocation, SeqSparseBitVector>> sparseAccessBitVectors =
        buildSparseBitVectors(SeqMemoryAccessType.ACCESS);
    Optional<ImmutableMap<SeqMemoryLocation, SeqSparseBitVector>> sparseReadBitVectors =
        buildSparseBitVectors(SeqMemoryAccessType.READ);
    Optional<ImmutableMap<SeqMemoryLocation, SeqSparseBitVector>> sparseWriteBitVectors =
        buildSparseBitVectors(SeqMemoryAccessType.WRITE);

    // prev bit vector used to store the bit vector of a thread before context switch
    Optional<PrevDenseBitVector> prevDenseReadBitVector =
        tryBuildPrevDenseBitVectorByAccessType(SeqMemoryAccessType.READ);
    Optional<PrevDenseBitVector> prevDenseWriteBitVector =
        tryBuildPrevDenseBitVectorByAccessType(SeqMemoryAccessType.WRITE);
    Optional<ImmutableMap<SeqMemoryLocation, PrevSparseBitVector>> prevSparseReadBitVectors =
        tryBuildPrevSparseBitVectorsByAccessType(SeqMemoryAccessType.READ);
    Optional<ImmutableMap<SeqMemoryLocation, PrevSparseBitVector>> prevSparseWriteBitVectors =
        tryBuildPrevSparseBitVectorsByAccessType(SeqMemoryAccessType.WRITE);

    return Optional.of(
        new SeqBitVectorVariables(
            denseAccessBitVectors,
            denseReadBitVectors,
            denseWriteBitVectors,
            sparseAccessBitVectors,
            sparseReadBitVectors,
            sparseWriteBitVectors,
            Optional.empty(),
            prevDenseReadBitVector,
            prevDenseWriteBitVector,
            Optional.empty(),
            prevSparseReadBitVectors,
            prevSparseWriteBitVectors));
  }

  // Dense / Sparse Bit Vectors ====================================================================

  private Optional<ImmutableSet<SeqDenseBitVector>> buildDenseBitVectorsByAccessType(
      SeqMemoryAccessType pAccessType) {

    if (!options.bitVectorEncoding().isDense) {
      return Optional.empty();
    }
    ImmutableSet.Builder<SeqDenseBitVector> rBitVectors = ImmutableSet.builder();
    for (MPORThread thread : threads) {
      Optional<CIdExpression> directVariable =
          buildDenseBitVector(thread, pAccessType, SeqMemoryReachType.DIRECT);
      Optional<CIdExpression> reachableVariable =
          buildDenseBitVector(thread, pAccessType, SeqMemoryReachType.REACHABLE);
      rBitVectors.add(new SeqDenseBitVector(thread, directVariable, reachableVariable));
    }
    return Optional.of(rBitVectors.build());
  }

  private Optional<CIdExpression> buildDenseBitVector(
      MPORThread pThread, SeqMemoryAccessType pAccessType, SeqMemoryReachType pReachType) {

    if (!SeqBitVectorUtil.isAccessReachPairNeeded(options, pAccessType, pReachType)) {
      return Optional.empty();
    }
    return switch (pReachType) {
      case DIRECT ->
          Optional.of(
              buildBitVectorIdExpression(
                  Optional.of(pThread),
                  Optional.empty(),
                  pAccessType,
                  SeqMemoryReachType.DIRECT,
                  SeqBitVectorDirection.CURRENT));
      case REACHABLE ->
          Optional.of(
              buildBitVectorIdExpression(
                  Optional.of(pThread),
                  Optional.empty(),
                  pAccessType,
                  SeqMemoryReachType.REACHABLE,
                  SeqBitVectorDirection.CURRENT));
    };
  }

  private Optional<ImmutableMap<SeqMemoryLocation, SeqSparseBitVector>> buildSparseBitVectors(
      SeqMemoryAccessType pAccessType) {

    if (options.bitVectorEncoding().isDense) {
      return Optional.empty();
    }
    ImmutableMap.Builder<SeqMemoryLocation, SeqSparseBitVector> rAccessVariables =
        ImmutableMap.builder();
    for (SeqMemoryLocation memoryLocation :
        pointerAliasingMap.getRelevantMemoryLocations().keySet()) {
      ImmutableMap<MPORThread, CIdExpression> directVariables =
          buildSparseBitVectors(memoryLocation, pAccessType, SeqMemoryReachType.DIRECT);
      ImmutableMap<MPORThread, CIdExpression> reachableVariables =
          buildSparseBitVectors(memoryLocation, pAccessType, SeqMemoryReachType.REACHABLE);
      rAccessVariables.put(
          memoryLocation,
          new SeqSparseBitVector(
              options.pruneSparseBitVectors(), directVariables, reachableVariables));
    }
    return Optional.of(rAccessVariables.buildOrThrow());
  }

  private ImmutableMap<MPORThread, CIdExpression> buildSparseBitVectors(
      SeqMemoryLocation pMemoryLocation,
      SeqMemoryAccessType pAccessType,
      SeqMemoryReachType pReachType) {

    if (!SeqBitVectorUtil.isAccessReachPairNeeded(options, pAccessType, pReachType)) {
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
      if (!options.pruneSparseBitVectors()
          || pointerAliasingMap.isMemoryLocationReachableByThread(
              pMemoryLocation, thread, substituteEdges, pAccessType)) {
        rAccessVariables.put(thread, idExpression);
      }
    }
    return rAccessVariables.buildOrThrow();
  }

  // Prev Bit Vectors ==============================================================================

  private Optional<PrevDenseBitVector> tryBuildPrevDenseBitVectorByAccessType(
      SeqMemoryAccessType pAccessType) {

    if (!options.abortCommutingContextSwitches() || options.bitVectorEncoding().isSparse) {
      return Optional.empty();
    }
    CIdExpression prevIdExpression =
        buildBitVectorIdExpression(
            Optional.empty(),
            Optional.empty(),
            pAccessType,
            SeqMemoryReachType.DIRECT,
            SeqBitVectorDirection.PREVIOUS);
    return Optional.of(new PrevDenseBitVector(prevIdExpression));
  }

  private Optional<ImmutableMap<SeqMemoryLocation, PrevSparseBitVector>>
      tryBuildPrevSparseBitVectorsByAccessType(SeqMemoryAccessType pAccessType) {

    if (!options.abortCommutingContextSwitches() || options.bitVectorEncoding().isDense) {
      return Optional.empty();
    }
    ImmutableMap.Builder<SeqMemoryLocation, PrevSparseBitVector> rMap = ImmutableMap.builder();
    for (SeqMemoryLocation memoryLocation :
        pointerAliasingMap.getRelevantMemoryLocations().keySet()) {
      CIdExpression prevIdExpression =
          buildBitVectorIdExpression(
              Optional.empty(),
              Optional.of(memoryLocation),
              pAccessType,
              SeqMemoryReachType.DIRECT,
              SeqBitVectorDirection.PREVIOUS);
      rMap.put(memoryLocation, new PrevSparseBitVector(prevIdExpression));
    }
    return Optional.of(rMap.buildOrThrow());
  }

  // Helper ========================================================================================

  public CIdExpression buildBitVectorIdExpression(
      Optional<MPORThread> pThread,
      Optional<SeqMemoryLocation> pMemoryLocation,
      SeqMemoryAccessType pAccessType,
      SeqMemoryReachType pReachType,
      SeqBitVectorDirection pDirection) {

    checkArgument(
        !options.bitVectorEncoding().isSparse || pMemoryLocation.isPresent(),
        "If the bitVectorEncoding is SPARSE, then pMemoryLocation must be present.");
    checkArgument(
        !pDirection.equals(SeqBitVectorDirection.PREVIOUS)
            || pReachType.equals(SeqMemoryReachType.DIRECT),
        "If the SeqBitVectorDirection is PREVIOUS, then the ReachType must be DIRECT.");

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
