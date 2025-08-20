// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;

public class BitVectorVariables {

  private final int numGlobalVariables;

  private final ImmutableMap<CVariableDeclaration, Integer> globalVariableIds;

  private final Optional<ImmutableSet<DenseBitVector>> denseAccessBitVectors;

  private final Optional<ImmutableSet<DenseBitVector>> denseReadBitVectors;

  private final Optional<ImmutableSet<DenseBitVector>> denseWriteBitVectors;

  private final Optional<ImmutableMap<CVariableDeclaration, SparseBitVector>>
      sparseAccessBitVectors;

  private final Optional<ImmutableMap<CVariableDeclaration, SparseBitVector>> sparseWriteBitVectors;

  private final Optional<LastDenseBitVector> lastDenseAccessBitVector;

  private final Optional<LastDenseBitVector> lastDenseWriteBitVector;

  // TODO last sparse access/write bit vectors

  public BitVectorVariables(
      ImmutableMap<CVariableDeclaration, Integer> pGlobalVariableIds,
      Optional<ImmutableSet<DenseBitVector>> pDenseAccessBitVectors,
      Optional<ImmutableSet<DenseBitVector>> pDenseReadBitVectors,
      Optional<ImmutableSet<DenseBitVector>> pDenseWriteBitVectors,
      Optional<ImmutableMap<CVariableDeclaration, SparseBitVector>> pSparseAccessBitVectors,
      Optional<ImmutableMap<CVariableDeclaration, SparseBitVector>> pSparseWriteBitVectors,
      Optional<LastDenseBitVector> pLastDenseAccessBitVector,
      Optional<LastDenseBitVector> pLastDenseWriteBitVector) {

    numGlobalVariables = pGlobalVariableIds.size();
    globalVariableIds = pGlobalVariableIds;
    denseAccessBitVectors = pDenseAccessBitVectors;
    denseReadBitVectors = pDenseReadBitVectors;
    denseWriteBitVectors = pDenseWriteBitVectors;
    sparseAccessBitVectors = pSparseAccessBitVectors;
    sparseWriteBitVectors = pSparseWriteBitVectors;
    lastDenseAccessBitVector = pLastDenseAccessBitVector;
    lastDenseWriteBitVector = pLastDenseWriteBitVector;
  }

  public CExpression getDenseDirectBitVectorByAccessType(
      BitVectorAccessType pAccessType, MPORThread pThread) {

    for (DenseBitVector variable : getDenseBitVectorsByAccessType(pAccessType)) {
      if (variable.thread.equals(pThread)) {
        return variable.directVariable.orElseThrow();
      }
    }
    throw new IllegalArgumentException("could not find pThread");
  }

  public CExpression getDenseReachableBitVectorByAccessType(
      BitVectorAccessType pAccessType, MPORThread pThread) {

    for (DenseBitVector variable : getDenseBitVectorsByAccessType(pAccessType)) {
      if (variable.thread.equals(pThread)) {
        return variable.reachableVariable.orElseThrow();
      }
    }
    throw new IllegalArgumentException("could not find pThread");
  }

  // TODO CIdExpression?
  public ImmutableSet<CExpression> getOtherDenseReachableBitVectorsByAccessType(
      BitVectorAccessType pAccessType, ImmutableSet<MPORThread> pOtherThreads) {

    Builder<CExpression> rVariables = ImmutableSet.builder();
    for (DenseBitVector variable : getDenseBitVectorsByAccessType(pAccessType)) {
      if (pOtherThreads.contains(variable.thread)) {
        rVariables.add(variable.reachableVariable.orElseThrow());
      }
    }
    return rVariables.build();
  }

  public ImmutableSet<DenseBitVector> getDenseBitVectorsByAccessType(
      BitVectorAccessType pAccessType) {

    return switch (pAccessType) {
      case NONE -> ImmutableSet.of();
      case ACCESS -> denseAccessBitVectors.orElseThrow();
      case READ -> denseReadBitVectors.orElseThrow();
      case WRITE -> denseWriteBitVectors.orElseThrow();
    };
  }

  public ImmutableMap<CVariableDeclaration, SparseBitVector> getSparseBitVectorByAccessType(
      BitVectorAccessType pAccessType) {

    return switch (pAccessType) {
      case NONE -> throw new IllegalArgumentException("no NONE sparse bit vectors");
      case ACCESS -> sparseAccessBitVectors.orElseThrow();
      case READ -> throw new IllegalArgumentException("no READ sparse bit vectors");
      case WRITE -> sparseWriteBitVectors.orElseThrow();
    };
  }

  public LastDenseBitVector getLastDenseBitVectorByAccessType(BitVectorAccessType pAccessType) {
    return switch (pAccessType) {
      case NONE -> throw new IllegalArgumentException("no NONE access type last dense bit vector");
      case ACCESS -> lastDenseAccessBitVector.orElseThrow();
      case READ -> throw new IllegalArgumentException("no READ access type last dense bit vector");
      case WRITE -> lastDenseWriteBitVector.orElseThrow();
    };
  }

  // Boolean Helpers ===============================================================================

  public boolean areSparseBitVectorsEmpty() {
    return sparseAccessBitVectors.isEmpty() && sparseWriteBitVectors.isEmpty();
  }

  public boolean areSparseAccessBitVectorsEmpty() {
    return sparseAccessBitVectors.isEmpty();
  }

  public boolean isLastDenseBitVectorPresentByAccessType(BitVectorAccessType pAccessType) {
    return switch (pAccessType) {
      case NONE -> throw new IllegalArgumentException("no NONE access type last dense bit vector");
      case ACCESS -> lastDenseAccessBitVector.isPresent();
      case READ -> false;
      case WRITE -> lastDenseWriteBitVector.isPresent();
    };
  }

  // Getters =======================================================================================

  public int getNumGlobalVariables() {
    return numGlobalVariables;
  }

  public ImmutableMap<CVariableDeclaration, Integer> getGlobalVariableIds() {
    return globalVariableIds;
  }

  public ImmutableMap<CVariableDeclaration, SparseBitVector> getSparseAccessBitVectors() {
    return sparseAccessBitVectors.orElseThrow();
  }

  public ImmutableMap<CVariableDeclaration, SparseBitVector> getSparseWriteBitVectors() {
    return sparseWriteBitVectors.orElseThrow();
  }
}
