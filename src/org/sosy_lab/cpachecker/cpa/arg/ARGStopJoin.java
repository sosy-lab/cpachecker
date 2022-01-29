// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.arg;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ForcedCoveringStopOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/** A stop join operator for ARGCPA, mostly copy from {@link ARGStopSep} */
public class ARGStopJoin implements StopOperator, ForcedCoveringStopOperator {

  private final boolean keepCoveredStatesInReached;
  private final boolean inCPAEnabledAnalysis;
  private final boolean coverTargetStates;
  private final StopOperator wrappedStop;
  private final LogManager logger;

  public ARGStopJoin(
      StopOperator pWrappedStop,
      LogManager pLogger,
      boolean pInCPAEnabledAnalysis,
      boolean pKeepCoveredStatesInReached,
      boolean pCoverTargetStates) {
    wrappedStop = pWrappedStop;
    logger = pLogger;
    keepCoveredStatesInReached = pKeepCoveredStatesInReached;
    inCPAEnabledAnalysis = pInCPAEnabledAnalysis;
    coverTargetStates = pCoverTargetStates;
  }

  @Override
  public boolean stop(
      AbstractState pElement, Collection<AbstractState> pReached, Precision pPrecision)
      throws CPAException, InterruptedException {

    ARGState argElement = (ARGState) pElement;
    assert !argElement.isCovered()
        : "Passing element to stop which is already covered: " + argElement;

    // First check if we can take a shortcut:
    // If the new state was merged into an existing element,
    // it is usually also covered by this existing element, so check this explicitly upfront.
    // We do this because we want to remove the new state from the ARG completely
    // in this case and not mark it as covered.

    if (argElement.getMergedWith() != null) {
      ARGState mergedWith = argElement.getMergedWith();

      if (pReached.contains(mergedWith)) {
        // we do this single check first as it should return true in most of the cases

        if (wrappedStop.stop(
            argElement.getWrappedState(),
            Collections.singleton(mergedWith.getWrappedState()),
            pPrecision)) {
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
        logger.log(
            Level.FINEST,
            "Element was merged into an element that's not in the reached set, merged-with element is",
            mergedWith);
      }
    }

    // Never try to cover target states except when explicitly stated
    if (!coverTargetStates && argElement.isTarget()) {
      return false;
    }

    // Now do the usual coverage checks

    Collection<AbstractState> mayCoverCandidates = getMayCoverCandidates(argElement, pReached);
    if (mayCoverCandidates.isEmpty()) {
      return false;
    }

    List<AbstractState> mayCoverWrappedStates = new ArrayList<>(mayCoverCandidates.size());
    for (AbstractState candidate : mayCoverCandidates) {
      mayCoverWrappedStates.add(((ARGState) candidate).getWrappedState());
    }

    if (wrappedStop.stop(argElement.getWrappedState(), mayCoverWrappedStates, pPrecision)) {

      // Currently, an arbitrary state is set as the covering state
      // TODO: change the covering relation from 1-to-1 to 1-to-n

      argElement.setCovered((ARGState) mayCoverCandidates.iterator().next());

      // if this option is true, we always return false here on purpose
      return !keepCoveredStatesInReached;
    }
    return false;
  }

  /** Retrieve the set of may-cover candidate states. */
  private Collection<AbstractState> getMayCoverCandidates(
      ARGState pElement, Collection<AbstractState> pReached) {
    List<AbstractState> candidates = new ArrayList<>();
    for (AbstractState reachedState : pReached) {
      ARGState argState = (ARGState) reachedState;
      if (!argState.mayCover()) {
        continue;
      }
      if (Objects.equals(pElement, argState)) {
        continue;
      }
      if (pElement.isOlderThan(argState)) {
        // This is never the case in usual predicate abstraction,
        // but possibly with other algorithms
        // Checking this also implies that pElement gets not covered by
        // one of its children (because they are all newer than pElement).
        continue;
      }
      candidates.add(reachedState);
    }
    return candidates;
  }

  @Override
  public boolean isForcedCoveringPossible(
      AbstractState pElement, AbstractState pReachedState, Precision pPrecision)
      throws CPAException, InterruptedException {
    if (!(wrappedStop instanceof ForcedCoveringStopOperator)) {
      return false;
    }

    ARGState element = (ARGState) pElement;
    ARGState reachedState = (ARGState) pReachedState;

    if (reachedState.isCovered() || !reachedState.mayCover()) {
      return false;
    }

    if (element.isOlderThan(reachedState)) {
      return false;
    }

    return ((ForcedCoveringStopOperator) wrappedStop)
        .isForcedCoveringPossible(
            element.getWrappedState(), reachedState.getWrappedState(), pPrecision);
  }

  @Override
  public Collection<AbstractState> getCoveringStates(
      AbstractState pElement, Collection<AbstractState> pReachedSet, Precision pPrecision)
      throws CPAException, InterruptedException {
    throw new AssertionError(
        "Class "
            + getClass().getSimpleName()
            + " does not support this function. It should not be called.");
  }
}
