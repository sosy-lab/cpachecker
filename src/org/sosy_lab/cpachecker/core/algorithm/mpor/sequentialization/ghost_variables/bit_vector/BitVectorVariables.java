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

  // TODO best make all these fields private

  public final int numGlobalVariables;

  public final ImmutableMap<CVariableDeclaration, Integer> globalVariableIds;

  private final Optional<ImmutableSet<DenseBitVector>> denseAccessBitVectors;

  private final Optional<ImmutableSet<DenseBitVector>> denseReadBitVectors;

  private final Optional<ImmutableSet<DenseBitVector>> denseWriteBitVectors;

  public final Optional<ImmutableMap<CVariableDeclaration, SparseBitVector>> sparseAccessBitVectors;

  public final Optional<ImmutableMap<CVariableDeclaration, SparseBitVector>> sparseReadBitVectors;

  public final Optional<ImmutableMap<CVariableDeclaration, SparseBitVector>> sparseWriteBitVectors;

  public BitVectorVariables(
      ImmutableMap<CVariableDeclaration, Integer> pGlobalVariableIds,
      Optional<ImmutableSet<DenseBitVector>> pDenseAccessBitVectors,
      Optional<ImmutableSet<DenseBitVector>> pDenseReadBitVectors,
      Optional<ImmutableSet<DenseBitVector>> pDenseWriteBitVectors,
      Optional<ImmutableMap<CVariableDeclaration, SparseBitVector>> pSparseAccessBitVectors,
      Optional<ImmutableMap<CVariableDeclaration, SparseBitVector>> pSparseReadBitVectors,
      Optional<ImmutableMap<CVariableDeclaration, SparseBitVector>> pSparseWriteBitVectors) {

    numGlobalVariables = pGlobalVariableIds.size();
    globalVariableIds = pGlobalVariableIds;
    denseReadBitVectors = pDenseReadBitVectors;
    denseWriteBitVectors = pDenseWriteBitVectors;
    denseAccessBitVectors = pDenseAccessBitVectors;
    sparseAccessBitVectors = pSparseAccessBitVectors;
    sparseReadBitVectors = pSparseReadBitVectors;
    sparseWriteBitVectors = pSparseWriteBitVectors;
  }

  public CExpression getDenseBitVectorByAccessType(
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
      case READ -> denseReadBitVectors.orElseThrow();
      case WRITE -> denseWriteBitVectors.orElseThrow();
    };
  }

  public ImmutableMap<CVariableDeclaration, SparseBitVector> getSparseBitVectorsByAccessType(
      BitVectorAccessType pAccessType) {

    return switch (pAccessType) {
      case NONE -> ImmutableMap.of();
      case ACCESS -> sparseAccessBitVectors.orElseThrow();
      case READ -> sparseReadBitVectors.orElseThrow();
      case WRITE -> sparseWriteBitVectors.orElseThrow();
    };
  }

  public boolean areDenseBitVectorsEmpty() {
    return getDenseBitVectorsByAccessType(BitVectorAccessType.READ).isEmpty()
        && getDenseBitVectorsByAccessType(BitVectorAccessType.WRITE).isEmpty();
  }

  public boolean areSparseBitVectorsEmpty() {
    return sparseReadBitVectors.isEmpty() && sparseWriteBitVectors.isEmpty();
  }
}
