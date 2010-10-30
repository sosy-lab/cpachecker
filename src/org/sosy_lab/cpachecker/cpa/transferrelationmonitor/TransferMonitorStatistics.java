package org.sosy_lab.cpachecker.cpa.transferrelationmonitor;

import java.io.PrintStream;

import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;

public class TransferMonitorStatistics implements Statistics {

  @Override
  public String getName() {
    return "Transfer Monitor Statistics";
  }

  @Override
  public void printStatistics(PrintStream pOut, Result pResult,
      ReachedSet pReached) {

    pOut.println("Max. Post Time:            " + TransferRelationMonitorTransferRelation.totalTimeOfTransfer.printMaxTime() + "s");
    pOut.println("Av Post Time:              " + TransferRelationMonitorTransferRelation.totalTimeOfTransfer.printAvgTime() + "s");
    pOut.println("Max Post time on a path:   " + TransferRelationMonitorTransferRelation.maxTotalTimeForPath + "ms" );
    pOut.println("Max size of a single path: " + TransferRelationMonitorTransferRelation.maxSizeOfSinglePath);
    pOut.println("Max number of branches:    " + TransferRelationMonitorTransferRelation.maxNumberOfBranches);
  }

}
