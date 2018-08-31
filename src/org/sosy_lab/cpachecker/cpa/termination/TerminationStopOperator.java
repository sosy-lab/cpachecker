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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.exceptions.CPAException;

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

    if (terminationState.isPartOfLoop()
        && terminationState.getHondaLocation() instanceof FunctionEntryNode) {
      return checkCoverageInRecursion(wrappedState, terminationState, wrappedReached, pPrecision);
    }

    return stopOperator.stop(wrappedState, wrappedReached, pPrecision);
  }

  public boolean checkCoverageInRecursion(
      final AbstractState pWrappedState,
      final TerminationState pTerminationState,
      final Collection<AbstractState> pWrappedReached,
      final Precision pPrecision)
      throws CPAException, InterruptedException {
    // maybe unsound, possibly also not sufficient
    // TODO currently does not work properly (stopAlways for callstack works)
    Preconditions.checkArgument(
        pWrappedState instanceof CompositeState,
        "Recursion detection assumes that TerminationCPA wraps CompositeCPA");

    List<AbstractState> elements =
        new ArrayList<>(((CompositeState) pWrappedState).getWrappedStates());
    CompositeState newWrappedState;

    int index = -1;
    CallstackState callState = null;

    for (int i = 0; i < elements.size(); i++) {
      if (elements.get(i) instanceof CallstackState) {
        callState = (CallstackState) elements.get(i);
        index = i;
        break;
      }
    }

    if (index >= 0) {
      String functionName = pTerminationState.getHondaLocation().getFunctionName();

      while (callState != null) {
        elements.set(index, callState);
        newWrappedState = new CompositeState(elements);

        if (stopOperator.stop(newWrappedState, pWrappedReached, pPrecision)) {
          return true;
        }

        do {
          callState = callState.getPreviousState();
        } while (callState != null && !callState.getCurrentFunction().equals(functionName));
      }

      return false;
    }

    return stopOperator.stop(pWrappedState, pWrappedReached, pPrecision);
  }
}
