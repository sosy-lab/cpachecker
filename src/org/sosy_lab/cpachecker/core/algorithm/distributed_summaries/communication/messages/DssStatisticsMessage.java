// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;
import java.util.Map;

public class DssStatisticsMessage extends DssMessage {

  public enum StatisticsKey {
    // Define your statistics keys here
    SERIALIZATION_COUNT("number of serialized states", false),
    DESERIALIZATION_COUNT("number of deserialized states", false),
    PROCEED_COUNT("number of proceeded states", false),
    SERIALIZATION_TIME("time spent serializing states", true),
    DESERIALIZATION_TIME("time spent deserializing states", true),
    PROCEED_TIME("time spent processing states", true),
    MESSAGES_SENT("number of messages sent", false),
    MESSAGES_RECEIVED("number of messages received", false),
    ANALYZE_PRECONDITION_TIME("time spent in analyzing preconditions", true),
    STORE_PRECONDITION_TIME("time spent in storing preconditions", true),
    ANALYZE_VIOLATION_CONDITION_TIME("time spent in analyzing violation conditions", true),
    STORE_VIOLATION_CONDITION_TIME("time spent in storing violation conditions", true);

    private final String key;
    private final boolean formatAsTime;

    StatisticsKey(String pKey, boolean pFormatAsTime) {
      key = pKey;
      formatAsTime = pFormatAsTime;
    }

    public String getKey() {
      return key;
    }

    public boolean isFormattedAsTime() {
      return formatAsTime;
    }
  }

  DssStatisticsMessage(String pSenderId, ImmutableMap<String, String> pContent) {
    super(pSenderId, DssMessageType.STATISTIC, pContent);
  }

  @Override
  boolean isValid(Map<String, String> pContent) {
    return FluentIterable.from(StatisticsKey.values())
        .transform(StatisticsKey::name)
        .toSet()
        .containsAll(pContent.keySet());
  }
}
