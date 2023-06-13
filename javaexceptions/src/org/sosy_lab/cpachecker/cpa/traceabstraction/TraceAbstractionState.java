// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.traceabstraction;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;

class TraceAbstractionState extends AbstractSingleWrapperState implements Graphable {

  /** Create a new 'TOP'-state with empty predicates. */
  static TraceAbstractionState createInitState(AbstractState pAbstractState) {
    return new TraceAbstractionState(pAbstractState, ImmutableMap.of());
  }

  /**
   * This map links {@link InterpolationSequence}-objects to predicates that currently hold in this
   * state. Only entries with non-trivial predicates are contained (i.e., other than true and
   * false).
   */
  private final ImmutableMap<InterpolationSequence, IndexedAbstractionPredicate> activePredicates;

  TraceAbstractionState(
      AbstractState pWrappedState,
      Map<InterpolationSequence, IndexedAbstractionPredicate> pActivePredicates) {
    super(pWrappedState);
    activePredicates = ImmutableMap.copyOf(pActivePredicates);
  }

  boolean containsPredicates() {
    return !activePredicates.isEmpty();
  }

  ImmutableMap<InterpolationSequence, IndexedAbstractionPredicate> getActivePredicates() {
    return activePredicates;
  }

  TraceAbstractionState withWrappedState(AbstractState pWrappedState) {
    if (pWrappedState == getWrappedState()) {
      return this;
    }
    return new TraceAbstractionState(pWrappedState, getActivePredicates());
  }

  boolean isLessOrEqual(TraceAbstractionState pOther) {
    // TODO: For now the states are only checked for equality.
    // 'activePredicates' might need to be additionally checked for a lesser-relation.
    return equals(pOther);
  }

  @Override
  public int hashCode() {
    return Objects.hash(activePredicates);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof TraceAbstractionState)) {
      return false;
    }
    TraceAbstractionState other = (TraceAbstractionState) obj;
    return Objects.equals(activePredicates, other.activePredicates);
  }

  @Override
  public String toString() {
    if (!containsPredicates()) {
      return super.toString() + "\n_empty_preds_";
    }

    return createString();
  }

  @Override
  public String toDOTLabel() {
    return createString();
  }

  private String createString() {
    StringBuilder sb = new StringBuilder();

    AbstractState wrappedState = getWrappedState();
    if (wrappedState instanceof Graphable) {
      sb.append(((Graphable) wrappedState).toDOTLabel());
      sb.append("\n");
    }

    sb.append(
        activePredicates.values().stream()
            .map(indexedPred -> indexedPred.getPredicate().getSymbolicAtom().toString())
            .collect(Collectors.joining("\n")));
    return sb.toString();
  }

  @Override
  public boolean shouldBeHighlighted() {
    AbstractState wrappedState = getWrappedState();
    return (wrappedState instanceof Graphable)
        ? ((Graphable) wrappedState).shouldBeHighlighted()
        : false;
  }
}
