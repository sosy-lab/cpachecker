// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.reachedset;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.LinkedHashSet;
import java.util.SequencedSet;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.util.Pair;

/**
 * A {@link ForwardingReachedSet} that tracks which states have been added since the last call to
 * {@link #clearDelta()}. Consumers that only need the newly added states can use the delta instead
 * of scanning the whole reached set. States that leave the delta are no longer checked.
 */
public class DeltaTrackingReachedSet extends ForwardingReachedSet {

  private final SequencedSet<AbstractState> delta = new LinkedHashSet<>();

  public DeltaTrackingReachedSet(ReachedSet pDelegate) {
    super(pDelegate);
  }

  /** Returns the states added since the last call to {@link #clearDelta()}. Do not modify. */
  public SequencedSet<AbstractState> getDelta() {
    return delta;
  }

  /** Discards the delta. Called once per unrolling, after all consumers have read it. */
  public void clearDelta() {
    delta.clear();
  }

  /**
   * Returns the states a consumer needs to consider: the delta if the reached set tracks one, the
   * full reached set otherwise.
   */
  public static Iterable<AbstractState> getConsideredStates(Iterable<AbstractState> pStates) {
    checkNotNull(pStates);
    return pStates instanceof DeltaTrackingReachedSet dtrs ? dtrs.getDelta() : pStates;
  }

  @Override
  public void add(AbstractState pState, Precision pPrecision) throws IllegalArgumentException {
    super.add(pState, pPrecision);
    delta.add(pState);
  }

  @Override
  public void addNoWaitlist(AbstractState pState, Precision pPrecision)
      throws IllegalArgumentException {
    super.addNoWaitlist(pState, pPrecision);
    delta.add(pState);
  }

  @Override
  public void addAll(Iterable<Pair<AbstractState, Precision>> pToAdd) {
    super.addAll(pToAdd);
    for (Pair<AbstractState, Precision> pair : pToAdd) {
      delta.add(pair.getFirst());
    }
  }

  @Override
  public void clear() {
    delta.clear();
    super.clear();
  }

  @Override
  public void remove(AbstractState pState) {
    // Removed states must leave the delta, otherwise consumers would later work on
    // states that are already destroyed in the ARG.
    delta.remove(pState);
    super.remove(pState);
  }

  @Override
  public void removeAll(Iterable<? extends AbstractState> pToRemove) {
    for (AbstractState s : pToRemove) {
      delta.remove(s);
    }
    super.removeAll(pToRemove);
  }
}
