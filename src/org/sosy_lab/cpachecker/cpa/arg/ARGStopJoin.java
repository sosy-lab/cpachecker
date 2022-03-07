// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.arg;

import de.uni_freiburg.informatik.ultimate.util.datastructures.ImmutableSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CoveringStateSetProvider;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.exceptions.CPAException;

class ARGStopJoin extends ARGStop {
  public ARGStopJoin(
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

    // get the list of may-cover ARG states
    Collection<ARGState> mayCoverCandidates = getMayCoverCandidates(argElement, pReached);
    if (mayCoverCandidates.isEmpty()) {
      return false;
    }

    // get the list of may-cover wrapped states
    List<AbstractState> mayCoverWrappedStates = new ArrayList<>(mayCoverCandidates.size());
    for (ARGState candidate : mayCoverCandidates) {
      mayCoverWrappedStates.add(candidate.getWrappedState());
    }

    if (!(wrappedStop instanceof CoveringStateSetProvider)) {
      if (wrappedStop.stop(argElement.getWrappedState(), mayCoverWrappedStates, pPrecision)) {
        argElement.setCovered(ImmutableSet.copyOf(mayCoverCandidates));
        return !keepCoveredStatesInReached;
      }
      return false;
    }

    CoveringStateSetProvider stopOp = (CoveringStateSetProvider) wrappedStop;
    Collection<AbstractState> coveringAbsStates =
        stopOp.getCoveringStates(argElement.getWrappedState(), mayCoverWrappedStates, pPrecision);
    if (coveringAbsStates.isEmpty()) {
      return false;
    }

    // mapping of wrapped state -> ARG state
    Map<AbstractState, ARGState> stateMap = new LinkedHashMap<>(mayCoverWrappedStates.size());
    for (ARGState argState : mayCoverCandidates) {
      AbstractState absState = argState.getWrappedState();
      stateMap.put(absState, argState);
    }

    // map the covering wrapped states back to the corresponding ARG states and collect them
    Set<ARGState> coveringARGStates = new LinkedHashSet<>(coveringAbsStates.size());
    for (AbstractState absState : coveringAbsStates) {
      coveringARGStates.add(stateMap.get(absState));
    }

    // store the coverage relation
    argElement.setCovered(ImmutableSet.copyOf(coveringARGStates));
    return !keepCoveredStatesInReached;
  }

  /** Retrieve the set of may-cover candidate states. */
  private Collection<ARGState> getMayCoverCandidates(
      ARGState pElement, Collection<AbstractState> pReached) {
    List<ARGState> candidates = new ArrayList<>();
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
    return candidates;
  }
}
