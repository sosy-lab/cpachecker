package fllesh.fql.fllesh.cpa;

import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.PartialOrder;
import cpa.mustmay.MustMayAnalysisPartialOrder;
import exceptions.CPAException;

public class QueryPartialOrder implements PartialOrder {

  private MustMayAnalysisPartialOrder mPartialOrder;
  
  private QueryTopElement mTopElement;
  private QueryBottomElement mBottomElement;
  
  public QueryPartialOrder(QueryTopElement pTopElement, QueryBottomElement pBottomElement, MustMayAnalysisPartialOrder pPartialOrder) {
    assert(pTopElement != null);
    assert(pBottomElement != null);
    assert(pPartialOrder != null);
    
    mPartialOrder = pPartialOrder;
    
    mTopElement = pTopElement;
    mBottomElement = pBottomElement;
  }
  
  @Override
  public boolean satisfiesPartialOrder(AbstractElement pElement1,
      AbstractElement pElement2) throws CPAException {
    if (pElement2.equals(mTopElement)) {
      return true;
    }
    
    if (pElement1.equals(mBottomElement)) {
      return true;
    }
    
    if (pElement1.equals(mTopElement)) {
      return false;
    }
    
    if (pElement2.equals(mBottomElement)) {
      return false;
    }
    
    QueryStandardElement lElement1 = (QueryStandardElement)pElement1;
    QueryStandardElement lElement2 = (QueryStandardElement)pElement2;
    
    if (!lElement1.getAutomatonState1().equals(lElement2.getAutomatonState1())) {
      return false;
    }
    
    if (!lElement1.getAutomatonState2().equals(lElement2.getAutomatonState2())) {
      return false;
    }
    
    boolean lCondition1 = !lElement2.getMustState1() || lElement1.getMustState1();
    boolean lCondition2 = !lElement2.getMustState2() || lElement1.getMustState2();
    
    if (lCondition1 && lCondition2) {
      return mPartialOrder.satisfiesPartialOrder(lElement1.getDataSpace(), lElement2.getDataSpace());
    }
    
    return false;
  }

}
