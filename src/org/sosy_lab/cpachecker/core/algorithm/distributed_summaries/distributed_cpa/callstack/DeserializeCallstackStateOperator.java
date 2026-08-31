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
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.ContentReader;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessage;
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
  public AbstractState deserialize(DssMessage pMessage, int pStateIndex) {
    ContentReader content = pMessage.getAbstractStateContent(CallstackState.class, pStateIndex);
    String stateJson = content.get(STATE_KEY);
    assert stateJson != null;
    // states of a block analysis that did not know its callstack must stay unrestricted
    boolean allowAllTransfers =
        Boolean.parseBoolean(
            content.getOrDefault(DistributedCallstackCPA.ALLOW_ALL_TRANSFERS_KEY, "false"));
    if (stateJson.isBlank()) {
      CFANode location = DeserializeOperator.startLocationFromMessageType(pMessage, blockNode);
      return parentCPA.createState(null, location.getFunctionName(), location, allowAllTransfers);
    }
    List<String> parts = Splitter.on(DistributedCallstackCPA.DELIMITER).splitToList(stateJson);
    CallstackState current = null;
    for (String part : parts) {
      List<String> properties = Splitter.on(".").limit(2).splitToList(part);
      current =
          parentCPA.createState(
              current,
              properties.get(1),
              Objects.requireNonNull(converter.apply(Integer.parseInt(properties.getFirst()))),
              allowAllTransfers);
    }
    return current;
  }
}
