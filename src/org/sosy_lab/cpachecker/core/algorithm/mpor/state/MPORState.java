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
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.preference_order.PreferenceOrder;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;

public class MPORState {

  /** The current state of the program, i.e. threads and their current CFANodes. */
  public final ImmutableMap<MPORThread, CFANode> threadNodes;

  /** The set of PreferenceOrders in this state, i.e. positional preference orders. */
  public final ImmutableSet<PreferenceOrder> preferenceOrders;

  /** The list of CFAEdges executed leading us to {@link MPORState#threadNodes}. */
  public final ExecutionTrace executionTrace;

  public final PredicateAbstractState abstractState;

  // TODO set of ConflictRelations should be here

  public MPORState(
      ImmutableMap<MPORThread, CFANode> pThreadNodes,
      ImmutableSet<PreferenceOrder> pPreferenceOrders,
      ExecutionTrace pExecutionTrace,
      PredicateAbstractState pAbstractState) {
    threadNodes = pThreadNodes;
    preferenceOrders = pPreferenceOrders;
    executionTrace = pExecutionTrace;
    abstractState = pAbstractState;
  }

  /**
   * Checks whether this state contains the exact same threadNodes as pThreadNodes.
   *
   * @param pThreadNodes the other threadNodes
   * @return true if pState.threadNodes contains all MPORThreads of {@link MPORState#threadNodes}
   *     and if the mapped CFANodes are equal
   * @throws IllegalArgumentException if {@link MPORState#threadNodes} or pState.threadNodes is
   *     empty
   */
  public boolean areThreadNodesEqual(ImmutableMap<MPORThread, CFANode> pThreadNodes) {
    if (!threadNodes.isEmpty() && !pThreadNodes.isEmpty()) {
      for (var entry : threadNodes.entrySet()) {
        if (pThreadNodes.containsKey(entry.getKey())) {
          CFANode cfaNode = pThreadNodes.get(entry.getKey());
          if (cfaNode != null) {
            if (!cfaNode.equals(entry.getValue())) {
              return false;
            }
          }
        } else {
          return false;
        }
      }
      return true;
    }
    throw new IllegalArgumentException("no threadNodes found to compare");
  }
}
