/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.defaults;

import org.sosy_lab.common.Triple;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.Targetable;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * Implementation of prec operator which does not change the precision or
 * the state, but checks for target states and signals a break in this case.
 */
public class BreakOnTargetsPrecisionAdjustment implements PrecisionAdjustment {

  /**
   * the counter for targets found so far
   */
  private int foundTargetCounter      = 0;

  /**
   * the counter for iterations since the first target found
   */
  private int extraIterations         = 0;

  /**
   * the size of the reached set in the previous call to {@link #prec(AbstractState, Precision, UnmodifiableReachedSet)}.
   */
  private int previousReachedSetSize  = 0;

  /**
   * the predefined limit determining at which number of found target states the analysis should receive a signal of
   * {@link Action#BREAK}
   */
  private final int foundTargetLimit;

  /**
   * the predefined limit of number of iterations since finding the first target, at which the analysis should receive a
   * signal of {@link Action#BREAK}, despite the number of {@link #foundTargetCounter} was not yet reached.
   */
  private final int extraIterationsLimit;

  private static PrecisionAdjustment instance;

  public static PrecisionAdjustment getInstance(final int pFoundTargetLimit, final int pExtraIterationsLimit) {
    if (instance == null) {
      instance = new BreakOnTargetsPrecisionAdjustment(pFoundTargetLimit, pExtraIterationsLimit);
    }

    return instance;
  }

  private BreakOnTargetsPrecisionAdjustment(final int pFoundTargetLimit, final int pExtraIterationsLimit) {
    foundTargetLimit      = pFoundTargetLimit;
    extraIterationsLimit  = pExtraIterationsLimit;
  }

  @Override
  public Triple<AbstractState, Precision, Action> prec(final AbstractState pState,
      final Precision pPrecision,
      final UnmodifiableReachedSet pStates)
          throws CPAException {

    resetCountersIfNecessary(pStates);

    if (foundTargetCounter > 0) {
      extraIterations++;
    }

    if (extraIterationsLimitReached()) {
      return Triple.of(pState, pPrecision, Action.BREAK);
    }

    if (((Targetable)pState).isTarget()) {
      foundTargetCounter++;

      if (foundTargetLimitReached()) {
        return Triple.of(pState, pPrecision, Action.BREAK);
      }
    }

    return Triple.of(pState, pPrecision, Action.CONTINUE);
  }

  /**
   * This method returns true if the limit of target states to be found is reached.
   *
   * @return true, if the limit of target states to be found is reached, else false
   */
  private boolean foundTargetLimitReached() {
    return foundTargetCounter >= foundTargetLimit;
  }

  /**
   * This method return true if the limit of extra iterations is reached.
   *
   * @return true, if the maximum of extra iterations is reached, else false
   */
  private boolean extraIterationsLimitReached() {
    return (foundTargetCounter > 0)
        && ((extraIterationsLimit != -1) && (extraIterations > extraIterationsLimit));
  }

  /**
   * This method resets the counter, if needed, e.g., when a refinement happened between calls to prec().
   *
   * @param pStates the current reached set
   * @return
   */
  private void resetCountersIfNecessary(final UnmodifiableReachedSet pStates) {
    if (pStates.size() < previousReachedSetSize) {
      resetCounters();
    }

    previousReachedSetSize = pStates.size();
  }

  /**
   * This method resets all counters
   */
  private void resetCounters() {
    foundTargetCounter  = 0;
    extraIterations     = 0;
  }
}