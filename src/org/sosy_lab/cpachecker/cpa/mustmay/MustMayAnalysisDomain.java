/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.mustmay;

import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/*
 * This join operator is defined point wise.
 */
public class MustMayAnalysisDomain implements AbstractDomain {

  AbstractDomain mMustDomain;
  AbstractDomain mMayDomain;

  MustMayAnalysisJoinOperator mJoinOperator;
  MustMayAnalysisPartialOrder mPartialOrder;

  public MustMayAnalysisDomain(AbstractDomain pMustDomain, AbstractDomain pMayDomain) {
    assert(pMustDomain != null);
    assert(pMayDomain != null);

    mMustDomain = pMustDomain;
    mMayDomain = pMayDomain;

    mJoinOperator = new MustMayAnalysisJoinOperator(mMustDomain, mMayDomain);
    mPartialOrder = new MustMayAnalysisPartialOrder(mMustDomain, mMayDomain);
  }

  @Override
  public AbstractElement join(AbstractElement pElement1,
      AbstractElement pElement2) throws CPAException {
    return mJoinOperator.join(pElement1, pElement2);
  }

  @Override
  public boolean isLessOrEqual(AbstractElement pElement1,
      AbstractElement pElement2) throws CPAException {
    return mPartialOrder.satisfiesPartialOrder(pElement1, pElement2);
  }
}
