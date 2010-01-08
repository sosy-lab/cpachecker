package cpa.transferrelationmonitor;

import java.util.Collections;

import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.AbstractWrapperElement;

public class TransferRelationMonitorElement implements AbstractElement, AbstractWrapperElement{

  private final TransferRelationMonitorCPA cpa;
  private final AbstractElement element;
  private boolean isBottom = false;

  protected TransferRelationMonitorElement(TransferRelationMonitorCPA pCpa, 
      AbstractElement pAbstractElement) {
    cpa = pCpa;
    element = pAbstractElement;
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
  public AbstractElement retrieveElementOfType(String pElementClass) {
    if(element.getClass().getSimpleName().equals(pElementClass)){
      return element;
    }
    else{
      return ((AbstractWrapperElement)element).retrieveElementOfType(pElementClass);
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
  
}