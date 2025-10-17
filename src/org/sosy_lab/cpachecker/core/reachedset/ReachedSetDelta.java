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
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;

/**
 * Immutable snapshot of the changes in a reached set. The class stores a record of the differences
 * between the current and the previous reached set. It is produced by the {@link
 * TrackingForwardingReachedSet} and consumed by the PredicateDelegatingRefiner and its
 * DelegatingRefinerHeuristics.
 */
public record ReachedSetDelta(
    ImmutableSet<AbstractState> addedStates, ImmutableSet<AbstractState> removedStates) {

  public ReachedSetDelta {
    checkNotNull(addedStates, "addedStates must not be null.");
    checkNotNull(removedStates, "removedStates must not be null.");
  }
}
