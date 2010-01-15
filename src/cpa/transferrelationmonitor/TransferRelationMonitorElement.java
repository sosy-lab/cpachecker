package cpa.transferrelationmonitor;

import java.util.Collections;

import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.AbstractWrapperElement;

public class TransferRelationMonitorElement implements AbstractElement, AbstractWrapperElement{

  private final TransferRelationMonitorCPA cpa;
  private final AbstractElement element;
  private boolean isBottom = false;
  
  private long timeOfTranferToComputeElement;
  private long totalTimeOnThePath;
  public static long maxTimeOfTransfer = 0;

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
  
  public TransferRelationMonitorCPA getCpa() {
    return cpa;
  }

  public boolean isBottom() {
    return isBottom;
  }

  protected void setBottom(boolean pIsBottom) {
    isBottom = pIsBottom;
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
    TransferRelationMonitorElement otherElem = (TransferRelationMonitorElement)pObj;
    AbstractElement otherWrappedElement = otherElem.element;
    return this.element.equals(otherWrappedElement);
  }
  
  @Override
  public String toString() {
    return element.toString();
  }
  
}