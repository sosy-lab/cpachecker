// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries;

import java.io.PrintStream;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.util.statistics.StatInt;
import org.sosy_lab.cpachecker.util.statistics.StatKind;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

public class DistributedSummarySynthesisStatistics implements Statistics {

  private final StatInt numberWorkers = new StatInt(StatKind.MAX, "Number of worker");
  private final StatInt averageNumberOfEdges =
      new StatInt(StatKind.AVG, "Average number of edges in block");
  private final StatInt numberWorkersWithoutAbstraction =
      new StatInt(StatKind.MAX, "Worker without abstraction");

  private final StatTimer decompositionTimer = new StatTimer("Decomposition time");
  private final StatTimer instrumentationTimer = new StatTimer("Instrumentation time");

  public StatInt getAverageNumberOfEdges() {
    return averageNumberOfEdges;
  }

  public StatInt getNumberWorkers() {
    return numberWorkers;
  }

  public StatInt getNumberWorkersWithoutAbstraction() {
    return numberWorkersWithoutAbstraction;
  }

  public StatTimer getDecompositionTimer() {
    return decompositionTimer;
  }

  public StatTimer getInstrumentationTimer() {
    return instrumentationTimer;
  }

  @Override
  public void printStatistics(PrintStream out, Result result, UnmodifiableReachedSet reached) {
    StatisticsWriter writer = StatisticsWriter.writingStatisticsTo(out);
    writer
        .put("Dss Statistics", "Statistics of Dss")
        .beginLevel()
        .put(numberWorkers)
        .put(numberWorkersWithoutAbstraction)
        .put(averageNumberOfEdges)
        .put(instrumentationTimer)
        .put(decompositionTimer);
  }

  @Override
  public @Nullable String getName() {
    return "Distributed Summary Synthesis Statistics";
  }
}
