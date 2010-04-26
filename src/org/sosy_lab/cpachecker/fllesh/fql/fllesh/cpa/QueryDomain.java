package org.sosy_lab.cpachecker.fllesh.fql.fllesh.cpa;

import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.cpa.mustmay.MustMayAnalysisJoinOperator;
import org.sosy_lab.cpachecker.cpa.mustmay.MustMayAnalysisPartialOrder;

public class QueryDomain implements AbstractDomain {

  private QueryJoinOperator mJoinOperator;
  private QueryPartialOrder mPartialOrder;
  
  private QueryTopElement mTopElement;
  private QueryBottomElement mBottomElement;
  
  public QueryDomain(MustMayAnalysisJoinOperator pJoinOperator, MustMayAnalysisPartialOrder pPartialOrder) {
    mTopElement = QueryTopElement.getInstance();
    mBottomElement = QueryBottomElement.getInstance();
    
    mJoinOperator = new QueryJoinOperator(mTopElement, mBottomElement, pJoinOperator);
    mPartialOrder = new QueryPartialOrder(mTopElement, mBottomElement, pPartialOrder);
  }
  
  @Override
  public QueryBottomElement getBottomElement() {
    return mBottomElement;
  }

  @Override
  public QueryTopElement getTopElement() {
    return mTopElement;
  }

  @Override
  public QueryJoinOperator getJoinOperator() {
    return mJoinOperator;
  }

  @Override
  public QueryPartialOrder getPartialOrder() {
    return mPartialOrder;
  }

}
