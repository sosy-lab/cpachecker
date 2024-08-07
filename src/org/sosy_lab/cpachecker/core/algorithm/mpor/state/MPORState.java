// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.state;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.preference_order.PreferenceOrder;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;

public class MPORState {

  /** The current state of the program, i.e. threads and their current CFANodes. */
  public final ImmutableMap<MPORThread, CFANode> threadNodes;

  /**
   * The function return nodes of each thread, i.e. their original context if their threadNode is in
   * another function.
   */
  public final ImmutableMap<MPORThread, Optional<CFANode>> funcReturnNodes;

  /** The set of PreferenceOrders in this state, i.e. positional preference orders. */
  public final ImmutableSet<PreferenceOrder> preferenceOrders;

  /** The list of CFAEdges executed leading us to {@link MPORState#threadNodes}. */
  public final ExecutionTrace executionTrace;

  public final PredicateAbstractState abstractState;

  // TODO set of ConflictRelations should be here

  public MPORState(
      ImmutableMap<MPORThread, CFANode> pThreadNodes,
      ImmutableMap<MPORThread, Optional<CFANode>> pFunctionReturnNodes,
      ImmutableSet<PreferenceOrder> pPreferenceOrders,
      ExecutionTrace pExecutionTrace,
      PredicateAbstractState pAbstractState) {
    threadNodes = pThreadNodes;
    funcReturnNodes = pFunctionReturnNodes;
    preferenceOrders = pPreferenceOrders;
    executionTrace = pExecutionTrace;
    abstractState = pAbstractState;
  }
}
