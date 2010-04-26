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
import org.sosy_lab.cpachecker.core.interfaces.JoinOperator;
import org.sosy_lab.cpachecker.cpa.mustmay.MustMayAnalysisElement;
import org.sosy_lab.cpachecker.cpa.mustmay.MustMayAnalysisJoinOperator;
import org.sosy_lab.cpachecker.exceptions.CPAException;

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
