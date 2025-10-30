// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector;

import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.ReachType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;

/**
 * Represents a dense bit vector variable, i.e. where each index is represented in a single
 * variable.
 *
 * @param thread The thread that this bit vector belongs to.
 * @param directVariable The bit vector for the next statement.
 * @param reachableVariable The bit vector for all reachable statements, relative to a location.
 */
public record DenseBitVector(
    MPORThread thread,
    Optional<CIdExpression> directVariable,
    Optional<CIdExpression> reachableVariable) {

  // note that both direct and reachable can be empty, when there are no global variables

  public boolean isVariablePresentByReachType(ReachType pReachType) {
    return switch (pReachType) {
      case DIRECT -> directVariable.isPresent();
      case REACHABLE -> reachableVariable.isPresent();
    };
  }

  public CIdExpression getVariableByReachType(ReachType pReachType) {
    return switch (pReachType) {
      case DIRECT -> directVariable.orElseThrow();
      case REACHABLE -> reachableVariable.orElseThrow();
    };
  }
}
