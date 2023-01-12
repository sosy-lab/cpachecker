// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.defaults;

import com.google.common.base.Function;
import java.util.Optional;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult.Action;
import org.sosy_lab.cpachecker.core.interfaces.Targetable;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * Implementation of prec operator which does not change the precision or the state, but checks for
 * target states and signals a break in this case.
 */
public class BreakOnTargetsPrecisionAdjustment implements PrecisionAdjustment {

  /** the counter for targets found so far */
  private int foundTargetCounter = 0;

  /** the counter for iterations since the first target found */
  private int extraIterations = 0;

  /** the size of the reached set in the previous call to {@link #prec}. */
  private int previousReachedSetSize = 0;

  /**
   * the predefined limit determining at which number of found target states the analysis should
   * receive a signal of {@link Action#BREAK}
   */
  private final int foundTargetLimit;

  /**
   * the predefined limit of number of iterations since finding the first target, at which the
   * analysis should receive a signal of {@link Action#BREAK}, despite the number of {@link
   * #foundTargetCounter} was not yet reached.
   */
  private final int extraIterationsLimit;

  public BreakOnTargetsPrecisionAdjustment(
      final int pFoundTargetLimit, final int pExtraIterationsLimit) {
    foundTargetLimit = pFoundTargetLimit;
    extraIterationsLimit = pExtraIterationsLimit;
  }

  @Override
  public Optional<PrecisionAdjustmentResult> prec(
      final AbstractState pState,
      final Precision pPrecision,
      final UnmodifiableReachedSet pStates,
      Function<AbstractState, AbstractState> projection,
      final AbstractState fullState)
      throws CPAException {

    resetCountersIfNecessary(pStates);

    if (foundTargetCounter > 0) {
      extraIterations++;
    }

    if (extraIterationsLimitReached()) {
      return Optional.of(PrecisionAdjustmentResult.create(pState, pPrecision, Action.BREAK));
    }

    if (((Targetable) pState).isTarget()) {
      foundTargetCounter++;

      if (foundTargetLimitReached()) {
        return Optional.of(PrecisionAdjustmentResult.create(pState, pPrecision, Action.BREAK));
      }
    }

    return Optional.of(PrecisionAdjustmentResult.create(pState, pPrecision, Action.CONTINUE));
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
   * This method resets the counter, if needed, e.g., when a refinement happened between calls to
   * prec().
   *
   * @param pStates the current reached set
   */
  private void resetCountersIfNecessary(final UnmodifiableReachedSet pStates) {
    if (pStates.size() < previousReachedSetSize) {
      resetCounters();
    }

    previousReachedSetSize = pStates.size();
  }

  /** This method resets all counters */
  private void resetCounters() {
    foundTargetCounter = 0;
    extraIterations = 0;
  }
}
