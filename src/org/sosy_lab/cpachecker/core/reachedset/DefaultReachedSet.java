// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.reachedset;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterators;
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Stream;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.waitlist.AbstractSortedWaitlist;
import org.sosy_lab.cpachecker.core.waitlist.Waitlist;
import org.sosy_lab.cpachecker.core.waitlist.Waitlist.WaitlistFactory;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.statistics.AbstractStatValue;

/** Basic implementation of ReachedSet. It does not group states by location or any other key. */
class DefaultReachedSet implements ReachedSet {

  private final ConfigurableProgramAnalysis cpa;
  private final Map<AbstractState, Precision> reached;
  private final Set<AbstractState> unmodifiableReached;
  private @Nullable AbstractState lastState = null;
  private @Nullable AbstractState firstState = null;
  private final Waitlist waitlist;

  DefaultReachedSet(ConfigurableProgramAnalysis pCpa, WaitlistFactory waitlistFactory) {
    cpa = checkNotNull(pCpa);
    reached = new LinkedHashMap<>();
    unmodifiableReached = Collections.unmodifiableSet(reached.keySet());
    waitlist = waitlistFactory.createWaitlistInstance();
  }

  @Override
  public void add(AbstractState state, Precision precision) {
    add(state, precision, /*updateWaitlist=*/ true);
  }

  @Override
  public void addNoWaitlist(AbstractState state, Precision precision) {
    add(state, precision, /*updateWaitlist=*/ false);
  }

  private void add(AbstractState state, Precision precision, boolean updateWaitlist) {
    Preconditions.checkNotNull(state);
    Preconditions.checkNotNull(precision);

    if (reached.isEmpty()) {
      firstState = state;
    }

    Precision previousPrecision = reached.put(state, precision);

    if (previousPrecision == null) {
      // State wasn't already in the reached set.
      if (updateWaitlist) {
        waitlist.add(state);
      }
      lastState = state;

    } else {
      // State was already in the reached set.
      // This happens only if the MergeOperator produces a state that is already there.

      // The state may or may not be currently in the waitlist.
      // In the first case, we are not allowed to add it to the waitlist,
      // otherwise it would be in there twice (this method is responsible for
      // enforcing the set semantics of the waitlist).
      // In the second case, we do not need
      // to add it to the waitlist, because it was already handled
      // (we assume that the CPA would always produce the same successors if we
      // give it the same state twice).

      // So do nothing here.

      // But check if the new and the old precisions are equal.
      if (!precision.equals(previousPrecision)) {

        // Restore previous state of reached set
        // (a method shouldn't change state if it throws an IAE).
        reached.put(state, previousPrecision);

        throw new IllegalArgumentException(
            "State added to reached set which is already contained, but with a different"
                + " precision");
      }
    }
  }

  @Override
  public void addAll(Iterable<Pair<AbstractState, Precision>> toAdd) {
    for (Pair<AbstractState, Precision> pair : toAdd) {
      add(pair.getFirst(), pair.getSecond());
    }
  }

  @Override
  public void reAddToWaitlist(AbstractState s) {
    Preconditions.checkNotNull(s);
    Preconditions.checkArgument(reached.containsKey(s), "State has to be in the reached set");

    if (!waitlist.contains(s)) {
      waitlist.add(s);
    }
  }

  @Override
  public void updatePrecision(AbstractState s, Precision newPrecision) {
    Preconditions.checkNotNull(s);
    Preconditions.checkNotNull(newPrecision);

    Precision oldPrecision = reached.put(s, newPrecision);
    if (oldPrecision == null) {
      // State was not contained in the reached set.
      // Restore previous state and throw exception.
      reached.remove(s);
      throw new IllegalArgumentException(
          "State needs to be in the reached set in order to change the precision.");
    }
  }

  @Override
  public void remove(AbstractState state) {
    Preconditions.checkNotNull(state);
    int hc = state.hashCode();
    if (firstState != null && hc == firstState.hashCode() && state.equals(firstState)) {
      firstState = null;
    }

    if (lastState != null && hc == lastState.hashCode() && state.equals(lastState)) {
      lastState = null;
    }
    waitlist.remove(state);
    reached.remove(state);
  }

  @Override
  public void removeAll(Iterable<? extends AbstractState> toRemove) {
    for (AbstractState state : toRemove) {
      remove(state);
    }
  }

  @Override
  public void removeOnlyFromWaitlist(AbstractState state) {
    checkNotNull(state);
    waitlist.remove(state);
  }

  @Override
  public void clear() {
    firstState = null;
    lastState = null;
    waitlist.clear();
    reached.clear();
  }

  @Override
  public void clearWaitlist() {
    waitlist.clear();
  }

  @Override
  public Set<AbstractState> asCollection() {
    return unmodifiableReached;
  }

  @Override
  public Iterator<AbstractState> iterator() {
    return unmodifiableReached.iterator();
  }

  @Override
  public Stream<AbstractState> stream() {
    return reached.keySet().stream();
  }

  @Override
  public Collection<Precision> getPrecisions() {
    return Collections.unmodifiableCollection(reached.values());
  }

  @Override
  public Collection<AbstractState> getReached(AbstractState state) {
    checkNotNull(state);
    return asCollection();
  }

  @Override
  public Collection<AbstractState> getReached(CFANode location) {
    checkNotNull(location);
    return asCollection();
  }

  @Override
  public @Nullable AbstractState getFirstState() {
    return firstState;
  }

  @Override
  public AbstractState getLastState() {
    return lastState;
  }

  @Override
  public boolean hasWaitingState() {
    return !waitlist.isEmpty();
  }

  @Override
  public Collection<AbstractState> getWaitlist() {
    return new AbstractCollection<>() {

      @Override
      public Iterator<AbstractState> iterator() {
        return Iterators.unmodifiableIterator(waitlist.iterator());
      }

      @Override
      public boolean contains(Object obj) {
        if (!(obj instanceof AbstractState)) {
          return false;
        }
        return waitlist.contains((AbstractState) obj);
      }

      @Override
      public boolean isEmpty() {
        return waitlist.isEmpty();
      }

      @Override
      public int size() {
        return waitlist.size();
      }

      @Override
      public String toString() {
        return waitlist.toString();
      }
    };
  }

  @Override
  public AbstractState popFromWaitlist() {
    return waitlist.pop();
  }

  @Override
  public Precision getPrecision(AbstractState state) {
    Preconditions.checkNotNull(state);
    Precision prec = reached.get(state);
    Preconditions.checkArgument(prec != null, "State not in reached set:\n%s", state);
    return prec;
  }

  @Override
  public void forEach(BiConsumer<? super AbstractState, ? super Precision> pAction) {
    reached.forEach(pAction);
  }

  @Override
  public boolean contains(AbstractState state) {
    Preconditions.checkNotNull(state);
    return reached.containsKey(state);
  }

  @Override
  public int size() {
    return reached.size();
  }

  @Override
  public boolean isEmpty() {
    return (size() == 0);
  }

  @Override
  public String toString() {
    return reached.keySet().toString();
  }

  @Override
  public ImmutableMap<String, AbstractStatValue> getStatistics() {
    if (waitlist instanceof AbstractSortedWaitlist) {
      return ImmutableMap.copyOf(((AbstractSortedWaitlist<?>) waitlist).getDelegationCounts());

    } else {
      return ImmutableMap.of();
    }
  }

  @Override
  public ConfigurableProgramAnalysis getCPA() {
    return cpa;
  }
}
