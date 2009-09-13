package cpa.art;

import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.AbstractElementWithLocation;
import cpa.common.interfaces.MergeOperator;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.RefinableCPA;
import exceptions.CPAException;

public class ARTMergeJoin implements MergeOperator {

  private RefinableCPA wrappedCpa;
  
  public ARTMergeJoin(RefinableCPA pWrappedCPA) {
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
    MergeOperator mergeOperator = wrappedCpa.getMergeOperator();
    AbstractElementWithLocation wrappedElement1 = ((ARTElement)pElement1).getAbstractElementOnArtNode();
    AbstractElementWithLocation wrappedElement2 = ((ARTElement)pElement2).getAbstractElementOnArtNode();
    Precision wrappedPrecision = (pPrecision == null) ? null : ((ARTPrecision)pPrecision).getPrecision();
    AbstractElementWithLocation retElement = mergeOperator.merge(wrappedElement1, wrappedElement2, wrappedPrecision);
    if(retElement == wrappedElement2){
      return pElement2;
    }
    AbstractDomain domain = ((ARTElement)pElement1).getDomain();
    ARTElement newElement = new ARTElement(domain, retElement, (ARTElement)pElement1);
    newElement.addSecondParent((ARTElement)pElement2);
    // TODO new mark or max mark of two elements?
    newElement.setMark();
    return newElement;
  }
}
