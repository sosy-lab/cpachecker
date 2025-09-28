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

public class DenseBitVector {

  /** The thread that this bit vector belongs to. */
  private final MPORThread thread;

  /** The bit vector for the next statement. */
  private final Optional<CIdExpression> directVariable;

  /** The bit vector for all reachable statements, relative to a location. */
  private final Optional<CIdExpression> reachableVariable;

  DenseBitVector(
      MPORThread pThread,
      // note that both direct and reachable can be empty, when there are no global variables
      Optional<CIdExpression> pDirectVariable,
      Optional<CIdExpression> pReachableVariable) {

    thread = pThread;
    directVariable = pDirectVariable;
    reachableVariable = pReachableVariable;
  }

  public MPORThread getThread() {
    return thread;
  }

  public boolean isDirectVariablePresent() {
    return directVariable.isPresent();
  }

  public CIdExpression getDirectVariable() {
    return directVariable.orElseThrow();
  }

  public boolean isReachableVariablePresent() {
    return reachableVariable.isPresent();
  }

  public CIdExpression getReachableVariable() {
    return reachableVariable.orElseThrow();
  }

  public CIdExpression getVariableByReachType(ReachType pReachType) {
    return switch (pReachType) {
      case DIRECT -> getDirectVariable();
      case REACHABLE -> getReachableVariable();
    };
  }
}
