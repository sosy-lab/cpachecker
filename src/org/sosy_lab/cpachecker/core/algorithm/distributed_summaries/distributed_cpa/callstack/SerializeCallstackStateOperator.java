// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.callstack;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.serialize.SerializeOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;

public class SerializeCallstackStateOperator implements SerializeOperator {
  private final Map<CFANode, Integer> nodeToId;

  public SerializeCallstackStateOperator(Map<CFANode, Integer> pNodeToId) {
    nodeToId = pNodeToId;
  }

  @Override
  public DssCallstackStatePayload serialize(AbstractState pCallstackState) {
    boolean allowAllTransfers = DistributedCallstackCPA.allowsAllTransfers(pCallstackState);
    if (allowAllTransfers) {
      // the sending block did not know its callstack, so the stack carries no information:
      // the receiving block starts with a fresh stack at its own initial location
      return new DssCallstackStatePayload(true, ImmutableList.of());
    }
    List<DssCallstackEntryPayload> states = new ArrayList<>();
    CallstackState callstackState = (CallstackState) pCallstackState;
    while (callstackState != null) {
      states.add(
          new DssCallstackEntryPayload(
              nodeToId.get(callstackState.getCallNode()), callstackState.getCurrentFunction()));
      callstackState = callstackState.getPreviousState();
    }
    Collections.reverse(states);
    return new DssCallstackStatePayload(false, ImmutableList.copyOf(states));
  }
}
