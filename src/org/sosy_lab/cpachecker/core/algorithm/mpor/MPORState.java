// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.mpor.preference_order.PreferenceOrder;

public class MPORState {

  /** The current state of the program, i.e. threads and their current CFANodes. */
  public final ImmutableMap<MPORThread, CFANode> threadNodes;

  /** The set of PreferenceOrders in this state, i.e. positional preference orders. */
  public final ImmutableSet<PreferenceOrder> preferenceOrders;

  public MPORState(
      ImmutableMap<MPORThread, CFANode> pThreadNodes,
      ImmutableSet<PreferenceOrder> pPreferenceOrders) {
    threadNodes = pThreadNodes;
    preferenceOrders = pPreferenceOrders;
  }

  /**
   * Returns a new state with the same threadNodes map except that the key pThread is assigned the
   * new value pUpdatedNode. This function also computes the PreferenceOrders of the new state.
   *
   * @param pThread The MPORThread that has a new CFANode (= state)
   * @param pUpdatedNode The updated CFANode (= state) of pThread
   * @return a new MPORState with the updated value pUpdatedNode at key pThread and the
   *     corresponding PreferenceOrders
   */
  public MPORState createUpdatedState(MPORThread pThread, CFANode pUpdatedNode) {
    checkArgument(threadNodes.containsKey(pThread), "threadNodes must contain pThread");
    ImmutableMap.Builder<MPORThread, CFANode> updatedThreadNodes = ImmutableMap.builder();
    for (var entry : threadNodes.entrySet()) {
      if (!entry.getKey().equals(pThread)) {
        updatedThreadNodes.put(entry);
      }
    }
    updatedThreadNodes.put(pThread, pUpdatedNode);
    return new MPORState(
        updatedThreadNodes.build(),
        MPORAlgorithm.getPreferenceOrdersForThreadNodes(updatedThreadNodes.build()));
  }
}
