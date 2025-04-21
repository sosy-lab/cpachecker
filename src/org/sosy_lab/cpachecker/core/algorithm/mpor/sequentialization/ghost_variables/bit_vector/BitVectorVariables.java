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
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;

public class BitVectorVariables {

  public final int numGlobalVariables;

  public final ImmutableMap<CVariableDeclaration, Integer> globalVariableIds;

  public final ImmutableMap<MPORThread, CIdExpression> bitVectors;

  public final ImmutableMap<CVariableDeclaration, ScalarBitVectorAccessVariables> scalarBitVectors;

  // TODO optionals?
  public BitVectorVariables(
      ImmutableMap<CVariableDeclaration, Integer> pGlobalVariableIds,
      ImmutableMap<MPORThread, CIdExpression> pBitVectors,
      ImmutableMap<CVariableDeclaration, ScalarBitVectorAccessVariables> pScalarBitVectors) {

    numGlobalVariables = pGlobalVariableIds.size();
    globalVariableIds = pGlobalVariableIds;
    bitVectors = pBitVectors;
    scalarBitVectors = pScalarBitVectors;
  }

  public ImmutableSet<Integer> getIdsByVariables(ImmutableSet<CVariableDeclaration> pVariables) {
    return pVariables.stream()
        .map(globalVariableIds::get)
        .filter(Objects::nonNull)
        .collect(ImmutableSet.toImmutableSet());
  }

  public CIdExpression getBitVectorExpression(MPORThread pThread) {
    return bitVectors.get(pThread);
  }

  public ImmutableSet<CExpression> getOtherBitVectorExpressions(CIdExpression pBitVector) {
    return bitVectors.values().stream()
        .filter(b -> !b.equals(pBitVector))
        .collect(ImmutableSet.toImmutableSet());
  }

  public static class ScalarBitVectorAccessVariables {
    public final ImmutableMap<MPORThread, CIdExpression> accessVariables;

    public ScalarBitVectorAccessVariables(
        ImmutableMap<MPORThread, CIdExpression> pAccessVariables) {
      accessVariables = pAccessVariables;
    }
  }
}
