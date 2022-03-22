// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.arg;

import static org.sosy_lab.common.collect.Collections3.transformedImmutableSetCopy;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Objects;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CoveringStateSetProvider;
import org.sosy_lab.cpachecker.core.interfaces.ForcedCoveringStopOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.exceptions.CPAException;

class ARGStop implements ForcedCoveringStopOperator {
  private final boolean keepCoveredStatesInReached;
  private final boolean inCPAEnabledAnalysis;
  private final boolean coverTargetStates;
  private final StopOperator wrappedStop;
  private final LogManager logger;

  public ARGStop(
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

    if (checkCoveredByMergedWith(pElement, pReached, pPrecision)) {
      return true;
    }

    // Never try to cover target states except when explicitly stated
    if (!coverTargetStates && argElement.isTarget()) {
      return false;
    }

    // Now do the usual coverage checks

    // get the list of may-cover ARG states
    ImmutableSet<ARGState> mayCoverARGStates = getMayCoverCandidates(argElement, pReached);
    if (mayCoverARGStates.isEmpty()) {
      return false;
    }

    // get the collection of may-cover wrapped states
    ImmutableSet<AbstractState> mayCoverWrappedStates =
        mayCoverARGStates.stream()
            .map(ARGState::getWrappedState)
            .collect(ImmutableSet.toImmutableSet());

    // if retrieval of covering state set is not possible --> do the normal stop check
    if (!(wrappedStop instanceof CoveringStateSetProvider)) {
      if (wrappedStop.stop(argElement.getWrappedState(), mayCoverWrappedStates, pPrecision)) {
        argElement.setCovered(mayCoverARGStates);
        return !keepCoveredStatesInReached;
      }
      return false;
    }

    CoveringStateSetProvider stopOp = (CoveringStateSetProvider) wrappedStop;
    Collection<AbstractState> coveringWrappedStates =
        stopOp.getCoveringStates(argElement.getWrappedState(), mayCoverWrappedStates, pPrecision);
    if (coveringWrappedStates.isEmpty()) {
      return false;
    }

    // mapping of wrapped state -> ARG state
    ImmutableMap<AbstractState, ARGState> stateMap =
        mayCoverARGStates.stream()
            .collect(ImmutableMap.toImmutableMap(ARGState::getWrappedState, argState -> argState));

    // map the covering wrapped states back to the corresponding ARG states and collect them
    ImmutableSet<ARGState> coveringARGStates =
        transformedImmutableSetCopy(coveringWrappedStates, absState -> stateMap.get(absState));

    // store the coverage relation
    argElement.setCovered(ImmutableSet.copyOf(coveringARGStates));
    return !keepCoveredStatesInReached;
  }

  /** Retrieve the set of may-cover candidate ARG states. */
  private ImmutableSet<ARGState> getMayCoverCandidates(
      ARGState pElement, Collection<AbstractState> pReached) {
    ImmutableSet.Builder<ARGState> candidates = ImmutableSet.builder();
    for (AbstractState reachedState : pReached) {
      ARGState argState = (ARGState) reachedState;
      if (!argState.mayCover()) {
        continue;
      }
      if (argState.isCovered()) {
        // TODO: newly added constraint, check validity
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
      candidates.add(argState);
    }
    return candidates.build();
  }

  private boolean checkCoveredByMergedWith(
      AbstractState pElement, Collection<AbstractState> pReached, Precision pPrecision)
      throws CPAException, InterruptedException {
    // First check if we can take a shortcut:
    // If the new state was merged into an existing element,
    // it is usually also covered by this existing element, so check this explicitly upfront.
    // We do this because we want to remove the new state from the ARG completely
    // in this case and not mark it as covered.

    ARGState argElement = (ARGState) pElement;
    if (argElement.getMergedWith() != null) {
      ARGState mergedWith = argElement.getMergedWith();

      if (pReached.contains(mergedWith)) {
        // we do this single check first as it should return true in most of the cases

        if (wrappedStop.stop(
            argElement.getWrappedState(),
            ImmutableSet.of(mergedWith.getWrappedState()),
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
            "Element was merged into an element that's not in the reached set, merged-with element"
                + " is",
            mergedWith);
      }
    }
    return false;
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
}
