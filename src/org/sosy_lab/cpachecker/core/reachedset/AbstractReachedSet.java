/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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

import static com.google.common.collect.FluentIterable.from;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.WaitlistElement;
import org.sosy_lab.cpachecker.core.waitlist.AbstractSortedWaitlist;
import org.sosy_lab.cpachecker.core.waitlist.Waitlist;
import org.sosy_lab.cpachecker.core.waitlist.Waitlist.WaitlistFactory;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.statistics.AbstractStatValue;

public abstract class AbstractReachedSet implements ReachedSet {

  protected final MainNestedReachedSet reached;
  protected final Waitlist waitlist;

  AbstractReachedSet(WaitlistFactory waitlistFactory, MainNestedReachedSet pReached) {
    reached = pReached;
    waitlist = waitlistFactory.createWaitlistInstance();
  }

  @Override
  public void add(AbstractState state, Precision precision) throws IllegalArgumentException {
    Preconditions.checkNotNull(state);
    Preconditions.checkNotNull(precision);

    if (reached.add(state, precision)) {
      addToWaitlist(state, precision);
    }
  }

  @Override
  public void addToReachedSet(AbstractState state, Precision precision)
      throws IllegalArgumentException {
    Preconditions.checkNotNull(state);
    Preconditions.checkNotNull(precision);

    reached.add(state, precision);
  }

  protected abstract void addToWaitlist(AbstractState pState, Precision pPrecision);

  @Override
  public void addToWaitlist(AbstractState pState) {
    addToWaitlist(pState, getPrecision(pState));
  }

  @Override
  public void addAll(Iterable<Pair<AbstractState, Precision>> toAdd) {
    for (Pair<AbstractState, Precision> pair : toAdd) {
      add(pair.getFirst(), pair.getSecond());
    }
  }

  @Override
  public void addToWaitlist(WaitlistElement element) {
    waitlist.add(element);
  }

  @Override
  public void updatePrecision(AbstractState s, Precision newPrecision) {
    reached.updatePrecision(s, newPrecision);
  }

  @Override
  public void remove(AbstractState state) {
    Preconditions.checkNotNull(state);
    Precision prec = reached.getPrecision(state);
    if (prec == null) {
      //Covered states are not add to reached set, but they are in subgraph
      assert !reached.contains(state);
      return;
    }
    Preconditions.checkNotNull(prec);
    removeOnlyFromWaitlist(state, prec);
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
    removeOnlyFromWaitlist(state, getPrecision(state));
  }

  protected abstract void removeOnlyFromWaitlist(AbstractState state, Precision pPrecision);

  @Override
  public void clear() {
    waitlist.clear();
    reached.clear();
  }

  @Override
  public Set<AbstractState> asCollection() {
    return reached.asCollection();
  }

  @Override
  public Iterator<AbstractState> iterator() {
    return reached.iterator();
  }

  @Override
  public Collection<Precision> getPrecisions() {
    return reached.getPrecisions();
  }

  @Override
  public Collection<AbstractState> getReached(AbstractState state) {
    return reached.getReached(state);
  }

  @Override
  public Collection<AbstractState> getReached(CFANode location) {
    return reached.getReached(location);
  }

  @Override
  public AbstractState getFirstState() {
    return reached.getFirstState();
  }

  @Override
  public AbstractState getLastState() {
    return reached.getLastState();
  }

  @Override
  public boolean hasWaitingState() {
    return !waitlist.isEmpty();
  }

  @Override
  public Collection<AbstractState> getWaitlist() {
    return from(waitlist)
        .transformAndConcat(WaitlistElement::getAbstractStates)
        .toSet();
  }

  @Override
  public int getWaitlistSize() {
    return waitlist.size();
  }

  @Override
  public WaitlistElement popFromWaitlist() {
    return waitlist.pop();
  }

  @Override
  public Precision getPrecision(AbstractState state) {
    Preconditions.checkNotNull(state);
    Precision prec = reached.getPrecision(state);
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
    return reached.contains(state);
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
    return reached.toString();
  }

  public Map<String, ? extends AbstractStatValue> getStatistics() {
    if (waitlist instanceof AbstractSortedWaitlist) {
      return ((AbstractSortedWaitlist<?>) waitlist).getDelegationCounts();

    } else {
      return ImmutableMap.of();
    }
  }

  @Override
  public void printStatistics(PrintStream out) {
    reached.printStatistics(out);
    waitlist.printStatistics(out);
  }

  @Override
  public boolean hasStatesToAdd() {
    return false;
  }

  @Override
  public Collection<Pair<AbstractState, Precision>> getStatesToAdd() {
    return Collections.emptySet();
  }
}
