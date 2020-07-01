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

import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class SLABDomain implements AbstractDomain {

  private AbstractDomain wrappedDomain;

  public SLABDomain(AbstractDomain pWrappedDomain) {
    wrappedDomain = pWrappedDomain;
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
    return wrappedSubsumption(state1, state2)
        && state2.getParents().containsAll(state1.getParents());
  }

  protected boolean wrappedSubsumption(
      AbstractSingleWrapperState pState1, AbstractSingleWrapperState pState2)
      throws CPAException, InterruptedException {
    return wrappedDomain.isLessOrEqual(pState1.getWrappedState(), pState2.getWrappedState());
  }
}
