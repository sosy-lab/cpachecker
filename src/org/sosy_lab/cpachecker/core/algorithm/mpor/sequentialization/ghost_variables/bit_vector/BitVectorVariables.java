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
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;

public class BitVectorVariables {

  public final int numGlobalVariables;

  // TODO this should also be optional, its only required when using non-scalar bit vectors
  public final ImmutableMap<CVariableDeclaration, Integer> globalVariableIds;

  public final Optional<ImmutableSet<BitVectorVariable>> bitVectorAccessVariables;

  public final Optional<ImmutableMap<CVariableDeclaration, ScalarBitVectorVariables>>
      scalarBitVectorAccessVariables;

  public final Optional<ImmutableSet<BitVectorVariable>> bitVectorReadVariables;

  public final Optional<ImmutableSet<BitVectorVariable>> bitVectorWriteVariables;

  public final Optional<ImmutableMap<CVariableDeclaration, ScalarBitVectorVariables>>
      scalarBitVectorReadVariables;

  public final Optional<ImmutableMap<CVariableDeclaration, ScalarBitVectorVariables>>
      scalarBitVectorWriteVariables;

  public BitVectorVariables(
      ImmutableMap<CVariableDeclaration, Integer> pGlobalVariableIds,
      Optional<ImmutableSet<BitVectorVariable>> pBitVectors,
      Optional<ImmutableMap<CVariableDeclaration, ScalarBitVectorVariables>>
          pScalarBitVectorVariables,
      Optional<ImmutableSet<BitVectorVariable>> pBitVectorReadVariables,
      Optional<ImmutableSet<BitVectorVariable>> pBitVectorWriteVariables,
      Optional<ImmutableMap<CVariableDeclaration, ScalarBitVectorVariables>>
          pScalarBitVectorReadVariables,
      Optional<ImmutableMap<CVariableDeclaration, ScalarBitVectorVariables>>
          pScalarBitVectorWriteVariables) {

    numGlobalVariables = pGlobalVariableIds.size();
    globalVariableIds = pGlobalVariableIds;
    bitVectorAccessVariables = pBitVectors;
    scalarBitVectorAccessVariables = pScalarBitVectorVariables;
    bitVectorReadVariables = pBitVectorReadVariables;
    bitVectorWriteVariables = pBitVectorWriteVariables;
    scalarBitVectorReadVariables = pScalarBitVectorReadVariables;
    scalarBitVectorWriteVariables = pScalarBitVectorWriteVariables;
  }

  public CExpression getBitVectorVariableByAccessType(
      BitVectorAccessType pAccessType, MPORThread pThread) {

    for (BitVectorVariable variable : getBitVectorVariablesByAccessType(pAccessType)) {
      if (variable.getThread().equals(pThread)) {
        return variable.getExpression();
      }
    }
    throw new IllegalArgumentException("could not find pThread");
  }

  // TODO CIdExpression?
  public ImmutableSet<CExpression> getOtherBitVectorVariablesByAccessType(
      BitVectorAccessType pAccessType, MPORThread pThread) {

    ImmutableSet.Builder<CExpression> rVariables = ImmutableSet.builder();
    for (BitVectorVariable variable : getBitVectorVariablesByAccessType(pAccessType)) {
      if (!variable.getThread().equals(pThread)) {
        rVariables.add(variable.getExpression());
      }
    }
    return rVariables.build();
  }

  private ImmutableSet<BitVectorVariable> getBitVectorVariablesByAccessType(
      BitVectorAccessType pAccessType) {

    return switch (pAccessType) {
      case NONE -> ImmutableSet.of();
      case ACCESS -> bitVectorAccessVariables.orElseThrow();
      case READ -> bitVectorReadVariables.orElseThrow();
      case WRITE -> bitVectorWriteVariables.orElseThrow();
    };
  }

  // TODO maybe separate file here
  public interface ScalarBitVectorVariables {
    ImmutableMap<MPORThread, CIdExpression> getIdExpressions();
  }

  public static class ScalarBitVectorAccessVariables implements ScalarBitVectorVariables {
    private final ImmutableMap<MPORThread, CIdExpression> accessVariables;

    public ScalarBitVectorAccessVariables(
        ImmutableMap<MPORThread, CIdExpression> pAccessVariables) {
      accessVariables = pAccessVariables;
    }

    @Override
    public ImmutableMap<MPORThread, CIdExpression> getIdExpressions() {
      return accessVariables;
    }
  }

  public static class ScalarBitVectorReadVariables implements ScalarBitVectorVariables {
    public final ImmutableMap<MPORThread, CIdExpression> readVariables;

    public ScalarBitVectorReadVariables(ImmutableMap<MPORThread, CIdExpression> pReadVariables) {
      readVariables = pReadVariables;
    }

    @Override
    public ImmutableMap<MPORThread, CIdExpression> getIdExpressions() {
      return readVariables;
    }
  }

  public static class ScalarBitVectorWriteVariables implements ScalarBitVectorVariables {
    public final ImmutableMap<MPORThread, CIdExpression> writeVariables;

    public ScalarBitVectorWriteVariables(ImmutableMap<MPORThread, CIdExpression> pWriteVariables) {
      writeVariables = pWriteVariables;
    }

    @Override
    public ImmutableMap<MPORThread, CIdExpression> getIdExpressions() {
      return writeVariables;
    }
  }
}
