/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.termination;

import com.google.common.base.Preconditions;

import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class TerminationAbstractDomain implements AbstractDomain {

  private final AbstractDomain abstractDomain;

  public TerminationAbstractDomain(AbstractDomain pAbstractDomain) {
    abstractDomain = Preconditions.checkNotNull(pAbstractDomain);
  }

  @Override
  public TerminationState join(AbstractState pState1, AbstractState pState2)
      throws CPAException, InterruptedException {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isLessOrEqual(AbstractState pState1, AbstractState pState2)
      throws CPAException, InterruptedException {
    TerminationState state1 = (TerminationState) pState1;
    TerminationState state2 = (TerminationState) pState2;
    AbstractState wrappedState1 = state1.getWrappedState();
    AbstractState wrappedState2 = state2.getWrappedState();

    return state1.isPartOfLoop() == state2.isPartOfLoop()
        && abstractDomain.isLessOrEqual(wrappedState1, wrappedState2);
  }
}
