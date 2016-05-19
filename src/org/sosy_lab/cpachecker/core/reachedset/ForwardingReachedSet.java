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
package org.sosy_lab.cpachecker.core.reachedset;

import static com.google.common.base.Preconditions.checkNotNull;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.util.Pair;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * Implementation of ReachedSet that forwards all calls to another instance.
 * The target instance is changable.
 */
public class ForwardingReachedSet implements ReachedSet {

  private volatile ReachedSet delegate;

  public ForwardingReachedSet(ReachedSet pDelegate) {
    this.delegate = checkNotNull(pDelegate);
  }

  public ReachedSet getDelegate() {
    return delegate;
  }

  public void setDelegate(ReachedSet pDelegate) {
    delegate = checkNotNull(pDelegate);
  }

  @Override
  public Set<AbstractState> asCollection() {
    return delegate.asCollection();
  }

  @Override
  public Iterator<AbstractState> iterator() {
    return delegate.iterator();
  }

  @Override
  public Collection<Precision> getPrecisions() {
    return delegate.getPrecisions();
  }

  @Override
  public Collection<AbstractState> getReached(AbstractState pState)
      throws UnsupportedOperationException {
    return delegate.getReached(pState);
  }

  @Override
  public Collection<AbstractState> getReached(CFANode pLocation) {
    return delegate.getReached(pLocation);
  }

  @Override
  public AbstractState getFirstState() {
    return delegate.getFirstState();
  }

  @Override
  public AbstractState getLastState() {
    return delegate.getLastState();
  }

  @Override
  public boolean hasWaitingState() {
    return delegate.hasWaitingState();
  }

  @Override
  public Collection<AbstractState> getWaitlist() {
    return delegate.getWaitlist();
  }

  @Override
  public Precision getPrecision(AbstractState pState)
      throws UnsupportedOperationException {
    return delegate.getPrecision(pState);
  }

  @Override
  public void forEach(BiConsumer<? super AbstractState, ? super Precision> pAction) {
    delegate.forEach(pAction);
  }

  @Override
  public boolean contains(AbstractState pState) {
    return delegate.contains(pState);
  }

  @Override
  public boolean isEmpty() {
    return delegate.isEmpty();
  }

  @Override
  public int size() {
    return delegate.size();
  }

  @Override
  public void add(AbstractState pState, Precision pPrecision)
      throws IllegalArgumentException {
    delegate.add(pState, pPrecision);
  }

  @Override
  public void addAll(Iterable<Pair<AbstractState, Precision>> pToAdd) {
    delegate.addAll(pToAdd);
  }

  @Override
  public void reAddToWaitlist(AbstractState pE) {
    delegate.reAddToWaitlist(pE);
  }

  @Override
  public void updatePrecision(AbstractState pE, Precision pNewPrecision) {
    delegate.updatePrecision(pE, pNewPrecision);
  }

  @Override
  public void remove(AbstractState pState) {
    delegate.remove(pState);
  }

  @Override
  public void removeAll(Iterable<? extends AbstractState> pToRemove) {
    delegate.removeAll(pToRemove);
  }

  @Override
  public void removeOnlyFromWaitlist(AbstractState pState) {
    delegate.removeOnlyFromWaitlist(pState);
  }

  @Override
  public void clear() {
    delegate.clear();
  }

  @Override
  public AbstractState popFromWaitlist() {
    return delegate.popFromWaitlist();
  }

  @Override
  public String toString() {
    return delegate.toString();
  }
}
