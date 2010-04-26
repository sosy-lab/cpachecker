package org.sosy_lab.cpachecker.cpa.mustmay;

import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;

/*
 * This join operator is defined point wise.
 */
public class MustMayAnalysisDomain implements AbstractDomain {

  AbstractDomain mMustDomain;
  AbstractDomain mMayDomain;
  
  MustMayAnalysisElement mTopElement;
  MustMayAnalysisElement mBottomElement;
  
  MustMayAnalysisJoinOperator mJoinOperator;
  MustMayAnalysisPartialOrder mPartialOrder;
  
  public MustMayAnalysisDomain(AbstractDomain pMustDomain, AbstractDomain pMayDomain) {
    assert(pMustDomain != null);
    assert(pMayDomain != null);
    
    mMustDomain = pMustDomain;
    mMayDomain = pMayDomain;
    
    mTopElement = new MustMayAnalysisElement(pMustDomain.getTopElement(), pMayDomain.getTopElement());
    mBottomElement = new MustMayAnalysisElement(pMustDomain.getBottomElement(), pMayDomain.getBottomElement());
    
    mJoinOperator = new MustMayAnalysisJoinOperator(mMustDomain.getJoinOperator(), mMayDomain.getJoinOperator());
    mPartialOrder = new MustMayAnalysisPartialOrder(mMustDomain.getPartialOrder(), mMayDomain.getPartialOrder());
  }
  
  @Override
  public MustMayAnalysisElement getBottomElement() {
    return mBottomElement;
  }

  @Override
  public MustMayAnalysisElement getTopElement() {
    return mTopElement;
  }

  @Override
  public MustMayAnalysisJoinOperator getJoinOperator() {
    return mJoinOperator;
  }

  @Override
  public MustMayAnalysisPartialOrder getPartialOrder() {
    return mPartialOrder;
  }

}
