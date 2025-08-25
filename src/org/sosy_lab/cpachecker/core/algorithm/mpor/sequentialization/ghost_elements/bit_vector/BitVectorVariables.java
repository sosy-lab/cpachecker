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
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryAccessType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryLocation;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;

public class BitVectorVariables {

  private final Optional<ImmutableSet<DenseBitVector>> denseAccessBitVectors;

  private final Optional<ImmutableSet<DenseBitVector>> denseReadBitVectors;

  private final Optional<ImmutableSet<DenseBitVector>> denseWriteBitVectors;

  private final Optional<ImmutableMap<MemoryLocation, SparseBitVector>> sparseAccessBitVectors;

  private final Optional<ImmutableMap<MemoryLocation, SparseBitVector>> sparseWriteBitVectors;

  private final Optional<LastDenseBitVector> lastDenseAccessBitVector;

  private final Optional<LastDenseBitVector> lastDenseWriteBitVector;

  private final Optional<ImmutableMap<MemoryLocation, LastSparseBitVector>>
      lastSparseAccessBitVector;

  private final Optional<ImmutableMap<MemoryLocation, LastSparseBitVector>>
      lastSparseWriteBitVector;

  public BitVectorVariables(
      Optional<ImmutableSet<DenseBitVector>> pDenseAccessBitVectors,
      Optional<ImmutableSet<DenseBitVector>> pDenseReadBitVectors,
      Optional<ImmutableSet<DenseBitVector>> pDenseWriteBitVectors,
      Optional<ImmutableMap<MemoryLocation, SparseBitVector>> pSparseAccessBitVectors,
      Optional<ImmutableMap<MemoryLocation, SparseBitVector>> pSparseWriteBitVectors,
      Optional<LastDenseBitVector> pLastDenseAccessBitVector,
      Optional<LastDenseBitVector> pLastDenseWriteBitVector,
      Optional<ImmutableMap<MemoryLocation, LastSparseBitVector>> pLastSparseAccessBitVector,
      Optional<ImmutableMap<MemoryLocation, LastSparseBitVector>> pLastSparseWriteBitVector) {

    denseAccessBitVectors = pDenseAccessBitVectors;
    denseReadBitVectors = pDenseReadBitVectors;
    denseWriteBitVectors = pDenseWriteBitVectors;
    sparseAccessBitVectors = pSparseAccessBitVectors;
    sparseWriteBitVectors = pSparseWriteBitVectors;
    lastDenseAccessBitVector = pLastDenseAccessBitVector;
    lastDenseWriteBitVector = pLastDenseWriteBitVector;
    lastSparseAccessBitVector = pLastSparseAccessBitVector;
    lastSparseWriteBitVector = pLastSparseWriteBitVector;
  }

  public CExpression getDenseDirectBitVectorByAccessType(
      MemoryAccessType pAccessType, MPORThread pThread) {

    for (DenseBitVector variable : getDenseBitVectorsByAccessType(pAccessType)) {
      if (variable.thread.equals(pThread)) {
        return variable.directVariable.orElseThrow();
      }
    }
    throw new IllegalArgumentException("could not find pThread");
  }

  public CExpression getDenseReachableBitVectorByAccessType(
      MemoryAccessType pAccessType, MPORThread pThread) {

    for (DenseBitVector variable : getDenseBitVectorsByAccessType(pAccessType)) {
      if (variable.thread.equals(pThread)) {
        return variable.reachableVariable.orElseThrow();
      }
    }
    throw new IllegalArgumentException("could not find pThread");
  }

  // TODO CIdExpression?
  public ImmutableSet<CExpression> getOtherDenseReachableBitVectorsByAccessType(
      MemoryAccessType pAccessType, ImmutableSet<MPORThread> pOtherThreads) {

    ImmutableSet.Builder<CExpression> rVariables = ImmutableSet.builder();
    for (DenseBitVector variable : getDenseBitVectorsByAccessType(pAccessType)) {
      if (pOtherThreads.contains(variable.thread)) {
        rVariables.add(variable.reachableVariable.orElseThrow());
      }
    }
    return rVariables.build();
  }

  public ImmutableSet<DenseBitVector> getDenseBitVectorsByAccessType(MemoryAccessType pAccessType) {

    return switch (pAccessType) {
      case NONE -> ImmutableSet.of();
      case ACCESS -> denseAccessBitVectors.orElseThrow();
      case READ -> denseReadBitVectors.orElseThrow();
      case WRITE -> denseWriteBitVectors.orElseThrow();
    };
  }

  public ImmutableMap<MemoryLocation, SparseBitVector> getSparseBitVectorByAccessType(
      MemoryAccessType pAccessType) {

    return switch (pAccessType) {
      case NONE -> throw new IllegalArgumentException("no NONE sparse bit vectors");
      case ACCESS -> sparseAccessBitVectors.orElseThrow();
      case READ -> throw new IllegalArgumentException("no READ sparse bit vectors");
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

  public Optional<ImmutableMap<MemoryLocation, LastSparseBitVector>>
      tryGetLastSparseBitVectorByAccessType(MemoryAccessType pAccessType) {

    return switch (pAccessType) {
      case NONE -> throw new IllegalArgumentException("no NONE access type last dense bit vector");
      case ACCESS -> lastSparseAccessBitVector;
      case READ -> throw new IllegalArgumentException("no READ access type last dense bit vector");
      case WRITE -> lastSparseWriteBitVector;
    };
  }

  public ImmutableMap<MemoryLocation, LastSparseBitVector> getLastSparseBitVectorByAccessType(
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

  public ImmutableMap<MemoryLocation, SparseBitVector> getSparseAccessBitVectors() {
    return sparseAccessBitVectors.orElseThrow();
  }

  public ImmutableMap<MemoryLocation, SparseBitVector> getSparseWriteBitVectors() {
    return sparseWriteBitVectors.orElseThrow();
  }
}
