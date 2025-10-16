// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableMap;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryAccessType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.ReachType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;

public class SparseBitVector {

  private final ImmutableMap<MPORThread, CIdExpression> directVariables;

  private final ImmutableMap<MPORThread, CIdExpression> reachableVariables;

  SparseBitVector(
      ImmutableMap<MPORThread, CIdExpression> pDirectVariables,
      ImmutableMap<MPORThread, CIdExpression> pReachableVariables,
      MemoryAccessType pAccessType) {

    checkArgument(!pAccessType.equals(MemoryAccessType.NONE));
    directVariables = pDirectVariables;
    reachableVariables = pReachableVariables;
  }

  public ImmutableMap<MPORThread, CIdExpression> getVariablesByReachType(ReachType pReachType) {
    return switch (pReachType) {
      case DIRECT -> directVariables;
      case REACHABLE -> reachableVariables;
    };
  }
}
