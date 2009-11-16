package cpa.art;

import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.AbstractElementWithLocation;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpa.common.interfaces.MergeOperator;
import cpa.common.interfaces.Precision;
import exceptions.CPAException;

public class ARTMergeJoin implements MergeOperator {

  private ConfigurableProgramAnalysis wrappedCpa;

  public ARTMergeJoin(ConfigurableProgramAnalysis pWrappedCPA) {
    wrappedCpa = pWrappedCPA;
  }

  @Override
  public AbstractElement merge(AbstractElement pElement1,
      AbstractElement pElement2, Precision pPrecision) throws CPAException {
    assert(false);
    return null;
  }

  @Override
  public AbstractElementWithLocation merge(
      AbstractElementWithLocation pElement1,
      AbstractElementWithLocation pElement2, Precision pPrecision)
  throws CPAException {
    ARTElement artElement1 = (ARTElement)pElement1;
    ARTElement artElement2 = (ARTElement)pElement2;
    
    MergeOperator mergeOperator = wrappedCpa.getMergeOperator();
    AbstractElementWithLocation wrappedElement1 = artElement1.getAbstractElementOnArtNode();
    AbstractElementWithLocation wrappedElement2 = artElement2.getAbstractElementOnArtNode();
    Precision wrappedPrecision = (pPrecision == null) ? null : ((ARTPrecision)pPrecision).getPrecision();
    AbstractElementWithLocation retElement = mergeOperator.merge(wrappedElement1, wrappedElement2, wrappedPrecision);
    if(retElement.equals(wrappedElement2)){
      return pElement2;
    }

    ARTElement mergedElement = new ARTElement(artElement1.getCpa(), retElement, null);

    for(ARTElement parentOfElement1: artElement1.getParents()) {
      mergedElement.addParent(parentOfElement1);
    }
  
    for(ARTElement parentOfElement2: artElement2.getParents()) {
      mergedElement.addParent(parentOfElement2);
    }
  
    artElement1.removeFromART();
    artElement2.removeFromART();

    // TODO new mark or max mark of two elements?
    mergedElement.setMark();
//  newElement.setMark(Math.max(((ARTElement)pElement1).getMark(), ((ARTElement)pElement2).getMark()));
    return mergedElement;
  }
}
