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

public record SeqBitVectorVariables(
    Optional<ImmutableSet<DenseBitVector>> denseAccessBitVectors,
    Optional<ImmutableSet<DenseBitVector>> denseReadBitVectors,
    Optional<ImmutableSet<DenseBitVector>> denseWriteBitVectors,
    Optional<ImmutableMap<SeqMemoryLocation, SparseBitVector>> sparseAccessBitVectors,
    Optional<ImmutableMap<SeqMemoryLocation, SparseBitVector>> sparseReadBitVectors,
    Optional<ImmutableMap<SeqMemoryLocation, SparseBitVector>> sparseWriteBitVectors,
    Optional<PrevDenseBitVector> prevDenseAccessBitVector,
    Optional<PrevDenseBitVector> prevDenseWriteBitVector,
    Optional<ImmutableMap<SeqMemoryLocation, PrevSparseBitVector>> prevSparseAccessBitVector,
    Optional<ImmutableMap<SeqMemoryLocation, PrevSparseBitVector>> prevSparseWriteBitVector) {

  /**
   * Represents a dense bit vector variable where each index represents a relevant memory locations.
   */
  public record DenseBitVector(
      MPORThread thread,
      Optional<CIdExpression> directVariable,
      Optional<CIdExpression> reachableVariable) {

    /**
     * Note that both direct and reachable can be empty, when there are no relevant memory
     * locations.
     */
    public CIdExpression getVariableByReachType(ReachType pReachType) {
      return switch (pReachType) {
        case DIRECT -> directVariable.orElseThrow();
        case REACHABLE -> reachableVariable.orElseThrow();
      };
    }
  }

  /**
   * Represents a sparse bit vector, where each memory location, for each thread, has its own
   * variable in the sequentialization which can be either {@code 0} or {@code 1}.
   */
  public record SparseBitVector(
      ImmutableMap<MPORThread, CIdExpression> directVariables,
      ImmutableMap<MPORThread, CIdExpression> reachableVariables) {

    public ImmutableMap<MPORThread, CIdExpression> getVariablesByReachType(ReachType pReachType) {
      return switch (pReachType) {
        case DIRECT -> directVariables;
        case REACHABLE -> reachableVariables;
      };
    }
  }

  /** The reachable dense bit vector for the thread that previously executed a statement. */
  public record PrevDenseBitVector(CIdExpression reachableVariable) {}

  /** The reachable sparse bit vector for the thread that previously executed a statement. */
  public record PrevSparseBitVector(CIdExpression reachableVariable) {}

  public CIdExpression getDenseBitVector(
      MPORThread pThread, MemoryAccessType pAccessType, ReachType pReachType) {

    for (DenseBitVector denseBitVector : getDenseBitVectorsByAccessType(pAccessType)) {
      if (denseBitVector.thread().equals(pThread)) {
        return denseBitVector.getVariableByReachType(pReachType);
      }
    }
    throw new IllegalArgumentException("could not find DenseBitVector");
  }

  public ImmutableSet<CExpression> getOtherDenseReachableBitVectorsByAccessType(
      MemoryAccessType pAccessType, ImmutableSet<MPORThread> pOtherThreads) {

    ImmutableSet.Builder<CExpression> rDenseBitVectors = ImmutableSet.builder();
    for (DenseBitVector denseBitVector : getDenseBitVectorsByAccessType(pAccessType)) {
      if (pOtherThreads.contains(denseBitVector.thread())) {
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

  public PrevDenseBitVector getPrevDenseBitVectorByAccessType(MemoryAccessType pAccessType) {
    return switch (pAccessType) {
      case NONE -> throw new IllegalArgumentException("no NONE access type prev dense bit vector");
      case ACCESS -> prevDenseAccessBitVector.orElseThrow();
      case READ -> throw new IllegalArgumentException("no READ access type prev dense bit vector");
      case WRITE -> prevDenseWriteBitVector.orElseThrow();
    };
  }

  public Optional<ImmutableMap<SeqMemoryLocation, PrevSparseBitVector>>
      tryGetPrevSparseBitVectorByAccessType(MemoryAccessType pAccessType) {

    return switch (pAccessType) {
      case NONE -> throw new IllegalArgumentException("no NONE access type prev dense bit vector");
      case ACCESS -> prevSparseAccessBitVector;
      case READ -> Optional.empty();
      case WRITE -> prevSparseWriteBitVector;
    };
  }

  public ImmutableMap<SeqMemoryLocation, PrevSparseBitVector> getPrevSparseBitVectorByAccessType(
      MemoryAccessType pAccessType) {

    return switch (pAccessType) {
      case NONE -> throw new IllegalArgumentException("no NONE access type prev dense bit vector");
      case ACCESS -> prevSparseAccessBitVector.orElseThrow();
      case READ -> throw new IllegalArgumentException("no READ access type prev dense bit vector");
      case WRITE -> prevSparseWriteBitVector.orElseThrow();
    };
  }

  // Boolean Helpers ===============================================================================

  public boolean areSparseBitVectorsEmpty() {
    return sparseAccessBitVectors.isEmpty() && sparseWriteBitVectors.isEmpty();
  }

  public boolean areSparseAccessBitVectorsEmpty() {
    return sparseAccessBitVectors.isEmpty();
  }

  public boolean isPrevDenseBitVectorPresentByAccessType(MemoryAccessType pAccessType) {
    return switch (pAccessType) {
      case NONE -> throw new IllegalArgumentException("no NONE access type prev dense bit vector");
      case ACCESS -> prevDenseAccessBitVector.isPresent();
      case READ -> false;
      case WRITE -> prevDenseWriteBitVector.isPresent();
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
