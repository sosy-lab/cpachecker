/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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

public class MustMayAnalysisPartialOrder {

  AbstractDomain mMustDomain;
  AbstractDomain mMayDomain;

  public MustMayAnalysisPartialOrder(AbstractDomain pMustDomain, AbstractDomain pMayDomain) {
    assert(pMustDomain != null);
    assert(pMayDomain != null);

    mMustDomain = pMustDomain;
    mMayDomain = pMayDomain;
  }

  public boolean satisfiesPartialOrder(AbstractElement pElement1,
      AbstractElement pElement2) throws CPAException {
    assert(pElement1 != null);
    assert(pElement2 != null);

    assert(pElement1 instanceof MustMayAnalysisElement);
    assert(pElement2 instanceof MustMayAnalysisElement);

    MustMayAnalysisElement lElement1 = (MustMayAnalysisElement)pElement1;
    MustMayAnalysisElement lElement2 = (MustMayAnalysisElement)pElement2;

    boolean mustSatisfies;
    if (lElement1.getMustElement() == MustMayAnalysisElement.DONT_KNOW_ELEMENT) {
      mustSatisfies = true;
    } else if (lElement2.getMustElement() == MustMayAnalysisElement.DONT_KNOW_ELEMENT) {
      mustSatisfies = false;
    } else {
      mustSatisfies = mMustDomain.isLessOrEqual(lElement1.getMustElement(), lElement2.getMustElement());
    }

    boolean maySatisfies = mMayDomain.isLessOrEqual(lElement1.getMayElement(), lElement2.getMayElement());
    return (mustSatisfies && maySatisfies);
  }

}
