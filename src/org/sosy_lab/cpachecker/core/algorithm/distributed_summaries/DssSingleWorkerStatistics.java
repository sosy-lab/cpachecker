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
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.DssAllWorkerStatistics.StatisticsKey;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DssBlockAnalysisStatistics;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DssThreadCpuTimer;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

/** Statistics collected by a single DSS analysis worker. */
public class DssSingleWorkerStatistics implements Statistics {

  private final String blockId;

  private final DssThreadCpuTimer analyzePreconditionTime =
      new DssThreadCpuTimer("Analyze Precondition");
  private final DssThreadCpuTimer storePreconditionTime =
      new DssThreadCpuTimer("Store Precondition");
  private final DssThreadCpuTimer analyzeViolationConditionTime =
      new DssThreadCpuTimer("Analyze Violation Condition");
  private final DssThreadCpuTimer storeViolationConditionTime =
      new DssThreadCpuTimer("Store Violation Condition");

  private @Nullable DssBlockAnalysisStatistics dcpaStatistics;

  private final StatCounter analyzePreconditionCount = new StatCounter("Analyze Precondition");
  private final StatCounter storePreconditionCount = new StatCounter("Store Precondition");
  private final StatCounter analyzeViolationConditionCount =
      new StatCounter("Analyze Violation Condition");
  private final StatCounter storeViolationConditionCount =
      new StatCounter("Store Violation Condition");

  public DssSingleWorkerStatistics(String pBlockId) {
    blockId = pBlockId;
  }

  public DssThreadCpuTimer getAnalyzePreconditionTimer() {
    return analyzePreconditionTime;
  }

  public DssThreadCpuTimer getStorePreconditionTimer() {
    return storePreconditionTime;
  }

  public DssThreadCpuTimer getAnalyzeViolationConditionTimer() {
    return analyzeViolationConditionTime;
  }

  public DssThreadCpuTimer getStoreViolationConditionTimer() {
    return storeViolationConditionTime;
  }

  public void setDcpaStatistics(DssBlockAnalysisStatistics pDcpaStatistics) {
    dcpaStatistics = pDcpaStatistics;
  }

  public StatCounter getAnalyzePreconditionCounter() {
    return analyzePreconditionCount;
  }

  public StatCounter getStorePreconditionCounter() {
    return storePreconditionCount;
  }

  public StatCounter getAnalyzeViolationConditionCounter() {
    return analyzeViolationConditionCount;
  }

  public StatCounter getStoreViolationConditionCounter() {
    return storeViolationConditionCount;
  }

  public @Nullable DssBlockAnalysisStatistics getDcpaStatistics() {
    return dcpaStatistics;
  }

  public long getValue(StatisticsKey key) {
    return switch (key) {
      case SERIALIZATION_COUNT ->
          dcpaStatistics != null ? dcpaStatistics.getSerializationCount().getUpdateCount() : 0;
      case DESERIALIZATION_COUNT ->
          dcpaStatistics != null ? dcpaStatistics.getDeserializationCount().getUpdateCount() : 0;
      case PROCEED_COUNT ->
          dcpaStatistics != null ? dcpaStatistics.getProceedCount().getUpdateCount() : 0;
      case SERIALIZATION_TIME ->
          dcpaStatistics != null ? dcpaStatistics.getSerializationTime().nanos() : 0;
      case DESERIALIZATION_TIME ->
          dcpaStatistics != null ? dcpaStatistics.getDeserializationTime().nanos() : 0;
      case PROCEED_TIME -> dcpaStatistics != null ? dcpaStatistics.getProceedTime().nanos() : 0;
      case ANALYZE_PRECONDITION_COUNT -> analyzePreconditionCount.getUpdateCount();
      case ANALYZE_PRECONDITION_TIME -> analyzePreconditionTime.nanos();
      case STORE_PRECONDITION_COUNT -> storePreconditionCount.getUpdateCount();
      case STORE_PRECONDITION_TIME -> storePreconditionTime.nanos();
      case ANALYZE_VIOLATION_CONDITION_COUNT -> analyzeViolationConditionCount.getUpdateCount();
      case ANALYZE_VIOLATION_CONDITION_TIME -> analyzeViolationConditionTime.nanos();
      case STORE_VIOLATION_CONDITION_COUNT -> storeViolationConditionCount.getUpdateCount();
      case STORE_VIOLATION_CONDITION_TIME -> storeViolationConditionTime.nanos();
    };
  }

  @Override
  public void printStatistics(PrintStream out, Result pResult, UnmodifiableReachedSet pReached) {
    StatisticsWriter writer =
        StatisticsWriter.writingStatisticsTo(out)
            .put("DSS Block Worker Statistics for", blockId)
            .beginLevel();

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
            StatisticsKey.ANALYZE_PRECONDITION_COUNT.getKey(),
            analyzePreconditionCount.getUpdateCount())
        .put(
            StatisticsKey.ANALYZE_PRECONDITION_TIME.getKey(),
            formatNanos(analyzePreconditionTime.nanos()))
        .put(
            StatisticsKey.STORE_PRECONDITION_COUNT.getKey(),
            storePreconditionCount.getUpdateCount())
        .put(
            StatisticsKey.STORE_PRECONDITION_TIME.getKey(),
            formatNanos(storePreconditionTime.nanos()))
        .put(
            StatisticsKey.ANALYZE_VIOLATION_CONDITION_COUNT.getKey(),
            analyzeViolationConditionCount.getUpdateCount())
        .put(
            StatisticsKey.ANALYZE_VIOLATION_CONDITION_TIME.getKey(),
            formatNanos(analyzeViolationConditionTime.nanos()))
        .put(
            StatisticsKey.STORE_VIOLATION_CONDITION_COUNT.getKey(),
            storeViolationConditionCount.getUpdateCount())
        .put(
            StatisticsKey.STORE_VIOLATION_CONDITION_TIME.getKey(),
            formatNanos(storeViolationConditionTime.nanos()));
  }

  static String formatNanos(long nanos) {
    return TimeSpan.ofNanos(nanos).formatAs(TimeUnit.SECONDS);
  }

  @Override
  public @Nullable String getName() {
    return "DSS Block Worker Statistics for " + blockId;
  }
}
