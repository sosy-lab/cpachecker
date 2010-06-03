package org.sosy_lab.cpachecker.cpa.transferrelationmonitor;

import java.io.PrintWriter;

import org.sosy_lab.cpachecker.core.ReachedElements;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;

public class TransferMonitorStatistics implements Statistics{

  @Override
  public String getName() {
    return "Transfer Monitor Statistics";
  }

  @Override
  public void printStatistics(PrintWriter pOut, Result pResult,
      ReachedElements pReached) {
    pOut.println("Max transfer time:            " + TransferRelationMonitorElement.maxTimeOfTransfer + "ms");
    pOut.println("Max transfer time for a path: " + TransferRelationMonitorElement.maxTotalTimeForPath +"ms" );
    pOut.println("Max size of a single path:    " + TransferRelationMonitorTransferRelation.maxSizeOfSinglePath);
  }
}
