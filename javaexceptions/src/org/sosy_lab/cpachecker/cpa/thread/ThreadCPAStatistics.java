// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.thread;

import java.io.PrintStream;
import java.util.HashSet;
import java.util.Set;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;
import org.sosy_lab.cpachecker.util.statistics.StatInt;
import org.sosy_lab.cpachecker.util.statistics.StatKind;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

public class ThreadCPAStatistics implements Statistics {

  public final StatTimer transfer = new StatTimer("Time for transfer relation");
  public final StatCounter threadCreates = new StatCounter("Number of thread creates");
  public final StatCounter threadJoins = new StatCounter("Number of thread joins");
  public final StatInt maxNumberOfThreads = new StatInt(StatKind.COUNT, "Max number of threads");
  public final Set<String> createdThreads = new HashSet<>();

  @Override
  public void printStatistics(PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {
    StatisticsWriter writer = StatisticsWriter.writingStatisticsTo(pOut);
    writer
        .put(transfer)
        .put(threadCreates)
        .put("Names of created threads:", createdThreads)
        .put(threadJoins)
        .put(maxNumberOfThreads);
  }

  @Override
  public String getName() {
    return "ThreadCPA";
  }
}
