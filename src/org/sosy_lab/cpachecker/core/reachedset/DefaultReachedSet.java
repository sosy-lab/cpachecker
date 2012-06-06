/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.reachedset;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Set;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.waitlist.Waitlist;
import org.sosy_lab.cpachecker.core.waitlist.Waitlist.WaitlistFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterators;

/**
 * Basic implementation of ReachedSet.
 * It does not group elements by location or any other key.
 */
class DefaultReachedSet implements ReachedSet {

  private final LinkedHashMap<AbstractState, Precision> reached;
  private final Set<AbstractState> unmodifiableReached;
  private AbstractState lastState = null;
  private AbstractState firstState = null;
  private final Waitlist waitlist;

  DefaultReachedSet(WaitlistFactory waitlistFactory) {
    reached = new LinkedHashMap<AbstractState, Precision>();
    unmodifiableReached = Collections.unmodifiableSet(reached.keySet());
    waitlist = waitlistFactory.createWaitlistInstance();
  }

  @Override
  public void add(AbstractState element, Precision precision) throws IllegalArgumentException {
    Preconditions.checkNotNull(element);
    Preconditions.checkNotNull(precision);

    if (reached.size() == 0) {
      firstState = element;
    }

    Precision previousPrecision = reached.put(element, precision);

    if (previousPrecision == null) {
      // Element wasn't already in the reached set.
      waitlist.add(element);
      lastState = element;

    } else {
      // Element was already in the reached set.
      // This happens only if the MergeOperator produces an element that is already there.

      // The element may or may not be currently in the waitlist.
      // In the first case, we are not allowed to add it to the waitlist,
      // otherwise it would be in there twice (this method is responsible for
      // enforcing the set semantics of the waitlist).
      // In the second case, we do not need
      // to add it to the waitlist, because it was already handled
      // (we assume that the CPA would always produce the same successors if we
      // give it the same element twice).

      // So do nothing here.

      // But check if the new and the old precisions are equal.
      if (!precision.equals(previousPrecision)) {

        // Restore previous state of reached set
        // (a method shouldn't change state if it throws an IAE).
        reached.put(element, previousPrecision);

        throw new IllegalArgumentException("Element added to reached set which is already contained, but with a different precision");
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
  public void reAddToWaitlist(AbstractState e) {
    Preconditions.checkNotNull(e);
    Preconditions.checkArgument(reached.containsKey(e), "Element has to be in the reached set");

    if (!waitlist.contains(e)) {
      waitlist.add(e);
    }
  }

  @Override
  public void updatePrecision(AbstractState e, Precision newPrecision) {
    Preconditions.checkNotNull(e);
    Preconditions.checkNotNull(newPrecision);

    Precision oldPrecision = reached.put(e, newPrecision);
    if (oldPrecision == null) {
      // Element was not contained in the reached set.
      // Restore previous state and throw exception.
      reached.remove(e);
      throw new IllegalArgumentException("Element needs to be in the reached set in order to change the precision.");
    }
  }

  @Override
  public void remove(AbstractState element) {
    Preconditions.checkNotNull(element);
    int hc = element.hashCode();
    if ((firstState == null) || hc == firstState.hashCode() && element.equals(firstState)) {
      firstState = null;
    }

    if ((lastState == null) || (hc == lastState.hashCode() && element.equals(lastState))) {
      lastState = null;
    }
    waitlist.remove(element);
    reached.remove(element);
  }

  @Override
  public void removeAll(Iterable<? extends AbstractState> toRemove) {
    for (AbstractState element : toRemove) {
      remove(element);
    }
    assert firstState != null || reached.isEmpty() : "firstState may only be removed if the whole reached set is cleared";
  }

  @Override
  public void removeOnlyFromWaitlist(AbstractState element) {
    checkNotNull(element);
    waitlist.remove(element);
  }

  @Override
  public void clear() {
    firstState = null;
    lastState = null;
    waitlist.clear();
    reached.clear();
  }

  @Override
  public Set<AbstractState> getReached() {
    return unmodifiableReached;
  }

  @Override
  public Iterator<AbstractState> iterator() {
    return unmodifiableReached.iterator();
  }

  @Override
  public Collection<Pair<AbstractState, Precision>> getReachedWithPrecision() {
    return Collections.unmodifiableCollection(
        Collections2.transform(reached.entrySet(),
                               Pair.<AbstractState, Precision>getPairFomMapEntry()));
  }

  @Override
  public Collection<Precision> getPrecisions() {
    return Collections.unmodifiableCollection(reached.values());
  }

  @Override
  public Collection<AbstractState> getReached(AbstractState element) {
    return getReached();
  }

  @Override
  public Collection<AbstractState> getReached(CFANode location) {
    return getReached();
  }

  @Override
  public AbstractState getFirstState() {
    Preconditions.checkState(firstState != null);
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
    return new AbstractCollection<AbstractState>() {

      @Override
      public Iterator<AbstractState> iterator() {
        return Iterators.unmodifiableIterator(waitlist.iterator());
      }

      @Override
      public boolean contains(Object obj) {
        if (!(obj instanceof AbstractState)) {
          return false;
        }
        return waitlist.contains((AbstractState)obj);
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
  public int getWaitlistSize() {
    return waitlist.size();
  }

  @Override
  public Precision getPrecision(AbstractState element) {
    Preconditions.checkNotNull(element);
    Precision prec = reached.get(element);
    Preconditions.checkArgument(prec != null, "Element not in reached set.");
    return prec;
  }

  @Override
  public boolean contains(AbstractState element) {
    Preconditions.checkNotNull(element);
    return reached.containsKey(element);
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
}
