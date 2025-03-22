// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.sosy_lab.common.time.Tickers;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.DssStatisticsMessage.DssStatisticType;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;

public class DssBlockAnalysisStatistics {

  public static class ThreadCPUTimer {
    private long sum;
    private long lastStart;
    private final String name;
    private boolean running;

    public ThreadCPUTimer(String pName) {
      sum = 0;
      lastStart = 0;
      name = pName;
      running = false;
    }

    public void start() {
      checkState(!running, "Timer has already been started.");
      lastStart = Tickers.getCurrentThreadCputime().read();
      running = true;
    }

    public void stop() {
      checkState(running, "Timer needs to be started first.");
      sum += Tickers.getCurrentThreadCputime().read() - lastStart;
      running = false;
    }

    public long nanos() {
      return sum;
    }

    public String getName() {
      return name;
    }
  }

  private final StatCounter serializationCount;
  private final StatCounter deserializationCount;

  private final ThreadCPUTimer serializationTime;
  private final ThreadCPUTimer deserializationTime;
  private final StatCounter proceedCount;
  private final ThreadCPUTimer proceedTime;

  public DssBlockAnalysisStatistics(String pId) {
    serializationCount = new StatCounter("Serialization Count " + pId);
    deserializationCount = new StatCounter("Deserialization Count " + pId);
    proceedCount = new StatCounter("Proceed Count " + pId);

    serializationTime = new ThreadCPUTimer("Serialization Time " + pId);
    deserializationTime = new ThreadCPUTimer("Deserialization Time " + pId);
    proceedTime = new ThreadCPUTimer("Proceed Time " + pId);
  }

  public StatCounter getDeserializationCount() {
    return deserializationCount;
  }

  public StatCounter getSerializationCount() {
    return serializationCount;
  }

  public ThreadCPUTimer getDeserializationTime() {
    return deserializationTime;
  }

  public ThreadCPUTimer getProceedTime() {
    return proceedTime;
  }

  public ThreadCPUTimer getSerializationTime() {
    return serializationTime;
  }

  public Map<String, Object> getStatistics() {
    return ImmutableMap.<String, Object>builder()
        .put(
            DssStatisticType.SERIALIZATION_COUNT.name(),
            Integer.toString(serializationCount.getUpdateCount()))
        .put(
            DssStatisticType.DESERIALIZATION_COUNT.name(),
            Integer.toString(deserializationCount.getUpdateCount()))
        .put(DssStatisticType.PROCEED_COUNT.name(), Integer.toString(proceedCount.getUpdateCount()))
        .put(DssStatisticType.SERIALIZATION_TIME.name(), serializationTime.nanos())
        .put(DssStatisticType.DESERIALIZATION_TIME.name(), deserializationTime.nanos())
        .put(DssStatisticType.PROCEED_TIME.name(), proceedTime.nanos())
        .buildOrThrow();
  }
}
