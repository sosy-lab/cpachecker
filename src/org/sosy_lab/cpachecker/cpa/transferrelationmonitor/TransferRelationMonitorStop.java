package org.sosy_lab.cpachecker.cpa.transferrelationmonitor;

import java.util.Collection;

import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class TransferRelationMonitorStop implements StopOperator {

  private final ConfigurableProgramAnalysis wrappedCpa;

  public TransferRelationMonitorStop(ConfigurableProgramAnalysis cpa) {
    this.wrappedCpa = cpa;
  }
  
  @Override
  public boolean stop(AbstractElement pElement,
      Collection<AbstractElement> pReached, Precision pPrecision) throws CPAException {

    TransferRelationMonitorElement transferRelationMonitorElement = (TransferRelationMonitorElement)pElement;   

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

    AbstractElement wrappedElement = pElement.getWrappedElement();
    AbstractElement wrappedReachedElement = pReachedElement.getWrappedElement();
    StopOperator stopOp = wrappedCpa.getStopOperator();
    return stopOp.stop(wrappedElement, wrappedReachedElement);
  }

  @Override
  public boolean stop(AbstractElement pElement, AbstractElement pReachedElement)
      throws CPAException {
    return stop((TransferRelationMonitorElement)pElement, (TransferRelationMonitorElement)pReachedElement);
  }
  
}
