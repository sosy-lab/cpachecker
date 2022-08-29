// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryStatisticsMessage.BlockSummaryStatisticType;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;

public class BlockAnalysisStatistics {

  private final StatCounter serializationCount;
  private final StatCounter deserializationCount;
  private final StatCounter combineCount;
  private final StatCounter proceedCount;

  private final StatTimer serializationTime;
  private final StatTimer deserializationTime;
  private final StatTimer combineTime;
  private final StatTimer proceedTime;

  public BlockAnalysisStatistics(String pId) {
    serializationCount = new StatCounter("Serialization Count " + pId);
    deserializationCount = new StatCounter("Deserialization Count " + pId);
    proceedCount = new StatCounter("Proceed Count " + pId);
    combineCount = new StatCounter("Combine Count " + pId);

    serializationTime = new StatTimer("Serialization Time " + pId);
    deserializationTime = new StatTimer("Deserialization Time " + pId);
    proceedTime = new StatTimer("Proceed Time " + pId);
    combineTime = new StatTimer("Combine Time " + pId);
  }

  public StatCounter getCombineCount() {
    return combineCount;
  }

  public StatCounter getDeserializationCount() {
    return deserializationCount;
  }

  public StatCounter getProceedCount() {
    return proceedCount;
  }

  public StatCounter getSerializationCount() {
    return serializationCount;
  }

  public StatTimer getCombineTime() {
    return combineTime;
  }

  public StatTimer getDeserializationTime() {
    return deserializationTime;
  }

  public StatTimer getProceedTime() {
    return proceedTime;
  }

  public StatTimer getSerializationTime() {
    return serializationTime;
  }

  public Map<String, Object> getStatistics() {
    return ImmutableMap.<String, Object>builder()
        .put(
            BlockSummaryStatisticType.SERIALIZATION_COUNT.name(),
            Integer.toString(serializationCount.getUpdateCount()))
        .put(
            BlockSummaryStatisticType.DESERIALIZATION_COUNT.name(),
            Integer.toString(deserializationCount.getUpdateCount()))
        .put(
            BlockSummaryStatisticType.PROCEED_COUNT.name(),
            Integer.toString(proceedCount.getUpdateCount()))
        .put(
            BlockSummaryStatisticType.COMBINE_COUNT.name(),
            Integer.toString(combineCount.getUpdateCount()))
        .put(
            BlockSummaryStatisticType.SERIALIZATION_TIME.name(),
            serializationTime.getConsumedTime().asNanos())
        .put(
            BlockSummaryStatisticType.DESERIALIZATION_TIME.name(),
            deserializationTime.getConsumedTime().asNanos())
        .put(BlockSummaryStatisticType.PROCEED_TIME.name(), proceedTime.getConsumedTime().asNanos())
        .put(BlockSummaryStatisticType.COMBINE_TIME.name(), combineTime.getConsumedTime().asNanos())
        .buildOrThrow();
  }
}
