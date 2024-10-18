/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.waitlist.Waitlist;
import org.sosy_lab.cpachecker.core.waitlist.Waitlist.WaitlistFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterators;

/**
 * This class implements a set of reached elements, including storing a
 * precision for each one. It groups elements by location (if enabled) and allows
 * access to either all elements or those with the same location as a given one.
 *
 * In all its operations it preserves the order in which the elements were added.
 * All the collections returned from methods of this class ensure this ordering, too.
 *
 * This class does not allow null values for elements and precisions.
 * All methods do not return null except when stated explicitly.
 */
public class ReachedSet implements UnmodifiableReachedSet {

  private final LinkedHashMap<AbstractElement, Precision> reached;
  private final Set<AbstractElement> unmodifiableReached;
  private AbstractElement lastElement = null;
  private AbstractElement firstElement = null;
  private final Waitlist waitlist;

  public ReachedSet(WaitlistFactory waitlistFactory) {
    reached = new LinkedHashMap<AbstractElement, Precision>();
    unmodifiableReached = Collections.unmodifiableSet(reached.keySet());
    waitlist = waitlistFactory.createWaitlistInstance();
  }

  /**
   * Add an element with a precision to the reached set and to the waitlist.
   * If the element is already in the reached set and the precisions are equal,
   * nothing is done.
   *
   * @param element An AbstractElement.
   * @param precision The Precision for the AbstractElement
   * @throws IllegalArgumentException If the element is already in the reached set, but with a different precision.
   */
  public void add(AbstractElement element, Precision precision) throws IllegalArgumentException {
    Preconditions.checkNotNull(element);
    Preconditions.checkNotNull(precision);

    if (reached.size() == 0) {
      firstElement = element;
    }

    Precision previousPrecision = reached.put(element, precision);

    if (previousPrecision == null) {
      // Element wasn't already in the reached set.
      waitlist.add(element);
      lastElement = element;

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


  public void addAll(Iterable<Pair<AbstractElement, Precision>> toAdd) {
    for (Pair<AbstractElement, Precision> pair : toAdd) {
      add(pair.getFirst(), pair.getSecond());
    }
  }

  /**
   * Re-add an element to the waitlist which is already contained in the reached set.
   */
  public void reAddToWaitlist(AbstractElement e) {
    Preconditions.checkNotNull(e);
    Preconditions.checkArgument(reached.containsKey(e), "Element has to be in the reached set");

    if (!waitlist.contains(e)) {
      waitlist.add(e);
    }
  }

  /**
   * Change the precision of an element that is already in the reached set.
   */
  public void updatePrecision(AbstractElement e, Precision newPrecision) {
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

  public void remove(AbstractElement element) {
    Preconditions.checkNotNull(element);
    int hc = element.hashCode();
    if ((firstElement == null) || hc == firstElement.hashCode() && element.equals(firstElement)) {
      firstElement = null;
    }

    if ((lastElement == null) || (hc == lastElement.hashCode() && element.equals(lastElement))) {
      lastElement = null;
    }
    waitlist.remove(element);
    reached.remove(element);
  }

  public void removeAll(Iterable<? extends AbstractElement> toRemove) {
    for (AbstractElement element : toRemove) {
      remove(element);
    }
    assert firstElement != null || reached.isEmpty() : "firstElement may only be removed if the whole reached set is cleared";
  }

  public void removeOnlyFromWaitlist(AbstractElement element) {
    checkNotNull(element);
    waitlist.remove(element);
  }

  public void clear() {
    firstElement = null;
    lastElement = null;
    waitlist.clear();
    reached.clear();
  }

  @Override
  public Set<AbstractElement> getReached() {
    return unmodifiableReached;
  }

  @Override
  public Iterator<AbstractElement> iterator() {
    return unmodifiableReached.iterator();
  }

  @Override
  public Collection<Pair<AbstractElement, Precision>> getReachedWithPrecision() {
    return Collections.unmodifiableCollection(
        Collections2.transform(reached.entrySet(),
                               Pair.<AbstractElement, Precision>getPairFomMapEntry()));
  }

  @Override
  public Collection<Precision> getPrecisions() {
    return Collections.unmodifiableCollection(reached.values());
  }

  /**
   * Returns a subset of the reached set, which contains at least all abstract
   * elements belonging to the same location as a given element. It may even
   * return an empty set if there are no such states. Note that it may return up to
   * all abstract states.
   *
   * The returned set is a view of the actual data, so it might change if nodes
   * are added to the reached set. Subsequent calls to this method with the same
   * parameter value will always return the same object.
   *
   * The returned set is unmodifiable.
   *
   * @param element An abstract element for whose location the abstract states should be retrieved.
   * @return A subset of the reached set.
   */
  @Override
  public Set<AbstractElement> getReached(AbstractElement element) {
    return getReached();
  }

  @Override
  public Set<AbstractElement> getReached(CFANode location) {
    return getReached();
  }

  /**
   * Returns the first element that was added to the reached set.
   * @throws IllegalStateException If the reached set is empty.
   */
  @Override
  public AbstractElement getFirstElement() {
    Preconditions.checkState(firstElement != null);
    return firstElement;
  }

  /**
   * Returns the last element that was added to the reached set.
   * May be null if it is unknown, which element was added last.
   */
  @Override
  public AbstractElement getLastElement() {
    return lastElement;
  }

  @Override
  public boolean hasWaitingElement() {
    return !waitlist.isEmpty();
  }

  @Override
  public Collection<AbstractElement> getWaitlist() {
    return new AbstractCollection<AbstractElement>() {

      @Override
      public Iterator<AbstractElement> iterator() {
        return Iterators.unmodifiableIterator(waitlist.iterator());
      }

      @Override
      public boolean contains(Object obj) {
        if (!(obj instanceof AbstractElement)) {
          return false;
        }
        return waitlist.contains((AbstractElement)obj);
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

  public AbstractElement popFromWaitlist() {
    return waitlist.pop();
  }

  @Override
  public int getWaitlistSize() {
    return waitlist.size();
  }

  /**
   * Returns the precision for an element.
   * @param element The element to look for. Has to be in the reached set.
   * @return The precision for the element.
   * @throws IllegalArgumentException If the element is not in the reached set.
   */
  @Override
  public Precision getPrecision(AbstractElement element) {
    Preconditions.checkNotNull(element);
    Precision prec = reached.get(element);
    if (prec == null){
      System.out.println("ERROR on "+element);
    }
    Preconditions.checkArgument(prec != null, "Element not in reached set.");
    return prec;
  }

  public boolean contains(AbstractElement element) {
    Preconditions.checkNotNull(element);
    return reached.containsKey(element);
  }

  @Override
  public int size() {
    return reached.size();
  }

  public boolean isEmpty() {
    return (size() == 0);
  }

  @Override
  public String toString() {
    return reached.keySet().toString();
  }
}
