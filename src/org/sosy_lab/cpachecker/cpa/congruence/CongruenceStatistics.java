// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
// SPDX-FileCopyrightText: 2014-2017 Université Grenoble Alpes
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.congruence;

import java.io.PrintStream;
import java.util.concurrent.TimeUnit;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;

public class CongruenceStatistics implements Statistics {

  public final Timer congruenceTimer = new Timer();

  @Override
  public void printStatistics(PrintStream out, Result result, UnmodifiableReachedSet reached) {
    printTimer(out, congruenceTimer, "computing congruence");
  }

  @Override
  public String getName() {
    return "Congruence calculation statistics";
  }

  private void printTimer(PrintStream out, Timer t, String name) {
    out.printf(
        "Time spent in %s: %s (Max: %s), (Avg: %s), (#intervals = %s)%n",
        name,
        t,
        t.getMaxTime().formatAs(TimeUnit.SECONDS),
        t.getAvgTime().formatAs(TimeUnit.SECONDS),
        t.getNumberOfIntervals());
  }
}
