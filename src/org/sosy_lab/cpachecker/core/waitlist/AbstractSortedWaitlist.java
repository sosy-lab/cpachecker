// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.waitlist;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.errorprone.annotations.ForOverride;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;
import org.sosy_lab.cpachecker.util.statistics.StatInt;
import org.sosy_lab.cpachecker.util.statistics.StatKind;

/**
 * Default implementation of a sorted waitlist. The key that is used for sorting is defined by
 * subclasses (it's type is the type parameter of this class).
 *
 * <p>There may be several abstract states with the same key, so this class delegates the decision
 * which of those should be chosen to a second waitlist implementation. A factory for this
 * implementation needs to be given to the constructor.
 *
 * <p>The iterators created by this class are unmodifiable.
 */
public abstract class AbstractSortedWaitlist<K extends Comparable<K>> implements Waitlist {

  private final WaitlistFactory wrappedWaitlist;

  // invariant: all entries in this map are non-empty
  private final NavigableMap<K, Waitlist> waitlist = new TreeMap<>();

  private int size = 0;

  private StatCounter popCount = null;
  private StatCounter delegationCount = null;
  private Map<String, StatInt> delegationCounts = null;

  /**
   * Constructor that needs a factory for the waitlist implementation that should be used to store
   * states with the same sorting key.
   */
  protected AbstractSortedWaitlist(WaitlistFactory pSecondaryStrategy) {
    wrappedWaitlist = Preconditions.checkNotNull(pSecondaryStrategy);
  }

  /**
   * Method that generates the sorting key for any abstract state. States with largest key are
   * considered first. This method may not return null. If this method throws an exception, no
   * guarantees about the state of the current instance of this class are made.
   */
  @ForOverride
  protected abstract K getSortKey(AbstractState pState);

  @Override
  public void add(AbstractState pState) {
    K key = getSortKey(pState);
    Waitlist localWaitlist = waitlist.get(key);
    if (localWaitlist == null) {
      localWaitlist = wrappedWaitlist.createWaitlistInstance();
      waitlist.put(key, localWaitlist);
    } else {
      assert !localWaitlist.isEmpty();
    }
    localWaitlist.add(pState);
    size++;
  }

  @Override
  public boolean contains(AbstractState pState) {
    K key = getSortKey(pState);
    Waitlist localWaitlist = waitlist.get(key);
    if (localWaitlist == null) {
      return false;
    }
    assert !localWaitlist.isEmpty();
    return localWaitlist.contains(pState);
  }

  @Override
  public void clear() {
    waitlist.clear();
    size = 0;
  }

  @Override
  public boolean isEmpty() {
    assert waitlist.isEmpty() == (size == 0);
    return waitlist.isEmpty();
  }

  @Override
  public Iterator<AbstractState> iterator() {
    return Iterables.concat(waitlist.values()).iterator();
  }

  @Override
  public final AbstractState pop() {
    getPopCount().inc();

    Entry<K, Waitlist> highestEntry = waitlist.lastEntry();
    Waitlist localWaitlist = highestEntry.getValue();
    assert !localWaitlist.isEmpty();
    AbstractState result = localWaitlist.pop();
    if (localWaitlist.isEmpty()) {
      waitlist.remove(highestEntry.getKey());
      addStatistics(localWaitlist);
    } else {
      getDelegationCount().inc();
    }
    size--;
    return result;
  }

  private void addStatistics(Waitlist pWaitlist) {
    if (pWaitlist instanceof AbstractSortedWaitlist) {
      Map<String, StatInt> delegCount =
          ((AbstractSortedWaitlist<?>) pWaitlist).getDelegationCounts();

      for (Entry<String, StatInt> e : delegCount.entrySet()) {
        String key = e.getKey();
        if (!getOrCreateDelegationCounts().containsKey(key)) {
          getOrCreateDelegationCounts().put(key, e.getValue());

        } else {
          getOrCreateDelegationCounts().get(key).add(e.getValue());
        }
      }
    }
  }

  private StatCounter getPopCount() {
    if (popCount == null) {
      popCount = new StatCounter("Pop requests handled (" + getClass().getSimpleName() + ")");
    }
    return popCount;
  }

  private StatCounter getDelegationCount() {
    if (delegationCount == null) {
      delegationCount =
          new StatCounter(
              "Pops with remaining states in nested waitlist (" + getClass().getSimpleName() + ")");
    }
    return delegationCount;
  }

  private Map<String, StatInt> getOrCreateDelegationCounts() {
    if (delegationCounts == null) {
      delegationCounts = new LinkedHashMap<>();
    }
    return delegationCounts;
  }

  /**
   * Returns a map of delegation counts for this waitlist and all waitlists delegated to. The keys
   * of the returned Map are the names of the waitlists, the values are the existing delegations.
   */
  public Map<String, StatInt> getDelegationCounts() {
    String waitlistName = getClass().getSimpleName();

    StatInt pops = new StatInt(StatKind.SUM, "Pop requests handled (" + waitlistName + ")");
    assert popCount == null || popCount.getValue() <= Integer.MAX_VALUE;
    pops.setNextValue(popCount != null ? (int) popCount.getValue() : 0);

    StatInt delegations =
        new StatInt(
            StatKind.SUM, "Pops with remaining states in nested waitlist (" + waitlistName + ")");
    assert delegationCount == null || delegationCount.getValue() <= Integer.MAX_VALUE;
    delegations.setNextValue(delegationCount != null ? (int) delegationCount.getValue() : 0);

    Map<String, StatInt> result = new LinkedHashMap<>(getOrCreateDelegationCounts());
    result.put(pops.getTitle(), pops);
    result.put(delegations.getTitle(), delegations);
    return result;
  }

  @Override
  public boolean remove(AbstractState pState) {
    K key = getSortKey(pState);
    Waitlist localWaitlist = waitlist.get(key);
    if (localWaitlist == null) {
      return false;
    }
    assert !localWaitlist.isEmpty();
    boolean result = localWaitlist.remove(pState);
    if (result) {
      if (localWaitlist.isEmpty()) {
        waitlist.remove(key);
      }
      size--;
    }
    return result;
  }

  @Override
  public int size() {
    return size;
  }

  @Override
  public String toString() {
    return waitlist.toString();
  }
}
