// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.reachedset;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.util.Pair;

/**
 * Extension of {@link ForwardingReachedSet} that tracks changes in the ReachedSet (such as added or
 * removed states). Intended for use with delegating refiner and its heuristics.
 */
public class TrackingForwardingReachedSet extends ForwardingReachedSet {

  private final ReachedSetDelta delta = new ReachedSetDelta();

  public TrackingForwardingReachedSet(ReachedSet pDelegate) {
    super(pDelegate);
  }

  public void resetTracking() {
    delta.clear();
  }

  public ReachedSetDelta getDelta() {
    return delta.copy();
  }

  @Override
  public void add(AbstractState pState, Precision pPrecision) {
    delta.storeAddedStates(pState);
    super.add(pState, pPrecision);
  }

  @Override
  public void addAll(Iterable<Pair<AbstractState, Precision>> pToAdd) {
    for (Pair<AbstractState, Precision> pair : pToAdd) {
      AbstractState pState = pair.getFirst();
      if (pState != null) {
        delta.storeAddedStates(pState);
      }
    }
    super.addAll(pToAdd);
  }

  @Override
  public void remove(AbstractState pState) {
    delta.storeRemovedState(pState);
    super.remove(pState);
  }

  @Override
  public void removeAll(Iterable<? extends AbstractState> pToRemove) {
    for (AbstractState pState : pToRemove) {
      delta.storeRemovedState(pState);
    }
    super.removeAll(pToRemove);
  }

  @Override
  public void clear() {
    resetTracking();
    super.clear();
  }
}
