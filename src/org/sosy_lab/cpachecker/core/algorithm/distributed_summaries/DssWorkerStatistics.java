// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DssBlockAnalysisStatistics;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

/**
 * Aggregate statistics for all DSS analysis workers. {@link DssBlockWorkerStatistics} objects are registered at
 * worker creation time via {@link #createWorkerStats} and written to by the workers directly.
 */
public class DssWorkerStatistics implements Statistics {

  /** Keys used to label statistics collected from DSS analysis workers. */
  public enum StatisticsKey {
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

  private final List<DssBlockWorkerStatistics> workerStats = new ArrayList<>();
  private final boolean printBlockLevelStats;

  public DssWorkerStatistics(boolean pPrintBlockLevelStats) {
    printBlockLevelStats = pPrintBlockLevelStats;
  }

  public synchronized DssBlockWorkerStatistics createWorkerStats(String pWorkerId) {
    DssBlockWorkerStatistics stats = new DssBlockWorkerStatistics(pWorkerId);
    workerStats.add(stats);
    return stats;
  }

  @Override
  public void printStatistics(PrintStream out, Result pResult, UnmodifiableReachedSet pReached) {
    if (printBlockLevelStats) {
      for (DssBlockWorkerStatistics ws : workerStats) {
        ws.printStatistics(out, pResult, pReached);
      }
    }
    printOverallStats(out);
  }

  private void printOverallStats(PrintStream out) {
    long serializationCount = 0;
    long deserializationCount = 0;
    long proceedCount = 0;
    long serializationNanos = 0;
    long deserializationNanos = 0;
    long proceedNanos = 0;
    long analyzePreconditionNanos = 0;
    long storePreconditionNanos = 0;
    long analyzeViolationConditionNanos = 0;
    long storeViolationConditionNanos = 0;
    long messagesSent = 0;
    long messagesReceived = 0;

    for (DssBlockWorkerStatistics ws : workerStats) {
      analyzePreconditionNanos += ws.getAnalyzePreconditionNanos();
      storePreconditionNanos += ws.getStorePreconditionNanos();
      analyzeViolationConditionNanos += ws.getAnalyzeViolationConditionNanos();
      storeViolationConditionNanos += ws.getStoreViolationConditionNanos();
      messagesSent += ws.getMessagesSent().getUpdateCount();
      messagesReceived += ws.getMessagesReceived().getUpdateCount();
      @Nullable DssBlockAnalysisStatistics dcpa = ws.getDcpaStatistics();
      if (dcpa != null) {
        serializationCount += dcpa.getSerializationCount().getUpdateCount();
        deserializationCount += dcpa.getDeserializationCount().getUpdateCount();
        proceedCount += dcpa.getProceedCount().getUpdateCount();
        serializationNanos += dcpa.getSerializationTime().nanos();
        deserializationNanos += dcpa.getDeserializationTime().nanos();
        proceedNanos += dcpa.getProceedTime().nanos();
      }
    }

    StatisticsWriter.writingStatisticsTo(out)
        .put("Overall Worker Statistics", "Sum across all blocks")
        .beginLevel()
        .put(StatisticsKey.SERIALIZATION_COUNT.getKey(), Long.toString(serializationCount))
        .put(StatisticsKey.DESERIALIZATION_COUNT.getKey(), Long.toString(deserializationCount))
        .put(StatisticsKey.PROCEED_COUNT.getKey(), Long.toString(proceedCount))
        .put(
            StatisticsKey.SERIALIZATION_TIME.getKey(),
            DssBlockWorkerStatistics.formatNanos(serializationNanos))
        .put(
            StatisticsKey.DESERIALIZATION_TIME.getKey(),
            DssBlockWorkerStatistics.formatNanos(deserializationNanos))
        .put(
            StatisticsKey.PROCEED_TIME.getKey(), DssBlockWorkerStatistics.formatNanos(proceedNanos))
        .put(
            StatisticsKey.ANALYZE_PRECONDITION_TIME.getKey(),
            DssBlockWorkerStatistics.formatNanos(analyzePreconditionNanos))
        .put(
            StatisticsKey.STORE_PRECONDITION_TIME.getKey(),
            DssBlockWorkerStatistics.formatNanos(storePreconditionNanos))
        .put(
            StatisticsKey.ANALYZE_VIOLATION_CONDITION_TIME.getKey(),
            DssBlockWorkerStatistics.formatNanos(analyzeViolationConditionNanos))
        .put(
            StatisticsKey.STORE_VIOLATION_CONDITION_TIME.getKey(),
            DssBlockWorkerStatistics.formatNanos(storeViolationConditionNanos))
        .put(StatisticsKey.MESSAGES_SENT.getKey(), Long.toString(messagesSent))
        .put(StatisticsKey.MESSAGES_RECEIVED.getKey(), Long.toString(messagesReceived));
  }

  @Override
  public @Nullable String getName() {
    return "DSS Worker Statistics";
  }
}
