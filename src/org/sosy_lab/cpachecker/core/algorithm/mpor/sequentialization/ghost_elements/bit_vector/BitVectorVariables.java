// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryAccessType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.ReachType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.SeqMemoryLocation;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;

public class BitVectorVariables {

  private final Optional<ImmutableSet<DenseBitVector>> denseAccessBitVectors;

  private final Optional<ImmutableSet<DenseBitVector>> denseReadBitVectors;

  private final Optional<ImmutableSet<DenseBitVector>> denseWriteBitVectors;

  private final Optional<ImmutableMap<SeqMemoryLocation, SparseBitVector>> sparseAccessBitVectors;

  private final Optional<ImmutableMap<SeqMemoryLocation, SparseBitVector>> sparseReadBitVectors;

  private final Optional<ImmutableMap<SeqMemoryLocation, SparseBitVector>> sparseWriteBitVectors;

  private final Optional<LastDenseBitVector> lastDenseAccessBitVector;

  private final Optional<LastDenseBitVector> lastDenseWriteBitVector;

  private final Optional<ImmutableMap<SeqMemoryLocation, LastSparseBitVector>>
      lastSparseAccessBitVector;

  private final Optional<ImmutableMap<SeqMemoryLocation, LastSparseBitVector>>
      lastSparseWriteBitVector;

  public BitVectorVariables(
      Optional<ImmutableSet<DenseBitVector>> pDenseAccessBitVectors,
      Optional<ImmutableSet<DenseBitVector>> pDenseReadBitVectors,
      Optional<ImmutableSet<DenseBitVector>> pDenseWriteBitVectors,
      Optional<ImmutableMap<SeqMemoryLocation, SparseBitVector>> pSparseAccessBitVectors,
      Optional<ImmutableMap<SeqMemoryLocation, SparseBitVector>> pSparseReadBitVectors,
      Optional<ImmutableMap<SeqMemoryLocation, SparseBitVector>> pSparseWriteBitVectors,
      Optional<LastDenseBitVector> pLastDenseAccessBitVector,
      Optional<LastDenseBitVector> pLastDenseWriteBitVector,
      Optional<ImmutableMap<SeqMemoryLocation, LastSparseBitVector>> pLastSparseAccessBitVector,
      Optional<ImmutableMap<SeqMemoryLocation, LastSparseBitVector>> pLastSparseWriteBitVector) {

    denseAccessBitVectors = pDenseAccessBitVectors;
    denseReadBitVectors = pDenseReadBitVectors;
    denseWriteBitVectors = pDenseWriteBitVectors;
    sparseAccessBitVectors = pSparseAccessBitVectors;
    sparseReadBitVectors = pSparseReadBitVectors;
    sparseWriteBitVectors = pSparseWriteBitVectors;
    lastDenseAccessBitVector = pLastDenseAccessBitVector;
    lastDenseWriteBitVector = pLastDenseWriteBitVector;
    lastSparseAccessBitVector = pLastSparseAccessBitVector;
    lastSparseWriteBitVector = pLastSparseWriteBitVector;
  }

  public CIdExpression getDenseBitVector(
      MPORThread pThread, MemoryAccessType pAccessType, ReachType pReachType) {

    for (DenseBitVector denseBitVector : getDenseBitVectorsByAccessType(pAccessType)) {
      if (denseBitVector.getThread().equals(pThread)) {
        return denseBitVector.getVariableByReachType(pReachType);
      }
    }
    throw new IllegalArgumentException("could not find DenseBitVector");
  }

  public ImmutableSet<CExpression> getOtherDenseReachableBitVectorsByAccessType(
      MemoryAccessType pAccessType, ImmutableSet<MPORThread> pOtherThreads) {

    ImmutableSet.Builder<CExpression> rDenseBitVectors = ImmutableSet.builder();
    for (DenseBitVector denseBitVector : getDenseBitVectorsByAccessType(pAccessType)) {
      if (pOtherThreads.contains(denseBitVector.getThread())) {
        rDenseBitVectors.add(denseBitVector.getVariableByReachType(ReachType.REACHABLE));
      }
    }
    return rDenseBitVectors.build();
  }

  public ImmutableSet<DenseBitVector> getDenseBitVectorsByAccessType(MemoryAccessType pAccessType) {
    return switch (pAccessType) {
      case NONE -> ImmutableSet.of();
      case ACCESS -> denseAccessBitVectors.orElseThrow();
      case READ -> denseReadBitVectors.orElseThrow();
      case WRITE -> denseWriteBitVectors.orElseThrow();
    };
  }

  public ImmutableMap<SeqMemoryLocation, SparseBitVector> getSparseBitVectorByAccessType(
      MemoryAccessType pAccessType) {

    return switch (pAccessType) {
      case NONE -> throw new IllegalArgumentException("no NONE sparse bit vectors");
      case ACCESS -> sparseAccessBitVectors.orElseThrow();
      case READ -> sparseReadBitVectors.orElseThrow();
      case WRITE -> sparseWriteBitVectors.orElseThrow();
    };
  }

  public LastDenseBitVector getLastDenseBitVectorByAccessType(MemoryAccessType pAccessType) {
    return switch (pAccessType) {
      case NONE -> throw new IllegalArgumentException("no NONE access type last dense bit vector");
      case ACCESS -> lastDenseAccessBitVector.orElseThrow();
      case READ -> throw new IllegalArgumentException("no READ access type last dense bit vector");
      case WRITE -> lastDenseWriteBitVector.orElseThrow();
    };
  }

  public Optional<ImmutableMap<SeqMemoryLocation, LastSparseBitVector>>
      tryGetLastSparseBitVectorByAccessType(MemoryAccessType pAccessType) {

    return switch (pAccessType) {
      case NONE -> throw new IllegalArgumentException("no NONE access type last dense bit vector");
      case ACCESS -> lastSparseAccessBitVector;
      case READ -> Optional.empty();
      case WRITE -> lastSparseWriteBitVector;
    };
  }

  public ImmutableMap<SeqMemoryLocation, LastSparseBitVector> getLastSparseBitVectorByAccessType(
      MemoryAccessType pAccessType) {

    return switch (pAccessType) {
      case NONE -> throw new IllegalArgumentException("no NONE access type last dense bit vector");
      case ACCESS -> lastSparseAccessBitVector.orElseThrow();
      case READ -> throw new IllegalArgumentException("no READ access type last dense bit vector");
      case WRITE -> lastSparseWriteBitVector.orElseThrow();
    };
  }

  // Boolean Helpers ===============================================================================

  public boolean areSparseBitVectorsEmpty() {
    return sparseAccessBitVectors.isEmpty() && sparseWriteBitVectors.isEmpty();
  }

  public boolean areSparseAccessBitVectorsEmpty() {
    return sparseAccessBitVectors.isEmpty();
  }

  public boolean isLastDenseBitVectorPresentByAccessType(MemoryAccessType pAccessType) {
    return switch (pAccessType) {
      case NONE -> throw new IllegalArgumentException("no NONE access type last dense bit vector");
      case ACCESS -> lastDenseAccessBitVector.isPresent();
      case READ -> false;
      case WRITE -> lastDenseWriteBitVector.isPresent();
    };
  }

  // Getters =======================================================================================

  public ImmutableMap<SeqMemoryLocation, SparseBitVector> getSparseAccessBitVectors() {
    return sparseAccessBitVectors.orElseThrow();
  }

  public ImmutableMap<SeqMemoryLocation, SparseBitVector> getSparseReadBitVectors() {
    return sparseReadBitVectors.orElseThrow();
  }

  public ImmutableMap<SeqMemoryLocation, SparseBitVector> getSparseWriteBitVectors() {
    return sparseWriteBitVectors.orElseThrow();
  }
}
