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

  public final int numGlobalVariables;

  public final ImmutableMap<CVariableDeclaration, Integer> globalVariableIds;

  public final Optional<ImmutableSet<DenseBitVector>> denseAccessBitVectors;

  public final Optional<ImmutableSet<DenseBitVector>> denseReadBitVectors;

  public final Optional<ImmutableSet<DenseBitVector>> denseWriteBitVectors;

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

  public CExpression getDenseBitVectorByAccessType(
      BitVectorAccessType pAccessType, MPORThread pThread) {

    for (DenseBitVector variable : getDenseBitVectorsByAccessType(pAccessType)) {
      if (variable.thread.equals(pThread)) {
        return variable.idExpression;
      }
    }
    throw new IllegalArgumentException("could not find pThread");
  }

  // TODO CIdExpression?
  public ImmutableSet<CExpression> getOtherDenseBitVectorsByAccessType(
      BitVectorAccessType pAccessType, MPORThread pThread) {

    ImmutableSet.Builder<CExpression> rVariables = ImmutableSet.builder();
    for (DenseBitVector variable : getDenseBitVectorsByAccessType(pAccessType)) {
      if (!variable.thread.equals(pThread)) {
        rVariables.add(variable.idExpression);
      }
    }
    return rVariables.build();
  }

  private ImmutableSet<DenseBitVector> getDenseBitVectorsByAccessType(
      BitVectorAccessType pAccessType) {

    return switch (pAccessType) {
      case NONE -> ImmutableSet.of();
      case ACCESS -> denseAccessBitVectors.orElseThrow();
      case READ -> denseReadBitVectors.orElseThrow();
      case WRITE -> denseWriteBitVectors.orElseThrow();
    };
  }
}
