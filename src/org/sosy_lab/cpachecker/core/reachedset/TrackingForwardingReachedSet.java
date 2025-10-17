// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.reachedset;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableSet;
import java.util.HashSet;
import java.util.Set;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.util.Pair;

/**
 * Extension of {@link ForwardingReachedSet} that tracks changes in the ReachedSet (such as added or
 * removed states) and exposes them as {@link ReachedSetDelta}. Intended for use with
 * PredicateDelegatingRefiner and its DelegatingRefinerHeuristics.
 */
public class TrackingForwardingReachedSet extends ForwardingReachedSet {
  private final Set<AbstractState> addedStates = new HashSet<>();
  private final Set<AbstractState> removedStates = new HashSet<>();

  public TrackingForwardingReachedSet(ReachedSet pDelegate) {
    super(pDelegate);
  }

  /** Clears all records of predicates added and removed in prior refinement iterations. */
  public void resetTracking() {
    addedStates.clear();
    removedStates.clear();
  }

  /**
   * Returns a snapshot of the predicates added and removed since the last refinement iteration.
   * Used by PredicateDelegating in its DelegatingRefinerHeuristics to evaluate refinement progress.
   *
   * @return a {@link ReachedSetDelta} containing added and removed states.
   */
  public ReachedSetDelta getDelta() {
    return new ReachedSetDelta(
        ImmutableSet.copyOf(addedStates), ImmutableSet.copyOf(removedStates));
  }

  @Override
  public void add(AbstractState pState, Precision pPrecision) {
    addedStates.add(checkNotNull(pState));
    super.add(pState, pPrecision);
  }

  @Override
  public void addAll(Iterable<Pair<AbstractState, Precision>> pToAdd) {
    for (Pair<AbstractState, Precision> pair : pToAdd) {
      AbstractState pState = pair.getFirst();
      if (pState != null) {
        addedStates.add(pState);
      }
    }
    super.addAll(pToAdd);
  }

  @Override
  public void remove(AbstractState pState) {
    removedStates.add(checkNotNull(pState));
    super.remove(pState);
  }

  @Override
  public void removeAll(Iterable<? extends AbstractState> pToRemove) {
    for (AbstractState pState : pToRemove) {
      removedStates.add(pState);
    }
    super.removeAll(pToRemove);
  }

  @Override
  public void clear() {
    resetTracking();
    super.clear();
  }
}
