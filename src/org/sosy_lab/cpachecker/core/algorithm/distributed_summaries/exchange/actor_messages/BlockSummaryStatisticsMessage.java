// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages;

import com.google.common.collect.ImmutableMap;
import java.time.Instant;
import java.util.Map;
import java.util.Map.Entry;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.BlockSummaryMessagePayload;

public class BlockSummaryStatisticsMessage extends BlockSummaryMessage {

  public enum StatisticsTypes {
    SERIALIZATION_COUNT("number of serializations"),
    SERIALIZATION_TIME("time for serialization"),
    DESERIALIZATION_COUNT("number of deserializations"),
    DESERIALIZATION_TIME("time for deserialization"),
    PROCEED_COUNT("number of calls to proceed"),
    PROCEED_TIME("time to decide whether to proceed"),
    COMBINE_COUNT("number of combines"),
    COMBINE_TIME("time for combining"),
    FORWARD_TIME("time for forward analysis"),
    BACKWARD_TIME("time for backward analysis"),
    BACKWARD_ABSTRACTION_TIME("time for backward analysis with abstraction"),
    MESSAGES_SENT("number of messages sent"),
    MESSAGES_RECEIVED("number of messages received"),
    FORWARD_ANALYSIS_STATS("forward"),
    BACKWARD_ANALYSIS_STATS("backward");

    private final String name;

    StatisticsTypes(String pName) {
      name = pName;
    }

    public String getName() {
      return name;
    }
  }


  protected BlockSummaryStatisticsMessage(String pUniqueBlockId, int pTargetNodeNumber,
      BlockSummaryMessagePayload pPayload, Instant pTimeStamp) {
    super(MessageType.STATISTICS, pUniqueBlockId, pTargetNodeNumber, pPayload, pTimeStamp);
  }

  @Override
  protected BlockSummaryMessage replacePayload(BlockSummaryMessagePayload pPayload) {
    return new BlockSummaryStatisticsMessage(getUniqueBlockId(), getTargetNodeNumber(), pPayload, getTimestamp());
  }

  public Map<String, Object> getStats() {
    Object result = getPayload().get(BlockSummaryMessagePayload.STATS);
    if (!(result instanceof Map)) {
      throw new AssertionError("Stats have to be present");
    }
    Map<?, ?> resultMap = (Map<?, ?>) result;
    ImmutableMap.Builder<String, Object> converted = ImmutableMap.builder();
    for (Entry<?, ?> entry : resultMap.entrySet()) {
      assert entry.getKey() instanceof String;
      converted.put((String) entry.getKey(), entry.getValue());
    }
    return converted.build();
  }
}
