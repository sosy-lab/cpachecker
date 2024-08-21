// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.termination;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.core.algorithm.termination.TerminationLoopInformation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;

public class TerminationStopOperator implements StopOperator {

  private final StopOperator stopOperator;
  private final TerminationLoopInformation terminationInformation;

  public TerminationStopOperator(
      StopOperator pStopOperator, TerminationLoopInformation pTerminationInformation) {
    stopOperator = Preconditions.checkNotNull(pStopOperator);
    terminationInformation = pTerminationInformation;
  }

  @Override
  public boolean stop(
      AbstractState pState, Collection<AbstractState> pReached, Precision pPrecision)
      throws CPAException, InterruptedException {
    TerminationState terminationState = (TerminationState) pState;
    AbstractState wrappedState = terminationState.getWrappedState();

    // Separate states from loop and stem.
    Collection<AbstractState> wrappedReached =
        pReached.stream()
            .map(s -> (TerminationState) s)
            .filter(s -> terminationState.isPartOfLoop() == s.isPartOfLoop())
            .map(TerminationState::getWrappedState)
            .collect(Collectors.toCollection(ArrayList::new));
    CFANode locNode = AbstractStates.extractLocation(wrappedState);

    if ((terminationState.isPartOfLoop()
            && terminationState.getHondaLocation() instanceof FunctionEntryNode)
        || (locNode instanceof FunctionEntryNode && terminationInformation.isLoopHead(locNode))) {
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
      String functionName;
      if (pTerminationState.isPartOfLoop()) {
        functionName = pTerminationState.getHondaLocation().getFunctionName();
      } else {
        functionName = AbstractStates.extractLocation(pWrappedState).getFunctionName();
      }

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
