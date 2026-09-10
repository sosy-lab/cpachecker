// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.execution;

import java.io.PrintStream;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;
import org.sosy_lab.cpachecker.util.statistics.StatInt;
import org.sosy_lab.cpachecker.util.statistics.StatKind;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

class ExecutionStatistics implements Statistics {

  final StatCounter executedSteps = new StatCounter("Number of executed program steps");
  final StatInt maxCallStackDepth =
      new StatInt(StatKind.MAX, "Maximum depth of the function-call stack");
  final StatCounter recursiveCalls = new StatCounter("Number of recursive function calls");

  @Override
  public void printStatistics(PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {
    StatisticsWriter.writingStatisticsTo(pOut)
        .put(executedSteps)
        .put(maxCallStackDepth)
        .put(recursiveCalls);
  }

  @Override
  public String getName() {
    return "ExecutionCPA";
  }
}
