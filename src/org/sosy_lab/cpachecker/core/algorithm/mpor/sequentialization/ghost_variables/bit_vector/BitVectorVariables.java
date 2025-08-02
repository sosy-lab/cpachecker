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
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;

public class BitVectorVariables {

  private final int numGlobalVariables;

  private final ImmutableMap<CVariableDeclaration, Integer> globalVariableIds;

  private final Optional<ImmutableSet<DenseBitVector>> denseAccessBitVectors;

  private final Optional<ImmutableSet<DenseBitVector>> denseWriteBitVectors;

  private final Optional<ImmutableMap<CVariableDeclaration, SparseBitVector>>
      sparseAccessBitVectors;

  private final Optional<ImmutableMap<CVariableDeclaration, SparseBitVector>> sparseWriteBitVectors;

  public BitVectorVariables(
      ImmutableMap<CVariableDeclaration, Integer> pGlobalVariableIds,
      Optional<ImmutableSet<DenseBitVector>> pDenseAccessBitVectors,
      Optional<ImmutableSet<DenseBitVector>> pDenseWriteBitVectors,
      Optional<ImmutableMap<CVariableDeclaration, SparseBitVector>> pSparseAccessBitVectors,
      Optional<ImmutableMap<CVariableDeclaration, SparseBitVector>> pSparseWriteBitVectors) {

    numGlobalVariables = pGlobalVariableIds.size();
    globalVariableIds = pGlobalVariableIds;
    denseAccessBitVectors = pDenseAccessBitVectors;
    denseWriteBitVectors = pDenseWriteBitVectors;
    sparseAccessBitVectors = pSparseAccessBitVectors;
    sparseWriteBitVectors = pSparseWriteBitVectors;
  }

  public CExpression getDenseDirectBitVectorByAccessType(
      BitVectorAccessType pAccessType, MPORThread pThread) {

    for (DenseBitVector variable : getDenseBitVectorsByAccessType(pAccessType)) {
      if (variable.thread.equals(pThread)) {
        return variable.directVariable;
      }
    }
    throw new IllegalArgumentException("could not find pThread");
  }

  public CExpression getDenseReachableBitVectorByAccessType(
      BitVectorAccessType pAccessType, MPORThread pThread) {

    for (DenseBitVector variable : getDenseBitVectorsByAccessType(pAccessType)) {
      if (variable.thread.equals(pThread)) {
        return variable.reachableVariable;
      }
    }
    throw new IllegalArgumentException("could not find pThread");
  }

  // TODO CIdExpression?
  public ImmutableSet<CExpression> getOtherDenseReachableBitVectorsByAccessType(
      BitVectorAccessType pAccessType, ImmutableSet<MPORThread> pOtherThreads) {

    ImmutableSet.Builder<CExpression> rVariables = ImmutableSet.builder();
    for (DenseBitVector variable : getDenseBitVectorsByAccessType(pAccessType)) {
      if (pOtherThreads.contains(variable.thread)) {
        rVariables.add(variable.reachableVariable);
      }
    }
    return rVariables.build();
  }

  public ImmutableSet<DenseBitVector> getDenseBitVectorsByAccessType(
      BitVectorAccessType pAccessType) {

    return switch (pAccessType) {
      case NONE -> ImmutableSet.of();
      case ACCESS -> denseAccessBitVectors.orElseThrow();
      // there are no separate READ bit vectors, only access and write
      case READ -> throw new IllegalArgumentException("READ bit vectors are not used");
      case WRITE -> denseWriteBitVectors.orElseThrow();
    };
  }

  // Boolean Helpers ===============================================================================

  public boolean areSparseBitVectorsEmpty() {
    return sparseAccessBitVectors.isEmpty() && sparseWriteBitVectors.isEmpty();
  }

  public boolean areSparseAccessBitVectorsEmpty() {
    return sparseAccessBitVectors.isEmpty();
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
