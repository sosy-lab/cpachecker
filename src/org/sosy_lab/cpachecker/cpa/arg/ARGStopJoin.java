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
package org.sosy_lab.cpachecker.cpa.arg;

import java.util.Collection;
import java.util.Collections;
import java.util.logging.Level;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ForcedCoveringStopOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.exceptions.CPAException;


/**
 * StopOperator  for slicing abstractions.
 * To be used together with ARGMergeLocationBased
 */
public class ARGStopJoin implements StopOperator, ForcedCoveringStopOperator {

  private final boolean keepCoveredStatesInReached;
  private final boolean inCPAEnabledAnalysis;
  private final StopOperator wrappedStop;
  private final LogManager logger;

  public ARGStopJoin(
      StopOperator pWrappedStop,
      LogManager pLogger,
      boolean pInCPAEnabledAnalysis,
      boolean pKeepCoveredStatesInReached) {
    wrappedStop = pWrappedStop;
    logger = pLogger;
    keepCoveredStatesInReached = pKeepCoveredStatesInReached;
    inCPAEnabledAnalysis = pInCPAEnabledAnalysis;
  }

  @Override
  public boolean isForcedCoveringPossible(AbstractState pState, AbstractState pReachedState,
      Precision pPrecision) throws CPAException, InterruptedException {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean stop(AbstractState pState, Collection<AbstractState> pReached,
      Precision pPrecision) throws CPAException, InterruptedException {
    ARGState argElement = (ARGState) pState;
    assert !argElement.isCovered() : "Passing element to stop which is already covered: " + argElement;

    //We always stop when previous merge was successful:
    if (argElement.getMergedWith() != null) {
      ARGState mergedWith = argElement.getMergedWith();
      assert pReached.contains(mergedWith);
      if (inCPAEnabledAnalysis) {
        argElement.setCovered(mergedWith);
      } else {
        argElement.removeFromARG();
      }
      logger.log(Level.FINEST, "Element is covered by the element it was merged into");

      // in this case, return true even if we should keep covered states
      // because we should anyway not keep merged states
      return true;
    } else if (argElement.isTarget()) {
      // Never try to cover target states
      return false;
    }

    // Now do the usual coverage checks
    for (AbstractState reachedState : pReached) {
      ARGState argReachedState = (ARGState)reachedState;
      if (stop(argElement, argReachedState, pPrecision)) {
        // if this option is true, we always return false here on purpose
        return !keepCoveredStatesInReached;
      }
    }
    return false;
  }

  private boolean stop(ARGState pElement, ARGState pReachedState, Precision pPrecision)
      throws CPAException, InterruptedException {

  if (!pReachedState.mayCover()) {
    return false;
  }
  if (pElement == pReachedState) {
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

  boolean stop = wrappedStop.stop(wrappedState, Collections.singleton(wrappedReachedState), pPrecision);

  if (stop) {
  pElement.setCovered(pReachedState);
  }
  return stop;
}

}
