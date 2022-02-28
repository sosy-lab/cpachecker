// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate;

import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import org.sosy_lab.cpachecker.core.defaults.StopJoinOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ForcedCoveringStopOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/** TODO: add description */
class PredicateStopJoinOperator extends StopJoinOperator implements ForcedCoveringStopOperator {
  private final boolean minimizeCovering;
  private final PredicateAbstractDomain domain;

  PredicateStopJoinOperator(AbstractDomain pD, boolean pMinimizeCovering) {
    super(pD);
    domain = (PredicateAbstractDomain) pD;
    minimizeCovering = pMinimizeCovering;
  }

  @Override
  public boolean isForcedCoveringPossible(
      AbstractState pElement, AbstractState pReachedState, Precision pPrecision)
      throws CPAException {

    // We support forced covering, so this is always possible,
    // if we have two abstraction elements.
    // Note that this does not say that the element will actually be covered,
    // it says only that we can try to cover it.
    return ((PredicateAbstractState) pElement).isAbstractionState()
        && ((PredicateAbstractState) pReachedState).isAbstractionState();
  }

  @Override
  public Collection<AbstractState> getCoveringStates(
      AbstractState pElement, Collection<AbstractState> pReachedSet, Precision pPrecision)
      throws CPAException, InterruptedException {
    if (stop(pElement, pReachedSet, pPrecision)) {
      return minimizeCovering
          ? minimizeCoveringStateSet(pElement, pReachedSet)
          : ImmutableSet.copyOf(pReachedSet);
    }

    return ImmutableSet.of();
  }

  private Set<AbstractState> minimizeCoveringStateSet(
      AbstractState pElement, Collection<AbstractState> pCoveringSet)
      throws CPAException, InterruptedException {
    Collection<AbstractState> coveringSubset = new LinkedHashSet<>(pCoveringSet.size());
    AbstractState joinedState = null;
    for (Iterator<AbstractState> it = pCoveringSet.iterator(); it.hasNext(); ) {
      AbstractState state = it.next();

      // check single-state coverage
      if (domain.isLessOrEqual(pElement, state)) {
        return ImmutableSet.of(state);
      }

      // check intersection
      if (!domain.hasIntersection(pElement, state)) {
        continue;
      }

      // check enlargement
      if (joinedState != null && domain.isLessOrEqual(state, joinedState)) {
        continue;
      }
      joinedState = (joinedState == null) ? state : domain.join(state, joinedState);
      coveringSubset.add(state);

      // check joined-state coverage
      if (domain.isLessOrEqual(pElement, joinedState)) {
        break;
      }
    }
    return ImmutableSet.copyOf(coveringSubset);
  }
}
