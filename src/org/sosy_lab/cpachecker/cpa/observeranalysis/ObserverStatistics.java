package org.sosy_lab.cpachecker.cpa.observeranalysis;

import java.io.PrintWriter;

import org.sosy_lab.cpachecker.core.ReachedElements;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;

class ObserverStatistics implements Statistics {

  private final ObserverAutomatonCPA mCpa;
  
  public ObserverStatistics(ObserverAutomatonCPA pCpa) {
    mCpa = pCpa;
  }
  
  @Override
  public String getName() {
    return "ObserverAnalysis";
  }

  @Override
  public void printStatistics(PrintWriter out, Result pResult,
      ReachedElements pReached) {
    
    ObserverTransferRelation trans = mCpa.getTransferRelation();
    out.println("Total time for sucessor computation: " + toTime(trans.totalPostTime));
    out.println("  Time for transition matches:       " + toTime(trans.matchTime));
    out.println("  Time for transition assertions:    " + toTime(trans.assertionsTime));
    out.println("  Time for transition actions:       " + toTime(trans.actionTime));
    out.println("Total time for strengthen operator:  " + toTime(trans.totalStrengthenTime));
  }
  
  private String toTime(long timeMillis) {
    return String.format("% 5d.%03ds", timeMillis/1000, timeMillis%1000);
  }

}
