/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker. 
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
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
