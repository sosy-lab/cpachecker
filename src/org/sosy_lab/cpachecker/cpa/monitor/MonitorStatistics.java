/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.monitor;

import java.io.PrintStream;
import java.util.concurrent.TimeUnit;

import org.sosy_lab.common.time.TimeSpan;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;

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
  public void printStatistics(PrintStream out, Result pResult,
      ReachedSet pReached) {

    MonitorTransferRelation trans = mCpa.getTransferRelation();

    out.println("Max. Post Time:            " + trans.totalTimeOfTransfer.getMaxTime().formatAs(TimeUnit.SECONDS));
    out.println("Avg. Post Time:            " + trans.totalTimeOfTransfer.getAvgTime().formatAs(TimeUnit.SECONDS));
    out.println("Max Post time on a path:   " + TimeSpan.ofMillis(trans.maxTotalTimeForPath).formatAs(TimeUnit.SECONDS));
  }

}
