// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.reachedset;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.SequencedSet;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.util.Pair;

/**
 * A {@link ForwardingReachedSet} that tracks, per registered observer, which states have been added
 * since that observer last called {@link #clearDelta(String)}. Consumers that only need the newly
 * added states can iterate the delta instead of scanning the whole reached set.
 *
 * <p>Observers are identified by a {@link String} id and are independent of each other: clearing
 * one observer's delta does not affect any other. Each observer additionally keeps up to {@code
 * maxHistorySize} of its past deltas, which are immutable snapshots.
 */
public class DeltaTrackingReachedSet extends ForwardingReachedSet {

  /** Constructor argument for keeping every past delta. */
  public static final int UNBOUNDED_HISTORY = -1;

  private final int maxHistorySize;
  private final Map<String, ObserverState> observers = new LinkedHashMap<>();

  private static final class ObserverState {
    private final SequencedSet<AbstractState> currentDelta = new LinkedHashSet<>();
    private final Deque<ImmutableSet<AbstractState>> history = new ArrayDeque<>();
  }

  public DeltaTrackingReachedSet(ReachedSet pDelegate) {
    this(pDelegate, UNBOUNDED_HISTORY);
  }

  /**
   * @param pMaxHistorySize number of past deltas each observer keeps, {@link #UNBOUNDED_HISTORY}
   *     for all of them, or {@code 0} for none.
   */
  public DeltaTrackingReachedSet(ReachedSet pDelegate, int pMaxHistorySize) {
    super(pDelegate);
    checkArgument(pMaxHistorySize >= UNBOUNDED_HISTORY, "Invalid history size %s", pMaxHistorySize);
    maxHistorySize = pMaxHistorySize;
  }

  /**
   * Registers a new observer under the given id.
   *
   * @throws IllegalArgumentException if an observer with this id is already registered.
   */
  public void registerObserver(String pId) {
    checkNotNull(pId);
    checkArgument(
        !observers.containsKey(pId), "An observer with the id %s is already registered", pId);
    observers.put(pId, new ObserverState());
  }

  public boolean hasObserver(String pId) {
    return observers.containsKey(checkNotNull(pId));
  }

  /** Returns the states added since this observer last cleared its delta. */
  public SequencedSet<AbstractState> getDelta(String pId) {
    return Collections.unmodifiableSequencedSet(observerFor(pId).currentDelta);
  }

  /**
   * Ends this observer's current delta and starts a new one. The ended delta is appended to the
   * observer's history. Does not affect any other observer.
   */
  public void clearDelta(String pId) {
    ObserverState observer = observerFor(pId);
    if (maxHistorySize != 0) {
      observer.history.addFirst(ImmutableSet.copyOf(observer.currentDelta));
      if (maxHistorySize != UNBOUNDED_HISTORY) {
        while (observer.history.size() > maxHistorySize) {
          observer.history.removeLast();
        }
      }
    }
    observer.currentDelta.clear();
  }

  /**
   * Returns this observer's past deltas, most recent first. Each entry records the states added
   * during that window; states removed from the reached set afterward are not retracted from it.
   */
  public ImmutableList<ImmutableSet<AbstractState>> getHistory(String pId) {
    return ImmutableList.copyOf(observerFor(pId).history);
  }

  private ObserverState observerFor(String pId) {
    ObserverState observer = observers.get(checkNotNull(pId));
    checkArgument(observer != null, "No observer is registered with the id %s", pId);
    return observer;
  }

  /**
   * Returns the states the given observer needs to consider: its delta if the reached set tracks
   * deltas, the full reached set otherwise.
   */
  public static Iterable<AbstractState> getConsideredStates(
      Iterable<AbstractState> pStates, String pObserverId) {
    checkNotNull(pStates);
    return pStates instanceof DeltaTrackingReachedSet dtrs ? dtrs.getDelta(pObserverId) : pStates;
  }

  private void addToDeltas(AbstractState pState) {
    for (ObserverState observer : observers.values()) {
      observer.currentDelta.add(pState);
    }
  }

  private void removeFromDeltas(AbstractState pState) {
    for (ObserverState observer : observers.values()) {
      observer.currentDelta.remove(pState);
    }
  }

  @Override
  public void add(AbstractState pState, Precision pPrecision) throws IllegalArgumentException {
    super.add(pState, pPrecision);
    addToDeltas(pState);
  }

  @Override
  public void addNoWaitlist(AbstractState pState, Precision pPrecision)
      throws IllegalArgumentException {
    super.addNoWaitlist(pState, pPrecision);
    addToDeltas(pState);
  }

  @Override
  public void addAll(Iterable<Pair<AbstractState, Precision>> pToAdd) {
    super.addAll(pToAdd);
    for (Pair<AbstractState, Precision> pair : pToAdd) {
      addToDeltas(pair.getFirst());
    }
  }

  /** Registrations survive, but all deltas and their history are discarded. */
  @Override
  public void clear() {
    for (ObserverState observer : observers.values()) {
      observer.currentDelta.clear();
      observer.history.clear();
    }
    super.clear();
  }

  @Override
  public void remove(AbstractState pState) {
    // Removed states must leave every delta, otherwise consumers would later work on
    // states that are already destroyed in the ARG.
    removeFromDeltas(pState);
    super.remove(pState);
  }

  @Override
  public void removeAll(Iterable<? extends AbstractState> pToRemove) {
    for (AbstractState s : pToRemove) {
      removeFromDeltas(s);
    }
    super.removeAll(pToRemove);
  }
}
