// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor;

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
   * TODO
   *
   * @param pThread TODO
   * @param pUpdatedNode TODO
   * @return TODO
   */
  public MPORState createUpdatedState(MPORThread pThread, CFANode pUpdatedNode) {
    ImmutableMap.Builder<MPORThread, CFANode> updatedThreadNodes = ImmutableMap.builder();
    for (var entry : threadNodes.entrySet()) {
      if (!entry.getKey().equals(pThread)) {
        updatedThreadNodes.put(entry);
      }
    }
    updatedThreadNodes.put(pThread, pUpdatedNode); // overwrite previous CFANode
    return new MPORState(
        updatedThreadNodes.build(),
        MPORAlgorithm.getPreferenceOrdersForThreadNodes(updatedThreadNodes.build()));
  }
}
