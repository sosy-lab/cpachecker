// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.callstack;

import com.google.common.base.Function;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessageWithStates;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.deserialize.DeserializeOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;
import org.sosy_lab.cpachecker.cpa.callstack.DssCallstackCPA;

public class DeserializeCallstackStateOperator implements DeserializeOperator {

  private final DssCallstackCPA parentCPA;
  private final Function<Integer, CFANode> converter;
  private final BlockNode blockNode;

  public DeserializeCallstackStateOperator(
      DssCallstackCPA pParentCPA, BlockNode pBlockNode, Function<Integer, CFANode> pConverter) {
    parentCPA = pParentCPA;
    converter = pConverter;
    blockNode = pBlockNode;
  }

  @Override
  public AbstractState deserialize(DssMessageWithStates pMessage, int pStateIndex) {
    DssCallstackStatePayload statePayload =
        DssCallstackStatePayload.fromContentMap(pMessage.getStates().get(pStateIndex));
    if (statePayload.states().isEmpty()) {
      CFANode location = DeserializeOperator.startLocationFromMessageType(pMessage, blockNode);
      return parentCPA.createState(
          null, location.getFunctionName(), location, statePayload.allowAllTransfers());
    }
    CallstackState current = null;
    for (DssCallstackEntryPayload entryPayload : statePayload.states()) {
      current =
          parentCPA.createState(
              current,
              entryPayload.function(),
              Objects.requireNonNull(converter.apply(entryPayload.nodeId())),
              statePayload.allowAllTransfers());
    }
    return current;
  }
}
