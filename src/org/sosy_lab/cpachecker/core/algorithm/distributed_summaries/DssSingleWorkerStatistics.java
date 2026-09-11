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
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DssBlockAnalysisStatistics;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DssThreadCpuTimer;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;
import org.sosy_lab.cpachecker.util.statistics.StatInt;
import org.sosy_lab.cpachecker.util.statistics.StatKind;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

/** Statistics collected by a single DSS analysis worker. */
public class DssSingleWorkerStatistics implements Statistics {

  /** Keys defining the statistics that are collected for a single DSS analysis worker. */
  public enum StatisticsKey {
    SERIALIZATION_COUNT("number of serialized states", false),
    DESERIALIZATION_COUNT("number of deserialized states", false),
    PROCEED_COUNT("number of proceeded states", false),
    SERIALIZATION_TIME("time spent serializing states", true),
    DESERIALIZATION_TIME("time spent deserializing states", true),
    PROCEED_TIME("time spent processing states", true),
    BLOCK_ANALYSIS_COUNT("number of block analyses", false),
    BLOCK_ANALYSIS_TIME("time spent in block analyses", true),
    STORE_PRECONDITION_STATES_COUNT("number of precondition states stored", false),
    STORE_PRECONDITION_STATES_TIME("time spent in storing precondition states", true),
    STORE_VIOLATION_CONDITION_STATES_COUNT("number of violation condition states stored", false),
    STORE_VIOLATION_CONDITION_STATES_TIME("time spent in storing violation condition states", true),
    SERIALIZED_STATES_SIZE("serialized states size (chars)", false);

    private final String label;
    private final boolean formatAsTime;

    StatisticsKey(String pLabel, boolean pFormatAsTime) {
      label = pLabel;
      formatAsTime = pFormatAsTime;
    }

    public String getLabel() {
      return label;
    }

    public boolean isFormattedAsTime() {
      return formatAsTime;
    }
  }

  private final String blockId;

  private final DssThreadCpuTimer storeViolationConditionStatesTime =
      new DssThreadCpuTimer(StatisticsKey.STORE_VIOLATION_CONDITION_STATES_TIME.getLabel());
  private final DssThreadCpuTimer storePreconditionStatesTime =
      new DssThreadCpuTimer(StatisticsKey.STORE_PRECONDITION_STATES_TIME.getLabel());
  private final DssThreadCpuTimer blockAnalysisTime =
      new DssThreadCpuTimer(StatisticsKey.BLOCK_ANALYSIS_TIME.getLabel());

  private @Nullable DssBlockAnalysisStatistics dcpaStatistics;

  private final StatCounter blockAnalysisCount =
      new StatCounter(StatisticsKey.BLOCK_ANALYSIS_COUNT.getLabel());
  private final StatCounter storePreconditionStatesCount =
      new StatCounter(StatisticsKey.STORE_PRECONDITION_STATES_COUNT.getLabel());
  private final StatCounter storeViolationConditionStatesCount =
      new StatCounter(StatisticsKey.STORE_VIOLATION_CONDITION_STATES_COUNT.getLabel());
  private final StatInt serializedStatesSize =
      new StatInt(StatKind.SUM, StatisticsKey.SERIALIZED_STATES_SIZE.getLabel());

  public DssSingleWorkerStatistics(String pBlockId) {
    blockId = pBlockId;
  }

  public DssThreadCpuTimer getBlockAnalysisTimer() {
    return blockAnalysisTime;
  }

  public DssThreadCpuTimer getStorePreconditionStatesTimer() {
    return storePreconditionStatesTime;
  }

  public DssThreadCpuTimer getStoreViolationConditionStatesTimer() {
    return storeViolationConditionStatesTime;
  }

  public void setDcpaStatistics(DssBlockAnalysisStatistics pDcpaStatistics) {
    dcpaStatistics = pDcpaStatistics;
  }

  public StatCounter getBlockAnalysisCounter() {
    return blockAnalysisCount;
  }

  public StatCounter getStorePreconditionStatesCounter() {
    return storePreconditionStatesCount;
  }

  public StatCounter getStoreViolationConditionStatesCounter() {
    return storeViolationConditionStatesCount;
  }

  public StatInt getSerializedStatesSizeStats() {
    return serializedStatesSize;
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
      case BLOCK_ANALYSIS_COUNT -> blockAnalysisCount.getUpdateCount();
      case BLOCK_ANALYSIS_TIME -> blockAnalysisTime.nanos();
      case STORE_PRECONDITION_STATES_COUNT -> storePreconditionStatesCount.getUpdateCount();
      case STORE_PRECONDITION_STATES_TIME -> storePreconditionStatesTime.nanos();
      case STORE_VIOLATION_CONDITION_STATES_COUNT ->
          storeViolationConditionStatesCount.getUpdateCount();
      case STORE_VIOLATION_CONDITION_STATES_TIME -> storeViolationConditionStatesTime.nanos();
      case SERIALIZED_STATES_SIZE -> serializedStatesSize.getValueSum();
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
              StatisticsKey.SERIALIZATION_COUNT.getLabel(),
              dcpaStatistics.getSerializationCount().getUpdateCount())
          .put(
              StatisticsKey.DESERIALIZATION_COUNT.getLabel(),
              dcpaStatistics.getDeserializationCount().getUpdateCount())
          .put(
              StatisticsKey.PROCEED_COUNT.getLabel(),
              dcpaStatistics.getProceedCount().getUpdateCount())
          .put(
              StatisticsKey.SERIALIZATION_TIME.getLabel(),
              formatNanos(dcpaStatistics.getSerializationTime().nanos()))
          .put(
              StatisticsKey.DESERIALIZATION_TIME.getLabel(),
              formatNanos(dcpaStatistics.getDeserializationTime().nanos()))
          .put(
              StatisticsKey.PROCEED_TIME.getLabel(),
              formatNanos(dcpaStatistics.getProceedTime().nanos()));
    }

    writer
        .put(StatisticsKey.BLOCK_ANALYSIS_COUNT.getLabel(), blockAnalysisCount.getUpdateCount())
        .put(StatisticsKey.BLOCK_ANALYSIS_TIME.getLabel(), formatNanos(blockAnalysisTime.nanos()))
        .put(
            StatisticsKey.STORE_PRECONDITION_STATES_COUNT.getLabel(),
            storePreconditionStatesCount.getUpdateCount())
        .put(
            StatisticsKey.STORE_PRECONDITION_STATES_TIME.getLabel(),
            formatNanos(storePreconditionStatesTime.nanos()))
        .put(
            StatisticsKey.STORE_VIOLATION_CONDITION_STATES_COUNT.getLabel(),
            storeViolationConditionStatesCount.getUpdateCount())
        .put(
            StatisticsKey.STORE_VIOLATION_CONDITION_STATES_TIME.getLabel(),
            formatNanos(storeViolationConditionStatesTime.nanos()))
        .put(StatisticsKey.SERIALIZED_STATES_SIZE.getLabel(), serializedStatesSize.toString());
  }

  static String formatNanos(long nanos) {
    return TimeSpan.ofNanos(nanos).formatAs(TimeUnit.SECONDS);
  }

  @Override
  public @Nullable String getName() {
    return "DSS Block Worker Statistics for " + blockId;
  }
}
