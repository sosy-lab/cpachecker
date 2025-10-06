// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.callstack;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.ContentBuilder;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.serialize.SerializeOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;

public class SerializeCallstackStateOperator implements SerializeOperator {
  private final Map<CFANode, Integer> nodeToId;

  public SerializeCallstackStateOperator(Map<CFANode, Integer> pNodeToId) {
    nodeToId = pNodeToId;
  }

  @Override
  public ImmutableMap<String, String> serialize(AbstractState pCallstackState) {
    List<String> states = new ArrayList<>();
    CallstackState callstackState = (CallstackState) pCallstackState;
    while (callstackState != null) {
      states.add(
          nodeToId.get(callstackState.getCallNode()) + "." + callstackState.getCurrentFunction());
      callstackState = callstackState.getPreviousState();
    }
    Collections.reverse(states);
    String result = Joiner.on(DistributedCallstackCPA.DELIMITER).join(states);
    return ContentBuilder.builder()
        .pushLevel(CallstackState.class.getName())
        .put(STATE_KEY, result)
        .build();
  }
}
