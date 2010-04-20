package fllesh.fql.fllesh.cpa;

import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.JoinOperator;
import cpa.mustmay.MustMayAnalysisElement;
import cpa.mustmay.MustMayAnalysisJoinOperator;
import exceptions.CPAException;

public class QueryJoinOperator implements JoinOperator {

  private MustMayAnalysisJoinOperator mJoinOperator;
  
  private QueryTopElement mTopElement;
  private QueryBottomElement mBottomElement;
  
  public QueryJoinOperator(QueryTopElement pTopElement, QueryBottomElement pBottomElement, MustMayAnalysisJoinOperator pJoinOperator) {
    assert(pTopElement != null);
    assert(pBottomElement != null);
    assert(pJoinOperator != null);
    
    mJoinOperator = pJoinOperator;
    
    mTopElement = pTopElement;
    mBottomElement = pBottomElement;
  }
  
  @Override
  public AbstractElement join(AbstractElement pElement1,
      AbstractElement pElement2) throws CPAException {
    
    if (pElement1.equals(mBottomElement)) {
      return pElement2;
    }
    
    if (pElement2.equals(mBottomElement)) {
      return pElement1;
    }
    
    if (pElement1.equals(mTopElement) || pElement2.equals(mTopElement)) {
      return mTopElement;
    }
    
    QueryStandardElement lElement1 = (QueryStandardElement)pElement1;
    QueryStandardElement lElement2 = (QueryStandardElement)pElement2;
    
    if (!lElement1.getAutomatonState1().equals(lElement2.getAutomatonState1())) {
      return mTopElement;
    }
    
    if (!lElement1.getAutomatonState2().equals(lElement2.getAutomatonState2())) {
      return mTopElement;
    }
    
    boolean lMustState1 = lElement1.getMustState1() && lElement2.getMustState1();
    boolean lMustState2 = lElement1.getMustState2() && lElement2.getMustState2();
    
    MustMayAnalysisElement lJoinedElement= mJoinOperator.join(lElement1.getDataSpace(), lElement2.getDataSpace());
    
    return new QueryStandardElement(lElement1.getAutomatonState1(), lMustState1, lElement2.getAutomatonState2(), lMustState2, lJoinedElement);
  }

}
