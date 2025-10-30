// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector;

import com.google.common.collect.ImmutableMap;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.ReachType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;

public record SparseBitVector(
    ImmutableMap<MPORThread, CIdExpression> directVariables,
    ImmutableMap<MPORThread, CIdExpression> reachableVariables) {

  public ImmutableMap<MPORThread, CIdExpression> getVariablesByReachType(ReachType pReachType) {
    return switch (pReachType) {
      case DIRECT -> directVariables;
      case REACHABLE -> reachableVariables;
    };
  }
}
