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
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.DssMessagePayload;

public class DssStatisticsMessage extends DssMessage {

  public enum DssStatisticType {
    SERIALIZATION_COUNT("number of serializations", false),
    SERIALIZATION_TIME("time for serialization", true),
    DESERIALIZATION_COUNT("number of deserializations", false),
    DESERIALIZATION_TIME("time for deserialization", true),
    PROCEED_COUNT("number of calls to proceed", false),
    PROCEED_TIME("time to decide whether to proceed", true),
    COMBINE_COUNT("number of combines", false),
    COMBINE_TIME("time for combining", true),
    FORWARD_TIME("time for forward analysis", true),
    BACKWARD_TIME("time for backward analysis", true),
    BACKWARD_ABSTRACTION_TIME("time for backward analysis with abstraction", true),
    MESSAGES_SENT("number of messages sent", false),
    MESSAGES_RECEIVED("number of messages received", false),
    FORWARD_ANALYSIS_STATS("forward", false),
    BACKWARD_ANALYSIS_STATS("backward", false);

    private final String name;
    private final boolean formatAsTime;

    DssStatisticType(String pName, boolean pFormatAsTime) {
      name = pName;
      formatAsTime = pFormatAsTime;
    }

    public boolean isFormatAsTime() {
      return formatAsTime;
    }

    public String getName() {
      return name;
    }
  }

  protected DssStatisticsMessage(
      String pUniqueBlockId, int pTargetNodeNumber, DssMessagePayload pPayload) {
    this(pUniqueBlockId, pTargetNodeNumber, pPayload, null);
  }

  /**
   * Creates a new instance of this object.
   *
   * @deprecated for debug mode only. use {@link #DssStatisticsMessage(String, int,
   *     DssMessagePayload)} instead.
   */
  @Deprecated
  protected DssStatisticsMessage(
      String pUniqueBlockId,
      int pTargetNodeNumber,
      DssMessagePayload pPayload,
      @Nullable Instant pTimeStamp) {
    super(MessageType.STATISTICS, pUniqueBlockId, pTargetNodeNumber, pPayload, pTimeStamp);
  }

  public Map<String, Object> getStats() {
    Object result = getPayload().get(DssMessagePayload.STATS);
    if (result instanceof Map<?, ?> resultMap) {
      ImmutableMap.Builder<String, Object> converted = ImmutableMap.builder();
      for (Entry<?, ?> entry : resultMap.entrySet()) {
        assert entry.getKey() instanceof String;
        converted.put((String) entry.getKey(), entry.getValue());
      }
      return converted.buildOrThrow();
    }
    throw new AssertionError("Statistics have to be present");
  }
}
