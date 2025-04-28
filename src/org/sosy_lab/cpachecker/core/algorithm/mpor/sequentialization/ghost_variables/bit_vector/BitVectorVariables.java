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

  public final ImmutableMap<CVariableDeclaration, Integer> globalVariableIds;

  public final Optional<ImmutableSet<BitVectorAccessVariable>> bitVectorAccessVariables;

  public final Optional<ImmutableMap<CVariableDeclaration, ScalarBitVectorVariables>>
      scalarBitVectorAccessVariables;

  public final Optional<ImmutableSet<BitVectorReadVariable>> bitVectorReadVariables;

  public final Optional<ImmutableSet<BitVectorWriteVariable>> bitVectorWriteVariables;

  public final Optional<ImmutableMap<CVariableDeclaration, ScalarBitVectorVariables>>
      scalarBitVectorReadVariables;

  public final Optional<ImmutableMap<CVariableDeclaration, ScalarBitVectorVariables>>
      scalarBitVectorWriteVariables;

  public BitVectorVariables(
      ImmutableMap<CVariableDeclaration, Integer> pGlobalVariableIds,
      Optional<ImmutableSet<BitVectorAccessVariable>> pBitVectors,
      Optional<ImmutableMap<CVariableDeclaration, ScalarBitVectorVariables>>
          pScalarBitVectorVariables,
      Optional<ImmutableSet<BitVectorReadVariable>> pBitVectorReadVariables,
      Optional<ImmutableSet<BitVectorWriteVariable>> pBitVectorWriteVariables,
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

  public CIdExpression getBitVectorExpressionByThread(MPORThread pThread) {
    for (BitVectorAccessVariable accessVariable : bitVectorAccessVariables.orElseThrow()) {
      if (accessVariable.thread.equals(pThread)) {
        return accessVariable.getIdExpression();
      }
    }
    throw new IllegalArgumentException("could not find pThread");
  }

  public ImmutableSet<CExpression> getOtherBitVectorExpressions(CIdExpression pBitVector) {
    ImmutableSet.Builder<CExpression> rExpressions = ImmutableSet.builder();
    for (BitVectorAccessVariable accessVariable : bitVectorAccessVariables.orElseThrow()) {
      CIdExpression idExpression = accessVariable.getIdExpression();
      if (!idExpression.equals(pBitVector)) {
        rExpressions.add(idExpression);
      }
    }
    return rExpressions.build(); // this can also be empty, if there is only one global variable
  }

  public ImmutableSet<BitVectorVariable> getAllBitVectorVariables() {
    ImmutableSet.Builder<BitVectorVariable> rAll = ImmutableSet.builder();
    if (bitVectorAccessVariables.isPresent()) {
      assert scalarBitVectorAccessVariables.isEmpty();
      rAll.addAll(bitVectorAccessVariables.orElseThrow());
    } else if (bitVectorReadVariables.isPresent()) {
      assert bitVectorWriteVariables.isPresent();
      rAll.addAll(bitVectorReadVariables.orElseThrow());
      rAll.addAll(bitVectorWriteVariables.orElseThrow());
    }
    return rAll.build();
  }

  public ImmutableMap<CVariableDeclaration, ScalarBitVectorVariables>
      getAllScalarBitVectorVariables() {

    ImmutableMap.Builder<CVariableDeclaration, ScalarBitVectorVariables> rAll =
        ImmutableMap.builder();
    if (scalarBitVectorAccessVariables.isPresent()) {
      assert scalarBitVectorReadVariables.isEmpty() && scalarBitVectorWriteVariables.isEmpty();
      rAll.putAll(scalarBitVectorAccessVariables.orElseThrow());
    } else if (scalarBitVectorReadVariables.isPresent()) {
      assert scalarBitVectorWriteVariables.isPresent();
      rAll.putAll(scalarBitVectorReadVariables.orElseThrow());
      rAll.putAll(scalarBitVectorWriteVariables.orElseThrow());
    }
    return rAll.build();
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
