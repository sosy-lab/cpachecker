// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.concurrent;

import java.io.PrintStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.blockgraph.Block;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.task.backward.BackwardAnalysisCoreStatistics;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.task.backward.BackwardAnalysisFullStatistics;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.task.forward.ForwardAnalysisStatistics;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.util.statistics.StatInt;
import org.sosy_lab.cpachecker.util.statistics.StatKind;

public class ConcurrentStatisticsCollector implements StatisticsProvider, Statistics, Runnable {
  private Map<Block, Integer> forwardAnalysisCount = new HashMap<>();

  @Override
  public void run() {

  }

  @Override
  public void printStatistics(
      PrintStream out, Result result, UnmodifiableReachedSet reached) {
    final StatInt forwardAnalysisCountValues = new StatInt(StatKind.AVG, "FA Count");

    for (final Map.Entry<Block, Integer> entry : forwardAnalysisCount.entrySet()) {
      out.format("Forward Analysis Tasks Count for Block with entry node %s: %d%n",
          entry.getKey().getEntry(), entry.getValue());
      forwardAnalysisCountValues.setNextValue(entry.getValue());
    }

    out.format("Forward Analysis Average Count: %f%n", forwardAnalysisCountValues.getAverage());
    out.format("Forward Analysis Max Count: %d%n", forwardAnalysisCountValues.getMaxValue());
    out.format("Forward Analysis Min Count: %d%n", forwardAnalysisCountValues.getMinValue());
  }

  @Override
  public @Nullable String getName() {
    return "Concurrent Analysis";
  }

  public void visit(@SuppressWarnings("unused") final BackwardAnalysisFullStatistics pStatistics) {
    //pStatistics.accept(this);
  }

  public void visit(@SuppressWarnings("unused") final BackwardAnalysisCoreStatistics pStatistics) {
    //pStatistics.accept(this);
  }

  public void visit(final ForwardAnalysisStatistics pStatistics) {
    final Block target = pStatistics.getTarget();
    int oldCount = forwardAnalysisCount.getOrDefault(target, 0);
    forwardAnalysisCount.put(target, ++oldCount);
  }

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {
    statsCollection.add(this);
  }


  public interface TaskStatistics {
    void accept(final ConcurrentStatisticsCollector collector);
  }
}
