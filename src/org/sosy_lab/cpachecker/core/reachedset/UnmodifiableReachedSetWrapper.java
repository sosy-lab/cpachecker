// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.reachedset;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.Iterators;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.function.BiConsumer;
import java.util.stream.Stream;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Targetable.TargetInformation;

public class UnmodifiableReachedSetWrapper implements UnmodifiableReachedSet {

  private final UnmodifiableReachedSet delegate;

  public UnmodifiableReachedSetWrapper(UnmodifiableReachedSet pDelegate) {
    delegate = checkNotNull(pDelegate);
  }

  @Override
  public Collection<AbstractState> asCollection() {
    return Collections.unmodifiableCollection(delegate.asCollection());
  }

  @Override
  public Iterator<AbstractState> iterator() {
    return Iterators.unmodifiableIterator(delegate.iterator());
  }

  @Override
  public Stream<AbstractState> stream() {
    return delegate.stream();
  }

  @Override
  public Collection<Precision> getPrecisions() {
    return Collections.unmodifiableCollection(delegate.getPrecisions());
  }

  @Override
  public Collection<AbstractState> getReached(AbstractState pState)
      throws UnsupportedOperationException {
    return Collections.unmodifiableCollection(delegate.getReached(pState));
  }

  @Override
  public Collection<AbstractState> getReached(CFANode pLocation) {
    return Collections.unmodifiableCollection(delegate.getReached(pLocation));
  }

  @Override
  public @Nullable AbstractState getFirstState() {
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
    return Collections.unmodifiableCollection(delegate.getWaitlist());
  }

  @Override
  public Precision getPrecision(AbstractState pState) throws UnsupportedOperationException {
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
  public String toString() {
    return delegate.toString();
  }

  @Override
  public boolean wasTargetReached() {
    return delegate.wasTargetReached();
  }

  @Override
  public Collection<TargetInformation> getTargetInformation() {
    return delegate.getTargetInformation();
  }
}
