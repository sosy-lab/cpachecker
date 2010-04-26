package org.sosy_lab.cpachecker.cpa.transferrelationmonitor;

import org.sosy_lab.cpachecker.util.assumptions.AvoidanceReportingElement;
import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperElement;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;

public class TransferRelationMonitorElement extends AbstractSingleWrapperElement implements AvoidanceReportingElement {

  private final TransferRelationMonitorCPA cpa;
  private boolean shouldStop = false;

  private long timeOfTranferToComputeElement;
  private long totalTimeOnThePath;
  public static long maxTimeOfTransfer = 0;
  public static long maxTotalTimeForPath = 0;
  private boolean ignore = false;

  protected TransferRelationMonitorElement(TransferRelationMonitorCPA pCpa, 
      AbstractElement pWrappedElement) {
    super(pWrappedElement);
    cpa = pCpa;
    timeOfTranferToComputeElement = 0;
    totalTimeOnThePath = 0;
  }

  public TransferRelationMonitorCPA getCpa() {
    return cpa;
  }

  protected void setTransferTime(long pTransferTime){
    timeOfTranferToComputeElement = pTransferTime;
    if(timeOfTranferToComputeElement > maxTimeOfTransfer){
      maxTimeOfTransfer = timeOfTranferToComputeElement;
    }
  }

  protected void setTotalTime(boolean pIsIgnore, long pTotalTime){
    ignore = pIsIgnore;
    totalTimeOnThePath = pTotalTime + timeOfTranferToComputeElement;
    if(totalTimeOnThePath > maxTotalTimeForPath){
      maxTotalTimeForPath = totalTimeOnThePath;
    }
  }

  protected void resetTotalTime(){
    totalTimeOnThePath = 0;
  }
  
  public long getTimeOfTranferToComputeElement() {
    return timeOfTranferToComputeElement;
  }

  public long getTotalTimeOnThePath() {
    return totalTimeOnThePath;
  }

  @Override
  public boolean equals(Object pObj) {
    if (this == pObj) {
      return true;
    } else if (pObj instanceof TransferRelationMonitorElement) {
      TransferRelationMonitorElement otherElem = (TransferRelationMonitorElement)pObj;
      return this.getWrappedElement().equals(otherElem.getWrappedElement());
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return getWrappedElement().hashCode();
  }

  public void setAsStopElement(){
    shouldStop = true;
  }

  @Override
  public boolean mustDumpAssumptionForAvoidance() {
    // returns true if the current element is the same as bottom
    if (shouldStop)
      return true;
    return false;
  }

  public boolean isIgnore() {
    return ignore;
  }

  public void setIgnore() {
    ignore = true;
  }

}