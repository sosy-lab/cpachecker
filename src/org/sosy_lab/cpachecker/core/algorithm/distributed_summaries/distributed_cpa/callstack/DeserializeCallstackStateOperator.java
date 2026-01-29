// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.callstack;

import com.google.common.base.Function;
import com.google.common.base.Splitter;
import java.util.List;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.deserialize.DeserializeOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackCPA;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;

public class DeserializeCallstackStateOperator implements DeserializeOperator {

  private final CallstackCPA parentCPA;
  private final Function<Integer, CFANode> converter;
  private final BlockNode blockNode;

  public DeserializeCallstackStateOperator(
      CallstackCPA pParentCPA, BlockNode pBlockNode, Function<Integer, CFANode> pConverter) {
    parentCPA = pParentCPA;
    converter = pConverter;
    blockNode = pBlockNode;
  }

  @Override
  public AbstractState deserialize(DssMessage pMessage) {
    String stateJson = pMessage.getAbstractStateContent(CallstackState.class).get(STATE_KEY);
    assert stateJson != null;
    if (stateJson.isBlank()) {
      CFANode location = DeserializeOperator.startLocationFromMessageType(pMessage, blockNode);
      return parentCPA.getInitialState(location, StateSpacePartition.getDefaultPartition());
    }
    List<String> parts = Splitter.on(DistributedCallstackCPA.DELIMITER).splitToList(stateJson);
    CallstackState previous = null;
    for (String part : parts) {
      List<String> properties = Splitter.on(".").limit(2).splitToList(part);
      previous =
          new CallstackState(
              previous,
              properties.get(1),
              Objects.requireNonNull(converter.apply(Integer.parseInt(properties.getFirst()))));
    }
    return previous;
  }
}
