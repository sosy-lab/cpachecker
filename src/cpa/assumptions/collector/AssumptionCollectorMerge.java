package cpa.assumptions.collector;

import assumptions.AssumptionWithLocation;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpa.common.interfaces.MergeOperator;
import cpa.common.interfaces.Precision;
import exceptions.CPAException;

public class AssumptionCollectorMerge implements MergeOperator {

  private final ConfigurableProgramAnalysis wrappedCPA;
  
  public AssumptionCollectorMerge(ConfigurableProgramAnalysis pWrappedCPA) {
    wrappedCPA = pWrappedCPA;
  }
  
  @Override
  public AbstractElement merge(AbstractElement element1,
      AbstractElement element2, Precision precision) throws CPAException {
    
    AssumptionCollectorElement collectorElement1= (AssumptionCollectorElement)element1;
    AssumptionCollectorElement collectorElement2 = (AssumptionCollectorElement)element2;

    MergeOperator mergeOperator = wrappedCPA.getMergeOperator();
    AbstractElement wrappedElement1 = collectorElement1.getWrappedElements().iterator().next();
    AbstractElement wrappedElement2 = collectorElement2.getWrappedElements().iterator().next();
    AbstractElement retElement = mergeOperator.merge(wrappedElement1, wrappedElement2, precision);
    if(retElement.equals(wrappedElement2)){
      return element2;
    }

    AssumptionWithLocation assumption1 = collectorElement1.getCollectedAssumptions();
    boolean shouldStop1 = collectorElement1.isStop();
    AssumptionWithLocation assumption2 = collectorElement2.getCollectedAssumptions();
    boolean shouldStop2 = collectorElement2.isStop();
    AssumptionWithLocation mergedAssumption = assumption1.and(assumption2);
    boolean mergedShouldStop = shouldStop1 && shouldStop2;
    AssumptionCollectorElement mergedElement = new AssumptionCollectorElement(retElement, mergedAssumption, mergedShouldStop);
    
    return mergedElement;
  }

}
