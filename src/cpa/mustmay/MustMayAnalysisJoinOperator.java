package cpa.mustmay;

import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.JoinOperator;
import cpa.mustmay.MustMayAnalysisElement;
import exceptions.CPAException;

public class MustMayAnalysisJoinOperator implements JoinOperator {

  JoinOperator mMustJoinOperator;
  JoinOperator mMayJoinOperator;
  
  public MustMayAnalysisJoinOperator(JoinOperator pMustJoinOperator, JoinOperator pMayJoinOperator) {
    assert(pMustJoinOperator != null);
    assert(pMayJoinOperator != null);
    
    mMustJoinOperator = pMustJoinOperator;
    mMayJoinOperator = pMayJoinOperator;
  }
  
  @Override
  public MustMayAnalysisElement join(AbstractElement pElement1,
      AbstractElement pElement2) throws CPAException {
    assert(pElement1 != null);
    assert(pElement2 != null);
    
    assert(pElement1 instanceof MustMayAnalysisElement);
    assert(pElement2 instanceof MustMayAnalysisElement);
    
    MustMayAnalysisElement lElement1 = (MustMayAnalysisElement)pElement1;
    MustMayAnalysisElement lElement2 = (MustMayAnalysisElement)pElement2;
    
    AbstractElement lMustElement = mMustJoinOperator.join(lElement1.getMustElement(), lElement2.getMustElement());
    AbstractElement lMayElement = mMayJoinOperator.join(lElement1.getMayElement(), lElement2.getMayElement());
    
    return new MustMayAnalysisElement(lMustElement, lMayElement);
  }
  
}
