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
import java.util.HashMap;
import java.util.Iterator;
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
 * sub-classes (it's type is the type parameter of this class).
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

  private final StatCounter popCount;
  private final StatCounter delegationCount;
  private final Map<String, StatInt> delegationCounts = new HashMap<>();

  /**
   * Constructor that needs a factory for the waitlist implementation that should be used to store
   * states with the same sorting key.
   */
  protected AbstractSortedWaitlist(WaitlistFactory pSecondaryStrategy) {
    wrappedWaitlist = Preconditions.checkNotNull(pSecondaryStrategy);
    popCount = new StatCounter("Pop requests to waitlist (" + getClass().getSimpleName() + ")");
    delegationCount =
        new StatCounter(
            "Pops delegated to wrapped waitlists ("
                + wrappedWaitlist.getClass().getSimpleName()
                + ")");
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
    popCount.inc();

    Entry<K, Waitlist> highestEntry = waitlist.lastEntry();
    Waitlist localWaitlist = highestEntry.getValue();
    assert !localWaitlist.isEmpty();
    AbstractState result = localWaitlist.pop();
    if (localWaitlist.isEmpty()) {
      waitlist.remove(highestEntry.getKey());
      addStatistics(localWaitlist);
    } else {
      delegationCount.inc();
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
        if (!delegationCounts.containsKey(key)) {
          delegationCounts.put(key, e.getValue());

        } else {
          delegationCounts.get(key).add(e.getValue());
        }
      }
    }
  }

  /**
   * Returns a map of delegation counts for this waitlist and all waitlists delegated to. The keys
   * of the returned Map are the names of the waitlists, the values are the existing delegations.
   */
  public Map<String, StatInt> getDelegationCounts() {
    String waitlistName = this.getClass().getSimpleName();
    StatInt directDelegations = new StatInt(StatKind.AVG, waitlistName);
    assert delegationCount.getValue() <= Integer.MAX_VALUE;
    directDelegations.setNextValue((int) delegationCount.getValue());
    delegationCounts.put(waitlistName, directDelegations);
    return delegationCounts;
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
