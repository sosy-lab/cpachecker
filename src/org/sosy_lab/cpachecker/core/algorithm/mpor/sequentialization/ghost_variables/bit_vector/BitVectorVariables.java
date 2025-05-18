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

  public final int numGlobalVariables;

  // TODO make this consider memory locations, not variable names
  public final ImmutableMap<CVariableDeclaration, Integer> globalVariableIds;

  private final Optional<ImmutableSet<DenseBitVector>> denseAccessBitVectors;

  private final Optional<ImmutableSet<DenseBitVector>> denseReadBitVectors;

  private final Optional<ImmutableSet<DenseBitVector>> denseWriteBitVectors;

  public final Optional<ImmutableMap<CVariableDeclaration, ScalarBitVector>> scalarAccessBitVectors;

  public final Optional<ImmutableMap<CVariableDeclaration, ScalarBitVector>> scalarReadBitVectors;

  public final Optional<ImmutableMap<CVariableDeclaration, ScalarBitVector>> scalarWriteBitVectors;

  public BitVectorVariables(
      ImmutableMap<CVariableDeclaration, Integer> pGlobalVariableIds,
      Optional<ImmutableSet<DenseBitVector>> pDenseAccessBitVectors,
      Optional<ImmutableSet<DenseBitVector>> pDenseReadBitVectors,
      Optional<ImmutableSet<DenseBitVector>> pDenseWriteBitVectors,
      Optional<ImmutableMap<CVariableDeclaration, ScalarBitVector>> pScalarAccessBitVectors,
      Optional<ImmutableMap<CVariableDeclaration, ScalarBitVector>> pScalarReadBitVectors,
      Optional<ImmutableMap<CVariableDeclaration, ScalarBitVector>> pScalarWriteBitVectors) {

    numGlobalVariables = pGlobalVariableIds.size();
    globalVariableIds = pGlobalVariableIds;
    denseReadBitVectors = pDenseReadBitVectors;
    denseWriteBitVectors = pDenseWriteBitVectors;
    denseAccessBitVectors = pDenseAccessBitVectors;
    scalarAccessBitVectors = pScalarAccessBitVectors;
    scalarReadBitVectors = pScalarReadBitVectors;
    scalarWriteBitVectors = pScalarWriteBitVectors;
  }

  public CExpression getDenseBitVectorByAccessAndReachType(
      BitVectorAccessType pAccessType, BitVectorReachType pReachType, MPORThread pThread) {

    for (DenseBitVector variable : getDenseBitVectorsByAccessType(pAccessType)) {
      if (variable.thread.equals(pThread)) {
        return switch (pReachType) {
          case DIRECT -> variable.directVariable;
          case REACHABLE -> variable.reachableVariable;
        };
      }
    }
    throw new IllegalArgumentException("could not find pThread");
  }

  // TODO CIdExpression?
  public ImmutableSet<CExpression> getOtherDenseReachableBitVectorsByAccessType(
      BitVectorAccessType pAccessType, MPORThread pThread) {

    Builder<CExpression> rVariables = ImmutableSet.builder();
    for (DenseBitVector variable : getDenseBitVectorsByAccessType(pAccessType)) {
      if (!variable.thread.equals(pThread)) {
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

  public ImmutableMap<CVariableDeclaration, ScalarBitVector> getScalarBitVectorsByAccessType(
      BitVectorAccessType pAccessType) {

    return switch (pAccessType) {
      case NONE -> ImmutableMap.of();
      case ACCESS -> scalarAccessBitVectors.orElseThrow();
      case READ -> scalarReadBitVectors.orElseThrow();
      case WRITE -> scalarWriteBitVectors.orElseThrow();
    };
  }

  public boolean areDenseBitVectorsEmpty() {
    return getDenseBitVectorsByAccessType(BitVectorAccessType.READ).isEmpty()
        && getDenseBitVectorsByAccessType(BitVectorAccessType.WRITE).isEmpty();
  }

  public boolean areScalarBitVectorsEmpty() {
    return scalarReadBitVectors.isEmpty() && scalarWriteBitVectors.isEmpty();
  }
}
