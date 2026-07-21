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
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

/**
 * Aggregate statistics for all DSS analysis workers. {@link DssSingleWorkerStatistics} objects are
 * registered at worker creation time via {@link #createWorkerStats} and written to by the workers
 * directly.
 */
public class DssAllWorkerStatistics implements Statistics {

  /** Keys used to label statistics collected from DSS analysis workers. */
  public enum StatisticsKey {
    SERIALIZATION_COUNT("number of serialized states", false),
    DESERIALIZATION_COUNT("number of deserialized states", false),
    PROCEED_COUNT("number of proceeded states", false),
    SERIALIZATION_TIME("time spent serializing states", true),
    DESERIALIZATION_TIME("time spent deserializing states", true),
    PROCEED_TIME("time spent processing states", true),
    ANALYZE_PRECONDITION_COUNT("number of preconditions analyzed", false),
    ANALYZE_PRECONDITION_TIME("time spent in analyzing preconditions", true),
    STORE_PRECONDITION_COUNT("number of preconditions stored", false),
    STORE_PRECONDITION_TIME("time spent in storing preconditions", true),
    ANALYZE_VIOLATION_CONDITION_COUNT("number of violation conditions analyzed", false),
    ANALYZE_VIOLATION_CONDITION_TIME("time spent in analyzing violation conditions", true),
    STORE_VIOLATION_CONDITION_COUNT("number of violation conditions stored", false),
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

  private final List<DssSingleWorkerStatistics> workerStats = new ArrayList<>();
  private final boolean printBlockLevelStats;

  public DssAllWorkerStatistics(boolean pPrintBlockLevelStats) {
    printBlockLevelStats = pPrintBlockLevelStats;
  }

  public synchronized DssSingleWorkerStatistics createWorkerStats(String pWorkerId) {
    DssSingleWorkerStatistics stats = new DssSingleWorkerStatistics(pWorkerId);
    workerStats.add(stats);
    return stats;
  }

  @Override
  public void printStatistics(PrintStream out, Result pResult, UnmodifiableReachedSet pReached) {
    if (printBlockLevelStats) {
      for (DssSingleWorkerStatistics ws : workerStats) {
        ws.printStatistics(out, pResult, pReached);
      }
    }
    printOverallStats(out);
  }

  private void printOverallStats(PrintStream out) {
    StatisticsWriter writer =
        StatisticsWriter.writingStatisticsTo(out)
            .put("Overall Worker Statistics", "Sum across all blocks")
            .beginLevel();
    for (StatisticsKey key : StatisticsKey.values()) {
      long total = workerStats.stream().mapToLong(ws -> ws.getValue(key)).sum();
      String formatted =
          key.isFormattedAsTime()
              ? DssSingleWorkerStatistics.formatNanos(total)
              : Long.toString(total);
      writer.put(key.getKey(), formatted);
    }
  }

  @Override
  public String getName() {
    return "DSS Worker Statistics";
  }
}
