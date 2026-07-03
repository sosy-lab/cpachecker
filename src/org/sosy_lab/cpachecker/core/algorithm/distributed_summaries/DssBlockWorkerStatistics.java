// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries;

import java.io.PrintStream;
import java.util.concurrent.TimeUnit;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.time.TimeSpan;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.DssWorkerStatistics.StatisticsKey;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DssBlockAnalysisStatistics;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DssThreadCPUTimer;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

/** Statistics collected by a single DSS analysis worker. */
public class DssBlockWorkerStatistics implements Statistics {

  private final String blockId;

  private final DssThreadCPUTimer analyzePreconditionTime =
      new DssThreadCPUTimer("Analyze Precondition");
  private final DssThreadCPUTimer storePreconditionTime =
      new DssThreadCPUTimer("Store Precondition");
  private final DssThreadCPUTimer analyzeViolationConditionTime =
      new DssThreadCPUTimer("Analyze Violation Condition");
  private final DssThreadCPUTimer storeViolationConditionTime =
      new DssThreadCPUTimer("Store Violation Condition");

  private @Nullable DssBlockAnalysisStatistics dcpaStatistics;

  private final StatCounter messagesSent = new StatCounter("Messages Sent");
  private final StatCounter messagesReceived = new StatCounter("Messages Received");

  public DssBlockWorkerStatistics(String pBlockId) {
    blockId = pBlockId;
  }

  public DssThreadCPUTimer getAnalyzePreconditionTime() {
    return analyzePreconditionTime;
  }

  public DssThreadCPUTimer getStorePreconditionTime() {
    return storePreconditionTime;
  }

  public DssThreadCPUTimer getAnalyzeViolationConditionTime() {
    return analyzeViolationConditionTime;
  }

  public DssThreadCPUTimer getStoreViolationConditionTime() {
    return storeViolationConditionTime;
  }

  public void setDcpaStatistics(DssBlockAnalysisStatistics pDcpaStatistics) {
    dcpaStatistics = pDcpaStatistics;
  }

  public long getAnalyzePreconditionNanos() {
    return analyzePreconditionTime.nanos();
  }

  public long getStorePreconditionNanos() {
    return storePreconditionTime.nanos();
  }

  public long getAnalyzeViolationConditionNanos() {
    return analyzeViolationConditionTime.nanos();
  }

  public long getStoreViolationConditionNanos() {
    return storeViolationConditionTime.nanos();
  }

  public StatCounter getMessagesSent() {
    return messagesSent;
  }

  public StatCounter getMessagesReceived() {
    return messagesReceived;
  }

  public @Nullable DssBlockAnalysisStatistics getDcpaStatistics() {
    return dcpaStatistics;
  }

  @Override
  public void printStatistics(PrintStream out, Result pResult, UnmodifiableReachedSet pReached) {
    StatisticsWriter writer =
        StatisticsWriter.writingStatisticsTo(out).put("Block " + blockId, blockId).beginLevel();

    if (dcpaStatistics != null) {
      writer
          .put(
              StatisticsKey.SERIALIZATION_COUNT.getKey(),
              dcpaStatistics.getSerializationCount().getUpdateCount())
          .put(
              StatisticsKey.DESERIALIZATION_COUNT.getKey(),
              dcpaStatistics.getDeserializationCount().getUpdateCount())
          .put(
              StatisticsKey.PROCEED_COUNT.getKey(),
              dcpaStatistics.getProceedCount().getUpdateCount())
          .put(
              StatisticsKey.SERIALIZATION_TIME.getKey(),
              formatNanos(dcpaStatistics.getSerializationTime().nanos()))
          .put(
              StatisticsKey.DESERIALIZATION_TIME.getKey(),
              formatNanos(dcpaStatistics.getDeserializationTime().nanos()))
          .put(
              StatisticsKey.PROCEED_TIME.getKey(),
              formatNanos(dcpaStatistics.getProceedTime().nanos()));
    }

    writer
        .put(
            StatisticsKey.ANALYZE_PRECONDITION_TIME.getKey(),
            formatNanos(analyzePreconditionTime.nanos()))
        .put(
            StatisticsKey.STORE_PRECONDITION_TIME.getKey(),
            formatNanos(storePreconditionTime.nanos()))
        .put(
            StatisticsKey.ANALYZE_VIOLATION_CONDITION_TIME.getKey(),
            formatNanos(analyzeViolationConditionTime.nanos()))
        .put(
            StatisticsKey.STORE_VIOLATION_CONDITION_TIME.getKey(),
            formatNanos(storeViolationConditionTime.nanos()))
        .put(StatisticsKey.MESSAGES_SENT.getKey(), messagesSent.getUpdateCount())
        .put(StatisticsKey.MESSAGES_RECEIVED.getKey(), messagesReceived.getUpdateCount());
  }

  static String formatNanos(long nanos) {
    return TimeSpan.ofNanos(nanos).formatAs(TimeUnit.SECONDS);
  }

  @Override
  public @Nullable String getName() {
    return null;
  }
}
