// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.monitor;

import java.io.PrintStream;
import java.util.concurrent.TimeUnit;
import org.sosy_lab.common.time.TimeSpan;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;

class MonitorStatistics implements Statistics {

  private final MonitorCPA mCpa;

  MonitorStatistics(MonitorCPA pCpa) {
    mCpa = pCpa;
  }

  @Override
  public String getName() {
    return "MonitorCPA";
  }

  @Override
  public void printStatistics(PrintStream out, Result pResult, UnmodifiableReachedSet pReached) {

    MonitorTransferRelation trans = mCpa.getTransferRelation();

    out.println(
        "Max. Post Time:            "
            + trans.totalTimeOfTransfer.getMaxTime().formatAs(TimeUnit.SECONDS));
    out.println(
        "Avg. Post Time:            "
            + trans.totalTimeOfTransfer.getAvgTime().formatAs(TimeUnit.SECONDS));
    out.println(
        "Max Post time on a path:   "
            + TimeSpan.ofMillis(trans.maxTotalTimeForPath).formatAs(TimeUnit.SECONDS));
  }
}
