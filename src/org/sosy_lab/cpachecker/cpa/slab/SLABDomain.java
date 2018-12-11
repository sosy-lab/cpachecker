/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.slab;

import static com.google.common.base.Preconditions.checkNotNull;

import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractDomain;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;

public class SLABDomain implements AbstractDomain {

  private PredicateAbstractDomain predicateDomain;

  public SLABDomain(PredicateAbstractDomain pWrappedDomain) {
    predicateDomain = pWrappedDomain;
  }

  @Override
  public AbstractState join(AbstractState pState1, AbstractState pState2)
      throws CPAException, InterruptedException {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isLessOrEqual(AbstractState pState1, AbstractState pState2)
      throws CPAException, InterruptedException {
    SLARGState state1 = (SLARGState) pState1;
    SLARGState state2 = (SLARGState) pState2;
    boolean answer = predicateSubsumption(state1, state2);
    answer &= state2.getParents().containsAll(state1.getParents());
    return answer;
  }

  boolean predicateSubsumption(SLARGState pState1, SLARGState pState2)
      throws CPAException, InterruptedException {
    PredicateAbstractState pred1 =
        checkNotNull(AbstractStates.extractStateByType(pState1, PredicateAbstractState.class));
    PredicateAbstractState pred2 =
        checkNotNull(AbstractStates.extractStateByType(pState2, PredicateAbstractState.class));
    return predicateDomain.isLessOrEqual(pred1, pred2)
        && predicateDomain.isLessOrEqual(pred2, pred1);
  }

}
