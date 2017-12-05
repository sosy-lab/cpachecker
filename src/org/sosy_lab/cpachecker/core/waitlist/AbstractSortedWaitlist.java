/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
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
 * Default implementation of a sorted waitlist.
 * The key that is used for sorting is defined by sub-classes (it's type is
 * the type parameter of this class).
 *
 * There may be several abstract states with the same key, so this class
 * delegates the decision which of those should be chosen to a second waitlist
 * implementation. A factory for this implementation needs to be given to the
 * constructor.
 *
 * The iterators created by this class are unmodifiable.
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
   * Constructor that needs a factory for the waitlist implementation that
   * should be used to store states with the same sorting key.
   */
  protected AbstractSortedWaitlist(WaitlistFactory pSecondaryStrategy) {
    wrappedWaitlist = Preconditions.checkNotNull(pSecondaryStrategy);
    popCount = new StatCounter("Pop requests to waitlist (" + getClass().getSimpleName() + ")");
    delegationCount = new StatCounter(
        "Pops delegated to wrapped waitlists (" + wrappedWaitlist.getClass().getSimpleName() +
            ")");
  }

  /**
   * Method that generates the sorting key for any abstract state.
   * States with largest key are considered first.
   * This method may not return null.
   * If this method throws an exception, no guarantees about the state of the
   * current instance of this class are made.
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
    Entry<K, Waitlist> highestEntry = null;
    highestEntry = waitlist.lastEntry();
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
   * Returns a map of delegation counts for this waitlist and all waitlists delegated to.
   * The keys of the returned Map are the names of the waitlists, the values
   * are the existing delegations.
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
