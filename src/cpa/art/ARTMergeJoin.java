package cpa.art;

import java.util.Set;

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
//    ARTElement parent1 = ((ARTElement)pElement1).getParent();
//    ARTElement parent2 = ((ARTElement)pElement2).getParent();

    // note that these assertions will fail if top sort is not used with this
    // analysis
//    assert(parent1.removeFromChildren(((ARTElement)pElement1)));
//    assert(parent2.removeFromChildren(((ARTElement)pElement2)));

    AbstractDomain domain = ((ARTElement)pElement1).getDomain();
    ARTElement newElement = new ARTElement(domain, retElement, null);
//    newElement.addParent(parent2);

    Set<ARTElement> otherParents1 = ((ARTElement)pElement1).getParents();
    Set<ARTElement> otherParents2 = ((ARTElement)pElement2).getParents();

    for(ARTElement otherParent: otherParents1){
      assert(otherParent.removeFromChildren(((ARTElement)pElement1)));
      newElement.addParent(otherParent);
    }

    for(ARTElement otherParent: otherParents2){
      assert(otherParent.removeFromChildren(((ARTElement)pElement2)));
      newElement.addParent(otherParent);
    }

    // TODO new mark or max mark of two elements?
    newElement.setMark();
//  newElement.setMark(Math.max(((ARTElement)pElement1).getMark(), ((ARTElement)pElement2).getMark()));
    return newElement;
  }
}
