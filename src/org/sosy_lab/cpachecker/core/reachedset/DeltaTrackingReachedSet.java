// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.reachedset;

import java.util.LinkedHashSet;
import java.util.SequencedSet;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.util.Pair;

/**
 * A {@link ForwardingReachedSet} that additionally tracks which states have been added since the
 * last call to {@link #clearDelta()}. Used to give consumers (e.g. adjustable-condition CPAs)
 * access to the delta of newly added states without requiring a full scan of the reached set.
 */
public class DeltaTrackingReachedSet extends ForwardingReachedSet {

  private final SequencedSet<AbstractState> addedSinceMark = new LinkedHashSet<>();

  public DeltaTrackingReachedSet(ReachedSet pDelegate) {
    super(pDelegate);
  }

  @Override
  public void add(AbstractState pState, Precision pPrecision) throws IllegalArgumentException {
    super.add(pState, pPrecision);
    addedSinceMark.add(pState);
  }

  @Override
  public void addNoWaitlist(AbstractState pState, Precision pPrecision)
      throws IllegalArgumentException {
    super.addNoWaitlist(pState, pPrecision);
    addedSinceMark.add(pState);
  }

  @Override
  public void addAll(Iterable<Pair<AbstractState, Precision>> pToAdd) {
    super.addAll(pToAdd);
    for (Pair<AbstractState, Precision> pair : pToAdd) {
      addedSinceMark.add(pair.getFirst());
    }
  }

  @Override
  public void clear() {
    addedSinceMark.clear();
    super.clear();
  }

  public SequencedSet<AbstractState> getDelta() {
    return addedSinceMark;
  }

  public void clearDelta() {
    addedSinceMark.clear();
  }
}
