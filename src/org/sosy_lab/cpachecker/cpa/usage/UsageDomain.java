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
package org.sosy_lab.cpachecker.cpa.usage;

import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.exceptions.CPAException;

class UsageDomain implements AbstractDomain {

  private final AbstractDomain wrappedDomain;

  UsageDomain(AbstractDomain pWrappedDomain) {
    wrappedDomain = pWrappedDomain;
  }

  @Override
  public AbstractState join(AbstractState pElement1, AbstractState pElement2) {
    //if (pElement1.getClass() != UsageStatisticsState.class || pElement2.getClass() != UsageStatisticsState.class)
      throw new UnsupportedOperationException();

    /*UsageStatisticsState state1 = (UsageStatisticsState) pElement1;
    UsageStatisticsState state2 = (UsageStatisticsState) pElement2;

    return state1.join(state2);*/
  }

  @Override
  public boolean isLessOrEqual(AbstractState pElement1, AbstractState pElement2) throws CPAException, InterruptedException {
    // returns true if element1 < element2 on lattice

    UsageState elem1 = (UsageState) pElement1;
    UsageState elem2 = (UsageState) pElement2;

    return (wrappedDomain.isLessOrEqual(elem1.getWrappedState(), elem2.getWrappedState()) &&
        elem1.isLessOrEqual(elem2));
  }
}
