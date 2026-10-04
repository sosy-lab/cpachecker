// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.symbolic_execution;

import java.io.PrintStream;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import org.sosy_lab.common.time.TimeSpan;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

/** Statistics of symbolic block execution. Counters and timers may be updated by any thread. */
final class SymbolicBlockExecutionStatistics implements Statistics {

  final StatTimer decompositionTime = new StatTimer("Time for decomposition");
  final StatTimer targetLocationTime = new StatTimer("Time for finding target locations");

  final SharedTimer encodingTime = new SharedTimer("Time for encoding blocks");
  final SharedTimer projectionTime = new SharedTimer("Time for projecting conditions");
  final SharedTimer quantifierEliminationTime = new SharedTimer("Time for quantifier elimination");
  final SharedTimer coverageTime = new SharedTimer("Time for coverage checks");

  final LongAdder satisfiabilityChecks = new LongAdder();
  final LongAdder coverageChecks = new LongAdder();
  final LongAdder quantifierEliminations = new LongAdder();
  final LongAdder failedQuantifierEliminations = new LongAdder();
  final LongAdder conditionsWithExistentials = new LongAdder();
  final LongAdder preconditionsReceived = new LongAdder();
  final LongAdder preconditionsCovered = new LongAdder();
  final LongAdder violationConditionsReceived = new LongAdder();
  final LongAdder violationConditionsCovered = new LongAdder();
  final LongAdder postconditionsSent = new LongAdder();
  final LongAdder violationConditionsSent = new LongAdder();
  final LongAdder contextSwitches = new LongAdder();
  final LongAdder stolenBlocks = new LongAdder();

  private volatile int blocks;
  private volatile int relevantBlocks;
  private volatile int targetLocations;

  void setBlockCounts(int pBlocks, int pRelevantBlocks, int pTargetLocations) {
    blocks = pBlocks;
    relevantBlocks = pRelevantBlocks;
    targetLocations = pTargetLocations;
  }

  @Override
  public void printStatistics(PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {
    StatisticsWriter.writingStatisticsTo(pOut)
        .put("Number of blocks", blocks)
        .put("Number of blocks that can reach a target", relevantBlocks)
        .put("Number of target locations", targetLocations)
        .put(decompositionTime)
        .put(targetLocationTime)
        .put(encodingTime.title, encodingTime)
        .put(projectionTime.title, projectionTime)
        .put(quantifierEliminationTime.title, quantifierEliminationTime)
        .put(coverageTime.title, coverageTime)
        .put("Number of satisfiability checks", satisfiabilityChecks.sum())
        .put("Number of coverage checks", coverageChecks.sum())
        .put("Number of quantifier eliminations", quantifierEliminations.sum())
        .put("Number of failed quantifier eliminations", failedQuantifierEliminations.sum())
        .put("Number of conditions with private variables", conditionsWithExistentials.sum())
        .put("Number of received preconditions", preconditionsReceived.sum())
        .put("Number of covered preconditions", preconditionsCovered.sum())
        .put("Number of received violation conditions", violationConditionsReceived.sum())
        .put("Number of covered violation conditions", violationConditionsCovered.sum())
        .put("Number of sent postconditions", postconditionsSent.sum())
        .put("Number of sent violation conditions", violationConditionsSent.sum())
        .put("Number of moves of a block to another solver context", contextSwitches.sum())
        .put("Number of blocks stolen by another thread", stolenBlocks.sum());
  }

  /** Sums up the wall time of intervals measured by any number of threads. */
  static final class SharedTimer {
    private final String title;
    private final LongAdder nanos = new LongAdder();
    private final LongAdder intervals = new LongAdder();

    private SharedTimer(String pTitle) {
      title = pTitle;
    }

    /** Starts an interval and returns its start, which {@link #stop} takes. */
    long start() {
      return System.nanoTime();
    }

    void stop(long pStart) {
      nanos.add(System.nanoTime() - pStart);
      intervals.increment();
    }

    @Override
    public String toString() {
      return TimeSpan.ofNanos(nanos.sum()).formatAs(TimeUnit.SECONDS)
          + " (in "
          + intervals.sum()
          + " intervals)";
    }
  }

  @Override
  public String getName() {
    return "Symbolic Block Execution";
  }
}
