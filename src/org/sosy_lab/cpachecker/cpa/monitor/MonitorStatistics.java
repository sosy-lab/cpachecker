package org.sosy_lab.cpachecker.cpa.monitor;

import java.io.PrintStream;

import org.sosy_lab.common.Timer;
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

    out.println("Max. Post Time:            " + trans.totalTimeOfTransfer.printMaxTime() + "s");
    out.println("Avg. Post Time:            " + trans.totalTimeOfTransfer.printAvgTime() + "s");
    out.println("Max Post time on a path:   " + Timer.formatTime(trans.maxTotalTimeForPath) + "s" );
  }

}
