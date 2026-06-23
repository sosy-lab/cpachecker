// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.time.TimeSpan;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssStatisticsMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssStatisticsMessage.StatisticsKey;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.executors.DssExecutor;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

/**
 * Stores {@link DssStatisticsMessage}s from workers, populated by a {@link DssExecutor} after all workers have finished.
 */
public class DssWorkerStatistics implements Statistics {

  private final Map<String, DssStatisticsMessage> statsPerBlock = new HashMap<>();

  public void addMessage(DssStatisticsMessage message) {
    statsPerBlock.put(message.getSenderId(), message);
  }

  public void addAllMessages(Map<String, DssStatisticsMessage> messages) {
    statsPerBlock.putAll(messages);
  }

  @Override
  public void printStatistics(PrintStream out, Result pResult, UnmodifiableReachedSet pReached) {
    StatisticsWriter writer = StatisticsWriter.writingStatisticsTo(out);
    Map<StatisticsKey, String> overall = new HashMap<>();
    for (Entry<String, DssStatisticsMessage> statEntry : statsPerBlock.entrySet()) {
      String blockId = statEntry.getKey();
      Map<StatisticsKey, String> blockStats = statEntry.getValue().getStats();
      writer = writer.put("BlockID " + blockId, blockId).beginLevel();
      for (Entry<StatisticsKey, String> entry : blockStats.entrySet()) {
        writer = writer.put(entry.getKey().getKey(), format(entry.getKey(), entry.getValue()));
        overall.merge(
            entry.getKey(),
            entry.getValue(),
            (v1, v2) -> Long.toString(Long.parseLong(v1) + Long.parseLong(v2)));
      }
      writer = writer.endLevel();
    }
    writer = writer.put("Overall", "Sum of all blocks").beginLevel();
    for (Entry<StatisticsKey, String> overallEntry : overall.entrySet()) {
      writer =
          writer.put(
              overallEntry.getKey().getKey() + " (overall)",
              format(overallEntry.getKey(), overallEntry.getValue()));
    }
  }

  private String format(StatisticsKey key, String value) {
    if (key.isFormattedAsTime()) {
      return TimeSpan.ofNanos(Long.parseLong(value)).formatAs(TimeUnit.SECONDS);
    }
    return value;
  }

  @Override
  public @Nullable String getName() {
    return "DSS Worker Statistics";
  }
}
