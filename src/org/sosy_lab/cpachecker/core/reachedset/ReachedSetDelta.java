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

/**
 * Central class to store the difference of ReachedSets between refinement iterations. Intended for
 * use with delegating refiner and its heuristics. Immutable snapshots can be created via {@link
 * #copy()}
 */
public final class ReachedSetDelta {
  private final Set<AbstractState> addedStates = new HashSet<>();
  private final Set<AbstractState> removedStates = new HashSet<>();

  void clear() {
    addedStates.clear();
    removedStates.clear();
  }

  void storeAddedStates(AbstractState pState) {
    addedStates.add(checkNotNull(pState));
  }

  void storeRemovedState(AbstractState pState) {
    removedStates.add(checkNotNull(pState));
  }

  public ImmutableSet<AbstractState> getAddedStates() {
    return ImmutableSet.copyOf(addedStates);
  }

  public ImmutableSet<AbstractState> getRemovedStates() {
    return ImmutableSet.copyOf(removedStates);
  }

  ReachedSetDelta copy() {
    ReachedSetDelta deltaCopy = new ReachedSetDelta();
    deltaCopy.addedStates.addAll(this.addedStates);
    deltaCopy.removedStates.addAll(this.removedStates);
    return deltaCopy;
  }
}
