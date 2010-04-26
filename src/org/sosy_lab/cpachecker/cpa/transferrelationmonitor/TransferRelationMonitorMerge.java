package org.sosy_lab.cpachecker.cpa.transferrelationmonitor;

import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class TransferRelationMonitorMerge implements MergeOperator{

  private ConfigurableProgramAnalysis wrappedCpa;

  public TransferRelationMonitorMerge(ConfigurableProgramAnalysis pWrappedCPA) {
    wrappedCpa = pWrappedCPA;
  }
  
  @Override
  public AbstractElement merge(
      AbstractElement pElement1,
      AbstractElement pElement2, Precision pPrecision)
  throws CPAException {
    TransferRelationMonitorElement transferRelationMonitorElement1= (TransferRelationMonitorElement)pElement1;
    TransferRelationMonitorElement transferRelationMonitorElement2 = (TransferRelationMonitorElement)pElement2;

    MergeOperator mergeOperator = wrappedCpa.getMergeOperator();
    AbstractElement wrappedElement1 = transferRelationMonitorElement1.getWrappedElement();
    AbstractElement wrappedElement2 = transferRelationMonitorElement2.getWrappedElement();
    AbstractElement retElement = mergeOperator.merge(wrappedElement1, wrappedElement2, pPrecision);
    if(retElement.equals(wrappedElement2)){
      return pElement2;
    }

    TransferRelationMonitorElement mergedElement = new TransferRelationMonitorElement(transferRelationMonitorElement1.getCpa(), retElement);

    mergedElement.setTotalTime(
        (transferRelationMonitorElement1.isIgnore() | transferRelationMonitorElement2.isIgnore()), 
        Math.max(transferRelationMonitorElement1.getTotalTimeOnThePath(), 
        transferRelationMonitorElement2.getTotalTimeOnThePath()));
    
    return mergedElement;
  }

}
