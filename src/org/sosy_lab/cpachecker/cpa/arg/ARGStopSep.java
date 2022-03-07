// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.arg;

import com.google.common.collect.Iterables;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.exceptions.CPAException;

class ARGStopSep extends ARGStop {

  public ARGStopSep(
      StopOperator pWrappedStop,
      LogManager pLogger,
      boolean pInCPAEnabledAnalysis,
      boolean pKeepCoveredStatesInReached,
      boolean pCoverTargetStates) {
    super(
        pWrappedStop,
        pLogger,
        pInCPAEnabledAnalysis,
        pKeepCoveredStatesInReached,
        pCoverTargetStates);
  }

  @Override
  public boolean stop(
      AbstractState pElement, Collection<AbstractState> pReached, Precision pPrecision)
      throws CPAException, InterruptedException {

    ARGState argElement = (ARGState) pElement;
    assert !argElement.isCovered()
        : "Passing element to stop which is already covered: " + argElement;

    if (checkCoveredByMergedWith(pElement, pReached, pPrecision)) {
      return true;
    }

    // Never try to cover target states except when explicitly stated
    if (!coverTargetStates && argElement.isTarget()) {
      return false;
    }

    // Now do the usual coverage checks

    // Check if the argElement has only one parent and remember it for later:
    ARGState parent = null;
    if (argElement.getParents().size() == 1) {
      parent = Iterables.get(argElement.getParents(), 0);
    }

    for (AbstractState reachedState : pReached) {
      ARGState argReachedState = (ARGState) reachedState;
      if (stop(argElement, argReachedState, pPrecision)) {
        if (parent != null && argReachedState.getParents().contains(parent)) {
          // if the covering state has the same parent as the covered state
          // and if the covered state has no other parents,
          // it should always be safe to remove the covered state:
          argElement.removeFromARG();
          return true;
        } else {
          // if this option is true, we always return false here on purpose
          return !keepCoveredStatesInReached;
        }
      }
    }
    return false;
  }

  private boolean stop(ARGState pElement, ARGState pReachedState, Precision pPrecision)
      throws CPAException, InterruptedException {

    if (!pReachedState.mayCover()) {
      return false;
    }
    if (Objects.equals(pElement, pReachedState)) {
      return false;
    }
    if (pElement.isOlderThan(pReachedState)) {
      // This is never the case in usual predicate abstraction,
      // but possibly with other algorithms
      // Checking this also implies that pElement gets not covered by
      // one of its children (because they are all newer than pElement).
      return false;
    }

    AbstractState wrappedState = pElement.getWrappedState();
    AbstractState wrappedReachedState = pReachedState.getWrappedState();

    boolean stop =
        wrappedStop.stop(wrappedState, Collections.singleton(wrappedReachedState), pPrecision);

    if (stop) {
      pElement.setCovered(pReachedState);
    }
    return stop;
  }
}
