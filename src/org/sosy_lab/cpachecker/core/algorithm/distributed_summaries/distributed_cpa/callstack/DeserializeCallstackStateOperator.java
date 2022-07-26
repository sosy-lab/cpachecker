// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.callstack;

import com.google.common.base.Splitter;
import java.util.List;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.DeserializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.ActorMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.Payload;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackCPA;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;

public class DeserializeCallstackStateOperator implements DeserializeOperator {

  private final CallstackCPA parentCPA;
  private final BlockNode block;

  public DeserializeCallstackStateOperator(CallstackCPA pParentCPA, BlockNode pBlockNode) {
    parentCPA = pParentCPA;
    block = pBlockNode;
  }

  @Override
  public AbstractState deserialize(ActorMessage pMessage) {
    Payload payload = pMessage.getPayload();
    if (!payload.containsKey(parentCPA.getClass().getName())) {
      return parentCPA.getInitialState(
          block.getNodeWithNumber(pMessage.getTargetNodeNumber()),
          StateSpacePartition.getDefaultPartition());
    }
    String callstackJSON = payload.getOrDefault(parentCPA.getClass().getName(), "");
    if (callstackJSON.isBlank()) {
      return parentCPA.getInitialState(
          block.getNodeWithNumber(pMessage.getTargetNodeNumber()),
          StateSpacePartition.getDefaultPartition());
    }
    List<String> parts = Splitter.on(DistributedCallstackCPA.DELIMITER).splitToList(callstackJSON);
    CallstackState previous = null;
    for (String part : parts) {
      List<String> properties = Splitter.on(".").limit(2).splitToList(part);
      previous =
          new CallstackState(
              previous,
              properties.get(1),
              block.getNodeWithNumber(Integer.parseInt(properties.get(0))));
    }
    return previous;
  }
}
