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
}
