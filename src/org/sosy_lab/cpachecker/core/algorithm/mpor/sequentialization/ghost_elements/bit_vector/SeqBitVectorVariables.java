// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Objects;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pointer_aliasing.SeqMemoryAccessType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pointer_aliasing.SeqMemoryLocation;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pointer_aliasing.SeqMemoryReachType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;

public record SeqBitVectorVariables(
    Optional<ImmutableSet<SeqDenseBitVector>> denseAccessBitVectors,
    Optional<ImmutableSet<SeqDenseBitVector>> denseReadBitVectors,
    Optional<ImmutableSet<SeqDenseBitVector>> denseWriteBitVectors,
    Optional<ImmutableMap<SeqMemoryLocation, SeqSparseBitVector>> sparseAccessBitVectors,
    Optional<ImmutableMap<SeqMemoryLocation, SeqSparseBitVector>> sparseReadBitVectors,
    Optional<ImmutableMap<SeqMemoryLocation, SeqSparseBitVector>> sparseWriteBitVectors,
    Optional<PrevDenseBitVector> prevDenseAccessBitVector,
    Optional<PrevDenseBitVector> prevDenseReadBitVector,
    Optional<PrevDenseBitVector> prevDenseWriteBitVector,
    Optional<ImmutableMap<SeqMemoryLocation, PrevSparseBitVector>> prevSparseAccessBitVector,
    Optional<ImmutableMap<SeqMemoryLocation, PrevSparseBitVector>> prevSparseReadBitVector,
    Optional<ImmutableMap<SeqMemoryLocation, PrevSparseBitVector>> prevSparseWriteBitVector) {

  /**
   * Represents a dense bit vector variable where each index represents a relevant memory locations.
   */
  public record SeqDenseBitVector(
      MPORThread thread,
      Optional<CIdExpression> directVariable,
      Optional<CIdExpression> reachableVariable) {

    /**
     * Note that both direct and reachable can be empty, when there are no relevant memory
     * locations.
     */
    public CIdExpression getVariableByReachType(SeqMemoryReachType pReachType) {
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
  public static final class SeqSparseBitVector {

    private final boolean isPruned;

    private final ImmutableMap<MPORThread, CIdExpression> directVariables;

    private final ImmutableMap<MPORThread, CIdExpression> reachableVariables;

    public SeqSparseBitVector(
        boolean pIsPruned,
        ImmutableMap<MPORThread, CIdExpression> pDirectVariables,
        ImmutableMap<MPORThread, CIdExpression> pReachableVariables) {

      isPruned = pIsPruned;
      directVariables = pDirectVariables;
      reachableVariables = pReachableVariables;
    }

    public Optional<CIdExpression> tryGetVariableByReachTypeAndThread(
        SeqMemoryReachType pReachType, MPORThread pThread) {

      return switch (pReachType) {
        case DIRECT -> {
          if (directVariables.containsKey(pThread)) {
            yield Optional.of(Objects.requireNonNull(directVariables.get(pThread)));
          } else {
            checkState(isPruned);
            yield Optional.empty();
          }
        }
        case REACHABLE -> {
          if (reachableVariables.containsKey(pThread)) {
            yield Optional.of(Objects.requireNonNull(reachableVariables.get(pThread)));
          } else {
            checkState(isPruned);
            yield Optional.empty();
          }
        }
      };
    }
  }

  /** The dense bit vector for the thread that previously executed a statement. */
  public record PrevDenseBitVector(CIdExpression directVariable) {}

  /** The reachable sparse bit vector for the thread that previously executed a statement. */
  public record PrevSparseBitVector(CIdExpression directVariable) {}

  public CIdExpression getDenseBitVector(
      MPORThread pThread, SeqMemoryAccessType pAccessType, SeqMemoryReachType pReachType) {

    for (SeqDenseBitVector denseBitVector : getDenseBitVectorsByAccessType(pAccessType)) {
      if (denseBitVector.thread().equals(pThread)) {
        return denseBitVector.getVariableByReachType(pReachType);
      }
    }
    throw new IllegalArgumentException("could not find DenseBitVector");
  }

  public ImmutableSet<CExpression> getOtherDenseReachableBitVectorsByAccessType(
      SeqMemoryAccessType pAccessType, ImmutableSet<MPORThread> pOtherThreads) {

    ImmutableSet.Builder<CExpression> rDenseBitVectors = ImmutableSet.builder();
    for (SeqDenseBitVector denseBitVector : getDenseBitVectorsByAccessType(pAccessType)) {
      if (pOtherThreads.contains(denseBitVector.thread())) {
        rDenseBitVectors.add(denseBitVector.getVariableByReachType(SeqMemoryReachType.REACHABLE));
      }
    }
    return rDenseBitVectors.build();
  }

  public ImmutableSet<SeqDenseBitVector> getDenseBitVectorsByAccessType(
      SeqMemoryAccessType pAccessType) {
    return switch (pAccessType) {
      case NONE -> ImmutableSet.of();
      case ACCESS -> denseAccessBitVectors.orElseThrow();
      case READ -> denseReadBitVectors.orElseThrow();
      case WRITE -> denseWriteBitVectors.orElseThrow();
    };
  }

  public ImmutableMap<SeqMemoryLocation, SeqSparseBitVector> getSparseBitVectorByAccessType(
      SeqMemoryAccessType pAccessType) {

    return switch (pAccessType) {
      case NONE -> throw new IllegalArgumentException("no NONE sparse bit vectors");
      case ACCESS -> sparseAccessBitVectors.orElseThrow();
      case READ -> sparseReadBitVectors.orElseThrow();
      case WRITE -> sparseWriteBitVectors.orElseThrow();
    };
  }

  public PrevDenseBitVector getPrevDenseBitVectorByAccessType(SeqMemoryAccessType pAccessType) {
    return switch (pAccessType) {
      case NONE -> throw new IllegalArgumentException("no NONE access type prev dense bit vector");
      case ACCESS -> prevDenseAccessBitVector.orElseThrow();
      case READ -> prevDenseReadBitVector.orElseThrow();
      case WRITE -> prevDenseWriteBitVector.orElseThrow();
    };
  }

  public Optional<ImmutableMap<SeqMemoryLocation, PrevSparseBitVector>>
      tryGetPrevSparseBitVectorByAccessType(SeqMemoryAccessType pAccessType) {

    return switch (pAccessType) {
      case NONE -> throw new IllegalArgumentException("no NONE access type prev dense bit vector");
      case ACCESS -> prevSparseAccessBitVector;
      case READ -> prevSparseReadBitVector;
      case WRITE -> prevSparseWriteBitVector;
    };
  }

  public ImmutableMap<SeqMemoryLocation, PrevSparseBitVector> getPrevSparseBitVectorByAccessType(
      SeqMemoryAccessType pAccessType) {

    return switch (pAccessType) {
      case NONE -> throw new IllegalArgumentException("no NONE access type prev dense bit vector");
      case ACCESS -> prevSparseAccessBitVector.orElseThrow();
      case READ -> prevSparseReadBitVector.orElseThrow();
      case WRITE -> prevSparseWriteBitVector.orElseThrow();
    };
  }

  // Boolean Helpers ===============================================================================

  public boolean areSparseBitVectorsPresentByAccessType(SeqMemoryAccessType pAccessType) {
    return switch (pAccessType) {
      case NONE, READ ->
          throw new IllegalArgumentException(
              String.format("no %s access type prev dense bit vector", pAccessType));
      case ACCESS -> sparseAccessBitVectors.isPresent();
      case WRITE -> sparseWriteBitVectors.isPresent();
    };
  }

  public boolean isPrevDenseBitVectorPresentByAccessType(SeqMemoryAccessType pAccessType) {
    return switch (pAccessType) {
      case NONE -> throw new IllegalArgumentException("no NONE access type prev dense bit vector");
      case ACCESS -> prevDenseAccessBitVector.isPresent();
      case READ -> prevDenseReadBitVector.isPresent();
      case WRITE -> prevDenseWriteBitVector.isPresent();
    };
  }
}
