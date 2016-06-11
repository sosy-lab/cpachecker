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

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.exceptions.CPAException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

public class TerminationStopOperator implements StopOperator {

  private final StopOperator stopOperator;

  public TerminationStopOperator(StopOperator pStopOperator) {
    stopOperator = Preconditions.checkNotNull(pStopOperator);
  }

  @Override
  public boolean stop(
      AbstractState pState, Collection<AbstractState> pReached, Precision pPrecision)
      throws CPAException, InterruptedException {
    TerminationState terminationState = (TerminationState) pState;
    AbstractState wrappedState = terminationState.getWrappedState();

    // Separate states from loop and stem.
    Collection<AbstractState> wrappedReached =
        pReached
            .stream()
            .map(s -> (TerminationState) s)
            .filter(s -> terminationState.isPartOfLoop() == s.isPartOfLoop())
            .map(TerminationState::getWrappedState)
            .collect(Collectors.toCollection(ArrayList::new));

    return stopOperator.stop(wrappedState, wrappedReached, pPrecision);
  }
}
