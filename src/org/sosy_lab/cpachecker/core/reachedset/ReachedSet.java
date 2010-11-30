/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Set;

import com.google.common.collect.Iterables;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.waitlist.Waitlist;
import org.sosy_lab.cpachecker.core.waitlist.Waitlist.WaitlistFactory;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Collections2;

/**
 * This class implements a set of reached elements, including storing a
 * precision for each one. It groups elements by location (if enabled) and allows
 * access to either all elements or those with the same location as a given one.
 *
 * In all its operations it preserves the order in which the elements were added.
 * All the collections returned from methods of this class ensure this ordering, too.
 */
public class ReachedSet implements UnmodifiableReachedSet {

  private final LinkedHashMap<AbstractElement, Precision> reached;
  private final Set<AbstractElement> unmodifiableReached;
  private final Collection<Pair<AbstractElement, Precision>> reachedWithPrecision;
  private AbstractElement lastElement = null;
  private AbstractElement firstElement = null;
  private final Waitlist waitlist;

  private final Function<AbstractElement, Pair<AbstractElement, Precision>> getPrecisionAsPair =
    new Function<AbstractElement, Pair<AbstractElement, Precision>>() {

      @Override
      public Pair<AbstractElement, Precision> apply(
                  AbstractElement element) {

        return Pair.of(element, getPrecision(element));
      }

  };

  public ReachedSet(WaitlistFactory waitlistFactory) {
    reached = new LinkedHashMap<AbstractElement, Precision>();
    unmodifiableReached = Collections.unmodifiableSet(reached.keySet());
    reachedWithPrecision = Collections2.transform(unmodifiableReached, getPrecisionAsPair);
    waitlist = waitlistFactory.createWaitlistInstance();
  }

  public void add(AbstractElement element, Precision precision) {
    if (reached.size() == 0) {
      firstElement = element;
    }

    reached.put(element, precision);
    waitlist.add(element);
    lastElement = element;
  }


  public void addAll(Collection<Pair<AbstractElement, Precision>> toAdd) {
    for (Pair<AbstractElement, Precision> pair : toAdd) {
      add(pair.getFirst(), pair.getSecond());
    }
  }

  /**
   * Re-add an element to the waitlist which is already contained in the reached set.
   */
  public void reAddToWaitlist(AbstractElement e) {
    Preconditions.checkArgument(reached.containsKey(e), "Element has to be in the reached set");

    if (!waitlist.contains(e)) {
      waitlist.add(e);
    }
  }

  public void remove(AbstractElement element) {
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

  public void removeAll(Collection<? extends AbstractElement> toRemove) {
    for (AbstractElement element : toRemove) {
      remove(element);
    }
    assert firstElement != null || reached.isEmpty() : "firstElement may only be removed if the whole reached set is cleared";
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
    return reachedWithPrecision; // this is unmodifiable
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

  @Override
  public AbstractElement getFirstElement() {
    return firstElement;
  }

  @Override
  public AbstractElement getLastElement() {
    return lastElement;
  }

  @Override
  public boolean hasWaitingElement() {
    return !waitlist.isEmpty();
  }

  @Override
  public Iterable<AbstractElement> getWaitlist() {
    return Iterables.unmodifiableIterable(waitlist);
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
   * @param element The element to look for.
   * @return The precision for the element or null.
   */
  @Override
  public Precision getPrecision(AbstractElement element) {
    return reached.get(element);
  }

  public boolean contains(AbstractElement element) {
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
