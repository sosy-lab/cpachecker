package org.sosy_lab.cpachecker.cpa.transferrelationmonitor;

import java.io.PrintWriter;
import java.text.NumberFormat;
import java.util.Locale;

import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;

public class TransferMonitorStatistics implements Statistics{

  @Override
  public String getName() {
    return "Transfer Monitor Statistics";
  }

  @Override
  public void printStatistics(PrintWriter pOut, Result pResult,
      ReachedSet pReached) {

    double averageTransferTime = ((new Long(TransferRelationMonitorElement.totalTimeOfTransfer)).doubleValue() /
        (new Long(TransferRelationMonitorElement.totalNumberOfTransfers)).doubleValue());
    NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.getDefault());
    numberFormat.setMaximumFractionDigits(2);

    pOut.println("Max. Post Time:            " + TransferRelationMonitorElement.maxTimeOfTransfer + "ms");
    pOut.println("Av Post Time:              " + numberFormat.format(averageTransferTime) + "ms");
    pOut.println("Max Post time on a path:   " + TransferRelationMonitorElement.maxTotalTimeForPath + "ms" );
    pOut.println("Max size of a single path: " + TransferRelationMonitorTransferRelation.maxSizeOfSinglePath);
  }
}
