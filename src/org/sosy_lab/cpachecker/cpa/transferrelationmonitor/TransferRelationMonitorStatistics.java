package org.sosy_lab.cpachecker.cpa.transferrelationmonitor;

import java.io.PrintStream;

import org.sosy_lab.common.Timer;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;

class TransferRelationMonitorStatistics implements Statistics {

  private final TransferRelationMonitorCPA mCpa;
  
  TransferRelationMonitorStatistics(TransferRelationMonitorCPA pCpa) {
    mCpa = pCpa;
  }
  
  @Override
  public String getName() {
    return "TransferRelationMonitor Statistics";
  }

  @Override
  public void printStatistics(PrintStream out, Result pResult,
      ReachedSet pReached) {

    TransferRelationMonitorTransferRelation trans = mCpa.getTransferRelation();
    
    out.println("Max. Post Time:            " + trans.totalTimeOfTransfer.printMaxTime() + "s");
    out.println("Avg. Post Time:            " + trans.totalTimeOfTransfer.printAvgTime() + "s");
    out.println("Max Post time on a path:   " + Timer.formatTime(trans.maxTotalTimeForPath) + "s" );
    out.println("Max size of a single path: " + trans.maxSizeOfSinglePath);
    out.println("Max number of branches:    " + trans.maxNumberOfBranches);
  }

}
