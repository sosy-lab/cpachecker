package cpa.transferrelationmonitor;

import java.util.Collection;

import compositeCPA.CompositeStopOperator;

import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.StopOperator;
import exceptions.CPAException;

public class TransferRelationMonitorStop implements StopOperator {

  private final ConfigurableProgramAnalysis wrappedCpa;

  public TransferRelationMonitorStop(ConfigurableProgramAnalysis cpa) {
    this.wrappedCpa = cpa;
  }
  
  @Override
  public <AE extends AbstractElement> boolean stop(AE pElement,
      Collection<AE> pReached, Precision pPrecision) throws CPAException {

    TransferRelationMonitorElement transferRelationMonitorElement = (TransferRelationMonitorElement)pElement;
    AbstractElement wrappedElement = transferRelationMonitorElement.getWrappedElements().iterator().next();
    
    // TODO this is ugly, perhaps introduce AbstractElement.isBottom() instead?
    StopOperator stopOp = wrappedCpa.getStopOperator();
    if(stopOp instanceof CompositeStopOperator){
      CompositeStopOperator compStopOp = (CompositeStopOperator) stopOp;
      if(compStopOp.containsBottomElement(wrappedElement)){
        transferRelationMonitorElement.setBottom(true);
        return true;
      }
    }

    for (AbstractElement reachedElement : pReached) {
      TransferRelationMonitorElement transferRelationMonitorReachedElement = (TransferRelationMonitorElement)reachedElement;
      if (stop(transferRelationMonitorElement, transferRelationMonitorReachedElement)) {
        return true;
      }
    }
    return false;

  }

  public boolean stop(TransferRelationMonitorElement pElement, TransferRelationMonitorElement pReachedElement)
                                                      throws CPAException {

    AbstractElement wrappedElement = pElement.getWrappedElements().iterator().next();
    AbstractElement wrappedReachedElement = pReachedElement.getWrappedElements().iterator().next();
    StopOperator stopOp = wrappedCpa.getStopOperator();
    return stopOp.stop(wrappedElement, wrappedReachedElement);
  }

  @Override
  public boolean stop(AbstractElement pElement, AbstractElement pReachedElement)
      throws CPAException {
    return stop((TransferRelationMonitorElement)pElement, (TransferRelationMonitorElement)pReachedElement);
  }
  
}
