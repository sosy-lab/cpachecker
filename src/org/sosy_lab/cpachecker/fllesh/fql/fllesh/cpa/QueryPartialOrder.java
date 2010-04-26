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

import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.PartialOrder;
import org.sosy_lab.cpachecker.cpa.mustmay.MustMayAnalysisPartialOrder;
import org.sosy_lab.cpachecker.exceptions.CPAException;

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
