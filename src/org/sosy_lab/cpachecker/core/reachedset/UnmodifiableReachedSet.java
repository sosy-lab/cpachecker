// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.reachedset;

import static com.google.common.collect.FluentIterable.from;

import java.util.Collection;
import java.util.Iterator;
import java.util.function.BiConsumer;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Property;
import org.sosy_lab.cpachecker.core.interfaces.Targetable;
import org.sosy_lab.cpachecker.util.AbstractStates;

/**
 * Interface representing an unmodifiable reached set
 */
public interface UnmodifiableReachedSet extends Iterable<AbstractState> {

  Collection<AbstractState> asCollection();

  @Override
  Iterator<AbstractState> iterator();

  Collection<Precision> getPrecisions();

  /**
   * Returns a subset of the reached set, which contains at least all abstract
   * states belonging to the same location as a given state. It may even
   * return an empty set if there are no such states. Note that it may return up to
   * all abstract states.
   *
   * The returned set is a view of the actual data, so it might change if nodes
   * are added to the reached set. Subsequent calls to this method with the same
   * parameter value will always return the same object.
   *
   * The returned set is unmodifiable.
   *
   * @param state An abstract state for whose location the abstract states should be retrieved.
   * @return A subset of the reached set.
   */
  Collection<AbstractState> getReached(AbstractState state)
    throws UnsupportedOperationException;

  /**
   * Returns a subset of the reached set, which contains at least all abstract
   * states belonging to given location. It may even
   * return an empty set if there are no such states. Note that it may return up to
   * all abstract states.
   *
   * The returned set is a view of the actual data, so it might change if nodes
   * are added to the reached set. Subsequent calls to this method with the same
   * parameter value will always return the same object.
   *
   * The returned set is unmodifiable.
   *
   * @param location A location
   * @return A subset of the reached set.
   */
  Collection<AbstractState> getReached(CFANode location);

  /**
   * Returns the first state that was added to the reached set.
   * May be null if the state gets removed from the reached set.
   *
   * @throws IllegalStateException If the reached set is empty.
   */
  @Nullable AbstractState getFirstState();

  /**
   * Returns the last state that was added to the reached set.
   * May be null if it is unknown, which state was added last.
   */
  @Nullable AbstractState getLastState();

  boolean hasWaitingState();

  /**
   * An unmodifiable view of the waitlist as an Collection.
   */
  Collection<AbstractState> getWaitlist();

  /**
   * Returns the precision for a state.
   * @param state The state to look for. Has to be in the reached set.
   * @return The precision for the state.
   * @throws IllegalArgumentException If the state is not in the reached set.
   */
  Precision getPrecision(AbstractState state)
    throws UnsupportedOperationException;

  /**
   * Iterate over all (state, precision) pairs in the reached set and apply an action for them.
   */
  void forEach(BiConsumer<? super AbstractState, ? super Precision> action);

  boolean contains(AbstractState state);

  boolean isEmpty();

  int size();

  /**
   * Detect whether this reached set contains a property violation.
   *
   * <p>In some cases (like checking for race conditions) this is not the same as checking each
   * state individually for a property violation.
   *
   * @return Is any property violated
   */
  default boolean hasViolatedProperties() {
    return from(asCollection()).anyMatch(AbstractStates::isTargetState);
  }

  /**
   * Return the set of violated properties in this reached set, of {@link #hasViolatedProperties()}
   * returns true.
   *
   * @return A set of violated properties, may be emtpy if no precise information is available.
   */
  default Collection<Property> getViolatedProperties() {
    return from(asCollection())
        .filter(AbstractStates::isTargetState)
        .filter(Targetable.class)
        .transformAndConcat(Targetable::getViolatedProperties)
        .toSet();
  }
}