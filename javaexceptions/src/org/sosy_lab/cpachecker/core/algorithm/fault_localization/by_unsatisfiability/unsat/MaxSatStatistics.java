// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.unsat;

import java.io.PrintStream;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;
import org.sosy_lab.cpachecker.util.statistics.StatKind;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

class MaxSatStatistics implements Statistics {

  final StatTimer totalTime = new StatTimer(StatKind.SUM, "Total time for max-sat algorithm");
  final StatCounter unsatCalls = new StatCounter("Number of calls to sat solver");
  final StatCounter savedCalls = new StatCounter("Number of calls saved through subset check");
  final StatTimer timeForSubSupCheck = new StatTimer(StatKind.SUM, "Time for subset/supset check");

  @Override
  public void printStatistics(PrintStream out, Result result, UnmodifiableReachedSet reached) {
    StatisticsWriter.writingStatisticsTo(out)
        .put(totalTime)
        .putIfUpdatedAtLeastOnce(unsatCalls)
        .putIfUpdatedAtLeastOnce(savedCalls)
        .beginLevel() // Statistics for sup/subset checks
        .putIfUpdatedAtLeastOnce(timeForSubSupCheck);
  }

  @Override
  public @Nullable String getName() {
    return "MAX-Sat algorithm";
  }
}
