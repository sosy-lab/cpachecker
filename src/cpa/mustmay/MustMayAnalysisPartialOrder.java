package cpa.mustmay;

import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.PartialOrder;
import exceptions.CPAException;

public class MustMayAnalysisPartialOrder implements PartialOrder {

  PartialOrder mMustPartialOrder;
  PartialOrder mMayPartialOrder;
  
  public MustMayAnalysisPartialOrder(PartialOrder pMustPartialOrder, PartialOrder pMayPartialOrder) {
    assert(pMustPartialOrder != null);
    assert(pMayPartialOrder != null);
    
    mMustPartialOrder = pMustPartialOrder;
    mMayPartialOrder = pMayPartialOrder;
  }
  
  @Override
  public boolean satisfiesPartialOrder(AbstractElement pElement1,
      AbstractElement pElement2) throws CPAException {
    assert(pElement1 != null);
    assert(pElement2 != null);
    
    assert(pElement1 instanceof MustMayAnalysisElement);
    assert(pElement2 instanceof MustMayAnalysisElement);
    
    MustMayAnalysisElement lElement1 = (MustMayAnalysisElement)pElement1;
    MustMayAnalysisElement lElement2 = (MustMayAnalysisElement)pElement2;
    
    return (mMustPartialOrder.satisfiesPartialOrder(lElement1.getMustElement(), lElement2.getMustElement()) && mMayPartialOrder.satisfiesPartialOrder(lElement1.getMayElement(), lElement2.getMayElement()));
  }

}
