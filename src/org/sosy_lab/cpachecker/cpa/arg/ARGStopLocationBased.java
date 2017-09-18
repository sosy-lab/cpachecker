/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
import java.util.HashSet;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ForcedCoveringStopOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;



/**
 * StopOperator for cyclic program analyses that use ABE (e.g. Kojak)
 * It stops for states that where there is aready a state in the reached
 * set that has the same location and the same (or more) parents.
 * Apart from that it behaves like {@link ARGStopSep}
 */
public class ARGStopLocationBased implements StopOperator, ForcedCoveringStopOperator {
  /*
   * TODO: This MergeOperator is mostly a copy of ARGMergeJoin =>
   * refactor both classes to remove code duplication
   */

  private final boolean keepCoveredStatesInReached;
  private final boolean inCPAEnabledAnalysis;
  private final StopOperator wrappedStop;
  private final LogManager logger;

  public ARGStopLocationBased(
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
  public boolean stop(AbstractState pElement,
      Collection<AbstractState> pReached, Precision pPrecision) throws CPAException, InterruptedException {

    ARGState argElement = (ARGState)pElement;
    assert !argElement.isCovered() : "Passing element to stop which is already covered: " + argElement;

    // First check if we can take a shortcut:
    // If the new state was merged into an existing element,
    // it is usually also covered by this existing element, so check this explicitly upfront.
    // We do this because we want to remove the new state from the ARG completely
    // in this case and not mark it as covered.

    if (argElement.getMergedWith() != null) {
      ARGState mergedWith = argElement.getMergedWith();

      if (pReached.contains(mergedWith)) {
        // we do this single check first as it should return true in most of the cases

        if (wrappedStop.stop(argElement.getWrappedState(), Collections.singleton(mergedWith.getWrappedState()), pPrecision)) {
          // merged and covered
          if (inCPAEnabledAnalysis) {
            argElement.setCovered(mergedWith);
          } else {
            argElement.removeFromARG();
          }
          logger.log(Level.FINEST, "Element is covered by the element it was merged into");

          // in this case, return true even if we should keep covered states
          // because we should anyway not keep merged states
          return true;

        } else {
          // unexpected case, but possible (if merge does not compute the join, but just widens e2)
          logger.log(Level.FINEST, "Element was merged but not covered:", pElement);
        }

      } else {
        // unexpected case, not sure if it this possible
        logger.log(Level.FINEST, "Element was merged into an element that's not in the reached set, merged-with element is", mergedWith);
      }
    }

    // Never try to cover target states
    if (argElement.isTarget()) {
      return false;
    }

    // Now do the usual coverage checks

    // Here this class differs from ARGStopSep:
    for (AbstractState reachedState : pReached) {
      ARGState argReachedState = (ARGState)reachedState;
      CFANode location1 = AbstractStates.extractLocation(argElement);
      CFANode location2 = AbstractStates.extractLocation(reachedState);
      HashSet<ARGState> parents1 = new HashSet<>(argElement.getParents());
      HashSet<ARGState> parents2 = new HashSet<>(((ARGState)reachedState).getParents());
      if ( (reachedState != pElement && parents2.containsAll(parents1) && location1 == location2) ||
           stop(argElement, argReachedState, pPrecision)) {
        // if this option is true, we always return false here on purpose
        if (keepCoveredStatesInReached) {
          return false;
        } else {
          // ensure consistency between ARG and reached set:
          argElement.removeFromARG();
          return true;
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

  @Override
  public boolean isForcedCoveringPossible(AbstractState pElement, AbstractState pReachedState, Precision pPrecision) throws CPAException, InterruptedException {
    if (!(wrappedStop instanceof ForcedCoveringStopOperator)) {
      return false;
    }

    ARGState element = (ARGState)pElement;
    ARGState reachedState = (ARGState)pReachedState;

    if (reachedState.isCovered() || !reachedState.mayCover()) {
      return false;
    }

    if (element.isOlderThan(reachedState)) {
      return false;
    }

    return ((ForcedCoveringStopOperator)wrappedStop).isForcedCoveringPossible(
        element.getWrappedState(), reachedState.getWrappedState(), pPrecision);
  }
}
