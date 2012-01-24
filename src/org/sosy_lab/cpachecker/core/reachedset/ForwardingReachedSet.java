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

import java.util.Collection;
import java.util.Iterator;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.Precision;

/**
 * Implementation of ReachedSet that forwards all calls to another instance.
 * The target instance is changable.
 */
public class ForwardingReachedSet implements ReachedSet {

  private volatile ReachedSet delegate;

  public ForwardingReachedSet(ReachedSet pDelegate) {
    this.delegate = checkNotNull(pDelegate);
  }

  public void setDelegate(ReachedSet pDelegate) {
    delegate = checkNotNull(pDelegate);
  }

  @Override
  public Collection<AbstractElement> getReached() {
    return delegate.getReached();
  }

  @Override
  public Iterator<AbstractElement> iterator() {
    return delegate.iterator();
  }

  @Override
  public Collection<Pair<AbstractElement, Precision>> getReachedWithPrecision() {
    return delegate.getReachedWithPrecision();
  }

  @Override
  public Collection<Precision> getPrecisions() {
    return delegate.getPrecisions();
  }

  @Override
  public Collection<AbstractElement> getReached(AbstractElement pElement)
      throws UnsupportedOperationException {
    return delegate.getReached(pElement);
  }

  @Override
  public Collection<AbstractElement> getReached(CFANode pLocation) {
    return delegate.getReached(pLocation);
  }

  @Override
  public AbstractElement getFirstElement() {
    return delegate.getFirstElement();
  }

  @Override
  public AbstractElement getLastElement() {
    return delegate.getLastElement();
  }

  @Override
  public boolean hasWaitingElement() {
    return delegate.hasWaitingElement();
  }

  @Override
  public Collection<AbstractElement> getWaitlist() {
    return delegate.getWaitlist();
  }

  @Override
  public int getWaitlistSize() {
    return delegate.getWaitlistSize();
  }

  @Override
  public Precision getPrecision(AbstractElement pElement)
      throws UnsupportedOperationException {
    return delegate.getPrecision(pElement);
  }

  @Override
  public boolean contains(AbstractElement pElement) {
    return delegate.contains(pElement);
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
  public void add(AbstractElement pElement, Precision pPrecision)
      throws IllegalArgumentException {
    delegate.add(pElement, pPrecision);
  }

  @Override
  public void addAll(Iterable<Pair<AbstractElement, Precision>> pToAdd) {
    delegate.addAll(pToAdd);
  }

  @Override
  public void reAddToWaitlist(AbstractElement pE) {
    delegate.reAddToWaitlist(pE);
  }

  @Override
  public void updatePrecision(AbstractElement pE, Precision pNewPrecision) {
    delegate.updatePrecision(pE, pNewPrecision);
  }

  @Override
  public void remove(AbstractElement pElement) {
    delegate.remove(pElement);
  }

  @Override
  public void removeAll(Iterable<? extends AbstractElement> pToRemove) {
    delegate.removeAll(pToRemove);
  }

  @Override
  public void removeOnlyFromWaitlist(AbstractElement pElement) {
    delegate.removeOnlyFromWaitlist(pElement);
  }

  @Override
  public void clear() {
    delegate.clear();
  }

  @Override
  public AbstractElement popFromWaitlist() {
    return delegate.popFromWaitlist();
  }

  @Override
  public String toString() {
    return delegate.toString();
  }
}
