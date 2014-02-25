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
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ForcedCoveringStopOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.pcc.ProofChecker;
import org.sosy_lab.cpachecker.exceptions.CPAException;

@Options(prefix="cpa.arg")
public class ARGStopSep implements StopOperator, ForcedCoveringStopOperator {

  @Option(description="whether to keep covered states in the reached set as addition to keeping them in the ARG")
  private boolean keepCoveredStatesInReached = false;

  @Option(
  description="inform ARG CPA if it is run in a predicated analysis because then it must"
    + "behave differntly during merge.")
  private boolean inPredicatedAnalysis = false;

  private final StopOperator wrappedStop;
  private final LogManager logger;

  public ARGStopSep(StopOperator pWrappedStop, LogManager pLogger, Configuration config) throws InvalidConfigurationException {
    config.inject(this);
    wrappedStop = pWrappedStop;
    logger = pLogger;
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
          if (inPredicatedAnalysis) {
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

  boolean isCoveredBy(AbstractState pElement, AbstractState pOtherElement, ProofChecker wrappedProofChecker) throws CPAException, InterruptedException {
    ARGState argElement = (ARGState)pElement;
    ARGState otherArtElement = (ARGState)pOtherElement;

    AbstractState wrappedState = argElement.getWrappedState();
    AbstractState wrappedOtherElement = otherArtElement.getWrappedState();

    return wrappedProofChecker.isCoveredBy(wrappedState, wrappedOtherElement);
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
