/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.automaton;

import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.exceptions.CPAException;

import com.google.common.base.Preconditions;


public class AutomatonDomain implements AbstractDomain {

  private final AutomatonState topState;
  private final AutomatonState inactiveState;

  public AutomatonDomain(AutomatonState pTopState, AutomatonState pInactiveState) {
    this.topState = Preconditions.checkNotNull(pTopState);
    this.inactiveState = Preconditions.checkNotNull(pInactiveState);
  }

  @Override
  public AbstractState join(AbstractState pE1, AbstractState pE2) throws CPAException {

    if (isLessOrEqual(pE1, pE2)) {
      return pE2;
    }

    if (isLessOrEqual(pE2, pE1)) {
      return pE1;
    }

    return topState;
  }

  @Override
  public boolean isLessOrEqual(AbstractState pNewState, AbstractState pReachedState) throws CPAException {
    final AutomatonState newState = (AutomatonState) pNewState;
    final AutomatonState reachedState = (AutomatonState) pReachedState;

    if (reachedState.equals(topState)) {
      return true;
    }

    if (newState.equals(topState)) {
      return reachedState.equals(topState);
    }

    if (newState.equals(reachedState)) {
      return true;
    }

    return false;
  }

}
