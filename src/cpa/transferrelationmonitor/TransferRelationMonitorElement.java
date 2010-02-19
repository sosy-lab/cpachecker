package cpa.transferrelationmonitor;

import java.util.Collections;

import assumptions.AvoidanceReportingElement;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.AbstractElementWithLocation;
import cpa.common.interfaces.AbstractWrapperElement;

public class TransferRelationMonitorElement implements AbstractElement, AbstractWrapperElement, AvoidanceReportingElement {

  private final TransferRelationMonitorCPA cpa;
  private final AbstractElement element;
  private boolean shouldStop = false;

  private long timeOfTranferToComputeElement;
  private long totalTimeOnThePath;
  static long maxTimeOfTransfer = 0;

  protected TransferRelationMonitorElement(TransferRelationMonitorCPA pCpa, 
      AbstractElement pAbstractElement) {
    cpa = pCpa;
    element = pAbstractElement;
    timeOfTranferToComputeElement = 0;
    totalTimeOnThePath = 0;
  }

  @Override
  public boolean isError() {
    return element.isError();
  }

  @Override
  public Iterable<AbstractElement> getWrappedElements() {
    return Collections.singletonList(element);
  }

  @Override
  public <T extends AbstractElement> T retrieveWrappedElement(Class<T> pType) {
    if (pType.isAssignableFrom(getClass())) {
      return pType.cast(this);
    } else if (pType.isAssignableFrom(element.getClass())) {
      return pType.cast(element);
    } else if (element instanceof AbstractWrapperElement) {
      return ((AbstractWrapperElement)element).retrieveWrappedElement(pType);
    } else {
      return null;
    }
  }

  @Override
  public AbstractElementWithLocation retrieveLocationElement() {
    return retrieveWrappedElement(AbstractElementWithLocation.class);
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

  protected void setTotalTime(long pTotalTime){
    totalTimeOnThePath = pTotalTime + timeOfTranferToComputeElement;
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
      return this.element.equals(otherElem.element);
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return element.hashCode();
  }

  @Override
  public String toString() {
    return element.toString();
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

}